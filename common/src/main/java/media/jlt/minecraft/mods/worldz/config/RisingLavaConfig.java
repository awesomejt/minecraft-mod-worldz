package media.jlt.minecraft.mods.worldz.config;

/**
 * Defaults for the world-hazard "rising lava floor" module (GOAL 29, DESIGN §35.2) -- a shared
 * runtime rule, not a typed preset, composable with any world type. Overworld only for this
 * phase (DESIGN §35.3).
 */
public final class RisingLavaConfig {
    /** Whether the lava level rises over time. */
    public boolean enabled = false;
    /** In-game days before the level starts rising. */
    public int delayDays = 3;
    /** Y the lava level starts at, before any rise. */
    public int startY = FlatConfig.OVERWORLD_MIN_Y;
    /** Y the lava level stops rising at. */
    public int maxY = 64;
    /** Blocks the level rises per {@link #rateDays}. */
    public int rateBlocks = 1;
    /** In-game days per {@link #rateBlocks} of rise. */
    public int rateDays = 1;

    /** Creates a config populated with defaults. */
    public RisingLavaConfig() {
    }
}
