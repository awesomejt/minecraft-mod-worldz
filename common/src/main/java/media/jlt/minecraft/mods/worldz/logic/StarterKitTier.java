package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/**
 * The {@code sky_island} preset's necessities-chest difficulty tier (GOALS 05, DESIGN §27.8):
 * selects which of {@code SkyIslandConfig}'s three kit sections a new world's starter chest is
 * built from. All three tiers must remain beatable given enough time -- only the amount of
 * head-start the player gets differs.
 */
public enum StarterKitTier {
    /** Generous starting materials. */
    EASY,
    /** A middle ground between {@link #EASY} and {@link #HARD}. */
    MEDIUM,
    /** Bare essentials only. */
    HARD;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching tier
     */
    public static StarterKitTier parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("starter kit tier must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown starter kit tier '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase tier name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
