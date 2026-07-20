package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.Arrays;
import java.util.List;

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
    IslandPlan.ExclusionZone exclusionZone
) {
    /**
     * Fraction of {@code cellSizeBlocks} an island's center may be jittered off the cell's own
     * center, each axis independently -- kept well under 0.5 so a fully-jittered island in one
     * cell can never wander past a neighboring cell's own center, which is what lets {@link #at}
     * limit its search to the immediate 3x3 neighborhood.
     */
    private static final double JITTER_FRACTION = 0.3;

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
            new IslandPlan.ExclusionZone(false, 256)
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
            new IslandPlan.ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks)
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
        String exclusionZoneRadiusBlocks
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
            )
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
        if (hash01(seed, "floating_island_present", cellX, cellZ, 0) >= spawnChance) {
            return Hit.NONE;
        }
        double centerX = cellCenter(cellX, seed, "floating_island_jitter_x", cellZ);
        double centerZ = cellCenter(cellZ, seed, "floating_island_jitter_z", cellX);
        if (exclusionZone.enabled() && Math.hypot(centerX, centerZ) < exclusionZone.radiusBlocks()) {
            return Hit.NONE;
        }
        double radius = minRadiusBlocks + hash01(seed, "floating_island_radius", cellX, cellZ, 0) * (maxRadiusBlocks - minRadiusBlocks);
        long cellSeed = splitmix64(seed ^ splitmix64(cellX) ^ splitmix64(cellZ * 0x2545F4914F6CDD1DL));
        double distance = IslandShapeProfile.distanceFromShore(
            (int) Math.round(x - centerX), (int) Math.round(z - centerZ), radius, shapeAmplitude, cellSeed
        );
        if (distance > 0.0) {
            return Hit.NONE;
        }
        String biome = biomeVariety
            ? islandBiomes.get(Math.floorMod((int) Math.floor(hash01(seed, "floating_island_biome", cellX, cellZ, 0) * islandBiomes.size()), islandBiomes.size()))
            : fallbackBiome;
        return new Hit(true, distance, biome);
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
