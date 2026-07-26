package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.LightSource;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:end_start} typed preset (GOALS 34, DESIGN §32), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class EndStartConfig {
    /**
     * Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. Every
     * tier guarantees a pickaxe (2026-07-25 follow-up, Jason's in-game retest of configs 63-65:
     * "mainly need a pickaxe to break out of the starting box" -- End Stone requires a pickaxe to
     * drop at all, confirmed against the real {@code BlockBehaviour.Properties.
     * requiresCorrectToolForDrops()} flag on vanilla's own {@code Blocks.END_STONE}, so hand-mining
     * it (the README/GOALS wording this project shipped with) never actually worked), escalating
     * by tier same as the rest of each kit's gear (copper/stone/wooden, easy to hard) -- Jason's
     * own choice over a single shared tier, mirroring Nether-start's own escalating-gear pattern.
     */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents (DESIGN §32.5): rockets, blocks, food, combat gear, and a copper pickaxe. */
    public StarterKitConfig easyKit = easyDefaults();
    /** Middle-ground starter-chest contents: fewer rockets, lighter gear, a stone pickaxe. */
    public StarterKitConfig mediumKit = mediumDefaults();
    /** Bare-essentials starter-chest contents: no guaranteed rockets or weapon, just a wooden pickaxe. */
    public StarterKitConfig hardKit = hardDefaults();
    /**
     * The guaranteed platform's shape and lighting (GOALS 34/41, generalized to {@code end_start}
     * 2026-07-25 after Jason's first in-game test of config 63 found the original fixed
     * 3x3-footprint/1x1-interior platform "too small" and asked for the Nether-start capsule
     * mechanism here too).
     */
    public StarterCapsuleConfig capsule = capsuleDefaults();

    /** Creates a config populated with defaults. */
    public EndStartConfig() {
    }

    private static StarterKitConfig easyDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:firework_rocket:16", "minecraft:cobblestone:64", "minecraft:bread:8",
            "minecraft:bow:1", "minecraft:arrow:32", "minecraft:iron_sword:1", "minecraft:copper_pickaxe:1"
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
            "minecraft:firework_rocket:8", "minecraft:cobblestone:32", "minecraft:bread:4",
            "minecraft:iron_sword:1", "minecraft:stone_pickaxe:1"
        );
        config.extras = list("minecraft:arrow:16", "minecraft:bow:1", "minecraft:ender_pearl:2");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig hardDefaults() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2", "minecraft:wooden_pickaxe:1");
        config.extras = list("minecraft:arrow:8", "minecraft:ender_pearl:1");
        config.extrasCount = 1;
        return config;
    }

    private static StarterCapsuleConfig capsuleDefaults() {
        StarterCapsuleConfig config = new StarterCapsuleConfig();
        config.lightSource = LightSource.GLOWSTONE;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
