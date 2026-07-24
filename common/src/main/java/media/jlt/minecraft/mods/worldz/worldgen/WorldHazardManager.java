package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ForeverNightPlan;
import media.jlt.minecraft.mods.worldz.logic.RisingLavaSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Applies world-hazard rules (GOALS 29-30, DESIGN §35): shared runtime rules, not worldgen,
 * composable with any world type -- mirrors {@link WorldLimitManager}'s exact server-tick +
 * saved-data shape. Overworld only (DESIGN §35.3).
 */
public final class WorldHazardManager {
    /** Real-tick interval between {@link ForeverNightPlan#relaxInsomnia()}'s upkeep pass --
     * cheap (a stat write per online player), no need for every-tick precision. */
    private static final int RELAX_INSOMNIA_INTERVAL_TICKS = 20;

    /**
     * Currently loaded Overworld chunk positions, tracked ourselves (DESIGN §35.0: vanilla's own
     * {@code ChunkMap.forEachBlockTickingChunk} isn't reachable from mod code) via {@link
     * #onChunkLoad}/{@link #onChunkUnload} -- unlike every other manager in this project, rising
     * lava genuinely needs this small piece of non-persisted, per-session mutable state; rebuilt
     * fresh every {@link #onServerStarted} so a dev-session world swap can't leave stale entries.
     */
    private static final Set<ChunkPos> LOADED_OVERWORLD_CHUNKS = new HashSet<>();
    /** Newly loaded chunk positions still needing their own one-time catch-up conversion,
     * drained on the next {@link #onServerTick} -- queued rather than applied synchronously
     * inside the load callback because NeoForge's own {@code ChunkEvent.Load} explicitly
     * forbids touching the level from inside it (DESIGN §35.0). */
    private static final Deque<ChunkPos> PENDING_CHUNK_CATCH_UP = new ArrayDeque<>();

    private WorldHazardManager() {
    }

    /**
     * Schedules or immediately applies forever night, and starts rising lava's own schedule
     * origin, once per world, after the server has loaded.
     *
     * @param server newly started logical server
     */
    public static void onServerStarted(MinecraftServer server) {
        LOADED_OVERWORLD_CHUNKS.clear();
        PENDING_CHUNK_CATCH_UP.clear();
        ForeverNightPlan foreverNight = ForeverNightPlan.fromConfig(WorldzCommon.config().foreverNight);
        boolean risingLavaEnabled = WorldzCommon.config().risingLava.enabled;
        if (!foreverNight.enabled() && !risingLavaEnabled) {
            return;
        }
        ServerLevel overworld = server.overworld();
        WorldHazardState state = overworld.getDataStorage().get(WorldHazardState.TYPE);
        if (state == null) {
            state = new WorldHazardState();
            overworld.getDataStorage().set(WorldHazardState.TYPE, state);
        }
        if (foreverNight.enabled() && !state.nightLocked() && state.pendingLockTick().isEmpty()) {
            long delayTicks = foreverNight.lockDelayTicks();
            if (delayTicks <= 0L) {
                applyNightLock(server, overworld);
                state.markNightLocked();
            } else {
                long targetTick = Math.addExact(dimensionTicks(overworld), delayTicks);
                state.schedulePendingLock(targetTick);
                WorldzCommon.LOGGER.info("Worldz: night will lock permanently at tick {}.", targetTick);
            }
        }
        if (risingLavaEnabled && state.lavaOriginTick().isEmpty()) {
            RisingLavaSchedule schedule = RisingLavaSchedule.fromConfig(WorldzCommon.config().risingLava);
            state.recordLavaOrigin(dimensionTicks(overworld));
            // The level "starts" at startY (GOAL 29's own "rises from a starting depth" wording)
            // -- already-loaded and future chunks alike catch up to this via the same per-chunk
            // conversion the ongoing rise itself uses, no separate initial-fill pass needed.
            state.recordLastAppliedLavaY(schedule.startY());
            WorldzCommon.LOGGER.info(
                "Worldz: rising lava schedule started at Y {}, rising to Y {}.", schedule.startY(), schedule.maxY()
            );
        }
    }

