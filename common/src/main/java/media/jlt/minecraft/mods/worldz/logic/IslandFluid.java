package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/**
 * The {@code ocean_island} preset's exterior/ocean-gradient fluid (GOALS 01, 28, 31; DESIGN
 * §26.1) -- independent of {@link IslandSource}, so any island source can pair with any fluid.
 */
public enum IslandFluid {
    /** Ordinary water (GOALS 01/02/03 default). */
    WATER,
    /** Lava instead of water (GOALS 28) -- the island itself is unchanged, just the fluid. */
    LAVA,
    /** No fluid at all -- drained, exposed basins (GOALS 31). */
    NONE;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching fluid
     */
    public static IslandFluid parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("island fluid must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown island fluid '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase fluid name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
