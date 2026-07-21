package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.ChunkIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

/**
 * Chunk-shaped islands cut from the seed's own natural chunks (GOALS 09/37, DESIGN §29): unlike
 * every other island-shaped preset, a selected chunk's real vanilla terrain is never replaced --
 * only unselected chunks mask to void. A cell either holds a present island or doesn't; there is
 * no jitter, radius, or coastline amplitude to resolve (the shape <em>is</em> the chunk), so this
 * is simpler than {@link FloatingIslandsPlan}'s jittered grid.
 *
 * <p>Reused unchanged across all three dimensions (Overworld/Nether/End, DESIGN §29.5): nothing
 * about the grid or exclusion zone is dimension-specific, only whether it applies at all
 * ({@code applyToNether}/{@code applyToEnd} resolve at the config-to-plan boundary, mirroring
 * {@link StripPlan#fromConfig}'s precedent, not fields on this record).
 *
 * @param enabled whether chunk islands generate at all
 * @param spawnChance probability ({@code 0..1}) that a given cell holds an island
 * @param cellSizeChunks grid-cell edge length in chunks -- {@code 1} rolls every chunk
 *     independently; a larger value groups chunks into a coarser grid so islands read as
 *     multi-chunk landmasses rather than a single-chunk checkerboard
 * @param topOnly whether a selected island keeps only its top {@link #topOnlyDepthBlocks},
 *     voiding everything below (GOALS 09's "like 5 deep to ensure access to stone"), rather than
 *     the entire natural column down to bedrock
 * @param topOnlyDepthBlocks depth kept below the real generated surface when {@link #topOnly}
 * @param exclusionZone void buffer around the starter island before scattered islands begin,
 *     reusing {@link IslandPlan.ExclusionZone} directly (same shared mechanism as GOALS 04/07/08)
 */
