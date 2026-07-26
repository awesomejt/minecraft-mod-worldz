package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.LightSource;

/**
 * Shape and lighting for a guaranteed starter capsule/base (GOALS 41, DESIGN §31.9). First used
 * by {@code nether_start} only (TODO 14b); named generically, not {@code NetherStartCapsuleConfig},
 * since Jason's own ask is for this to eventually generalize to every world type/starting
 * scenario ({@code cave}, {@code end_start}, and beyond) once revisited -- mirrors {@code
 * FloatingIslandsConfig}'s own "single current owner, generic shape" precedent.
 */
public final class StarterCapsuleConfig {
    /** Interior footprint width/depth, in blocks -- must stay odd so the room has a true center column. */
    public int sizeBlocks = 5;
    /** Interior height, in blocks. */
    public int heightBlocks = 3;
    /** Which block lights the capsule; defaults are overridden per world type ("appropriate to the world type", Jason 2026-07-25). */
    public LightSource lightSource = LightSource.TORCH;
    /** Spacing, in blocks, between embedded/hung light sources; ignored by {@link LightSource#GLOW_LICHEN}, which always covers the entire interior surface. */
    public int lightSpacingBlocks = 5;

    /** Creates a config populated with defaults. */
    public StarterCapsuleConfig() {
    }
}
