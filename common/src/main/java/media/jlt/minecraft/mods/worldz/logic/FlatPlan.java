package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code jlt_worldz:flat} typed preset's resolved settings (GOAL 15, DESIGN §33.2): a classic
 * flat world built from an editable layer stack, with no noise/carving of any kind (mirrors
 * vanilla {@code FlatLevelSource}'s own zero-caves behavior, DESIGN §33.1's correction). Like
 * {@code cave}/{@code netherStart}/{@code endStart}, this plan is never read from {@code
 * LimitedBiomeSource} -- persisted entirely on {@code EnvelopedChunkGenerator}'s own codec
 * instead, since {@code LimitedBiomeSource}'s own codec is already full and none of this needs
 * per-column biome-source consultation.
 *
 * @param enabled whether the flat shape applies
 * @param layers ordered bottom-to-top layer stack, stacked starting at the dimension's own min Y
 * @param biome the single fixed biome reported everywhere
 * @param decoration whether ordinary biome decoration (trees, ore veins, etc.) runs
 * @param structureOverrides structure-set ids eligible to place; empty means every registered set
 */
public record FlatPlan(
    boolean enabled,
    List<FlatLayerSpec> layers,
    String biome,
    boolean decoration,
    List<String> structureOverrides
) {
    /** Validates persisted values even while the flat shape is disabled. */
    public FlatPlan {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("Flat layer list must not be empty.");
        }
        int totalHeight = layers.stream().mapToInt(FlatLayerSpec::heightBlocks).sum();
        if (totalHeight > FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS) {
            throw new IllegalArgumentException(
                "Flat layer heights sum to " + totalHeight + ", more than " + FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS + "."
            );
        }
        if (biome == null || biome.isBlank()) {
            throw new IllegalArgumentException("Flat biome is required.");
        }
        layers = List.copyOf(layers);
        structureOverrides = List.copyOf(structureOverrides == null ? List.of() : structureOverrides);
    }

    /**
     * Returns a plan with the flat shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static FlatPlan disabled() {
        return new FlatPlan(false, List.of(new FlatLayerSpec("minecraft:bedrock", 1)), "minecraft:plains", false, List.of());
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized flat configuration
     * @return resolved, enabled plan
     */
    public static FlatPlan fromConfig(FlatConfig config) {
        List<FlatLayerSpec> layers = new ArrayList<>();
        for (String raw : config.layers) {
            layers.add(FlatLayerSpec.parse(raw));
        }
        return new FlatPlan(true, layers, config.biome, config.decoration, List.copyOf(config.structureOverrides));
    }

    /**
     * Returns the total layer stack height, i.e. how far above the dimension's min Y the flat
     * surface sits.
     *
     * @return summed layer height in blocks
     */
    public int totalHeightBlocks() {
        return this.layers.stream().mapToInt(FlatLayerSpec::heightBlocks).sum();
    }
}
