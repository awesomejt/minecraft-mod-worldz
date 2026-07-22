package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:nether_start} typed preset (GOALS 27, DESIGN §31), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class NetherStartConfig {
    /** Target Y for the safe-site search (GOALS 27's "spawn-point safety"). */
    public int spawnY = NetherStartPlan.DEFAULT_SPAWN_Y;
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents (DESIGN §31.6): a full portal, ready to use. */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents: a full frame's worth of obsidian, no ignition. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents: no guaranteed obsidian at all. */
    public StarterKitConfig hardKit = hardDefaults();

    /** Creates a config populated with defaults. */
    public NetherStartConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:obsidian:10", "minecraft:flint_and_steel:1", "minecraft:bread:8");
        config.extras = list(
            "minecraft:golden_pickaxe:1", "minecraft:golden_sword:1", "minecraft:gold_ingot:8", "minecraft:torch:16"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig mediumDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:obsidian:10", "minecraft:bread:4");
        config.extras = list("minecraft:gold_ingot:4", "minecraft:torch:8", "minecraft:iron_sword:1");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2");
        config.extras = list("minecraft:gold_ingot:2", "minecraft:torch:4");
        config.extrasCount = 1;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
