package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

/** Configuration for terrain outside one dimension's central square. */
public final class ExteriorConfig {
    /** Exterior terrain mode. */
    public ExteriorMode mode = ExteriorMode.NORMAL;
    /** Outer envelope half-width, or zero to derive it from an enabled border. */
    public int boundaryRadiusBlocks;
    /** Ocean width inside the outer boundary; ignored by normal and void modes. */
    public int oceanTransitionWidthBlocks = 128;

    /** Creates the backward-compatible normal-terrain default. */
    public ExteriorConfig() {
    }
}
