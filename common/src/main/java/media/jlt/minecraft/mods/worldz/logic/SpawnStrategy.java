package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/**
 * How a newly created Worldz world chooses its layout origin and initial
 * spawn point (DESIGN §18). Persisted per world; existing worlds always
 * decode as {@link #STARTER_AT_ORIGIN}, today's only behavior.
 */
public enum SpawnStrategy {
    /** Force the starter biome and land around the fixed origin, then spawn there. */
    STARTER_AT_ORIGIN,
    /** Search near the origin for a preferred natural biome and move the layout origin there. */
    PREFERRED_NATURAL_BIOME,
    /** Keep Minecraft's ordinary seed-derived spawn selection; the layout origin stays fixed. */
    VANILLA_SPAWN;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching strategy
     */
    public static SpawnStrategy parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("spawn strategy must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown spawn strategy '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase strategy name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
