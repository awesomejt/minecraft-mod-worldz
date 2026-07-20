package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.NaturalIslandSearch;
import media.jlt.minecraft.mods.worldz.logic.SpawnSearchPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.WorldSnapshotWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Resolves and applies the {@code PREFERRED_NATURAL_BIOME} layout origin
 * (DESIGN §18). Two distinct entry points, matching the Phase 16.1 spike's
 * two-hook design:
 *
 * <ul>
 * <li>{@link #reapplyPersistedOrigin} — call on every level load (NeoForge's
 * {@code LevelEvent.Load}, Fabric's {@code ServerLevelEvents.LOAD}). Only
 * re-applies an already-resolved origin to the freshly codec-decoded
 * {@code LimitedBiomeSource} (whose mutable origin field always resets to
 * {@code 0} on decode); does nothing for a world whose origin is not yet
 * resolved, leaving that to the hook below.</li>
 * <li>{@link #resolveFreshOrigin} — call only where vanilla itself is about
 * to choose the initial spawn for a brand-new world (NeoForge's cancellable
 * {@code LevelEvent.CreateSpawnPosition}; an equivalent Fabric mixin into
 * {@code MinecraftServer.setInitialSpawn}, since Fabric API has no matching
 * event). Performs the search, persists the result, and returns the spawn
 * position to use in place of vanilla's own search.</li>
 * </ul>
 */
public final class SpawnOriginManager {
    private SpawnOriginManager() {
    }

    /**
     * Re-applies an already-persisted origin to this (freshly constructed)
     * {@code LimitedBiomeSource} instance. Safe to call for every level, every
     * load; does nothing when the source is not a {@code LimitedBiomeSource}
     * or the origin has not been resolved yet.
     *
     * @param overworld the just-loaded Overworld level
     */
    public static void reapplyPersistedOrigin(ServerLevel overworld) {
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return;
        }
        SpawnOriginState state = overworld.getDataStorage().get(SpawnOriginState.TYPE);
        if (state != null && state.initialized()) {
            limitedSource.setOrigin(state.originBlockX(), state.originBlockZ());
        }
    }

    /**
     * Resolves the layout origin for a brand-new world, only meaningful once
     * per world. The caller must only invoke this where vanilla itself is
     * about to choose the initial spawn (already guarded there by
     * {@code !levelData.isInitialized()}), since a fresh
     * {@code PREFERRED_NATURAL_BIOME} search only makes sense before any
     * spawn-chunk generation has happened.
     *
     * @param overworld the newly constructed Overworld level
     * @return the spawn position to use instead of vanilla's own search;
     *     empty only for {@code VANILLA_SPAWN}, which means exactly what it
     *     says -- let vanilla's own spawn selection proceed unmodified
     */
    public static Optional<BlockPos> resolveFreshOrigin(ServerLevel overworld) {
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return Optional.empty();
        }

        SpawnOriginState existing = overworld.getDataStorage().get(SpawnOriginState.TYPE);
        if (existing != null && existing.initialized()) {
            // Already resolved (e.g. a prior boot crashed after resolving but before
            // levelData.setInitialized(true) committed). Re-apply and defer to vanilla.
            limitedSource.setOrigin(existing.originBlockX(), existing.originBlockZ());
            return Optional.empty();
        }

        IslandPlan island = limitedSource.island();
        if (island.enabled() && island.hasLand() && !island.syntheticLand()) {
            // GOALS 02: ocean_island's own NATURAL source. Independent of spawnStrategy (which
            // stays STARTER_AT_ORIGIN for every ocean_island source, DESIGN §24.8) since this
            // needs an isolation-aware search PREFERRED_NATURAL_BIOME's simple point-match
            // can't do -- see DESIGN §25.4.
            return resolveNaturalIslandOrigin(overworld, limitedSource, island.radiusBlocks());
        }

        if (limitedSource.spawnStrategy() == SpawnStrategy.VANILLA_SPAWN) {
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.empty();
        }

        if (limitedSource.spawnStrategy() != SpawnStrategy.PREFERRED_NATURAL_BIOME) {
            // STARTER_AT_ORIGIN (and any future default): place spawn at the guaranteed
            // starter zone explicitly (2026-07-17 fix) instead of deferring to vanilla's own
            // findSpawnPosition(), which searches the *underlying, unmodified* climate
            // sampler for a "spawn-favorable" signature -- entirely independent of whatever
            // biome LimitedBiomeSource actually reports there. For seeds whose raw climate
            // near (0,0) doesn't match vanilla's criteria, that search can travel up to
            // ~2048 blocks away even though the starter zone itself is guaranteed safe,
            // solid, and the intended biome. Confirmed in-game (Jason, 2026-07-17): a
            // ChunkBase-selected seed with vanilla-favorable climate at (0,0) spawned
            // correctly on both loaders, proving this was never loader-specific.
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        Optional<Holder<Biome>> target = limitedSource.starterBiome();
        if (target.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Layout strategy preferred_natural_biome needs a starter biome; using starter_at_origin instead."
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        ChunkGenerator delegate = generator instanceof EnvelopedChunkGenerator enveloped ? enveloped.delegate() : generator;
        if (!(delegate instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            WorldzCommon.LOGGER.warn(
                "Cannot build a real climate sampler for preferred_natural_biome; using starter_at_origin instead."
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        Optional<BlockPos> found = search(overworld, noiseGenerator, target.get());
        if (found.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "preferred_natural_biome search found no {} within {} blocks of the origin; using starter_at_origin instead.",
                target.get().getRegisteredName(),
                SpawnSearchPlan.DEFAULT_MAX_RADIUS_BLOCKS
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        int originX = found.get().getX();
        int originZ = found.get().getZ();
        markResolved(overworld, limitedSource, originX, originZ);
        return Optional.of(safeSpawnNear(overworld, limitedSource, originX, originZ));
    }

    /**
     * Resolves a safe surface spawn position at the given origin, without relying on
     * vanilla's own climate-based {@code findSpawnPosition()} search (see the
     * {@code STARTER_AT_ORIGIN} note in {@link #resolveFreshOrigin} for why that search is
     * unsuitable here). Offsets into the center of the origin chunk when there's room, but
     * clamps that offset to stay inside the Overworld border's initial radius -- otherwise a
     * small border (as low as 1 block, since 0.2.13) would place spawn beyond the border
     * itself, triggering vanilla's own push-back/damage the instant the world loads.
     */
    private static BlockPos safeSpawnNear(
        ServerLevel overworld,
        LimitedBiomeSource limitedSource,
        int originX,
        int originZ
    ) {
        int offset = limitedSource.worldLimits().overworld().safeSpawnOffsetBlocks();
        int spawnX = originX + offset;
        int spawnZ = originZ + offset;
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        int height = generator.getSpawnHeight(overworld);
        if (height < overworld.getMinY()) {
            height = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, spawnX, spawnZ);
        }
        return new BlockPos(spawnX, height, spawnZ);
    }

    private static Optional<BlockPos> search(
        ServerLevel overworld,
        NoiseBasedChunkGenerator noiseGenerator,
        Holder<Biome> target
    ) {
        HolderGetter<Biome> biomeGetter = overworld.registryAccess().lookupOrThrow(Registries.BIOME);
        // Built independently rather than reusing the level's own RandomState, kept
        // as a belt-and-suspenders safeguard even now that ChunkMapMixin fixes the
        // level's own RandomState too (it used to silently fall back to a dummy,
        // zero-density one for any generator that wasn't directly `instanceof
        // NoiseBasedChunkGenerator`, which EnvelopedChunkGenerator never was -- see
        // DESIGN's "Known caveats" and MEMORY.md's "Known Risks").
        RandomState realRandomState = RandomState.create(
            noiseGenerator.generatorSettings().value(),
            overworld.registryAccess().lookupOrThrow(Registries.NOISE),
            overworld.getSeed()
        );
        Climate.Sampler sampler = realRandomState.sampler();
        Climate.ParameterList<Holder<Biome>> overworldParameters = new MultiNoiseBiomeSourceParameterList(
            MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD,
            biomeGetter
        ).parameters();
        MultiNoiseBiomeSource naturalView = MultiNoiseBiomeSource.createFromList(overworldParameters);
        int sampleQuartY = QuartPos.fromBlock(overworld.getSeaLevel());

        for (SpawnSearchPlan.Offset offset : SpawnSearchPlan.defaults().offsetsInSearchOrder()) {
            Holder<Biome> sampled = naturalView.getNoiseBiome(
                QuartPos.fromBlock(offset.x()), sampleQuartY, QuartPos.fromBlock(offset.z()), sampler
            );
            if (sampled.equals(target)) {
                return Optional.of(new BlockPos(offset.x(), 0, offset.z()));
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the origin for GOALS 02's {@code NATURAL} island source (DESIGN §25.4): searches
     * for an isolated natural landmass instead of a specific target biome, falling back to the
     * world origin (real terrain there is used as-is, whatever it happens to be) exactly like
     * every other search failure path in this class.
     */
    private static Optional<BlockPos> resolveNaturalIslandOrigin(
        ServerLevel overworld, LimitedBiomeSource limitedSource, int radiusBlocks
    ) {
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        ChunkGenerator delegate = generator instanceof EnvelopedChunkGenerator enveloped ? enveloped.delegate() : generator;
        if (!(delegate instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            WorldzCommon.LOGGER.warn(
                "Cannot build a real climate sampler for the natural island search; using the origin instead."
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        Optional<BlockPos> found = searchNaturalIsland(overworld, noiseGenerator, radiusBlocks);
        if (found.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Natural island search found no isolated land within {} blocks of the origin; using the origin instead.",
                SpawnSearchPlan.DEFAULT_MAX_RADIUS_BLOCKS
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));
        }

        int originX = found.get().getX();
        int originZ = found.get().getZ();
        markResolved(overworld, limitedSource, originX, originZ);
        return Optional.of(safeSpawnNear(overworld, limitedSource, originX, originZ));
    }

    private static Optional<BlockPos> searchNaturalIsland(
        ServerLevel overworld, NoiseBasedChunkGenerator noiseGenerator, int radiusBlocks
    ) {
        HolderGetter<Biome> biomeGetter = overworld.registryAccess().lookupOrThrow(Registries.BIOME);
        RandomState realRandomState = RandomState.create(
            noiseGenerator.generatorSettings().value(),
            overworld.registryAccess().lookupOrThrow(Registries.NOISE),
            overworld.getSeed()
        );
        Climate.Sampler sampler = realRandomState.sampler();
        Climate.ParameterList<Holder<Biome>> overworldParameters = new MultiNoiseBiomeSourceParameterList(
            MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD,
            biomeGetter
        ).parameters();
        MultiNoiseBiomeSource naturalView = MultiNoiseBiomeSource.createFromList(overworldParameters);
        int sampleQuartY = QuartPos.fromBlock(overworld.getSeaLevel());

        for (SpawnSearchPlan.Offset offset : SpawnSearchPlan.defaults().offsetsInSearchOrder()) {
            boolean centerIsOcean = isOceanBiomeAt(naturalView, sampler, sampleQuartY, offset.x(), offset.z());
            boolean isolated = NaturalIslandSearch.isIsolatedLand(
                offset.x(), offset.z(), radiusBlocks, centerIsOcean,
                (ringX, ringZ) -> isOceanBiomeAt(naturalView, sampler, sampleQuartY, ringX, ringZ)
            );
            if (isolated) {
                return Optional.of(new BlockPos(offset.x(), 0, offset.z()));
            }
        }
        return Optional.empty();
    }

    private static boolean isOceanBiomeAt(
        MultiNoiseBiomeSource naturalView, Climate.Sampler sampler, int sampleQuartY, int x, int z
    ) {
        Holder<Biome> biome = naturalView.getNoiseBiome(QuartPos.fromBlock(x), sampleQuartY, QuartPos.fromBlock(z), sampler);
        return biome.is(BiomeTags.IS_OCEAN);
    }

    private static void markResolved(ServerLevel overworld, LimitedBiomeSource source, int originX, int originZ) {
        overworld.getDataStorage().set(SpawnOriginState.TYPE, new SpawnOriginState(true, originX, originZ));
        source.setOrigin(originX, originZ);
        writeSnapshot(overworld, source);
    }

    /**
     * Best-effort per-world settings snapshot (DESIGN §20.3): a reference record only, never
     * read back by the mod, so any failure here must never block world creation.
     */
    private static void writeSnapshot(ServerLevel overworld, LimitedBiomeSource source) {
        try {
            WorldSnapshotWriter.WorldSnapshot snapshot = new WorldSnapshotWriter.WorldSnapshot(
                WorldzCommon.MOD_VERSION,
                Instant.now().toString(),
                source.allowedBiomes().stream().map(SpawnOriginManager::registeredName).toList(),
                source.starterBiome().map(SpawnOriginManager::registeredName).orElse(""),
                source.starterRadiusBlocks(),
                source.starterLandPlan(),
                source.worldLimits(),
                source.exteriorPlan(),
                source.effectiveLayoutPlan(),
                source.spawnStrategy(),
                source.originBlockX(),
                source.originBlockZ(),
                source.allowRivers(),
                source.allowOceans()
            );
            Path target = overworld.getServer().getWorldPath(LevelResource.ROOT)
                .resolve(WorldzCommon.MOD_ID + "-snapshot.yaml");
            Files.writeString(target, WorldSnapshotWriter.render(snapshot));
        } catch (IOException | RuntimeException exception) {
            WorldzCommon.LOGGER.warn("Could not write the per-world settings snapshot: {}", exception.getMessage());
        }
    }

    private static String registeredName(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElseGet(holder::getRegisteredName);
    }
}
