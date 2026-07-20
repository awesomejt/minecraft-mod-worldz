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
 * <p>{@code exclusionZone} is a nested record rather than two flat fields (DESIGN §26.1):
 * {@code RecordCodecBuilder.create}'s {@code instance.group(...)} tops out at 14 fields in this
 * DFU version, and this record was already at exactly 14 before Phase 9 added {@code fluid} --
 * nesting the least-widely-used existing pair (exclusion zone) freed a slot without touching any
 * of the higher-traffic fields. Convenience accessors ({@link #exclusionZoneEnabled()}, {@link
 * #exclusionZoneRadiusBlocks()}) keep every existing call site source-compatible.
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
 * @param hasLand whether any land exists at all (DESIGN §25.2). {@code false} only for the
 *     {@code CHEST_BOAT} island source (GOALS 03): the interior and shore-ring branches never
 *     fire, and the ocean gradient starts right at the origin instead of past a shore ring that
 *     doesn't exist. {@code radiusBlocks}/{@code islandBiome} are unused placeholders in that case.
 * @param syntheticLand whether the land within {@code radiusBlocks} is artificially shaped
 *     (DESIGN §25.4). {@code false} only for the {@code NATURAL} island source (GOALS 02): no
 *     interior biome override, no shore ring, no terrain raise -- the real seed's own terrain
 *     and biome show through unmodified within {@code radiusBlocks}, and the ocean gradient
 *     begins immediately past it (no separate shore-ring width to subtract). Meaningless
 *     (harmless placeholder {@code true}) whenever {@link #hasLand} is {@code false}.
 * @param exclusionZone whether/where island/ocean shaping releases, letting the seed's natural
 *     terrain resume beyond it (GOALS 04)
 * @param fluid the exterior/ocean gradient's fluid (DESIGN §26.1): {@code WATER} (GOALS 01/02/03
 *     default), {@code LAVA} (GOALS 28), or {@code NONE} (GOALS 31, drained basins)
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
    boolean hasLand,
    boolean syntheticLand,
    ExclusionZone exclusionZone,
    IslandFluid fluid
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
        if (exclusionZone == null) {
            throw new IllegalArgumentException("Exclusion zone settings are required.");
        }
        if (exclusionZone.radiusBlocks() <= 0) {
            throw new IllegalArgumentException("Exclusion zone radius must be positive.");
        }
        if (fluid == null) {
            throw new IllegalArgumentException("Fluid is required.");
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
            true, true,
            new ExclusionZone(false, DEFAULT_EXCLUSION_ZONE_RADIUS_BLOCKS),
            IslandFluid.WATER
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized ocean-island configuration
     * @return resolved, enabled plan with artificially shaped land present
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
            true,
            true,
            new ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks),
            config.fluid
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
            false,
            true,
            new ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks),
            config.fluid
        );
    }

    /**
     * Resolves a natural-land plan from sanitized YAML configuration (GOALS 02, {@code
     * IslandSource.NATURAL}): {@link #hasLand} is {@code true} but {@link #syntheticLand} is
     * {@code false} -- no interior biome or shore ring is ever selected, and the ocean gradient
     * begins immediately past {@code radiusBlocks} with no separate ring to subtract.
     * {@code shapeAmplitude}/{@code islandBiome} become unused placeholders (the real seed's own
     * terrain provides the shape and biome variety).
     *
     * @param config sanitized ocean-island configuration
     * @return resolved, enabled plan with real, natural land
     */
    public static IslandPlan fromConfigNatural(OceanIslandConfig config) {
        return new IslandPlan(
            true,
            config.radiusBlocks,
            0.0,
            "minecraft:plains",
            config.shoreWidthBlocks,
            config.oceanShallowWidthBlocks,
            config.oceanDeepenWidthBlocks,
            config.oceanShallowDepthBlocks,
            config.oceanDeepDepthBlocks,
            config.oceanRegionScaleBlocks,
            true,
            false,
            new ExclusionZone(config.exclusionZoneEnabled, config.exclusionZoneRadiusBlocks),
            config.fluid
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
        if (!exclusionZone.enabled()) {
            return true;
        }
        long distance = Math.max(Math.abs((long) x), Math.abs((long) z));
        return distance <= exclusionZone.radiusBlocks();
    }

    /**
     * Whether island/ocean shaping releases beyond {@link #exclusionZoneRadiusBlocks} (GOALS 04).
     *
     * @return whether the exclusion zone is enabled
     */
    public boolean exclusionZoneEnabled() {
        return exclusionZone.enabled();
    }

    /**
     * Radius beyond which shaping releases, when the exclusion zone is enabled.
     *
     * @return exclusion zone radius in blocks
     */
    public int exclusionZoneRadiusBlocks() {
        return exclusionZone.radiusBlocks();
    }

    /**
     * Whether/where island/ocean shaping releases, letting the seed's natural terrain resume
     * beyond it (GOALS 04).
     *
     * @param enabled whether the exclusion zone is active
     * @param radiusBlocks radius beyond which shaping releases, when enabled
     */
    public record ExclusionZone(boolean enabled, int radiusBlocks) {
    }
}
