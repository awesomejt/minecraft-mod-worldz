package media.jlt.minecraft.mods.worldz.logic;

/**
 * Display unit for a block-radius input field (GOALS 17/18: "certain number of
 * blocks... or chunks"). Blocks remain the one persisted/validated unit
 * ({@link WorldzCustomization}); this only converts what the player sees and types.
 */
public enum RadiusUnit {
    /** The persisted, always-valid unit. */
    BLOCKS,
    /** Display-only convenience unit; 1 chunk = {@value #BLOCKS_PER_CHUNK} blocks. */
    CHUNKS;

    private static final int BLOCKS_PER_CHUNK = 16;
    /** Sentinel text (an exterior boundary's "auto") left untouched by conversion. */
    public static final String AUTO = "auto";

    /**
     * Returns the other unit.
     *
     * @return {@link #CHUNKS} if this is {@link #BLOCKS}, otherwise {@link #BLOCKS}
     */
    public RadiusUnit next() {
        return this == BLOCKS ? CHUNKS : BLOCKS;
    }

    /**
     * Renders a persisted block radius as this unit's display text.
     *
     * @param blocks persisted radius in blocks
     * @return display text in this unit
     */
    public String toDisplayText(int blocks) {
        return this == BLOCKS ? Integer.toString(blocks) : Integer.toString(Math.max(1, Math.round(blocks / (float) BLOCKS_PER_CHUNK)));
    }

    /**
     * Re-renders field text currently shown in this unit as the other unit's text, leaving
     * non-numeric text (e.g. {@link #AUTO}) unchanged.
     *
     * @param text currently displayed text in this unit
     * @param target unit to convert to
     * @return converted display text, or {@code text} unchanged if it does not parse as an integer
     */
    public String convert(String text, RadiusUnit target) {
        if (this == target) {
            return text;
        }
        try {
            return target.toDisplayText(this.toBlocksOrThrow(text));
        } catch (NumberFormatException notNumeric) {
            return text;
        }
    }

    /**
     * Converts display text to a blocks string for existing blocks-only validation, passing
     * {@link #AUTO} through unchanged.
     *
     * @param text display text in this unit
     * @return equivalent blocks as text, or {@code text} unchanged when not a plain integer
     */
    public String toBlocksText(String text) {
        String trimmed = text.trim();
        if (this == BLOCKS || trimmed.equalsIgnoreCase(AUTO)) {
            return text;
        }
        try {
            return Integer.toString(toBlocksOrThrow(trimmed));
        } catch (NumberFormatException notNumeric) {
            return text;
        }
    }

    private int toBlocksOrThrow(String text) {
        int value = Integer.parseInt(text.trim());
        return this == BLOCKS ? value : value * BLOCKS_PER_CHUNK;
    }
}
