package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:flat} typed preset (GOAL 15, DESIGN §33.2), consulted only
 * when that preset resolves without explicit Customize-screen values. Each layer entry is
 * {@code "<block id>"} or {@code "<block id>:<height>"} -- see {@code FlatLayerSpec}.
 */
public final class FlatConfig {
    /** Every Overworld dimension in this project starts layer 0 at this Y (matches {@code
     * NoiseGeneratorSettings.overworld()}'s own {@code min_y}). */
    public static final int OVERWORLD_MIN_Y = -64;
    /** Total build height available to a layer stack (matches the Overworld's own {@code height}). */
    public static final int MAX_TOTAL_HEIGHT_BLOCKS = 384;

    /**
     * Ordered bottom-to-top layer list. Deliberately does not mirror vanilla's own
     * {@code classic_flat} preset numbers (bedrock+2 dirt+grass, a 4-block stack that lands at
     * the very bottom of the modern -64..320 build range, a well-known vanilla quirk never
     * rebalanced after the 1.18 height change) -- reaches a comfortable Y 64 surface instead, a
     * deliberate Worldz-native baseline. Players who want the traditional look can still paste it
     * in via the layer editor's own text import (a "classic" example is documented in README).
     */
    public List<String> layers = defaultLayers();
    /** Single fixed biome for the whole world. */
    public String biome = "minecraft:plains";
    /** Whether ordinary biome decoration (trees, flowers, ore veins, etc.) runs. */
    public boolean decoration = false;
    /** Structure sets eligible to place; empty means every registered set is eligible. */
    public List<String> structureOverrides = defaultStructureOverrides();
    /** Biome reported at/below {@link #undergroundBelowSurfaceBlocks} blocks under the surface
     * (GOAL 42, DESIGN §37.3); empty disables the underground band entirely (default), keeping
     * {@code biome} reported at every depth as before this field existed. */
    public String undergroundBiome = "";
    /** How many blocks below the flat surface the underground band starts; only takes effect
     * when {@link #undergroundBiome} is also set. Ignored (and the band never applies) at
     * {@code 0} even with a biome configured. */
    public int undergroundBelowSurfaceBlocks = 10;

    /** Creates a config populated with defaults. */
    public FlatConfig() {
    }

    private static List<String> defaultLayers() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:bedrock:1");
        list.add("minecraft:stone:123");
        list.add("minecraft:dirt:3");
        list.add("minecraft:grass_block:1");
        return list;
    }

    private static List<String> defaultStructureOverrides() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:villages");
        list.add("minecraft:strongholds");
        return list;
    }
}
