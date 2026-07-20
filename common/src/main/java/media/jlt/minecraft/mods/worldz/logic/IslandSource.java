package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/** How the {@code ocean_island} preset's land is sourced (GOALS 01, 02, 03; DESIGN §25.1). */
public enum IslandSource {
    /** A natural-looking artificial island, shaped by {@link IslandShapeProfile} (GOALS 01). */
    ARTIFICIAL,
    /** A real island found in the seed's own unmodified terrain (GOALS 02). */
    NATURAL,
    /** No land at all; the player starts on a chest boat in open ocean (GOALS 03). */
    CHEST_BOAT;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching source
     */
    public static IslandSource parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("island source must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown island source '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase source name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
