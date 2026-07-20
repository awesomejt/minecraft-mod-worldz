package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;

/**
 * Defaults for the {@code jlt_worldz:ocean_island} typed preset (GOALS 01, 02, 03, 04; DESIGN
 * §24, §25), consulted only when that preset resolves without explicit Customize-screen values.
 */
public final class OceanIslandConfig {
    /** How the land is sourced (DESIGN §25.1): artificial, natural (seed), or chest-boat/none. */
    public IslandSource islandSource = IslandSource.ARTIFICIAL;
    /** The one biome that fills the island's interior. */
    public String islandBiome = "minecraft:plains";
    /** Configured (unperturbed) island radius. */
    public int radiusBlocks = 128;
    /** Coastline perturbation strength. */
    public double shapeAmplitude = IslandShapeProfile.DEFAULT_AMPLITUDE;
    /** Width of the beach/stony-shore ring; also the terrain-height taper width. */
    public int shoreWidthBlocks = IslandPlan.DEFAULT_SHORE_WIDTH_BLOCKS;
    /** Width of the shallow ocean band immediately beyond the shore. */
    public int oceanShallowWidthBlocks = IslandPlan.DEFAULT_OCEAN_SHALLOW_WIDTH_BLOCKS;
    /** Width over which the seabed ramps from shallow to deep. */
    public int oceanDeepenWidthBlocks = IslandPlan.DEFAULT_OCEAN_DEEPEN_WIDTH_BLOCKS;
    /** Seabed depth below sea level in the shallow band. */
    public int oceanShallowDepthBlocks = IslandPlan.DEFAULT_OCEAN_SHALLOW_DEPTH_BLOCKS;
    /** Seabed depth below sea level once fully deep. */
    public int oceanDeepDepthBlocks = IslandPlan.DEFAULT_OCEAN_DEEP_DEPTH_BLOCKS;
    /** Grid-cell edge length for the ocean biome's per-region pick. */
    public int oceanRegionScaleBlocks = IslandPlan.DEFAULT_OCEAN_REGION_SCALE_BLOCKS;
    /** Whether island/ocean shaping releases beyond {@link #exclusionZoneRadiusBlocks} (GOALS 04). */
    public boolean exclusionZoneEnabled;
    /** Radius beyond which shaping releases, when enabled. */
    public int exclusionZoneRadiusBlocks = IslandPlan.DEFAULT_EXCLUSION_ZONE_RADIUS_BLOCKS;
    /** Chest-boat starter kit, consulted only when {@link #islandSource} is {@code CHEST_BOAT}. */
    public StarterKitConfig starterKit = new StarterKitConfig();

    /** Creates a config populated with defaults. */
    public OceanIslandConfig() {
    }
}
