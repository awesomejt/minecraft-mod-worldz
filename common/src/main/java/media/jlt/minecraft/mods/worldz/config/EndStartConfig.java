package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:end_start} typed preset (GOALS 34, DESIGN §32), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class EndStartConfig {
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents (DESIGN §32.5): rockets, blocks, food, and combat gear. */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents: fewer rockets, lighter gear. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents: no guaranteed rockets or weapon at all. */
    public StarterKitConfig hardKit = hardDefaults();

    /** Creates a config populated with defaults. */
    public EndStartConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:firework_rocket:16", "minecraft:cobblestone:64", "minecraft:bread:8",
            "minecraft:bow:1", "minecraft:arrow:32", "minecraft:iron_sword:1"
        );
        config.extras = list(
            "minecraft:iron_chestplate:1", "minecraft:iron_helmet:1", "minecraft:golden_apple:2", "minecraft:ender_pearl:4"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig mediumDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:firework_rocket:8", "minecraft:cobblestone:32", "minecraft:bread:4", "minecraft:iron_sword:1"
        );
        config.extras = list("minecraft:arrow:16", "minecraft:bow:1", "minecraft:ender_pearl:2");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2");
        config.extras = list("minecraft:arrow:8", "minecraft:ender_pearl:1");
        config.extrasCount = 1;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