    /**
     * Applies a due delayed night lock, runs {@link ForeverNightPlan#relaxInsomnia()}'s periodic
     * upkeep once locked, drains queued newly-loaded-chunk lava catch-up, and advances rising
     * lava's own level when its schedule has moved forward.
     *
     * @param server ticking logical server
     */
    public static void onServerTick(MinecraftServer server) {
        ForeverNightPlan foreverNight = ForeverNightPlan.fromConfig(WorldzCommon.config().foreverNight);
        boolean risingLavaEnabled = WorldzCommon.config().risingLava.enabled;
        if (!foreverNight.enabled() && !risingLavaEnabled) {
            return;
        }
        ServerLevel overworld = server.overworld();
        WorldHazardState state = overworld.getDataStorage().get(WorldHazardState.TYPE);
        if (state == null) {
            return;
        }
        if (foreverNight.enabled()) {
            tickForeverNight(server, overworld, foreverNight, state);
        }
        if (risingLavaEnabled) {
            RisingLavaSchedule schedule = RisingLavaSchedule.fromConfig(WorldzCommon.config().risingLava);
            drainPendingChunkCatchUp(overworld, state);
            tickRisingLava(overworld, schedule, state);
        }
    }

    /**
     * Records a newly loaded Overworld chunk for {@link #LOADED_OVERWORLD_CHUNKS} tracking and,
     * if rising lava has already started, queues it for catch-up conversion.
     *
     * @param level the chunk's own level
     * @param chunk the newly loaded chunk
     */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        LOADED_OVERWORLD_CHUNKS.add(pos);
        PENDING_CHUNK_CATCH_UP.add(pos);
    }

    /**
     * Stops tracking an unloaded Overworld chunk.
     *
     * @param level the chunk's own level
     * @param chunk the unloaded chunk
     */
    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        LOADED_OVERWORLD_CHUNKS.remove(chunk.getPos());
    }

    private static void tickForeverNight(
        MinecraftServer server, ServerLevel overworld, ForeverNightPlan foreverNight, WorldHazardState state
    ) {
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

    private static void drainPendingChunkCatchUp(ServerLevel overworld, WorldHazardState state) {
        if (PENDING_CHUNK_CATCH_UP.isEmpty()) {
            return;
        }
        OptionalInt lastApplied = state.lastAppliedLavaY();
        if (lastApplied.isEmpty()) {
            PENDING_CHUNK_CATCH_UP.clear();
            return;
        }
        int minY = overworld.getMinY();
        ChunkPos pos;
        while ((pos = PENDING_CHUNK_CATCH_UP.poll()) != null) {
            convertChunkColumnRange(overworld, pos, minY, lastApplied.getAsInt());
        }
    }

    private static void tickRisingLava(ServerLevel overworld, RisingLavaSchedule schedule, WorldHazardState state) {
        OptionalLong origin = state.lavaOriginTick();
        OptionalInt lastApplied = state.lastAppliedLavaY();
        if (origin.isEmpty() || lastApplied.isEmpty()) {
            return;
        }
        long elapsed = dimensionTicks(overworld) - origin.getAsLong();
        int currentLevel = schedule.levelAtTick(elapsed);
        if (currentLevel <= lastApplied.getAsInt()) {
            return;
        }
        for (ChunkPos pos : LOADED_OVERWORLD_CHUNKS) {
            convertChunkColumnRange(overworld, pos, lastApplied.getAsInt() + 1, currentLevel);
        }
        state.recordLastAppliedLavaY(currentLevel);
        WorldzCommon.LOGGER.info("Worldz: rising lava now at Y {}.", currentLevel);
    }

    /**
     * Converts every air or water column position within {@code [minYInclusive, maxYInclusive]}
     * to lava for one chunk -- GOAL 29's own "air/water below the level" rule (DESIGN §35.2),
     * verified real API: {@link Level#setBlock} (not the worldgen-time {@code ChunkAccess}
     * primitive), matching this project's existing {@code Block.UPDATE_ALL} convention.
     */
    private static void convertChunkColumnRange(ServerLevel level, ChunkPos pos, int minYInclusive, int maxYInclusive) {
        if (maxYInclusive < minYInclusive) {
            return;
        }
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minBlockX = pos.getMinBlockX();
        int minBlockZ = pos.getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                cursor.set(minBlockX + x, 0, minBlockZ + z);
                for (int y = minYInclusive; y <= maxYInclusive; y++) {
                    cursor.setY(y);
                    BlockState current = level.getBlockState(cursor);
                    if (current.isAir() || current.is(Blocks.WATER)) {
                        level.setBlock(cursor, lava, Block.UPDATE_ALL);
                    }
                }
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
