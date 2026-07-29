package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The 14 shipped {@code kits} library entries (DESIGN §44.4.1, §44.5, TODO 25.8b): named, reusable
 * starter-kit definitions every preset's chest kit can either name or still define inline. Each
 * private factory below is a <strong>verbatim</strong> copy (not retyped or re-derived) of the
 * private {@code *Defaults()} method it replaces -- {@code CaveConfig.easyDefaults/mediumDefaults/
 * hardDefaults}, the same trio in {@code SkyIslandConfig}/{@code NetherStartConfig}/{@code
 * EndStartConfig}, and {@code FloatingIslandsConfig.lootKitDefaults} -- so behavior stays
 * byte-identical. {@code ocean-island-default} is the sole exception: it is not a moved factory
 * method at all, but the existing no-arg {@link StarterKitConfig} constructor's own defaults
 * (DESIGN §44.3.2/§44.4.1), load-bearing for partial-inline kits.
 *
 * <p>Those 14 original private factory methods stay in place, still called by their still-inline
 * sites (TODO 25.8b, DESIGN §44.8 row b) -- deleting them is TODO 25.8c/25.8d's job, once each site
 * actually converts to {@code StarterKitSchema.reference(...)}.
 */
public final class KitLibrary {
    private KitLibrary() {
    }

    /**
     * Builds a fresh library: a new {@link LinkedHashMap} of 14 brand-new {@link StarterKitConfig}
     * instances on every call, so mutation from sanitizing one config load's {@code kits} map can
     * never leak into another's (DESIGN R11) -- there is no shared static prototype instance
     * anywhere here for two calls to alias.
     *
     * @return the 14 shipped kits, keyed by name (DESIGN §44.5)
     */
    public static LinkedHashMap<String, StarterKitConfig> shipped() {
        LinkedHashMap<String, StarterKitConfig> kits = new LinkedHashMap<>();
        kits.put("cave-easy", caveEasy());
        kits.put("cave-medium", caveMedium());
        kits.put("cave-hard", caveHard());
        kits.put("sky-island-easy", skyIslandEasy());
        kits.put("sky-island-medium", skyIslandMedium());
        kits.put("sky-island-hard", skyIslandHard());
        kits.put("nether-start-easy", netherStartEasy());
        kits.put("nether-start-medium", netherStartMedium());
        kits.put("nether-start-hard", netherStartHard());
        kits.put("end-start-easy", endStartEasy());
        kits.put("end-start-medium", endStartMedium());
        kits.put("end-start-hard", endStartHard());
        kits.put("ocean-island-default", new StarterKitConfig());
        kits.put("floating-islands-loot", floatingIslandsLoot());
        return kits;
    }

    // -- CaveConfig.easyDefaults/mediumDefaults/hardDefaults, verbatim (CaveConfig.java:49-83) --

    private static StarterKitConfig caveEasy() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:oak_log:4", "minecraft:bread:6", "minecraft:wheat_seeds:4",
            "minecraft:oak_sapling:3", "minecraft:torch:16", "minecraft:dirt:8"
        );
        config.extras = list("minecraft:cobblestone:16", "minecraft:coal:8");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig caveMedium() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:torch:8", "minecraft:oak_log:2", "minecraft:dirt:4");
        config.extras = list("minecraft:coal:4");
        config.extrasCount = 1;
        return config;
    }

    private static StarterKitConfig caveHard() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:torch:1", "minecraft:wooden_pickaxe:1");
        config.extras = list();
        config.extrasCount = 0;
        return config;
    }

    // -- SkyIslandConfig.easyDefaults/mediumDefaults/hardDefaults, verbatim (SkyIslandConfig.java:62-88) --

    private static StarterKitConfig skyIslandEasy() {
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

    private static StarterKitConfig skyIslandMedium() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:3", "minecraft:bread:4", "minecraft:lava_bucket:1");
        config.extras = list("minecraft:wooden_pickaxe:1", "minecraft:torch:8", "minecraft:cobblestone:16");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig skyIslandHard() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:oak_sapling:2", "minecraft:lava_bucket:1");
        config.extras = list("minecraft:bread:2", "minecraft:torch:4");
        config.extrasCount = 1;
        return config;
    }

    // -- NetherStartConfig.easyDefaults/mediumDefaults/hardDefaults, verbatim (NetherStartConfig.java:37-63) --

    private static StarterKitConfig netherStartEasy() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:obsidian:10", "minecraft:flint_and_steel:1", "minecraft:bread:8", "minecraft:wooden_pickaxe:1"
        );
        config.extras = list(
            "minecraft:golden_pickaxe:1", "minecraft:golden_sword:1", "minecraft:gold_ingot:8", "minecraft:torch:16"
        );
        config.extrasCount = 3;
        return config;
    }

    private static StarterKitConfig netherStartMedium() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:obsidian:10", "minecraft:bread:4", "minecraft:wooden_pickaxe:1");
        config.extras = list("minecraft:gold_ingot:4", "minecraft:torch:8", "minecraft:iron_sword:1");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig netherStartHard() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2", "minecraft:wooden_pickaxe:1");
        config.extras = list("minecraft:gold_ingot:2", "minecraft:torch:4");
        config.extrasCount = 1;
        return config;
    }

    // -- EndStartConfig.easyDefaults/mediumDefaults/hardDefaults, verbatim (EndStartConfig.java:43-73) --

    private static StarterKitConfig endStartEasy() {
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

    private static StarterKitConfig endStartMedium() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list(
            "minecraft:firework_rocket:8", "minecraft:cobblestone:32", "minecraft:bread:4",
            "minecraft:iron_sword:1", "minecraft:stone_pickaxe:1"
        );
        config.extras = list("minecraft:arrow:16", "minecraft:bow:1", "minecraft:ender_pearl:2");
        config.extrasCount = 2;
        return config;
    }

    private static StarterKitConfig endStartHard() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2", "minecraft:wooden_pickaxe:1");
        config.extras = list("minecraft:arrow:8", "minecraft:ender_pearl:1");
        config.extrasCount = 1;
        return config;
    }

    // -- FloatingIslandsConfig.lootKitDefaults, verbatim (FloatingIslandsConfig.java:72-80) --

    private static StarterKitConfig floatingIslandsLoot() {
        StarterKitConfig config = new StarterKitConfig();
        config.essentials = list("minecraft:bread:2");
        config.extras = list(
            "minecraft:iron_ingot:2", "minecraft:emerald:1", "minecraft:arrow:8", "minecraft:golden_apple:1", "minecraft:ender_pearl:1"
        );
        config.extrasCount = 2;
        return config;
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
