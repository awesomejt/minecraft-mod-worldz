package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.CaveConfig;

/**
 * The {@code cave} typed preset's resolved settings (GOALS 25-26, DESIGN §30): underground
 * spawn placement, an optional solid roof sealing off sky access, and an optional large carved
 * cavern around spawn. Unlike every other typed preset this plan is never read from {@code
 * LimitedBiomeSource} -- persisted entirely on {@code EnvelopedChunkGenerator}'s own codec
 * instead (DESIGN §30.1), since none of its three pieces need per-column biome-source
 * consultation and {@code LimitedBiomeSource}'s own codec is already full.
 *
 * @param enabled whether the cave shape applies
 * @param spawnDepthY target Y for the underground spawn-cavity search (GOALS 25's
 *     "configurable depth")
 * @param sealedSurface whether a solid roof seals off sky access everywhere
 * @param sealedSurfaceY the roof's Y; terrain above it is capped solid
 * @param sealedSurfaceBlock the block the roof is built from (Jason, 2026-07-25: stone,
 *     deepslate, or bedrock)
 * @param sealedSurfaceThicknessBlocks the roof's thickness in blocks
 * @param cavernEnabled whether the mega-cavern (GOALS 26) is carved around spawn
 * @param cavernRadiusBlocks the mega-cavern's horizontal half-width
 * @param cavernHeightBlocks the mega-cavern's vertical half-height
 * @param chestEnabled whether a starter chest is placed at spawn (GOALS 25's "optionally")
 * @param chestTier the starter chest's difficulty tier, meaningful only when {@link
 *     #chestEnabled}
 */
public record CavePlan(
    boolean enabled,
    int spawnDepthY,
    boolean sealedSurface,
    int sealedSurfaceY,
    SealedSurfaceBlock sealedSurfaceBlock,
    int sealedSurfaceThicknessBlocks,
    boolean cavernEnabled,
    int cavernRadiusBlocks,
    int cavernHeightBlocks,
    boolean chestEnabled,
    StarterKitTier chestTier
) {
    /** GOALS 25's own default: clearly underground, matching {@code ProgressionGuarantees}' existing convention. */
    public static final int DEFAULT_SPAWN_DEPTH_Y = -32;
    /** Comfortably above ordinary hills; true mountains above it are deliberately clipped flat. */
    public static final int DEFAULT_SEALED_SURFACE_Y = 128;
    /** The original fixed roof material, kept as the default (Jason, 2026-07-25). */
    public static final SealedSurfaceBlock DEFAULT_SEALED_SURFACE_BLOCK = SealedSurfaceBlock.STONE;
    /** The original fixed roof thickness, kept as the default. */
    public static final int DEFAULT_SEALED_SURFACE_THICKNESS_BLOCKS = 5;
    /** Smallest supported roof thickness -- must still block sky access. */
    public static final int MIN_SEALED_SURFACE_THICKNESS_BLOCKS = 1;
    /** Largest supported roof thickness -- a generous ceiling, not a tuned limit. */
    public static final int MAX_SEALED_SURFACE_THICKNESS_BLOCKS = 64;
    /** Fixture-verified default -- a believable "world in a cave" without being excessive. */
    public static final int DEFAULT_CAVERN_RADIUS_BLOCKS = 48;
    /** Fixture-verified default vertical half-height. */
    public static final int DEFAULT_CAVERN_HEIGHT_BLOCKS = 24;
    /** Smallest supported cavern dimension. */
    public static final int MIN_CAVERN_BLOCKS = 8;
    /** Largest supported cavern dimension -- a generous ceiling, not a tuned limit. */
    public static final int MAX_CAVERN_BLOCKS = 256;

    /** Validates persisted values even while the cave shape is disabled. */
    public CavePlan {
        if (sealedSurface && sealedSurfaceY < CaveConfig.MIN_SEALED_SURFACE_Y) {
            throw new IllegalArgumentException(
                "Sealed surface Y must be at least " + CaveConfig.MIN_SEALED_SURFACE_Y + "."
            );
        }
        if (sealedSurfaceBlock == null) {
            throw new IllegalArgumentException("Sealed surface block is required.");
        }
        if (sealedSurfaceThicknessBlocks < MIN_SEALED_SURFACE_THICKNESS_BLOCKS
            || sealedSurfaceThicknessBlocks > MAX_SEALED_SURFACE_THICKNESS_BLOCKS) {
            throw new IllegalArgumentException(
                "Sealed surface thickness must be between " + MIN_SEALED_SURFACE_THICKNESS_BLOCKS
                    + " and " + MAX_SEALED_SURFACE_THICKNESS_BLOCKS + "."
            );
        }
        if (cavernRadiusBlocks < MIN_CAVERN_BLOCKS || cavernRadiusBlocks > MAX_CAVERN_BLOCKS) {
            throw new IllegalArgumentException(
                "Cavern radius must be between " + MIN_CAVERN_BLOCKS + " and " + MAX_CAVERN_BLOCKS + "."
            );
        }
        if (cavernHeightBlocks < MIN_CAVERN_BLOCKS || cavernHeightBlocks > MAX_CAVERN_BLOCKS) {
            throw new IllegalArgumentException(
                "Cavern height must be between " + MIN_CAVERN_BLOCKS + " and " + MAX_CAVERN_BLOCKS + "."
            );
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
    }

    /**
     * Returns a plan with the cave shape switched off.
     *
     * @return disabled cave plan with safe placeholder values
     */
    public static CavePlan disabled() {
        return new CavePlan(
            false, DEFAULT_SPAWN_DEPTH_Y, false, DEFAULT_SEALED_SURFACE_Y,
            DEFAULT_SEALED_SURFACE_BLOCK, DEFAULT_SEALED_SURFACE_THICKNESS_BLOCKS,
            false, DEFAULT_CAVERN_RADIUS_BLOCKS, DEFAULT_CAVERN_HEIGHT_BLOCKS, false, StarterKitTier.MEDIUM
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized cave configuration
     * @return resolved, enabled plan
     */
    public static CavePlan fromConfig(CaveConfig config) {
        return new CavePlan(
            true, config.spawnDepthY, config.sealedSurface, config.sealedSurfaceY,
            config.sealedSurfaceBlock, config.sealedSurfaceThicknessBlocks,
            config.cavernEnabled, config.cavernRadiusBlocks, config.cavernHeightBlocks,
            config.chestEnabled, config.chestTier
        );
    }
}
