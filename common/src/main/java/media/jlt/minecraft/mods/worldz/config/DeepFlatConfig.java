package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:deep_flat} typed preset (GOAL 16, DESIGN §33.4), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class DeepFlatConfig {
    /** The flat cap height -- everything above clears to air, real terrain below stays. */
    public int surfaceY = DeepFlatPlan.DEFAULT_SURFACE_Y;
    /** Land-cap layer stack, painted immediately below {@link #surfaceY}. */
    public List<String> capLayers = defaultCapLayers();
    /** Whether a river/ocean biome column gets a water-surface cap instead of the land cap. */
    public boolean riversEnabled = true;
    /** Radius around the origin within which river/ocean columns always get the land cap. */
    public int riverExclusionRadiusBlocks = DeepFlatPlan.DEFAULT_RIVER_EXCLUSION_RADIUS_BLOCKS;

    /** Creates a config populated with defaults. */
    public DeepFlatConfig() {
    }

    private static List<String> defaultCapLayers() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:dirt:3");
        list.add("minecraft:grass_block:1");
        return list;
    }
}
