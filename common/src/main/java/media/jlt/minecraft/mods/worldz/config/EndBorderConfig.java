package media.jlt.minecraft.mods.worldz.config;

/**
 * Configuration for optionally carrying the Overworld's border size into the End (GOALS 17),
 * clamped up to a floor so the dragon fight stays winnable even at very small Overworld sizes.
 */
public final class EndBorderConfig {
    /** Whether the End receives a border matching the Overworld's eventual (final) radius. */
    public boolean carryFromOverworld;
    /** Smallest End border half-width regardless of the carried Overworld radius. */
    public int minimumRadiusBlocks = 256;

    /** Creates the disabled default configuration. */
    public EndBorderConfig() {
    }
}
