package media.jlt.minecraft.mods.worldz.config;

/**
 * Defaults for the {@code jlt_worldz:strip_world} typed preset (GOALS 32, DESIGN §23),
 * consulted only when that preset resolves without explicit Customize-screen values. The
 * corridor width itself lives in the shared top-level {@link StripConfig} (one width,
 * optionally mirrored to the Nether -- not a per-preset value).
 */
public final class StripWorldConfig {
    /** Layout-origin and initial-spawn strategy. */
    public SpawnConfig spawn = new SpawnConfig();
    /** Optional ordered biome-band sequence along the strip's length (GOALS 36). */
    public StripBandsConfig bands = new StripBandsConfig();

    /** Creates a config populated with defaults. */
    public StripWorldConfig() {
    }
}
