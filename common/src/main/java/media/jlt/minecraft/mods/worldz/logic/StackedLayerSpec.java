package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * One horizontal band in a {@code jlt_worldz:stacked} layer stack (GOAL 35, DESIGN §34.1): a
 * reported biome (feature-list source only, not a block-choice input -- see DESIGN §34.3), a
 * bottom-to-top block stack reusing {@link FlatLayerSpec} exactly as {@code FlatPlan.layers}
 * already does, and an air gap above the block stack sized for that biome's own vegetation to
 * grow into (GOALS 35's own "tall trees need real headroom" wording).
 *
 * @param biome the layer's reported biome id
 * @param blocks ordered bottom-to-top block stack for this layer only
 * @param airGapBlocks open space above {@link #blocks}, before the next layer begins
 */
public record StackedLayerSpec(String biome, List<FlatLayerSpec> blocks, int airGapBlocks) {
    /** Validates the entry. */
    public StackedLayerSpec {
        if (biome == null || biome.isBlank()) {
            throw new IllegalArgumentException("Layer biome is required.");
        }
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("Layer block stack must not be empty.");
        }
        if (airGapBlocks < 0) {
            throw new IllegalArgumentException("Layer air gap must not be negative.");
        }
        blocks = List.copyOf(blocks);
    }

    /**
     * Returns this layer's own total height: block stack plus air gap.
     *
     * @return summed layer height in blocks
     */
    public int totalHeightBlocks() {
        return blocksHeightBlocks() + this.airGapBlocks;
    }

    /**
     * Returns this layer's block stack height only, excluding the air gap.
     *
     * @return summed block height in blocks
     */
    public int blocksHeightBlocks() {
        return this.blocks.stream().mapToInt(FlatLayerSpec::heightBlocks).sum();
    }

    /**
     * Parses the config-file/text-editor shorthand: either the full {@code
     * "<biome>;<blocks>;<air gap>"} form ({@code <blocks>} a comma-separated list of {@link
     * FlatLayerSpec} shorthand entries -- mirrors DESIGN §33.5's established "reuse the project's
     * own shorthand convention" choice rather than inventing an unrelated syntax), or, since
     * DESIGN §34.8, a simplified bare biome id with no {@code ;} at all (e.g. {@code
     * "minecraft:taiga"}) -- {@link StackedBiomeDefaults} supplies that biome's own standard block
     * composition and this preset's own default air gap, so a whole layer list can be written as
     * just an ordered biome list when hand-tuning each band's own material stack isn't wanted.
     *
     * @param raw shorthand text
     * @return parsed layer entry
     */
    public static StackedLayerSpec parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (!trimmed.contains(";")) {
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Stacked layer entry must not be blank.");
            }
            return new StackedLayerSpec(trimmed, StackedBiomeDefaults.blocksFor(trimmed), StackedBiomeDefaults.DEFAULT_AIR_GAP_BLOCKS);
        }
        String[] parts = trimmed.split(";", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                "Stacked layer entry must be \"<biome>;<blocks>;<air gap>\" or a bare biome id: " + raw
            );
        }
        List<FlatLayerSpec> blocks = new ArrayList<>();
        for (String entry : parts[1].split(",")) {
            String blockEntry = entry.trim();
            if (!blockEntry.isEmpty()) {
                blocks.add(FlatLayerSpec.parse(blockEntry));
            }
        }
        int airGap;
        try {
            airGap = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Stacked layer air gap must be an integer: " + raw);
        }
        return new StackedLayerSpec(parts[0].trim(), blocks, airGap);
    }

    /**
     * Renders the config-file/text-editor shorthand.
     *
     * @return {@code "<biome>;<blocks>;<air gap>"}
     */
    public String format() {
        StringBuilder blocksText = new StringBuilder();
        for (FlatLayerSpec block : this.blocks) {
            if (!blocksText.isEmpty()) {
                blocksText.append(',');
            }
            blocksText.append(block.format());
        }
        return this.biome + ";" + blocksText + ";" + this.airGapBlocks;
    }
}
