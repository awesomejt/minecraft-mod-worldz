package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;

/**
 * Defaults for the {@code jlt_worldz:sky_island} typed preset (GOALS 05, DESIGN §27), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class SkyIslandConfig {
    /** The one biome that fills the island's interior. */
    public String islandBiome = "minecraft:plains";
    /** Configured (unperturbed) island radius -- small by default, matching Skyblock's scale. */
    public int radiusBlocks = 16;
    /** Coastline perturbation strength. */
    public double shapeAmplitude = IslandShapeProfile.DEFAULT_AMPLITUDE;
    /** The island's walkable surface Y (GOALS 05 default 64, slime rule). */
    public int surfaceY = SkyIslandPlan.DEFAULT_SURFACE_Y;
    /** How many blocks of solid ground extend below {@link #surfaceY}. */
    public int thicknessBlocks = SkyIslandPlan.DEFAULT_THICKNESS_BLOCKS;

    /** Creates a config populated with defaults. */
    public SkyIslandConfig() {
    }
}
