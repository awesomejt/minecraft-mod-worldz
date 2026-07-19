package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/** How a new Worldz world coordinates terrain shape with configured biomes. */
public enum LayoutMode {
    /** Pre-Phase-15 climate filtering: vanilla continental shape, restricted biome labels only. */
    LEGACY,
    /** A starter island in an otherwise infinite ocean of selected ocean biomes. */
    OCEAN,
    /** One selected biome fills the generated world. */
    SINGLE_BIOME,
    /** A starter island floating in an otherwise infinite sky void. */
    VOID,
    /** Seed-shuffled land biome regions over completely untouched vanilla terrain. */
    CHAOS,
    /** Ordered (or once-shuffled) biome bands along a strip's length, GOALS 36. */
    STRIP_BANDS;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching mode
     */
    public static LayoutMode parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("layout mode must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown layout mode '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase mode name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
