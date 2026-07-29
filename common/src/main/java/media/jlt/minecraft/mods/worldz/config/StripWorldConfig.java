package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

/**
 * Configuration for the generic world's optional strip corridor and defaults for the {@code
 * jlt_worldz:strip_world} typed preset (GOALS 32, DESIGN §23/§45).
 */
public final class StripWorldConfig {
    /** Whether the generic {@code worldz} preset applies the corridor. */
    public boolean enabled;
    /** Absolute corridor width in blocks. */
    public int width = 65;
    /** Terrain generated beyond the width -- void or ocean, never normal. */
    public ExteriorMode widthMode = ExteriorMode.VOID;
    /** Whether the same corridor width also applies to the Nether. */
    public boolean applyToNether;
    /** Layout-origin and initial-spawn strategy. */
    public SpawnConfig spawn = new SpawnConfig();
    /** Optional ordered biome-band sequence along the strip's length (GOALS 36). */
    public StripBandsConfig bands = new StripBandsConfig();

    /** Creates a config populated with defaults. */
    public StripWorldConfig() {
    }
}