public record ChunkIslandPlan(
    boolean enabled,
    double spawnChance,
    int cellSizeChunks,
    boolean topOnly,
    int topOnlyDepthBlocks,
    IslandPlan.ExclusionZone exclusionZone
) {
    /**
     * Minimum radius (in cells, converted to blocks by the caller) the guaranteed portal-room
     * cell sits beyond the exclusion zone -- mirrors {@link FloatingIslandsPlan}'s guaranteed-
     * village reservation shape (DESIGN §28.3/§29.4) exactly, one full cell clear of the zone
     * rather than a fixed block distance, so it scales with whatever grid the world configured.
     */
    private static final int PORTAL_CELL_CLEARANCE_CELLS = 1;

    /** Validates persisted values even while the feature is disabled. */
    public ChunkIslandPlan {
        if (spawnChance < 0.0 || spawnChance > 1.0) {
            throw new IllegalArgumentException("Chunk island spawn chance must be between 0 and 1.");
        }
        if (cellSizeChunks < 1 || cellSizeChunks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS / 16) {
            throw new IllegalArgumentException("Chunk island cell size must be at least 1 chunk and reasonably bounded.");
        }
        if (topOnlyDepthBlocks < 1) {
            throw new IllegalArgumentException("Chunk island top-only depth must be positive.");
        }
        if (exclusionZone == null) {
            throw new IllegalArgumentException("Exclusion zone is required.");
        }
    }

    /**
     * Returns a plan with chunk islands switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static ChunkIslandPlan disabled() {
        return new ChunkIslandPlan(false, 0.35, 1, false, 5, new IslandPlan.ExclusionZone(false, 256));
    }

    /**
     * Resolves a dimension's plan from sanitized YAML configuration. The Nether/End only receive
     * chunk islands when their own {@code applyTo*} toggle is set (mirrors {@link
     * StripPlan#fromConfig}'s exact per-dimension gating).
     *
     * @param config sanitized chunk-island configuration
     * @param dimension which dimension this resolution is for
     * @return resolved plan, disabled unless applicable
     */
    public static ChunkIslandPlan fromConfig(ChunkIslandConfig config, Dimension dimension) {
        boolean applies = switch (dimension) {
            case OVERWORLD -> config.enabled;
            case NETHER -> config.enabled && config.applyToNether;
            case END -> config.enabled && config.applyToEnd;
        };
        if (!applies) {
            return disabled();
        }
        return new ChunkIslandPlan(
            true, config.spawnChance, config.cellSizeChunks, config.topOnly, config.topOnlyDepthBlocks,
            new IslandPlan.ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks)
        );
    }

    /** Which dimension a resolution is for -- see {@link #fromConfig}. */
    public enum Dimension {
        /** The Overworld -- always eligible when the plan is enabled at all. */
        OVERWORLD,
        /** The Nether -- eligible only when {@code applyToNether} is also set. */
        NETHER,
        /** The End -- eligible only when {@code applyToEnd} is also set. */
        END
    }

    /**
     * One resolved cell's placement: whether it holds a present island and, if so, its own
     * depth mode. Unlike {@link FloatingIslandsPlan.Hit} there is no shore distance -- a chunk
     * island's edge is exactly the chunk boundary, no coastline shape to speak of.
     *
     * @param present whether this chunk is a selected island
     * @param topOnly whether this specific island keeps only its top slice
     * @param topOnlyDepthBlocks depth kept below the surface, meaningless unless {@code topOnly}
     */
    public record Hit(boolean present, boolean topOnly, int topOnlyDepthBlocks) {
        /** The "not an island" result. */
        public static final Hit NONE = new Hit(false, false, 0);
    }

    /**
     * Classifies one chunk against the grid: whether it is a selected island, and (independent
     * of {@link #topOnly}'s plan-wide default) its own resolved depth mode -- always the plan's
     * own {@link #topOnly}/{@link #topOnlyDepthBlocks} today; per-island depth-mode variety
     * (GOALS 37) is layered on by scattered-island callers, not this base method.
     *
     * @param chunkX chunk X coordinate, relative to the origin chunk
     * @param chunkZ chunk Z coordinate, relative to the origin chunk
     * @param seed sampling seed (the world's real seed, resolved live)
     * @return the chunk's resolved hit
     */
    public Hit at(int chunkX, int chunkZ, long seed) {
        if (!enabled) {
            return Hit.NONE;
        }
        long cellX = Math.floorDiv(chunkX, cellSizeChunks);
        long cellZ = Math.floorDiv(chunkZ, cellSizeChunks);
        if (isStarterCell(cellX, cellZ)) {
            return new Hit(true, topOnly, topOnlyDepthBlocks);
        }
        PortalCell portal = reservedPortalCell(seed);
        if (cellX == portal.cellX() && cellZ == portal.cellZ()) {
            return new Hit(true, topOnly, topOnlyDepthBlocks);
        }
        boolean present = hash01(seed, "chunk_island_present", cellX, cellZ) < spawnChance
            && !withinExclusionZone(cellX, cellZ);
        return present ? new Hit(true, topOnly, topOnlyDepthBlocks) : Hit.NONE;
    }

    private boolean isStarterCell(long cellX, long cellZ) {
        return cellX == 0 && cellZ == 0;
    }

    private boolean withinExclusionZone(long cellX, long cellZ) {
        if (!exclusionZone.enabled()) {
            return false;
        }
        double centerX = cellX * (double) cellSizeChunks * 16.0 + cellSizeChunks * 8.0;
        double centerZ = cellZ * (double) cellSizeChunks * 16.0 + cellSizeChunks * 8.0;
        return Math.hypot(centerX, centerZ) < exclusionZone.radiusBlocks();
    }

    /**
     * One reserved cell's grid coordinates -- see {@link #reservedPortalCell}.
     *
     * @param cellX cell grid X coordinate
     * @param cellZ cell grid Z coordinate
     */
    public record PortalCell(long cellX, long cellZ) {
        /**
         * The cell's block-space center, for placing the forced structure.
         *
         * @param cellSizeChunks grid-cell edge length in chunks
         * @return center block X/Z as a two-element array ({@code [x, z]})
         */
        public int[] centerBlock(int cellSizeChunks) {
            int centerX = (int) (cellX * cellSizeChunks * 16L + cellSizeChunks * 8L);
            int centerZ = (int) (cellZ * cellSizeChunks * 16L + cellSizeChunks * 8L);
            return new int[] {centerX, centerZ};
        }
    }

    /**
     * Resolves the guaranteed portal-room cell's grid coordinates (GOALS 09, DESIGN §29.4):
     * always the same cell for a given seed, one full cell size beyond the exclusion zone at a
     * hash-picked angle -- the same "reserved cell" shape {@link
     * FloatingIslandsPlan#resolveVillageCell} already established for the guaranteed village.
     *
     * @param seed sampling seed
     * @return the reserved cell's coordinates
     */
    public PortalCell reservedPortalCell(long seed) {
        double angle = hash01(seed, "chunk_island_portal_angle", 0, 0) * 2.0 * Math.PI;
        double exclusionRadius = exclusionZone.enabled() ? exclusionZone.radiusBlocks() : 0.0;
        double cellSizeBlocks = cellSizeChunks * 16.0;
        double distance = exclusionRadius + cellSizeBlocks * PORTAL_CELL_CLEARANCE_CELLS
            + hash01(seed, "chunk_island_portal_distance", 0, 0) * cellSizeBlocks;
        long cellX = Math.floorDiv(Math.round(Math.cos(angle) * distance), (long) cellSizeBlocks);
        long cellZ = Math.floorDiv(Math.round(Math.sin(angle) * distance), (long) cellSizeBlocks);
        return new PortalCell(cellX, cellZ);
    }

    private static double hash01(long seed, String salt, long a, long b) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt.hashCode());
        h = splitmix64(h ^ a);
        h = splitmix64(h ^ b);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
