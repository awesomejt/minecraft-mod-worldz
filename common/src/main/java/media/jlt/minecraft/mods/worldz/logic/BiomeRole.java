package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/** The terrain class a biome participates in within a coordinated world layout. */
public enum BiomeRole {
    /** Ordinary dry land, including rivers crossing it. */
    LAND,
    /** Open ocean regions. */
    OCEAN,
    /** Coast-transition biomes between land and ocean regions. */
    BEACH;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching role
     */
    public static BiomeRole parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("biome role must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown biome role '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase role name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
