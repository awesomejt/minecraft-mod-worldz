package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

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
        // The ocean island (GOALS 01, DESIGN §24) deliberately never expresses itself through
        // ExteriorPlan -- the flat single-depth envelope model can't represent its shallow-to-
        // deep gradient (§24.5) -- so this gate must check it separately, or an ocean-island
        // world looks like an unlimited normal world here and skips the fallback End portal.
        IslandPlan overworldIsland = limitedSource.island();
        // Same reasoning, same fix shape, for the sky island (GOALS 05, DESIGN §27.5): it also
        // never expresses itself through ExteriorPlan (Overworld side always stays normal;
        // SkyIslandPlan supplies the entire Overworld exterior itself), so it needs the same
        // explicit gate as the ocean island or a sky-island world silently never gets its
        // fallback End portal either.
        SkyIslandPlan overworldSkyIsland = limitedSource.skyIsland();
        // Same reasoning again for the chunk island (GOALS 09/37, DESIGN §29.4): it never
        // expresses itself through ExteriorPlan either (the Overworld side always stays
        // normal -- effectiveModeAt's chunk-island branch supplies the exterior itself), so
        // it needs the same explicit gate or a chunk-island world's fallback End-portal
        // guarantee would silently never fire. Unlike island/skyIsland it has no radius of its
        // own to narrow the fallback search by (ProgressionGuarantees.ensureEndPortal's existing
        // skyIsland-disabled branch already falls through to the correct plain border/envelope
        // check), so no new ObjectiveSite.supportiveRadius overload is needed -- only this gate.
        ChunkIslandPlan overworldChunkIsland = limitedSource.chunkIsland();
        // Cave (GOALS 25-26, DESIGN §30) reads its plan straight off the generator, never
        // LimitedBiomeSource, unlike every plan above -- see §30.1.
        CavePlan overworldCave = overworldGenerator instanceof EnvelopedChunkGenerator caveEnveloped
            ? caveEnveloped.cave()
            : CavePlan.disabled();
        // Fetched early (rather than only inside the `nether != null` block below) so its own
        // sky island plan (GOALS 06, DESIGN §27.6) can join the exteriorObjective gate the same
        // way the Overworld's does -- it never expresses itself through ExteriorPlan either.
        ServerLevel nether = server.getLevel(Level.NETHER);
        ChunkGenerator netherGenerator = nether == null ? null : nether.getChunkSource().getGenerator();
        StripPlan netherStrip = netherGenerator instanceof EnvelopedChunkGenerator enveloped
            ? enveloped.strip()
            : StripPlan.disabled();
        SkyIslandPlan netherSkyIsland = netherGenerator instanceof EnvelopedChunkGenerator enveloped
            ? enveloped.skyIsland()
            : SkyIslandPlan.disabled();
        // Nether-start (GOALS 27, DESIGN §31) reads its plan straight off the Nether generator,
        // never LimitedBiomeSource -- see §31.5. Fetched here rather than only inside the
        // `nether != null` block below purely to sit next to netherStrip/netherSkyIsland's own
        // identical fetch, even though (unlike them) it doesn't join the exteriorObjective gate:
        // Nether-start never expresses itself through ExteriorPlan either way (the Overworld and
        // Nether both stay ordinary vanilla terrain, §31.5), but it also isn't a beatability
        // fallback the way the End-portal/blaze guarantees are -- it's the primary mechanism, so
        // it gates on its own needsNetherStart flag below instead.
        NetherStartPlan netherStart = netherGenerator instanceof EnvelopedChunkGenerator enveloped
            ? enveloped.netherStart()
            : NetherStartPlan.disabled();
        boolean exteriorObjective = (plan.overworld().ensureObjective()
            && (exterior.overworld().mode() != ExteriorMode.NORMAL || overworldStrip.enabled()
                || overworldIsland.enabled() || overworldSkyIsland.enabled() || overworldChunkIsland.enabled()))
            || (plan.nether().ensureObjective()
                && (exterior.nether().mode() != ExteriorMode.NORMAL || netherSkyIsland.enabled()));
        // GOALS 03's chest-boat spawn is unrelated to borders/the End-portal guarantee -- gated
        // in here anyway so it still runs (once) for a chest-boat world with no border/objective
        // configured at all, reusing the same one-time WorldLimitState guard below.
        boolean needsChestBoat = overworldIsland.enabled() && !overworldIsland.hasLand();
        // Same reasoning for the sky island's own necessities chest (GOALS 05, DESIGN §27.8):
        // every sky island world gets one, regardless of whether any border/objective is
        // configured at all.
        boolean needsStarterChest = overworldSkyIsland.enabled();
        // Same reasoning again for the guaranteed village (GOALS 07, DESIGN §28.3): every world
        // with scattered floating islands enabled gets one, unconditionally.
        boolean needsGuaranteedVillage = overworldSkyIsland.enabled() && overworldSkyIsland.floatingIslands().enabled();
        // Same reasoning again for the guaranteed portal room (GOALS 09, DESIGN §29.4): every
        // chunk-island world gets one, unconditionally -- it's the primary beatability
        // mechanism, not a fallback (exteriorObjective's own generic vault is still a secondary
        // safety net on top, per limit.ensureObjective()).
        boolean needsGuaranteedPortalRoom = overworldChunkIsland.enabled();
        // Same reasoning again for cave's own optional starter chest (GOALS 25, DESIGN §30.3):
        // every cave world with the chest option enabled gets one, regardless of border/objective.
        boolean needsCaveChest = overworldCave.enabled() && overworldCave.chestEnabled();
        // Same reasoning again for Nether-start's own safe-site resolution (GOALS 27, DESIGN
        // §31.2): every nether_start world needs its world-spawn redirect, unconditionally.
        boolean needsNetherStart = netherStart.enabled();
        if (!plan.enabled() && !exteriorObjective && !needsChestBoat && !needsStarterChest
            && !needsGuaranteedVillage && !needsGuaranteedPortalRoom && !needsCaveChest && !needsNetherStart) {
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
            overworld, plan.overworld(), exterior.overworld(), overworldStrip, overworldIsland, overworldSkyIsland,
            limitedSource.worldLayoutPlan(), originX, originZ
        );
        if (needsChestBoat) {
            StarterKitDeployment.spawnChestBoat(overworld, originX, originZ);
        }
        if (needsStarterChest) {
            StarterKitDeployment.spawnStarterChest(overworld, originX, originZ, overworldSkyIsland);
        }
        if (needsGuaranteedVillage) {
            FloatingIslandsDeployment.placeGuaranteedVillage(overworld, originX, originZ, overworldSkyIsland);
        }
        if (needsGuaranteedPortalRoom) {
            ChunkIslandDeployment.placeGuaranteedPortalRoom(overworld, originX, originZ, overworldChunkIsland);
        }
        if (needsCaveChest) {
            // Cave's own spawn is resolved by SpawnOriginManager.resolveCaveOrigin, never
            // originX/originZ (which stay 0,0 for this preset -- cave has no layout-origin
            // search of its own) -- read the actual placed position back from vanilla's own
            // persisted respawn data instead of threading a second stored coordinate through
            // SpawnOriginState (DESIGN §30.3).
            StarterKitDeployment.spawnCaveStarterChest(overworld, overworld.getRespawnData().pos(), overworldCave);
        }
        if (overworldChunkIsland.enabled() && overworldGenerator instanceof EnvelopedChunkGenerator enveloped
            && enveloped.delegate() instanceof NoiseBasedChunkGenerator noiseGenerator) {
            enveloped.setChunkIslandShowcaseCells(ChunkIslandShowcaseSearch.findShowcaseCells(
                overworld, noiseGenerator, overworldChunkIsland, originX, originZ
            ));
        }
        BorderInitResult netherResult = BorderInitResult.NONE;
        if (nether != null) {
            // Layout origins are Overworld-only (DESIGN §18); the Nether's border and
            // progression objective remain centered at the world origin (0, 0).
            netherResult = initializeBorder(nether, plan.nether(), "Nether", 0, 0);
            ProgressionGuarantees.ensureBlazeAccess(nether, plan.nether(), exterior.nether(), netherStrip, netherSkyIsland);
            if (needsNetherStart) {
                NetherStartDeployment.resolveAndRedirectSpawn(overworld, nether, netherStart);
            }
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
