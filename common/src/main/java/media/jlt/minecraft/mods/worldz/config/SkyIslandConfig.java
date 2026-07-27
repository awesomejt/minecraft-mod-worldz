package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;

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
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /**
     * Whether the Nether is also a sky island (GOALS 06), reusing this same radius/shapeAmplitude/
     * surfaceY/thicknessBlocks shape -- one corridor-width-style toggle, not independently
     * configurable Nether dimensions (DESIGN §27.6, mirroring {@code StripConfig.applyToNether}).
     */
    public boolean applyToNether = false;
    /**
     * Whether a biome-pinning buffer surrounds the starter island before real-seed biomes resume
     * beyond it (DESIGN §27.10, 2026-07-21 follow-up from Jason's in-game testing). On by default:
     * without it, the void immediately past the island's own tiny footprint reports whatever the
     * real seed's noise says there, with no buffer at all.
     */
    public boolean exclusionZoneEnabled = true;
    /** Buffer radius in blocks beyond the island's own edge, when {@link #exclusionZoneEnabled}. */
    public int exclusionZoneRadiusBlocks = 128;
    /** Biome reported at/below {@link #undergroundBelowSurfaceBlocks} blocks under {@link
     * #surfaceY}, within the island's own footprint (GOAL 42, DESIGN §37.3); empty disables the
     * underground band entirely (default), keeping {@link #islandBiome} reported at every depth. */
    public String undergroundBiome = "";
    /** How many blocks below {@link #surfaceY} the underground band starts; only takes effect
     * when {@link #undergroundBiome} is also set. Ignored (band never applies) at {@code 0}. */
    public int undergroundBelowSurfaceBlocks = 10;
    /** Generous starter-chest contents (DESIGN §27.8). */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents. */
    public StarterKitConfig hardKit = hardDefaults();
    /** Scattered floating islands beyond the starter island's own footprint (GOALS 07-08, DESIGN §28). */
    public FloatingIslandsConfig floatingIslands = new FloatingIslandsConfig();

    /** Creates a config populated with defaults. */
    public SkyIslandConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:oak_sapling:4", "minecraft:bread:8", "minecraft:crafting_table:1", "minecraft:lava_bucket:1"
        );
        config.extras = list(
            "minecraft:wooden_pickaxe:1", "minecraft:wooden_axe:1", "minecraft:torch:16", "minecraft:cobblestone:32"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig mediumDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:3", "minecraft:bread:4", "minecraft:lava_bucket:1");
        config.extras = list("minecraft:wooden_pickaxe:1", "minecraft:torch:8", "minecraft:cobblestone:16");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:2", "minecraft:lava_bucket:1");
        config.extras = list("minecraft:bread:2", "minecraft:torch:4");
        config.extrasCount = 1;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
