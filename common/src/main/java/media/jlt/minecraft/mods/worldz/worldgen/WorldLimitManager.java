package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.WorldBorder;

import java.util.OptionalInt;
import java.util.OptionalLong;

/** Applies the border plan baked into a new Worldz world's biome source. */
public final class WorldLimitManager {
    private WorldLimitManager() {
    }

    /**
     * Initializes both dimension borders once, after the server has loaded.
     * Vanilla persists and advances each resulting border transition.
     *
     * @param server newly started logical server
     */
    public static void onServerStarted(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return;
        }

        WorldLimitPlan plan = limitedSource.worldLimits();
        ExteriorPlan exterior = limitedSource.exteriorPlan();
        boolean exteriorObjective = (plan.overworld().ensureObjective()
            && exterior.overworld().mode() != ExteriorMode.NORMAL)
            || (plan.nether().ensureObjective() && exterior.nether().mode() != ExteriorMode.NORMAL);
        if (!plan.enabled() && !exteriorObjective) {
            return;
        }

        WorldLimitState existing = overworld.getDataStorage().get(WorldLimitState.TYPE);
        if (existing != null && existing.initialized()) {
            return;
        }

        int originX = limitedSource.originBlockX();
        int originZ = limitedSource.originBlockZ();
        long overworldStartTick = initializeBorder(overworld, plan.overworld(), "Overworld", originX, originZ);
        ProgressionGuarantees.ensureEndPortal(
            overworld, plan.overworld(), exterior.overworld(), limitedSource.worldLayoutPlan(), originX, originZ
        );
        ServerLevel nether = server.getLevel(Level.NETHER);
        long netherStartTick = -1L;
        if (nether != null) {
            // Layout origins are Overworld-only (DESIGN §18); the Nether's border and
            // progression objective remain centered at the world origin (0, 0).
            netherStartTick = initializeBorder(nether, plan.nether(), "Nether", 0, 0);
            ProgressionGuarantees.ensureBlazeAccess(nether, plan.nether(), exterior.nether());
        }
        ServerLevel end = server.getLevel(Level.END);
        if (end != null) {
            initializeEndBorder(end, plan.end(), plan.overworld());
        }
        overworld.getDataStorage().set(
            WorldLimitState.TYPE,
            new WorldLimitState(true, overworldStartTick, netherStartTick)
        );
    }

    /**
     * Starts delayed transitions whose persisted game-time deadline is due.
     *
     * @param server ticking logical server
     */
    public static void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        WorldLimitState state = overworld.getDataStorage().get(WorldLimitState.TYPE);
        if (state == null || !state.initialized() || !state.hasPendingStarts()) {
            return;
        }
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return;
        }

        WorldLimitPlan plan = limitedSource.worldLimits();
        startIfDue(state, true, overworld, plan.overworld(), "Overworld");
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether != null) {
            startIfDue(state, false, nether, plan.nether(), "Nether");
        }
    }

    private static long initializeBorder(
        ServerLevel level,
        WorldLimitPlan.DimensionLimit limit,
        String dimensionName,
        int originX,
        int originZ
    ) {
        if (!limit.enabled()) {
            return -1L;
        }

        BorderSchedule schedule = limit.schedule();
        WorldBorder border = level.getWorldBorder();
        border.setCenter(originX, originZ);
        if (schedule.initialRadiusBlocks() == schedule.finalRadiusBlocks()) {
            border.setSize(schedule.finalDiameterBlocks());
            logSchedule(dimensionName, limit, schedule, "static");
            return -1L;
        }
        if (schedule.delayTicks() > 0L) {
            border.setSize(schedule.initialDiameterBlocks());
            long startTick = Math.addExact(level.getGameTime(), schedule.delayTicks());
            logSchedule(dimensionName, limit, schedule, "waiting until game tick " + startTick);
            return startTick;
        }
        startTransition(level, limit, dimensionName);
        return -1L;
    }

    private static void initializeEndBorder(ServerLevel end, WorldLimitPlan.EndLimit limit, WorldLimitPlan.DimensionLimit overworld) {
        OptionalInt radius = limit.resolveRadiusBlocks(overworld);
        if (radius.isEmpty()) {
            return;
        }
        // Static only -- GOALS 17 asks to carry the Overworld's eventual size into the End, not
        // to independently animate an End-specific expand/collapse schedule. Vanilla End
        // generation (main island, obsidian pillars, exit portal) is otherwise left untouched;
        // this only limits how far a player can fly from it.
        WorldBorder border = end.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(radius.getAsInt() * 2.0);
        WorldzCommon.LOGGER.info("Worldz End border: static radius {} (carried from Overworld, floor applied).", radius.getAsInt());
    }

    private static void startIfDue(
        WorldLimitState state,
        boolean overworld,
        ServerLevel level,
        WorldLimitPlan.DimensionLimit limit,
        String dimensionName
    ) {
        OptionalLong pending = state.pendingStartTick(overworld);
        if (pending.isEmpty() || level.getGameTime() < pending.getAsLong()) {
            return;
        }
        startTransition(level, limit, dimensionName);
        state.clearPendingStart(overworld);
    }

    private static void startTransition(
        ServerLevel level,
        WorldLimitPlan.DimensionLimit limit,
        String dimensionName
    ) {
        BorderSchedule schedule = limit.schedule();
        WorldBorder border = level.getWorldBorder();
        if (schedule.durationTicks() == 0L) {
            border.setSize(schedule.finalDiameterBlocks());
        } else {
            border.lerpSizeBetween(
                schedule.initialDiameterBlocks(),
                schedule.finalDiameterBlocks(),
                schedule.durationTicks(),
                level.getGameTime()
            );
        }
        logSchedule(dimensionName, limit, schedule, "started");
    }

    private static void logSchedule(
        String dimensionName,
        WorldLimitPlan.DimensionLimit limit,
        BorderSchedule schedule,
        String status
    ) {
        WorldzCommon.LOGGER.info(
            "Worldz {} border {}: initial radius {}, final radius {}, delay {} ticks, duration {} ticks{}.",
            dimensionName,
            status,
            limit.initialRadiusBlocks(),
            limit.finalRadiusBlocks(),
            schedule.delayTicks(),
            schedule.durationTicks(),
            schedule.usesRate()
                ? " (" + limit.resizeRateBlocks() + " blocks per " + limit.resizeRateDays() + " days)"
                : ""
        );
    }
}
