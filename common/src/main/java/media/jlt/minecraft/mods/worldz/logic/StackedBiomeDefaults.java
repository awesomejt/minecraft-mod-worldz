package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standard per-biome block composition and air gap for {@code jlt_worldz:stacked}'s simplified
 * layer shorthand (DESIGN §34.8, requested by Jason 2026-07-24): a bare biome id (no
 * {@code ;blocks;air gap} at all) instead of the full {@link StackedLayerSpec} shorthand, so a
 * layer list can be written as just an ordered list of biomes when the player doesn't want to
 * hand-tune each band's own material stack. Deliberately not stack-position-aware (no biome entry
 * here includes bedrock or unusual thickness for "being the bottom layer") -- that is still an
 * explicit, full-shorthand choice the player makes for whichever entry they place there, exactly
 * like {@code StackedConfig}'s own shipped default does for its taiga bottom layer.
 */
final class StackedBiomeDefaults {
    /** Air gap applied to a simplified (biome-only) layer entry, matching this preset's own
     * default minimum air gap (DESIGN §34.7). */
    static final int DEFAULT_AIR_GAP_BLOCKS = 30;

    private static final List<FlatLayerSpec> GENERIC = blocks("minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1");

    private static final Map<String, List<FlatLayerSpec>> BY_BIOME = Map.ofEntries(
        Map.entry("minecraft:plains", GENERIC),
        Map.entry("minecraft:sunflower_plains", GENERIC),
        Map.entry("minecraft:meadow", GENERIC),
        Map.entry("minecraft:forest", GENERIC),
        Map.entry("minecraft:birch_forest", GENERIC),
        Map.entry("minecraft:flower_forest", GENERIC),
        Map.entry("minecraft:cherry_grove", GENERIC),
        Map.entry("minecraft:dark_forest", blocks("minecraft:stone:5,minecraft:dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:desert", blocks("minecraft:sandstone:7,minecraft:sand:3")),
        Map.entry("minecraft:badlands", blocks("minecraft:stone:4,minecraft:terracotta:5,minecraft:red_sand:1")),
        Map.entry("minecraft:eroded_badlands", blocks("minecraft:stone:4,minecraft:terracotta:5,minecraft:red_sand:1")),
        Map.entry("minecraft:wooded_badlands", blocks("minecraft:stone:4,minecraft:terracotta:5,minecraft:coarse_dirt:1")),
        Map.entry("minecraft:swamp", blocks("minecraft:stone:5,minecraft:mud:4,minecraft:grass_block:1")),
        Map.entry("minecraft:mangrove_swamp", blocks("minecraft:stone:5,minecraft:mud:4,minecraft:moss_block:1")),
        Map.entry("minecraft:jungle", blocks("minecraft:stone:5,minecraft:dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:bamboo_jungle", blocks("minecraft:stone:5,minecraft:dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:sparse_jungle", blocks("minecraft:stone:5,minecraft:dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:savanna", blocks("minecraft:stone:5,minecraft:coarse_dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:savanna_plateau", blocks("minecraft:stone:5,minecraft:coarse_dirt:4,minecraft:grass_block:1")),
        Map.entry("minecraft:windswept_savanna", blocks("minecraft:stone:6,minecraft:coarse_dirt:3,minecraft:grass_block:1")),
        Map.entry("minecraft:taiga", blocks("minecraft:stone:7,minecraft:podzol:2,minecraft:grass_block:1")),
        Map.entry("minecraft:old_growth_pine_taiga", blocks("minecraft:stone:7,minecraft:podzol:2,minecraft:grass_block:1")),
        Map.entry("minecraft:old_growth_spruce_taiga", blocks("minecraft:stone:7,minecraft:podzol:2,minecraft:grass_block:1")),
        Map.entry("minecraft:snowy_taiga", blocks("minecraft:stone:5,minecraft:dirt:3,minecraft:podzol:1,minecraft:snow_block:1")),
        Map.entry("minecraft:snowy_plains", blocks("minecraft:stone:6,minecraft:dirt:3,minecraft:snow_block:1")),
        Map.entry("minecraft:ice_spikes", blocks("minecraft:stone:6,minecraft:packed_ice:3,minecraft:snow_block:1")),
        Map.entry("minecraft:grove", blocks("minecraft:stone:6,minecraft:dirt:3,minecraft:snow_block:1")),
        Map.entry("minecraft:snowy_slopes", blocks("minecraft:stone:7,minecraft:packed_ice:2,minecraft:snow_block:1")),
        Map.entry("minecraft:frozen_peaks", blocks("minecraft:stone:7,minecraft:packed_ice:2,minecraft:snow_block:1")),
        Map.entry("minecraft:jagged_peaks", blocks("minecraft:stone:7,minecraft:packed_ice:2,minecraft:snow_block:1")),
        Map.entry("minecraft:windswept_hills", blocks("minecraft:stone:8,minecraft:dirt:1,minecraft:grass_block:1")),
        Map.entry("minecraft:windswept_forest", blocks("minecraft:stone:7,minecraft:gravel:2,minecraft:grass_block:1")),
        Map.entry("minecraft:windswept_gravelly_hills", blocks("minecraft:stone:7,minecraft:gravel:2,minecraft:grass_block:1")),
        Map.entry("minecraft:stony_shore", blocks("minecraft:stone:9,minecraft:gravel:1")),
        Map.entry("minecraft:stony_peaks", blocks("minecraft:stone:9,minecraft:gravel:1")),
        Map.entry("minecraft:mushroom_fields", blocks("minecraft:stone:6,minecraft:dirt:3,minecraft:mycelium:1"))
    );

    private StackedBiomeDefaults() {
    }

    /**
     * Returns the standard block composition for a simplified (biome-only) layer entry --
     * {@link #GENERIC} for any biome id not specifically tuned above, so any biome id still works
     * rather than failing to parse.
     *
     * @param biomeId the layer's biome id, namespace-optional (bare {@code "taiga"} matches
     *     {@code "minecraft:taiga"}, mirroring vanilla's own implicit-namespace convention)
     * @return the biome's standard bottom-to-top block stack
     */
    static List<FlatLayerSpec> blocksFor(String biomeId) {
        String normalized = biomeId.contains(":") ? biomeId : "minecraft:" + biomeId;
        return BY_BIOME.getOrDefault(normalized, GENERIC);
    }

    private static List<FlatLayerSpec> blocks(String shorthand) {
        List<FlatLayerSpec> result = new ArrayList<>();
        for (String entry : shorthand.split(",")) {
            result.add(FlatLayerSpec.parse(entry));
        }
        return List.copyOf(result);
    }
}
