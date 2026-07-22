package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/**
 * Defaults for the {@code jlt_worldz:nether_start} typed preset (GOALS 27, DESIGN §31), consulted
 * only when that preset resolves without explicit Customize-screen values.
 */
public final class NetherStartConfig {
    /** Target Y for the safe-site search (GOALS 27's "spawn-point safety"). */
    public int spawnY = NetherStartPlan.DEFAULT_SPAWN_Y;
    /** Which starter-chest difficulty tier is used (kit contents land in TODO 14.2b). */
    public StarterKitTier chestTier = StarterKitTier.MEDIUM;

    /** Creates a config populated with defaults. */
    public NetherStartConfig() {
    }
}
