package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.OceanIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

/**
 * A natural-looking artificial island surrounded by an endless generated ocean (GOALS 01,
 * DESIGN §24): a new, additive, ocean-island-only mechanism, not a retrofit of the shared
 * {@code StarterLandPlan}/{@code StarterZone} system every other typed preset uses (DESIGN
 * §24.1). Threaded through {@code EnvelopedChunkGenerator} and {@code LimitedBiomeSource}
 * exactly like {@link StripPlan}.
 *
 * @param enabled whether the island shape applies
 * @param radiusBlocks configured (unperturbed) island radius
 * @param shapeAmplitude coastline perturbation strength, {@code 0} through {@code
 *     IslandShapeProfile.MAX_AMPLITUDE}
 * @param islandBiome the one biome that fills the island's interior
 * @param shoreWidthBlocks width of the beach/stony-shore ring measured from the coastline;
 *     also the terrain-height taper width
 * @param oceanShallowWidthBlocks width of the shallow ocean band immediately beyond the shore
 * @param oceanDeepenWidthBlocks width over which the seabed ramps from shallow to deep
 * @param oceanShallowDepthBlocks seabed depth below sea level in the shallow band
 * @param oceanDeepDepthBlocks seabed depth below sea level once fully deep
 * @param oceanRegionScaleBlocks grid-cell edge length for the ocean biome's per-region pick
 * @param exclusionZoneEnabled whether island/ocean shaping releases beyond {@link
 *     #exclusionZoneRadiusBlocks}, letting the seed's natural terrain resume (GOALS 04)
 * @param exclusionZoneRadiusBlocks radius beyond which shaping releases, when enabled
 * @param hasLand whether any land exists at all (DESIGN §25.2). {@code false} only for the
 *     {@code CHEST_BOAT} island source (GOALS 03): the interior and shore-ring branches never
 *     fire, and the ocean gradient starts right at the origin instead of past a shore ring that
 *     doesn't exist. {@code radiusBlocks}/{@code islandBiome} are unused placeholders in that case.
 */
