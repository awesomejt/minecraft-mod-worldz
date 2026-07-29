package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/**
 * Defaults for the {@code jlt_worldz:cave} typed preset (GOALS 25-26, DESIGN §30), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class CaveConfig {
    /** Smallest sealed-surface Y accepted -- comfortably above bedrock, room for a real cave below it. */
    public static final int MIN_SEALED_SURFACE_Y = -32;

    /** Target Y for the underground spawn-cavity search (GOALS 25's "configurable depth"). */
    public int spawnDepthY = CavePlan.DEFAULT_SPAWN_DEPTH_Y;
    /** Whether a solid roof seals off sky access everywhere (GOALS 25). */
    public boolean sealedSurface = false;
    /** The roof's Y, meaningful only when {@link #sealedSurface} is set. */
    public int sealedSurfaceY = CavePlan.DEFAULT_SEALED_SURFACE_Y;
    /** The roof's block (Jason, 2026-07-25): stone, deepslate, or bedrock. */
    public SealedSurfaceBlock sealedSurfaceBlock = CavePlan.DEFAULT_SEALED_SURFACE_BLOCK;
    /** The roof's thickness in blocks. */
    public int sealedSurfaceThicknessBlocks = CavePlan.DEFAULT_SEALED_SURFACE_THICKNESS_BLOCKS;
    /** Whether the mega-cavern (GOALS 26) is carved around spawn. */
    public boolean cavernEnabled = false;
    /** The mega-cavern's horizontal half-width. */
    public int cavernRadiusBlocks = CavePlan.DEFAULT_CAVERN_RADIUS_BLOCKS;
    /** The mega-cavern's vertical half-height. */
    public int cavernHeightBlocks = CavePlan.DEFAULT_CAVERN_HEIGHT_BLOCKS;
    /** Whether a starter chest is placed at spawn (GOALS 25's "optionally"). */
    public boolean chestEnabled = false;
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents (DESIGN §44.5: {@code cave-easy} in the {@code kits} library). */
    public StarterKitConfig easyKit = StarterKitConfig.reference("cave-easy");
    /** Middle-ground starter-chest contents ({@code cave-medium}). */
    public StarterKitConfig mediumKit = StarterKitConfig.reference("cave-medium");
    /** Bare-essentials starter-chest contents ({@code cave-hard}). */
    public StarterKitConfig hardKit = StarterKitConfig.reference("cave-hard");

    /** Creates a config populated with defaults. */
    public CaveConfig() {
    }
}
