package media.jlt.minecraft.mods.worldz.config;

/** Configuration for one dimension's square, origin-centered world border. */
public final class BorderConfig {
    /** Whether this dimension receives a limited border. */
    public boolean enabled;
    /** Border half-width when the world is created. */
    public int initialRadiusBlocks = 512;
    /** Border half-width after the configured resize period. */
    public int finalRadiusBlocks = 512;
    /** In-game days used for the linear transition from initial to final radius. */
    public int resizeDays;
    /** Radius blocks traversed per rate interval, or zero to use {@link #resizeDays}. */
    public int resizeRateBlocks;
    /** In-game days per rate interval, or zero to use {@link #resizeDays}. */
    public int resizeRateDays;
    /** Whether the dimension's progression objective must be reachable inside the final border. */
    public boolean ensureObjective = true;

    /** Creates the disabled, static default configuration. */
    public BorderConfig() {
    }
}
