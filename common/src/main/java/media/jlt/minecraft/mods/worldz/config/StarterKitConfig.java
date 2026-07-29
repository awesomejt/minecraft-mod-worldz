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

    /** Names a {@code kits} library entry supplying this kit's contents (DESIGN §44.3.2); {@code
     * null} means the {@link #essentials}/{@link #extras}/{@link #extrasCount} above are the
     * definition. A blank or unknown name warns and falls back to this site's own shipped default
     * kit ({@code Rule.KitReference}, TODO 25.8b). {@code null}, not {@code ""}, distinguishes
     * "inline" from "referenced" -- a blank string must remain a nameable (and therefore
     * diagnosable) state, not silently mean "inline with constructor defaults". Deliberately not a
     * {@code Setting} of its own -- it is never a YAML key. */
    public String ref = null;

    /** Creates a config populated with defaults. */
    public StarterKitConfig() {
    }

    /** Creates a reference-only kit: no inline contents, resolved from the {@code kits} library at
     * sanitize time (DESIGN §44.3.4). */
    public static StarterKitConfig reference(String name) {
        StarterKitConfig config = new StarterKitConfig();
        config.ref = name;
        config.essentials = new ArrayList<>();
        config.extras = new ArrayList<>();
        config.extrasCount = 0;
        return config;
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
