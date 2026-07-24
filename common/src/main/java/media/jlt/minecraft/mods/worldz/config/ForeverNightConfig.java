package media.jlt.minecraft.mods.worldz.config;

/**
 * Defaults for the world-hazard "forever night" module (GOAL 30, DESIGN §35.1) -- a shared
 * runtime rule, not a typed preset, composable with any world type.
 */
public final class ForeverNightConfig {
    /** Whether night eventually locks permanently. */
    public boolean enabled = false;
    /** In-game days before night locks; zero locks immediately at world creation. */
    public int lockAfterDays = 0;
    /** Whether phantom/insomnia pressure is suppressed once night is locked. */
    public boolean relaxInsomnia = false;

    /** Creates a config populated with defaults. */
    public ForeverNightConfig() {
    }
}
