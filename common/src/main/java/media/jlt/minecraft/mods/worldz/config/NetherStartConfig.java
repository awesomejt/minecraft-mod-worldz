package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.LightSource;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/**
 * Defaults for the {@code jlt_worldz:nether_start} typed preset (GOALS 27, DESIGN §31), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class NetherStartConfig {
    /** Target Y for the safe-site search (GOALS 27's "spawn-point safety"). */
    public int spawnY = NetherStartPlan.DEFAULT_SPAWN_Y;
    /** Which of {@link #easyKit}/{@link #mediumKit}/{@link #hardKit} the starter chest uses. */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;
    /** Generous starter-chest contents (DESIGN §31.6): a full portal, ready to use ({@code
     * nether-start-easy} in the {@code kits} library, DESIGN §44.5). */
    public StarterKitConfig easyKit = StarterKitConfig.reference("nether-start-easy");
    /** Middle-ground starter-chest contents: a full frame's worth of obsidian, no ignition
     * ({@code nether-start-medium}). */
    public StarterKitConfig mediumKit = StarterKitConfig.reference("nether-start-medium");
    /** Bare-essentials starter-chest contents: no guaranteed obsidian at all ({@code nether-start-hard}). */
    public StarterKitConfig hardKit = StarterKitConfig.reference("nether-start-hard");
    /**
     * Whether to always build the guaranteed capsule instead of only falling back to it when the
     * natural safe-site search comes up empty (GOALS 41.1, DESIGN §31.9).
     */
    public boolean forceCapsule = false;
    /** The capsule's shape and lighting (GOALS 41, DESIGN §31.9), Nether-appropriate defaults. */
    public StarterCapsuleConfig capsule = capsuleDefaults();

    /** Creates a config populated with defaults. */
    public NetherStartConfig() {
    }

    private static StarterCapsuleConfig capsuleDefaults() {
        StarterCapsuleConfig config = new StarterCapsuleConfig();
        config.lightSource = LightSource.GLOWSTONE;
        return config;
    }
}
