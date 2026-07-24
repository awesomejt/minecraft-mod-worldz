package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import media.jlt.minecraft.mods.worldz.config.StackedConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The {@code jlt_worldz:stacked} typed preset's resolved settings (GOAL 35, DESIGN §34.1): a
 * limited-size Overworld whose underground is replaced by stacked horizontal biome layers instead
 * of normal caves. Like {@code flat}/{@code deep_flat}/{@code cave}, this plan is never read from
 * {@code LimitedBiomeSource}'s own codec (already full 14/14 fields) -- persisted entirely on
 * {@code EnvelopedChunkGenerator}'s own codec instead. Unlike them, {@code LimitedBiomeSource}
 * still needs *live* (non-codec) access to this plan at runtime, since per-Y biome reporting is
 * what makes real vanilla decoration/ore placement work correctly per layer (DESIGN §34.3).
 *
 * @param enabled whether the stacked shape applies
 * @param layers ordered bottom-to-top layer stack, stacked starting at the dimension's own min Y
 * @param seedRandomizedOrder whether {@link #resolvedLayers} shuffles {@link #layers}, seeded off
 *     the real world seed, instead of using the configured order as-is
 */
public record StackedPlan(boolean enabled, List<StackedLayerSpec> layers, boolean seedRandomizedOrder) {
    /** Validates persisted values even while the stacked shape is disabled. */
    public StackedPlan {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("Stacked layer list must not be empty.");
        }
        // Order-independent (a sum), so this validates the configured list directly rather than
        // a resolvedLayers(seed) result -- mirrors FlatPlan's own MAX_TOTAL_HEIGHT_BLOCKS check.
        int totalHeight = layers.stream().mapToInt(StackedLayerSpec::totalHeightBlocks).sum();
        if (totalHeight > FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS) {
            throw new IllegalArgumentException(
                "Stacked layer heights sum to " + totalHeight + ", more than " + FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS + "."
            );
        }
        layers = List.copyOf(layers);
    }

    /**
     * Returns a plan with the stacked shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static StackedPlan disabled() {
        return new StackedPlan(
            false,
            List.of(new StackedLayerSpec("minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 1)), 0)),
            false
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized stacked configuration
     * @return resolved, enabled plan
     */
    public static StackedPlan fromConfig(StackedConfig config) {
        List<StackedLayerSpec> layers = new ArrayList<>();
        for (String raw : config.layers) {
            layers.add(StackedLayerSpec.parse(raw));
        }
        return new StackedPlan(true, layers, config.seedRandomizedOrder);
    }

    /**
     * Returns {@link #layers} in the order actual generation should use: unchanged, or shuffled
     * deterministically from the real Minecraft world seed when {@link #seedRandomizedOrder} is
     * set -- mirrors this project's established "reproducible from the real seed" determinism
     * expectation (e.g. {@code ChunkIslandPlan}'s own hash-grid, DESIGN §34.1) rather than a
     * per-session random order.
     *
     * @param seed the real Minecraft world seed
     * @return bottom-to-top layer order to actually generate
     */
    public List<StackedLayerSpec> resolvedLayers(long seed) {
        if (!this.seedRandomizedOrder) {
            return this.layers;
        }
        List<StackedLayerSpec> shuffled = new ArrayList<>(this.layers);
        Collections.shuffle(shuffled, new Random(seed));
        return List.copyOf(shuffled);
    }

    /**
     * Returns the total stack height for a given resolved layer order: the sum of every layer's
     * own block-stack thickness plus air gap.
     *
     * @param resolvedLayers layer order from {@link #resolvedLayers}
     * @return summed height in blocks
     */
    public static int totalHeightBlocks(List<StackedLayerSpec> resolvedLayers) {
        return resolvedLayers.stream().mapToInt(StackedLayerSpec::totalHeightBlocks).sum();
    }

    /**
     * Returns the layer covering a given block Y, walking {@code resolvedLayers} bottom to top
     * starting at the dimension's own min Y (DESIGN §34.1/§34.3) -- covers the whole vertical span
     * between a layer's floor and the next layer's floor, air gap included, so decoration running
     * anywhere inside a layer's headroom still reports that layer's own biome. Out-of-range Y
     * (below the stack or above its top) clamps to the nearest end layer rather than throwing --
     * harmless, since nothing meaningful generates there.
     *
     * @param resolvedLayers layer order from {@link #resolvedLayers}
     * @param blockY absolute block Y
     * @return the covering layer
     */
    public static StackedLayerSpec layerAt(List<StackedLayerSpec> resolvedLayers, int blockY) {
        int relativeY = blockY - FlatConfig.OVERWORLD_MIN_Y;
        if (relativeY < 0) {
            return resolvedLayers.get(0);
        }
        int cursor = 0;
        for (StackedLayerSpec layer : resolvedLayers) {
            int height = layer.totalHeightBlocks();
            if (relativeY < cursor + height) {
                return layer;
            }
            cursor += height;
        }
        return resolvedLayers.get(resolvedLayers.size() - 1);
    }
}
