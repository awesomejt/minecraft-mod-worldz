package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/** How a border schedule's rate fields drive the transition between radii. */
public enum ResizeStyle {
    /** One smooth vanilla lerp across the whole transition. */
    CONTINUOUS,
    /** Abrupt jumps of the rate distance every rate interval. */
    STEPPED;

    /**
     * Returns the other style, for a two-value toggle button.
     *
     * @return {@link #STEPPED} if this is {@link #CONTINUOUS}, otherwise {@link #CONTINUOUS}
     */
    public ResizeStyle next() {
        return this == CONTINUOUS ? STEPPED : CONTINUOUS;
    }

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching style
     */
    public static ResizeStyle parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("resize style must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown resize style '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase style name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
