package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/** Terrain generated outside a dimension's central playable envelope. */
public enum ExteriorMode {
    /** Preserve the delegated vanilla terrain everywhere. */
    NORMAL,
    /** Replace the exterior with an infinite deep ocean. */
    OCEAN,
    /** Replace the exterior with empty space. */
    VOID;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching mode
     */
    public static ExteriorMode parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("exterior mode must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown exterior mode '" + value + "'", exception);
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
