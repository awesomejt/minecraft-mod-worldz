package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code jlt_worldz:flat} typed preset's resolved settings (GOAL 15, DESIGN §33.2): a classic
 * flat world built from an editable layer stack, with no noise/carving of any kind (mirrors
 * vanilla {@code FlatLevelSource}'s own zero-caves behavior, DESIGN §33.1's correction).
 * Everything except {@link #undergroundBiome}/{@link #undergroundBelowSurfaceBlocks} is persisted
 * entirely on {@code EnvelopedChunkGenerator}'s own codec (like {@code cave}/{@code netherStart}/
 * {@code endStart}), since {@code LimitedBiomeSource}'s own codec is already full -- those two
 * fields are also pushed onto {@code LimitedBiomeSource} directly via a mutable setter (GOAL 42,
 * DESIGN §37.3, mirroring {@code StackedPlan}'s own identical {@code setStackedLayers} precedent),
 * since reporting an underground biome genuinely does need per-column biome-source consultation.
 *
 * @param enabled whether the flat shape applies
 * @param layers ordered bottom-to-top layer stack, stacked starting at the dimension's own min Y
 * @param biome the single fixed biome reported everywhere above the underground band
 * @param decoration whether ordinary biome decoration (trees, ore veins, etc.) runs
 * @param structureOverrides structure-set ids eligible to place; empty means every registered set
 * @param undergroundBiome biome reported below {@code undergroundBelowSurfaceBlocks} blocks under
 *     the surface; blank disables the underground band entirely
 * @param undergroundBelowSurfaceBlocks how many blocks below the surface the underground band
 *     starts; the band never applies at {@code 0} even with a biome configured
 */
public record FlatPlan(
    boolean enabled,
    List<FlatLayerSpec> layers,
    String biome,
    boolean decoration,
    List<String> structureOverrides,
    String undergroundBiome,
    int undergroundBelowSurfaceBlocks
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
        if (undergroundBelowSurfaceBlocks < 0) {
            throw new IllegalArgumentException("Flat undergroundBelowSurfaceBlocks must not be negative.");
        }
        layers = List.copyOf(layers);
        structureOverrides = List.copyOf(structureOverrides == null ? List.of() : structureOverrides);
        undergroundBiome = undergroundBiome == null ? "" : undergroundBiome;
    }

    /**
     * Returns a plan with the flat shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static FlatPlan disabled() {
        return new FlatPlan(false, List.of(new FlatLayerSpec("minecraft:bedrock", 1)), "minecraft:plains", false, List.of(), "", 10);
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
        return new FlatPlan(
            true, layers, config.biome, config.decoration, List.copyOf(config.structureOverrides),
            config.undergroundBiome, config.undergroundBelowSurfaceBlocks
        );
    }

    /**
     * Reports whether the underground band is actually active (GOAL 42, DESIGN §37.3) -- both a
     * non-blank {@link #undergroundBiome} and a positive {@link #undergroundBelowSurfaceBlocks}
     * are required, matching {@code FlatConfig}'s own documented "0 blocks or a blank biome means
     * off" contract.
     *
     * @return {@code true} when the underground band applies
     */
    public boolean undergroundEnabled() {
        return !this.undergroundBiome.isBlank() && this.undergroundBelowSurfaceBlocks > 0;
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
