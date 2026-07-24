package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:stacked} typed preset (GOAL 35, DESIGN §34.1), consulted
 * only when that preset resolves without explicit Customize-screen values. Each layer entry is
 * {@code "<biome>;<blocks>;<air gap>"} -- see {@code StackedLayerSpec}.
 */
public final class StackedConfig {
    /** Ordered bottom-to-top layer list: plains above desert above taiga, Jason's own example. */
    public List<String> layers = defaultLayers();
    /** Whether the configured layer order is shuffled, seeded off the real world seed. */
    public boolean seedRandomizedOrder = false;

    /** Creates a config populated with defaults. */
    public StackedConfig() {
    }

    private static List<String> defaultLayers() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:taiga;minecraft:bedrock:1,minecraft:stone:40,minecraft:podzol:2;6");
        list.add("minecraft:desert;minecraft:sandstone:20,minecraft:sand:3;6");
        list.add("minecraft:plains;minecraft:stone:20,minecraft:dirt:3,minecraft:grass_block:1;0");
        return list;
    }
}
