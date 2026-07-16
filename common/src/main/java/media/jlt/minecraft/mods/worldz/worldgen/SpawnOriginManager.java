package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.SpawnSearchPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

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
     * @return the spawn position to use instead of vanilla's own search, only
     *     when a {@code PREFERRED_NATURAL_BIOME} search found its target
     *     biome; empty means the caller should let vanilla's own spawn
     *     selection proceed unmodified (covers {@code STARTER_AT_ORIGIN},
     *     {@code VANILLA_SPAWN}, and every fallback case)
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

        if (limitedSource.spawnStrategy() != SpawnStrategy.PREFERRED_NATURAL_BIOME) {
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.empty();
        }

        Optional<Holder<Biome>> target = limitedSource.starterBiome();
        if (target.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Layout strategy preferred_natural_biome needs a starter biome; using starter_at_origin instead."
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.empty();
        }

        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        ChunkGenerator delegate = generator instanceof EnvelopedChunkGenerator enveloped ? enveloped.delegate() : generator;
        if (!(delegate instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            WorldzCommon.LOGGER.warn(
                "Cannot build a real climate sampler for preferred_natural_biome; using starter_at_origin instead."
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.empty();
        }

        Optional<BlockPos> found = search(overworld, noiseGenerator, target.get());
        if (found.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "preferred_natural_biome search found no {} within {} blocks of the origin; using starter_at_origin instead.",
                target.get().getRegisteredName(),
                SpawnSearchPlan.DEFAULT_MAX_RADIUS_BLOCKS
            );
            markResolved(overworld, limitedSource, 0, 0);
            return Optional.empty();
        }

        int originX = found.get().getX();
        int originZ = found.get().getZ();
        markResolved(overworld, limitedSource, originX, originZ);

        int height = generator.getSpawnHeight(overworld);
        if (height < overworld.getMinY()) {
            height = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, originX + 8, originZ + 8);
        }
        return Optional.of(new BlockPos(originX + 8, height, originZ + 8));
    }

    private static Optional<BlockPos> search(
        ServerLevel overworld,
        NoiseBasedChunkGenerator noiseGenerator,
        Holder<Biome> target
    ) {
        HolderGetter<Biome> biomeGetter = overworld.registryAccess().lookupOrThrow(Registries.BIOME);
        // Independent of the level's own RandomState, which is not meaningful for a
        // wrapping top-level generator like EnvelopedChunkGenerator (see MEMORY.md's
        // "Known Risks": ChunkMap only builds a real RandomState for an actual
        // NoiseBasedChunkGenerator). Built fresh from the delegate's real settings so
        // this search reflects genuine vanilla climate rather than a dummy router.
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

    private static void markResolved(ServerLevel overworld, LimitedBiomeSource source, int originX, int originZ) {
        overworld.getDataStorage().set(SpawnOriginState.TYPE, new SpawnOriginState(true, originX, originZ));
        source.setOrigin(originX, originZ);
    }
}
