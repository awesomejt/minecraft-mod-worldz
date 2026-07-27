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
 * @param reliefBlocks maximum per-column height bump applied to each layer's own surface (DESIGN
 *     §34.7), traded out of that layer's own air gap so a layer's cumulative-height zone boundary
 *     -- and therefore {@link #layerAt}'s Y-only biome lookup -- never moves; zero restores the
 *     original perfectly flat layers
 */
public record StackedPlan(boolean enabled, List<StackedLayerSpec> layers, boolean seedRandomizedOrder, int reliefBlocks) {
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
        if (reliefBlocks < 0) {
            throw new IllegalArgumentException("Stacked relief must not be negative.");
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
            false,
            0
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
        return new StackedPlan(true, layers, config.seedRandomizedOrder, config.reliefBlocks);
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

    /**
     * Returns a deterministic per-column height bump for one layer's own surface (DESIGN §34.7,
     * GOAL 35's "terrain should not just be flat"), in {@code [0, maxReliefBlocks]}. Bumps only
     * (never dips). Deliberately its own small seeded hash rather than vanilla's {@code
     * RandomState}-based noise (mirrors {@link ChunkIslandPlan}'s own {@code hash01}/{@code
     * splitmix64} precedent): both {@link
     * media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator}'s block-painting side and
     * {@link media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource}'s biome-per-Y lookup
     * need to compute the identical value from just the world seed, but only the former has a
     * {@code RandomState} available.
     *
     * @param seed the real Minecraft world seed
     * @param layerIndex the layer's own index in {@code resolvedLayers} (salts the hash so every
     *     layer's bump pattern differs, even at the same X/Z)
     * @param blockX absolute block X
     * @param blockZ absolute block Z
     * @param maxReliefBlocks the configured {@link #reliefBlocks} ceiling
     * @return the column's own height bump for this layer, zero when {@code maxReliefBlocks <= 0}
     */
    public static int reliefBlocksAt(long seed, int layerIndex, int blockX, int blockZ, int maxReliefBlocks) {
        if (maxReliefBlocks <= 0) {
            return 0;
        }
        return (int) Math.round(reliefNoise01(seed, layerIndex, blockX, blockZ) * maxReliefBlocks);
    }

    /** One lattice cell's own span for {@link #reliefNoise01}'s broad, hill-scale octave. */
    private static final int RELIEF_BASE_CELL_BLOCKS = 16;
    /** One lattice cell's own span for {@link #reliefNoise01}'s finer detail octave. */
    private static final int RELIEF_DETAIL_CELL_BLOCKS = 4;
    private static final double RELIEF_BASE_WEIGHT = 0.7;
    private static final double RELIEF_DETAIL_WEIGHT = 0.3;

    /**
     * Two-octave value noise in {@code [0, 1)} (found and fixed 2026-07-26, Jason's real config
     * 72-75 playtest): the original single-cell version sampled one independent hash per whole
     * 4-block cell with no blending between cells at all, producing a hard-edged "waffle" of flat
     * plateaus rather than the "gentle undulation" this method's own contract always promised.
     * Each octave now bilinearly interpolates between its own lattice corners (see {@link
     * #latticeValueAt}), and the two octaves are summed at weights that keep the total in {@code
     * [0, 1)} (0.7 + 0.3 = 1.0): a broad 16-block-cell octave for the overall rolling shape, plus a
     * finer 4-block-cell octave layered on top purely for small-scale variation -- one octave alone
     * still reads as a repetitive field of same-size, same-shape bumps ("egg carton" noise); a
     * second, smaller-scale layer is what actually breaks that regularity up.
     */
    private static double reliefNoise01(long seed, int layerIndex, int blockX, int blockZ) {
        double base = latticeValueAt(seed, layerIndex, "base", blockX, blockZ, RELIEF_BASE_CELL_BLOCKS);
        double detail = latticeValueAt(seed, layerIndex, "detail", blockX, blockZ, RELIEF_DETAIL_CELL_BLOCKS);
        return RELIEF_BASE_WEIGHT * base + RELIEF_DETAIL_WEIGHT * detail;
    }

    /**
     * Bilinearly interpolates a hash sampled at the four lattice corners surrounding
     * {@code (blockX, blockZ)}, spaced {@code cellSizeBlocks} apart -- classic value noise. Faded
     * with the Perlin ease curve ({@code 6t^5-15t^4+10t^3}) rather than a plain linear blend so the
     * *slope* is continuous too, not just the height; a linear blend still creases visibly at every
     * lattice line.
     */
    private static double latticeValueAt(long seed, int layerIndex, String octave, int blockX, int blockZ, int cellSizeBlocks) {
        int cellX = Math.floorDiv(blockX, cellSizeBlocks);
        int cellZ = Math.floorDiv(blockZ, cellSizeBlocks);
        double fx = fade((blockX - cellX * (long) cellSizeBlocks) / (double) cellSizeBlocks);
        double fz = fade((blockZ - cellZ * (long) cellSizeBlocks) / (double) cellSizeBlocks);
        double h00 = latticeHash01(seed, layerIndex, octave, cellX, cellZ);
        double h10 = latticeHash01(seed, layerIndex, octave, cellX + 1, cellZ);
        double h01 = latticeHash01(seed, layerIndex, octave, cellX, cellZ + 1);
        double h11 = latticeHash01(seed, layerIndex, octave, cellX + 1, cellZ + 1);
        return lerp(lerp(h00, h10, fx), lerp(h01, h11, fx), fz);
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double latticeHash01(long seed, int layerIndex, String octave, int cellX, int cellZ) {
        return hash01(seed, "stacked_relief_" + octave, (((long) layerIndex) << 32) | (cellX & 0xFFFFFFFFL), cellZ);
    }

    private static double hash01(long seed, String salt, long a, long b) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt.hashCode());
        h = splitmix64(h ^ a);
        h = splitmix64(h ^ b);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
