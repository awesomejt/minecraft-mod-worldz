package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.DeepFlatConfig;

import java.util.List;

/**
 * The {@code jlt_worldz:deep_flat} typed preset's resolved settings (GOAL 16, DESIGN §33.4): a
 * flat surface capped over real, unmodified vanilla terrain -- caves, cave biomes, aquifers,
 * ores, and structures all come from the delegate's own real generation, completely untouched
 * below the cap. Like {@code cave}/{@code flat}, this plan is never read from {@code
 * LimitedBiomeSource} -- persisted entirely on {@code EnvelopedChunkGenerator}'s own codec
 * instead, since none of it needs per-column biome-source consultation and {@code
 * LimitedBiomeSource}'s own codec is already full.
 *
 * @param enabled whether the deep-flat shape applies
 * @param surfaceY the flat cap height -- everything above clears to air, real terrain below stays
 * @param capLayers land-cap layer stack, painted immediately below {@code surfaceY}
 * @param riversEnabled whether a river/ocean biome column gets a water-surface cap instead of the
 *     ordinary land cap
 * @param riverExclusionRadiusBlocks radius around the origin within which river/ocean columns
 *     always get the ordinary land cap regardless of {@link #riversEnabled} (GOAL 16's "far away
 *     from spawn")
 */
public record DeepFlatPlan(
    boolean enabled,
    int surfaceY,
    List<FlatLayerSpec> capLayers,
    boolean riversEnabled,
    int riverExclusionRadiusBlocks
) {
    /** Fixture-verified default: vanilla sea level, comfortable headroom for real caves below. */
    public static final int DEFAULT_SURFACE_Y = 64;
    /** Smallest supported surface Y -- keeps well clear of the Overworld's own bedrock floor. */
    public static final int MIN_SURFACE_Y = -32;
    /** Largest supported surface Y -- a generous ceiling, not a tuned limit. */
    public static final int MAX_SURFACE_Y = 256;
    /** Fixture-verified default: comfortably far from spawn without being excessive. */
    public static final int DEFAULT_RIVER_EXCLUSION_RADIUS_BLOCKS = 512;

    /** Validates persisted values even while the deep-flat shape is disabled. */
    public DeepFlatPlan {
        if (capLayers == null || capLayers.isEmpty()) {
            throw new IllegalArgumentException("Deep-flat cap layer list must not be empty.");
        }
        if (surfaceY < MIN_SURFACE_Y || surfaceY > MAX_SURFACE_Y) {
            throw new IllegalArgumentException("Deep-flat surface Y must be between " + MIN_SURFACE_Y + " and " + MAX_SURFACE_Y + ".");
        }
        if (riverExclusionRadiusBlocks < 0) {
            throw new IllegalArgumentException("River exclusion radius must not be negative.");
        }
        capLayers = List.copyOf(capLayers);
    }

    /**
     * Returns a plan with the deep-flat shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static DeepFlatPlan disabled() {
        return new DeepFlatPlan(
            false, DEFAULT_SURFACE_Y, List.of(new FlatLayerSpec("minecraft:grass_block", 1)), true, DEFAULT_RIVER_EXCLUSION_RADIUS_BLOCKS
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized deep-flat configuration
     * @return resolved, enabled plan
     */
    public static DeepFlatPlan fromConfig(DeepFlatConfig config) {
        List<FlatLayerSpec> layers = config.capLayers.stream().map(FlatLayerSpec::parse).toList();
        return new DeepFlatPlan(true, config.surfaceY, layers, config.riversEnabled, config.riverExclusionRadiusBlocks);
    }

    /**
     * Returns the cap band's total thickness.
     *
     * @return summed cap layer height in blocks
     */
    public int capThicknessBlocks() {
        return this.capLayers.stream().mapToInt(FlatLayerSpec::heightBlocks).sum();
    }
}
