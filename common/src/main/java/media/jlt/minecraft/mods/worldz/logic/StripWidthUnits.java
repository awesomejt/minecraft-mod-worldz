package media.jlt.minecraft.mods.worldz.logic;

import java.math.BigDecimal;

/**
 * Lossless display conversion for an absolute strip width. Unlike radius controls, an arbitrary
 * whole-block width need not be chunk-aligned, so chunk display may contain an exact fractional
 * value such as {@code 4.0625} for 65 blocks.
 */
public final class StripWidthUnits {
    private static final BigDecimal BLOCKS_PER_CHUNK = BigDecimal.valueOf(16);

    private StripWidthUnits() {
    }

    /** Renders a whole-block width in the selected display unit without rounding. */
    public static String toDisplayText(int blocks, RadiusUnit unit) {
        return unit == RadiusUnit.BLOCKS ? Integer.toString(blocks) : chunksText(blocks);
    }

    /** Converts editable text between display units, leaving invalid values for validation. */
    public static String convert(String text, RadiusUnit source, RadiusUnit target) {
        if (source == target) {
            return text;
        }
        return target == RadiusUnit.CHUNKS ? blocksToChunksText(text) : chunksToBlocksText(text);
    }

    /** Converts selected-unit text to the canonical whole-block text used by customization. */
    public static String toBlocksText(String text, RadiusUnit unit) {
        return unit == RadiusUnit.BLOCKS ? text : chunksToBlocksText(text);
    }

    private static String blocksToChunksText(String text) {
        try {
            return chunksText(Integer.parseInt(text.trim()));
        } catch (NumberFormatException invalid) {
            return text;
        }
    }

    private static String chunksText(int blocks) {
        return BigDecimal.valueOf(blocks).divide(BLOCKS_PER_CHUNK).stripTrailingZeros().toPlainString();
    }

    private static String chunksToBlocksText(String text) {
        try {
            int blocks = new BigDecimal(text.trim()).multiply(BLOCKS_PER_CHUNK).intValueExact();
            return Integer.toString(blocks);
        } catch (ArithmeticException | NumberFormatException invalid) {
            return text;
        }
    }
}
