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
    /** Generous starter-chest contents (DESIGN §27.8). */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents. */
    public StarterKitConfig hardKit = hardDefaults();

    /** Creates a config populated with defaults. */
    public SkyIslandConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:4", "minecraft:bread:8", "minecraft:crafting_table:1");
        config.extras = list(
            "minecraft:wooden_pickaxe:1", "minecraft:wooden_axe:1", "minecraft:torch:16", "minecraft:cobblestone:32"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig mediumDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:3", "minecraft:bread:4");
        config.extras = list("minecraft:wooden_pickaxe:1", "minecraft:torch:8", "minecraft:cobblestone:16");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:2");
        config.extras = list("minecraft:bread:2", "minecraft:torch:4");
        config.extrasCount = 1;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
