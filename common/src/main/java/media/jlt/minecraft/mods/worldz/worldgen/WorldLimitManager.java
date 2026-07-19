package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.OptionalInt;
import java.util.OptionalLong;

/** Applies the border plan baked into a new Worldz world's biome source. */
public final class WorldLimitManager {
    private WorldLimitManager() {
    }

    /**
     * Initializes both dimension borders once, after the server has loaded.
     * Vanilla persists and advances each resulting continuous transition; stepped
     * transitions are driven every tick by {@link #onServerTick}.
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
        ChunkGenerator overworldGenerator = overworld.getChunkSource().getGenerator();
        StripPlan overworldStrip = overworldGenerator instanceof EnvelopedChunkGenerator enveloped
            ? enveloped.strip()
            : StripPlan.disabled();
        boolean exteriorObjective = (plan.overworld().ensureObjective()
            && (exterior.overworld().mode() != ExteriorMode.NORMAL || overworldStrip.enabled()))
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
        BorderInitResult overworldResult = initializeBorder(overworld, plan.overworld(), "Overworld", originX, originZ);
        ProgressionGuarantees.ensureEndPortal(
            overworld, plan.overworld(), exterior.overworld(), overworldStrip, limitedSource.worldLayoutPlan(), originX, originZ
        );
        ServerLevel nether = server.getLevel(Level.NETHER);
        BorderInitResult netherResult = BorderInitResult.NONE;
        if (nether != null) {
            // Layout origins are Overworld-only (DESIGN §18); the Nether's border and
            // progression objective remain centered at the world origin (0, 0).
            netherResult = initializeBorder(nether, plan.nether(), "Nether", 0, 0);
            ChunkGenerator netherGenerator = nether.getChunkSource().getGenerator();
            StripPlan netherStrip = netherGenerator instanceof EnvelopedChunkGenerator enveloped
                ? enveloped.strip()
                : StripPlan.disabled();
            ProgressionGuarantees.ensureBlazeAccess(nether, plan.nether(), exterior.nether(), netherStrip);
        }
        ServerLevel end = server.getLevel(Level.END);
        if (end != null) {
            initializeEndBorder(end, plan.end(), plan.overworld());
        }
        overworld.getDataStorage().set(
            WorldLimitState.TYPE,
            new WorldLimitState(
                true,
                overworldResult.pendingStartTick(), netherResult.pendingStartTick(),
                overworldResult.stepOriginTick(), netherResult.stepOriginTick()
            )
        );
    }

    /**
     * Starts delayed continuous transitions whose persisted game-time deadline is due, and
     * drives every tick still-active stepped transitions.
     *
     * @param server ticking logical server
     */
    public static void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        WorldLimitState state = overworld.getDataStorage().get(WorldLimitState.TYPE);
        if (state == null || !state.initialized() || (!state.hasPendingStarts() && !state.hasActiveSteps())) {
            return;
        }
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return;
        }

        WorldLimitPlan plan = limitedSource.worldLimits();
        startIfDue(state, true, overworld, plan.overworld(), "Overworld");
        driveStepIfActive(state, true, overworld, plan.overworld(), "Overworld");
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether != null) {
            startIfDue(state, false, nether, plan.nether(), "Nether");
            driveStepIfActive(state, false, nether, plan.nether(), "Nether");
        }
    }

    /** Pending continuous-delay and active-stepped-origin ticks resulting from initialization. */
    private record BorderInitResult(long pendingStartTick, long stepOriginTick) {
        private static final BorderInitResult NONE = new BorderInitResult(-1L, -1L);
    }

    private static BorderInitResult initializeBorder(
        ServerLevel level,
        WorldLimitPlan.DimensionLimit limit,
        String dimensionName,
        int originX,
        int originZ
    ) {
        if (!limit.enabled()) {
            return BorderInitResult.NONE;
        }

        BorderSchedule schedule = limit.schedule();
        WorldBorder border = level.getWorldBorder();
        border.setCenter(originX, originZ);
        if (schedule.initialRadiusBlocks() == schedule.finalRadiusBlocks()) {
            border.setSize(schedule.finalDiameterBlocks());
            logSchedule(dimensionName, limit, schedule, "static");
            return BorderInitResult.NONE;
        }
        if (schedule.style() == ResizeStyle.STEPPED) {
            border.setSize(schedule.initialDiameterBlocks());
            long originTick = dimensionTicks(level);
            logSchedule(dimensionName, limit, schedule, "stepped, tracking from tick " + originTick);
            return new BorderInitResult(-1L, originTick);
        }
        if (schedule.delayTicks() > 0L) {
            border.setSize(schedule.initialDiameterBlocks());
            long startTick = Math.addExact(dimensionTicks(level), schedule.delayTicks());
            logSchedule(dimensionName, limit, schedule, "waiting until game tick " + startTick);
            return new BorderInitResult(startTick, -1L);
        }
        startTransition(level, limit, dimensionName);
        return BorderInitResult.NONE;
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
        if (pending.isEmpty() || dimensionTicks(level) < pending.getAsLong()) {
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
                dimensionTicks(level)
            );
        }
        logSchedule(dimensionName, limit, schedule, "started");
    }

    /**
     * Applies the current tick's radius for an active stepped resize, and stops tracking it
     * once it reaches its final radius. The radius is a pure function of elapsed clock ticks
     * (see {@link BorderSchedule#radiusAtTick}), so this self-heals across restarts and any
     * ticks missed while the server was closed -- there is no separate persisted lerp state.
     */
    private static void driveStepIfActive(
        WorldLimitState state,
        boolean overworld,
        ServerLevel level,
        WorldLimitPlan.DimensionLimit limit,
        String dimensionName
    ) {
        OptionalLong origin = state.stepOriginTick(overworld);
        if (origin.isEmpty()) {
            return;
        }
        BorderSchedule schedule = limit.schedule();
        long elapsed = dimensionTicks(level) - origin.getAsLong();
        WorldBorder border = level.getWorldBorder();
        if (elapsed >= schedule.totalDurationTicks()) {
            border.setSize(schedule.finalDiameterBlocks());
            state.clearStepOrigin(overworld);
            logSchedule(dimensionName, limit, schedule, "stepped, finished");
            return;
        }
        border.setSize(schedule.radiusAtTick(elapsed) * 2.0);
    }

    /**
     * Returns this dimension's own real elapsed-tick counter. 26.2 moved authoritative elapsed
     * time to a per-dimension {@code WorldClock} ({@code Level.getDefaultClockTime()}, backed by
     * {@code data/minecraft/world_clocks.dat}'s per-dimension {@code total_ticks}) --
     * {@code ServerLevel.getGameTime()} (the legacy {@code LevelData} field) is no longer kept in
     * sync with real play time in this snapshot, which silently stalled every delayed border
     * transition (confirmed in-game: `world_limits.dat`'s pending start tick never cleared even
     * after `world_clocks.dat` showed over a million elapsed overworld ticks).
     */
    private static long dimensionTicks(ServerLevel level) {
        return level.getDefaultClockTime();
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
