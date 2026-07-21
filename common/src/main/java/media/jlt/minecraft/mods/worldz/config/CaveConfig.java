package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:cave} typed preset (GOALS 25-26, DESIGN §30), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class CaveConfig {
    /** Smallest sealed-surface Y accepted -- comfortably above bedrock, room for a real cave below it. */
    public static final int MIN_SEALED_SURFACE_Y = -32;

    /** Target Y for the underground spawn-cavity search (GOALS 25's "configurable depth"). */
    public int spawnDepthY = CavePlan.DEFAULT_SPAWN_DEPTH_Y;
    /** Whether a solid roof seals off sky access everywhere (GOALS 25). */
    public boolean sealedSurface = false;
    /** The roof's Y, meaningful only when {@link #sealedSurface} is set. */
    public int sealedSurfaceY = CavePlan.DEFAULT_SEALED_SURFACE_Y;
    /** Whether the mega-cavern (GOALS 26) is carved around spawn. */
    public boolean cavernEnabled = false;
    /** The mega-cavern's horizontal half-width. */
    public int cavernRadiusBlocks = CavePlan.DEFAULT_CAVERN_RADIUS_BLOCKS;
    /** The mega-cavern's vertical half-height. */
    public int cavernHeightBlocks = CavePlan.DEFAULT_CAVERN_HEIGHT_BLOCKS;
    /** Whether a starter chest is placed at spawn (GOALS 25's "optionally"). */
    public boolean chestEnabled = false;
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents. */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents. */
    public StarterKitConfig hardKit = hardDefaults();

    /** Creates a config populated with defaults. */
    public CaveConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:torch:32", "minecraft:bread:8", "minecraft:crafting_table:1");
        config.extras = list(
            "minecraft:wooden_pickaxe:1", "minecraft:iron_pickaxe:1", "minecraft:cobblestone:32", "minecraft:coal:16"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig mediumDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:torch:16", "minecraft:bread:4");
        config.extras = list("minecraft:wooden_pickaxe:1", "minecraft:cobblestone:16", "minecraft:coal:8");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:torch:8");
        config.extras = list("minecraft:bread:2", "minecraft:coal:4");
        config.extrasCount = 1;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
