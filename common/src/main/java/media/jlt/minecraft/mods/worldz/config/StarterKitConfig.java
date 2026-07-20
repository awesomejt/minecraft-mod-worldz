package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the ocean_island chest-boat starter kit (GOALS 03; DESIGN §25.3). Each entry is
 * {@code "<item id>"} or {@code "<item id>:<count>"} -- see {@code StarterKitPlan.ItemAmount}.
 */
public final class StarterKitConfig {
    /** Always-included items (GOALS 03's own named list: lily pad, dirt, grass block, saplings). */
    public List<String> essentials = defaultEssentials();
    /** Candidate items the random picks draw from. */
    public List<String> extras = defaultExtras();
    /** How many extras to pick, with replacement. */
    public int extrasCount = 2;

    /** Creates a config populated with defaults. */
    public StarterKitConfig() {
    }

    private static List<String> defaultEssentials() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:lily_pad:1");
        list.add("minecraft:dirt:4");
        list.add("minecraft:grass_block:2");
        list.add("minecraft:oak_sapling:3");
        return list;
    }

    private static List<String> defaultExtras() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:bread:3");
        list.add("minecraft:wooden_axe:1");
        list.add("minecraft:wooden_pickaxe:1");
        list.add("minecraft:torch:8");
        list.add("minecraft:water_bucket:1");
        return list;
    }
}
