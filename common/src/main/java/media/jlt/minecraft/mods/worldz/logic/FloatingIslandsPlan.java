package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Scattered small floating islands filling the void beyond a sky island's own footprint (GOALS
 * 08, DESIGN §28.1): a new, additive, sky-island-only mechanism -- not a {@link WorldLayoutPlan}
 * region (that machinery colors an entire cell one biome, with no notion of a localized shape
 * crossing a cell boundary) and not a retrofit of {@link SkyIslandPlan}'s own single-circle
 * footprint. A jittered grid of {@code cellSizeBlocks}-edged cells, each independently present
 * or empty ({@link #spawnChance}), holding one island with a hash-picked center offset and
 * radius. Bounded jitter guarantees an island can never reach further than one cell away, so
 * {@link #at} only ever needs to check the query cell and its 8 neighbors.
 *
 * @param enabled whether scattered islands generate at all
 * @param minRadiusBlocks smallest hash-picked island radius
 * @param maxRadiusBlocks largest hash-picked island radius
 * @param shapeAmplitude coastline perturbation strength, reusing {@link IslandShapeProfile}
 * @param cellSizeBlocks grid-cell edge length -- the primary "how far apart" knob (GOALS 08's
 *     "sufficiently far away to require a lot of bridging")
 * @param spawnChance probability ({@code 0..1}) that a given cell holds an island, independent
 *     of spacing -- controls density without changing how far apart present islands are
 * @param biomeVariety whether each island hash-picks its own biome from {@link #islandBiomes}
 *     ({@code true}) or shares the starter sky island's single biome, passed into {@link #at}
 *     ({@code false})
 * @param islandBiomes candidate biome pool when {@link #biomeVariety} is {@code true}
 * @param exclusionZone void buffer around the starter island before scattered islands begin
 *     (reuses {@link IslandPlan.ExclusionZone} directly -- GOALS 07/08's shared mechanism, DESIGN
 *     §28.1)
 * @param oreDepositsEnabled whether each island gets one embedded vanilla ore-vein feature
 *     (GOALS 08, DESIGN §28.2). The actual candidate feature-id pool is config-only, read live at
 *     placement time -- the same "list stays in config, never persisted" precedent {@code
 *     StarterKitPlan}'s own essentials/extras pool already established (DESIGN §28's revision
 *     note), only this toggle is persisted so a reloaded world remembers whether it was enabled
 * @param lootChestEnabled whether each island gets one placed loot chest (GOALS 08, DESIGN
 *     §28.2), reusing {@link StarterKitPlan} directly. Same config-only-list precedent as {@link
 *     #oreDepositsEnabled}: the actual kit contents live in config, only this toggle is persisted
 */
public record FloatingIslandsPlan(
    boolean enabled,
    int minRadiusBlocks,
    int maxRadiusBlocks,
    double shapeAmplitude,
    int cellSizeBlocks,
    double spawnChance,
    boolean biomeVariety,
    List<String> islandBiomes,
    IslandPlan.ExclusionZone exclusionZone,
    boolean oreDepositsEnabled,
    boolean lootChestEnabled
) {
    /**
     * Fraction of {@code cellSizeBlocks} an island's center may be jittered off the cell's own
     * center, each axis independently -- kept well under 0.5 so a fully-jittered island in one
     * cell can never wander past a neighboring cell's own center, which is what lets {@link #at}
     * limit its search to the immediate 3x3 neighborhood.
     */
    private static final double JITTER_FRACTION = 0.3;

    /**
     * Minimum radius for the guaranteed village's own reserved island (GOALS 07, DESIGN §28.3) --
     * bigger than any ordinary configured range, so the compact fallback-vault situation §24.6
     * accepted for tiny ocean islands doesn't recur for a real vanilla village.
     */
    public static final int VILLAGE_MIN_RADIUS_BLOCKS = 96;

    /** Village-compatible biome ids, paired index-for-index with {@link #VILLAGE_STRUCTURE_IDS}. */
    private static final String[] VILLAGE_BIOME_IDS = {
        "minecraft:plains", "minecraft:desert", "minecraft:savanna", "minecraft:snowy_plains", "minecraft:taiga"
    };

    /** Real vanilla village structure ids, one per {@link #VILLAGE_BIOME_IDS} entry (DESIGN §28.3). */
    private static final String[] VILLAGE_STRUCTURE_IDS = {
        "minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
        "minecraft:village_snowy", "minecraft:village_taiga"
    };

    /** Validates persisted values even while the feature is disabled. */
    public FloatingIslandsPlan {
        if (minRadiusBlocks < WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS || minRadiusBlocks > WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Floating island min radius must be between " + WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS
                    + " and " + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS + "."
            );
        }
        if (maxRadiusBlocks < minRadiusBlocks || maxRadiusBlocks > WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Floating island max radius must be at least the min radius and at most " + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS + "."
            );
        }
        if (shapeAmplitude < 0.0 || shapeAmplitude > IslandShapeProfile.MAX_AMPLITUDE) {
            throw new IllegalArgumentException(
                "Floating island shape amplitude must be between 0 and " + IslandShapeProfile.MAX_AMPLITUDE + "."
            );
        }
        if (cellSizeBlocks < WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS || cellSizeBlocks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS) {
            throw new IllegalArgumentException(
                "Floating island cell size must be between " + WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
                    + " and " + WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS + "."
            );
        }
        if (spawnChance < 0.0 || spawnChance > 1.0) {
            throw new IllegalArgumentException("Floating island spawn chance must be between 0 and 1.");
        }
        islandBiomes = List.copyOf(islandBiomes);
        if (biomeVariety && islandBiomes.isEmpty()) {
            throw new IllegalArgumentException("Floating island biome variety is enabled but the biome pool is empty.");
        }
        if (exclusionZone == null) {
            throw new IllegalArgumentException("Exclusion zone is required.");
        }
    }

    /**
     * Returns a plan with scattered islands switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static FloatingIslandsPlan disabled() {
        return new FloatingIslandsPlan(
            false, WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, 0.0,
            WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, 0.0, false, List.of("minecraft:plains"),
            new IslandPlan.ExclusionZone(false, 256), false, false
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized floating-islands configuration
     * @return resolved plan
     */
    public static FloatingIslandsPlan fromConfig(FloatingIslandsConfig config) {
        return new FloatingIslandsPlan(
            config.enabled, config.minRadiusBlocks, config.maxRadiusBlocks, config.shapeAmplitude, config.cellSizeBlocks,
            config.spawnChance, config.biomeVariety, config.islandBiomes,
            new IslandPlan.ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks),
            config.oreDepositsEnabled, config.lootChestEnabled
        );
    }

    /**
     * Parses client text/toggle fields into validated values.
     *
     * @param enabled whether scattered islands generate at all
     * @param minRadiusBlocks decimal minimum radius
     * @param maxRadiusBlocks decimal maximum radius
     * @param shapeAmplitude decimal coastline perturbation strength
     * @param cellSizeBlocks decimal grid-cell edge length
     * @param spawnChance decimal spawn probability
     * @param biomeVariety whether each island hash-picks its own biome
     * @param islandBiomesText newline- or comma-separated candidate biome ids
     * @param exclusionZoneEnabled whether a void buffer precedes scattered islands
     * @param exclusionZoneRadiusBlocks decimal exclusion-zone radius
     * @param oreDepositsEnabled whether each island gets one embedded ore-vein feature
     * @param lootChestEnabled whether each island gets one placed loot chest
     * @return canonical immutable values
     */
    public static FloatingIslandsPlan fromText(
        boolean enabled,
        String minRadiusBlocks,
        String maxRadiusBlocks,
        String shapeAmplitude,
        String cellSizeBlocks,
        String spawnChance,
        boolean biomeVariety,
        String islandBiomesText,
        boolean exclusionZoneEnabled,
        String exclusionZoneRadiusBlocks,
        boolean oreDepositsEnabled,
        boolean lootChestEnabled
    ) {
        List<String> islandBiomes = Arrays.stream(islandBiomesText.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new FloatingIslandsPlan(
            enabled,
            parseInteger(minRadiusBlocks, "Floating island min radius"),
            parseInteger(maxRadiusBlocks, "Floating island max radius"),
            parseDouble(shapeAmplitude, "Floating island shape amplitude"),
            parseInteger(cellSizeBlocks, "Floating island cell size"),
            parseDouble(spawnChance, "Floating island spawn chance"),
            biomeVariety,
            islandBiomes,
            new IslandPlan.ExclusionZone(
                exclusionZoneEnabled, parseInteger(exclusionZoneRadiusBlocks, "Floating island exclusion zone radius")
            ),
            oreDepositsEnabled, lootChestEnabled
        );
    }

    /**
     * Renders the configured island biome ids one per line for the multi-line editor.
     *
     * @return newline-separated canonical values
     */
    public String islandBiomesText() {
        return String.join("\n", islandBiomes);
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }

    private static double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a number.", exception);
        }
    }

    /**
     * The result of querying one column against the scattered-island grid.
     *
     * @param present whether the column falls inside a scattered island
     * @param distanceFromShore signed shore distance (see {@link IslandShapeProfile#distanceFromShore}),
     *     meaningless when {@code present} is {@code false}
     * @param biome the containing island's biome, meaningless when {@code present} is {@code false}
     */
    public record Hit(boolean present, double distanceFromShore, String biome) {
        /** The "no island here" result. */
        public static final Hit NONE = new Hit(false, Double.POSITIVE_INFINITY, "");
    }

    /**
     * Classifies one column against the scattered-island grid.
     *
     * @param x block X relative to the sky island origin
     * @param z block Z relative to the sky island origin
     * @param seed sampling seed (the world's real seed, resolved live)
     * @param fallbackBiome biome to report when {@link #biomeVariety} is {@code false} (normally
     *     the starter sky island's own single biome)
     * @return the nearest containing island's hit, or {@link Hit#NONE}
     */
    public Hit at(int x, int z, long seed, String fallbackBiome) {
        if (!enabled) {
            return Hit.NONE;
        }
        long cellX = Math.floorDiv(x, cellSizeBlocks);
        long cellZ = Math.floorDiv(z, cellSizeBlocks);
        Hit best = Hit.NONE;
        for (long neighborX = cellX - 1; neighborX <= cellX + 1; neighborX++) {
            for (long neighborZ = cellZ - 1; neighborZ <= cellZ + 1; neighborZ++) {
                Hit candidate = hitFromCell(neighborX, neighborZ, x, z, seed, fallbackBiome);
                if (candidate.present() && candidate.distanceFromShore() < best.distanceFromShore()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private Hit hitFromCell(long cellX, long cellZ, int x, int z, long seed, String fallbackBiome) {
        return resolveCell(cellX, cellZ, seed, fallbackBiome)
            .map(island -> {
                long cellSeed = splitmix64(seed ^ splitmix64(cellX) ^ splitmix64(cellZ * 0x2545F4914F6CDD1DL));
                double distance = IslandShapeProfile.distanceFromShore(
                    (int) Math.round(x - island.centerX()), (int) Math.round(z - island.centerZ()),
                    island.radius(), shapeAmplitude, cellSeed
                );
                return distance > 0.0 ? Hit.NONE : new Hit(true, distance, island.biome());
            })
            .orElse(Hit.NONE);
    }

    /**
     * The resolved placement of one present island: its (jittered) center, radius, and biome --
     * unlike {@link Hit}, not tied to any particular query column. Used to find which one chunk
     * owns a given island's center, for one-time resource placement (an embedded ore deposit, a
     * loot chest, the guaranteed village -- DESIGN §28.2/§28.3).
     *
     * @param centerX jittered center block X, relative to the sky island origin
     * @param centerZ jittered center block Z, relative to the sky island origin
     * @param radius hash-picked (unperturbed) island radius
     * @param biome the island's biome
     */
    public record ResolvedIsland(double centerX, double centerZ, double radius, String biome) {
        /**
         * Deterministically picks one entry from a candidate list for this island's resource
         * placement (an ore feature id, DESIGN §28.2). The same island always picks the same
         * entry for a given seed and salt.
         *
         * @param candidates non-empty candidate pool
         * @param seed sampling seed
         * @param salt distinguishes this pick from any other made against the same island
         * @return one entry from {@code candidates}
         */
        public String pick(List<String> candidates, long seed, String salt) {
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Candidate list must not be empty.");
            }
            double fraction = hash01(seed, salt, cellCoordinate(centerX), cellCoordinate(centerZ), 0);
            return candidates.get(Math.floorMod((int) Math.floor(fraction * candidates.size()), candidates.size()));
        }

        /**
         * Deterministically picks a Y within an inclusive range for this island's resource
         * placement.
         *
         * @param minY inclusive lower bound
         * @param maxY inclusive upper bound, at least {@code minY}
         * @param seed sampling seed
         * @param salt distinguishes this pick from any other made against the same island
         * @return a Y in {@code [minY, maxY]}
         */
        public int pickY(int minY, int maxY, long seed, String salt) {
            if (maxY < minY) {
                throw new IllegalArgumentException("maxY must be at least minY.");
            }
            double fraction = hash01(seed, salt, cellCoordinate(centerX), cellCoordinate(centerZ), 1);
            return minY + (int) Math.floor(fraction * (maxY - minY + 1));
        }

        private static long cellCoordinate(double value) {
            return Math.round(value);
        }
    }

    /**
     * Resolves every present island among the 3x3 grid-cell neighborhood around one reference
     * point (a chunk's own center, normally) -- unlike {@link #at}, does not check whether the
     * reference point itself falls inside any resolved island. Used to find which one chunk owns
     * a given island's center for one-time resource placement, since a jittered center can land
     * in a different chunk than a naive "which cell does this chunk's own center belong to"
     * lookup would suggest (DESIGN §28.2/§28.3).
     *
     * @param x reference block X relative to the sky island origin
     * @param z reference block Z relative to the sky island origin
     * @param seed sampling seed
     * @param fallbackBiome biome to report when {@link #biomeVariety} is {@code false}
     * @return every present island among the 9 neighboring cells
     */
    public List<ResolvedIsland> nearbyIslands(int x, int z, long seed, String fallbackBiome) {
        if (!enabled) {
            return List.of();
        }
        long cellX = Math.floorDiv(x, cellSizeBlocks);
        long cellZ = Math.floorDiv(z, cellSizeBlocks);
        List<ResolvedIsland> islands = new ArrayList<>();
        for (long neighborX = cellX - 1; neighborX <= cellX + 1; neighborX++) {
            for (long neighborZ = cellZ - 1; neighborZ <= cellZ + 1; neighborZ++) {
                resolveCell(neighborX, neighborZ, seed, fallbackBiome).ifPresent(islands::add);
            }
        }
        return islands;
    }

    private Optional<ResolvedIsland> resolveCell(long cellX, long cellZ, long seed, String fallbackBiome) {
        VillageCell village = resolveVillageCell(seed);
        if (cellX == village.cellX() && cellZ == village.cellZ()) {
            // The guaranteed village's own reserved cell (GOALS 07, DESIGN §28.3): always present,
            // forced radius/biome, bypassing the ordinary spawnChance/exclusion-zone gating below
            // entirely -- it's constructed to already sit beyond the exclusion zone.
            double villageCenterX = cellCenter(cellX, seed, "floating_island_jitter_x", cellZ);
            double villageCenterZ = cellCenter(cellZ, seed, "floating_island_jitter_z", cellX);
            return Optional.of(new ResolvedIsland(
                villageCenterX, villageCenterZ, Math.max(minRadiusBlocks, VILLAGE_MIN_RADIUS_BLOCKS), villageBiome(seed)
            ));
        }
        if (hash01(seed, "floating_island_present", cellX, cellZ, 0) >= spawnChance) {
            return Optional.empty();
        }
        double centerX = cellCenter(cellX, seed, "floating_island_jitter_x", cellZ);
        double centerZ = cellCenter(cellZ, seed, "floating_island_jitter_z", cellX);
        if (exclusionZone.enabled() && Math.hypot(centerX, centerZ) < exclusionZone.radiusBlocks()) {
            return Optional.empty();
        }
        double radius = minRadiusBlocks + hash01(seed, "floating_island_radius", cellX, cellZ, 0) * (maxRadiusBlocks - minRadiusBlocks);
        String biome = biomeVariety
            ? islandBiomes.get(Math.floorMod((int) Math.floor(hash01(seed, "floating_island_biome", cellX, cellZ, 0) * islandBiomes.size()), islandBiomes.size()))
            : fallbackBiome;
        return Optional.of(new ResolvedIsland(centerX, centerZ, radius, biome));
    }

    /**
     * The result of resolving the guaranteed village's placement (GOALS 07, DESIGN §28.3): its
     * (jittered) center and which vanilla village structure variant to force-generate there.
     *
     * @param centerX jittered center block X, relative to the sky island origin
     * @param centerZ jittered center block Z, relative to the sky island origin
     * @param structureId the vanilla village structure id matching the forced biome
     */
    public record VillageSite(double centerX, double centerZ, String structureId) {
    }

    /**
     * Resolves the guaranteed village's placement (GOALS 07, DESIGN §28.3). Only meaningful when
     * {@link #enabled} -- callers gate on that themselves, matching every other deployment method
     * in this codebase (e.g. {@code StarterKitDeployment}'s own unguarded internals).
     *
     * @param seed sampling seed (the world's real seed, resolved live)
     * @return the village's resolved placement
     */
    public VillageSite guaranteedVillageSite(long seed) {
        VillageCell village = resolveVillageCell(seed);
        double centerX = cellCenter(village.cellX(), seed, "floating_island_jitter_x", village.cellZ());
        double centerZ = cellCenter(village.cellZ(), seed, "floating_island_jitter_z", village.cellX());
        return new VillageSite(centerX, centerZ, villageStructureId(seed));
    }

    private record VillageCell(long cellX, long cellZ) {
    }

    /**
     * Resolves the guaranteed village's grid-cell coordinates: a hash-picked angle and distance
     * (always at least one full cell size beyond the exclusion zone) converted to cell indices --
     * always the same cell for a given seed, independent of the ordinary per-cell {@link
     * #spawnChance} roll.
     */
    private VillageCell resolveVillageCell(long seed) {
        double angle = hash01(seed, "floating_island_village_angle", 0, 0, 0) * 2.0 * Math.PI;
        double exclusionRadius = exclusionZone.enabled() ? exclusionZone.radiusBlocks() : 0.0;
        double distance = exclusionRadius + cellSizeBlocks
            + hash01(seed, "floating_island_village_distance", 0, 0, 0) * cellSizeBlocks;
        long cellX = Math.floorDiv(Math.round(Math.cos(angle) * distance), (long) cellSizeBlocks);
        long cellZ = Math.floorDiv(Math.round(Math.sin(angle) * distance), (long) cellSizeBlocks);
        return new VillageCell(cellX, cellZ);
    }

    private static int villageVariantIndex(long seed) {
        double fraction = hash01(seed, "floating_island_village_biome", 0, 0, 0);
        return Math.floorMod((int) Math.floor(fraction * VILLAGE_BIOME_IDS.length), VILLAGE_BIOME_IDS.length);
    }

    private static String villageBiome(long seed) {
        return VILLAGE_BIOME_IDS[villageVariantIndex(seed)];
    }

    private static String villageStructureId(long seed) {
        return VILLAGE_STRUCTURE_IDS[villageVariantIndex(seed)];
    }

    private double cellCenter(long cell, long seed, String salt, long otherCell) {
        double base = cell * (double) cellSizeBlocks + cellSizeBlocks / 2.0;
        double jitter = (hash01(seed, salt, cell, otherCell, 0) * 2.0 - 1.0) * cellSizeBlocks * JITTER_FRACTION;
        return base + jitter;
    }

    private static double hash01(long seed, String salt, long a, long b, long c) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt.hashCode());
        h = splitmix64(h ^ a);
        h = splitmix64(h ^ b);
        h = splitmix64(h ^ c);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