public record IslandPlan(
    boolean enabled,
    int radiusBlocks,
    double shapeAmplitude,
    String islandBiome,
    int shoreWidthBlocks,
    int oceanShallowWidthBlocks,
    int oceanDeepenWidthBlocks,
    int oceanShallowDepthBlocks,
    int oceanDeepDepthBlocks,
    int oceanRegionScaleBlocks,
    boolean exclusionZoneEnabled,
    int exclusionZoneRadiusBlocks,
    boolean hasLand
) {
    /** Fixture-verified default shore-ring width; see DESIGN §24.4. */
    public static final int DEFAULT_SHORE_WIDTH_BLOCKS = 12;
    /** Fixture-verified default ocean gradient widths/depths; see DESIGN §24.5. */
    public static final int DEFAULT_OCEAN_SHALLOW_WIDTH_BLOCKS = 64;
    public static final int DEFAULT_OCEAN_DEEPEN_WIDTH_BLOCKS = 128;
    public static final int DEFAULT_OCEAN_SHALLOW_DEPTH_BLOCKS = 8;
    public static final int DEFAULT_OCEAN_DEEP_DEPTH_BLOCKS = 32;
    public static final int DEFAULT_OCEAN_REGION_SCALE_BLOCKS = 128;
    /** Default exclusion-zone radius shared with every other module reusing DESIGN §20.7. */
    public static final int DEFAULT_EXCLUSION_ZONE_RADIUS_BLOCKS = 2000;

    /** Validates persisted values even while the island is disabled. */
    public IslandPlan {
        if (radiusBlocks < WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS || radiusBlocks > WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Island radius must be between " + WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS
                    + " and " + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS + "."
            );
        }
        if (shapeAmplitude < 0.0 || shapeAmplitude > IslandShapeProfile.MAX_AMPLITUDE) {
            throw new IllegalArgumentException(
                "Island shape amplitude must be between 0 and " + IslandShapeProfile.MAX_AMPLITUDE + "."
            );
        }
        if (islandBiome == null || islandBiome.isBlank()) {
            throw new IllegalArgumentException("Island biome is required.");
        }
        if (shoreWidthBlocks <= 0) {
            throw new IllegalArgumentException("Shore width must be positive.");
        }
        if (oceanShallowWidthBlocks < 0 || oceanDeepenWidthBlocks < 0) {
            throw new IllegalArgumentException("Ocean gradient widths must not be negative.");
        }
        if (oceanShallowDepthBlocks <= 0 || oceanDeepDepthBlocks <= 0) {
            throw new IllegalArgumentException("Ocean gradient depths must be positive.");
        }
        if (oceanRegionScaleBlocks <= 0) {
            throw new IllegalArgumentException("Ocean region scale must be positive.");
        }
        if (exclusionZoneRadiusBlocks <= 0) {
            throw new IllegalArgumentException("Exclusion zone radius must be positive.");
        }
    }

    /**
     * Returns a plan with the island shape switched off.
     *
     * @return disabled island plan with safe placeholder values
     */
    public static IslandPlan disabled() {
        return new IslandPlan(
            false, WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, 0.0, "minecraft:plains",
            DEFAULT_SHORE_WIDTH_BLOCKS, DEFAULT_OCEAN_SHALLOW_WIDTH_BLOCKS, DEFAULT_OCEAN_DEEPEN_WIDTH_BLOCKS,
            DEFAULT_OCEAN_SHALLOW_DEPTH_BLOCKS, DEFAULT_OCEAN_DEEP_DEPTH_BLOCKS, DEFAULT_OCEAN_REGION_SCALE_BLOCKS,
            false, DEFAULT_EXCLUSION_ZONE_RADIUS_BLOCKS, true
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized ocean-island configuration
     * @return resolved, enabled plan with land present
     */
    public static IslandPlan fromConfig(OceanIslandConfig config) {
        return new IslandPlan(
            true,
            config.radiusBlocks,
            config.shapeAmplitude,
            config.islandBiome,
            config.shoreWidthBlocks,
            config.oceanShallowWidthBlocks,
            config.oceanDeepenWidthBlocks,
            config.oceanShallowDepthBlocks,
            config.oceanDeepDepthBlocks,
            config.oceanRegionScaleBlocks,
            config.exclusionZoneEnabled,
            config.exclusionZoneRadiusBlocks,
            true
        );
    }

    /**
     * Resolves a land-free plan from sanitized YAML configuration (GOALS 03, {@code
     * IslandSource.CHEST_BOAT}): the ocean gradient and exclusion zone still apply exactly as
     * configured, but {@link #hasLand} is {@code false} so no interior biome or shore ring is
     * ever selected -- {@code radiusBlocks}/{@code islandBiome} become unused placeholders.
     *
     * @param config sanitized ocean-island configuration
     * @return resolved, enabled plan with no land
     */
    public static IslandPlan fromConfigWithoutLand(OceanIslandConfig config) {
        return new IslandPlan(
            true,
            WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS,
            0.0,
            "minecraft:plains",
            config.shoreWidthBlocks,
            config.oceanShallowWidthBlocks,
            config.oceanDeepenWidthBlocks,
            config.oceanShallowDepthBlocks,
            config.oceanDeepDepthBlocks,
            config.oceanRegionScaleBlocks,
            config.exclusionZoneEnabled,
            config.exclusionZoneRadiusBlocks,
            false
        );
    }

    /**
     * Computes the signed distance from the perturbed coastline at one column.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param seed sampling seed (the world's real seed, resolved live -- see DESIGN §24.2)
     * @return negative inside the island, positive outside, zero at the coastline
     */
    public double distanceFromShore(int x, int z, long seed) {
        return IslandShapeProfile.distanceFromShore(x, z, radiusBlocks, shapeAmplitude, seed);
    }

    /**
     * Tests whether a column is inside the exclusion zone (or the zone is disabled), meaning
     * island/ocean shaping should apply. Beyond the zone, shaping releases and the seed's
     * natural terrain resumes (GOALS 04).
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @return whether island/ocean shaping applies at this column
     */
    public boolean withinExclusionZone(int x, int z) {
        if (!exclusionZoneEnabled) {
            return true;
        }
        long distance = Math.max(Math.abs((long) x), Math.abs((long) z));
        return distance <= exclusionZoneRadiusBlocks;
    }
}
