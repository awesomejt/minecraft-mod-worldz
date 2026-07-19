package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

/** Configuration for a narrow strip-world corridor (GOALS 32). */
public final class StripConfig {
    /** Whether the corridor's width constraint applies. */
    public boolean enabled;
    /** Half-width from the origin; the corridor is twice this wide. */
    public int widthRadiusBlocks = 32;
    /** Terrain generated beyond the width -- void or ocean, never normal. */
    public ExteriorMode widthMode = ExteriorMode.VOID;
    /** Whether the same corridor width also applies to the Nether. */
    public boolean applyToNether;

    /** Creates the disabled default configuration. */
    public StripConfig() {
    }
}
