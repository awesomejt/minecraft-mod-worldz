package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ForeverNightPlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.OptionalLong;

/**
 * Applies world-hazard rules (GOALS 29-30, DESIGN §35): shared runtime rules, not worldgen,
 * composable with any world type -- mirrors {@link WorldLimitManager}'s exact server-tick +
 * saved-data shape.
 */
public final class WorldHazardManager {
    /** Real-tick interval between {@link ForeverNightPlan#relaxInsomnia()}'s upkeep pass --
     * cheap (a stat write per online player), no need for every-tick precision. */
    private static final int RELAX_INSOMNIA_INTERVAL_TICKS = 20;

    private WorldHazardManager() {
    }

    /**
     * Schedules or immediately applies forever night, once per world, after the server has
     * loaded.
     *
     * @param server newly started logical server
     */
    public static void onServerStarted(MinecraftServer server) {
        ForeverNightPlan foreverNight = ForeverNightPlan.fromConfig(WorldzCommon.config().foreverNight);
        if (!foreverNight.enabled()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        WorldHazardState state = overworld.getDataStorage().get(WorldHazardState.TYPE);
        if (state != null) {
            // Already initialized on an earlier start -- either locked already, or a delayed
            // lock is already scheduled and onServerTick will pick it up.
            return;
        }
        long delayTicks = foreverNight.lockDelayTicks();
        if (delayTicks <= 0L) {
            applyNightLock(server, overworld);
            overworld.getDataStorage().set(WorldHazardState.TYPE, new WorldHazardState(true, -1L));
            return;
        }
        long targetTick = Math.addExact(dimensionTicks(overworld), delayTicks);
        overworld.getDataStorage().set(WorldHazardState.TYPE, new WorldHazardState(false, targetTick));
        WorldzCommon.LOGGER.info("Worldz: night will lock permanently at tick {}.", targetTick);
    }

    /**
     * Applies a due delayed lock, and runs {@link ForeverNightPlan#relaxInsomnia()}'s periodic
     * upkeep once locked.
     *
     * @param server ticking logical server
     */
    public static void onServerTick(MinecraftServer server) {
        ForeverNightPlan foreverNight = ForeverNightPlan.fromConfig(WorldzCommon.config().foreverNight);
        if (!foreverNight.enabled()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        WorldHazardState state = overworld.getDataStorage().get(WorldHazardState.TYPE);
        if (state == null) {
            return;
        }
        if (!state.nightLocked()) {
            OptionalLong pending = state.pendingLockTick();
            if (pending.isPresent() && dimensionTicks(overworld) >= pending.getAsLong()) {
                applyNightLock(server, overworld);
                state.markNightLocked();
            }
            return;
        }
        if (foreverNight.relaxInsomnia() && server.getTickCount() % RELAX_INSOMNIA_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : overworld.players()) {
                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            }
        }
    }

    /**
     * Disables the {@code advance_time} gamerule and jumps the Overworld's own clock to night --
     * verified real 26.2 mechanism (DESIGN §35.0): vanilla's own sleep-skip logic already gates
     * on this same gamerule, so "sleeping cannot skip the night" (GOAL 30) is a free consequence,
     * not a separate mixin.
     */
    private static void applyNightLock(MinecraftServer server, ServerLevel overworld) {
        server.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        server.clockManager().moveToTimeMarker(server.registryAccess().getOrThrow(WorldClocks.OVERWORLD), ClockTimeMarkers.NIGHT);
        WorldzCommon.LOGGER.info("Worldz: night locked permanently in {}.", overworld.dimension().identifier());
    }

    /**
     * Returns the Overworld's own real elapsed-tick counter -- {@link WorldLimitManager}'s own
     * identical helper explains why {@code getGameTime()} is wrong in this snapshot; the same
     * reasoning applies here (DESIGN §35.0 records the same clock also driving day/night).
     */
    private static long dimensionTicks(ServerLevel level) {
        return level.getDefaultClockTime();
    }
}
