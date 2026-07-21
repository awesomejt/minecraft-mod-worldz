package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnSearchPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Seed-search for chunk islands that naturally showcase underground content (GOALS 37, DESIGN
 * §29.6): reuses {@code SpawnOriginManager.searchNaturalIsland}'s exact technique for cave
 * biomes -- sampling the real seed's <em>unmodified</em> climate at a candidate point via {@link
 * MultiNoiseBiomeSource#getNoiseBiome}, no chunk generation required -- and {@code
 * ChunkGenerator.findNearestMapStructure} (the real vanilla structure-placement query) for
 * structure-bearing chunks. Deliberately does not attempt depth-aware biome forcing (the
 * GOALS-15 Backlog item stays out of scope, DESIGN §29.6); this only <em>prefers</em> chunks the
 * seed already, naturally has.
 *
 * <p>A single representative sampling depth is used per cave biome rather than DESIGN §29.6's
 * originally sketched multi-depth scan -- a scoped simplification (matches this project's
 * precedent, e.g. {@code SkyIslandProfile}'s surface-material heuristic): cave biomes in vanilla
 * noise caves are not perfectly flat bands, so no single depth is guaranteed to catch every
 * occurrence, but one reasonable depth per target is enough for "prefer a naturally-qualifying
 * chunk when one is nearby," not a guarantee.
 */
final class ChunkIslandShowcaseSearch {
    /** Representative sampling depth for the cave-biome search -- comfortably underground. */
    private static final int CAVE_BIOME_SAMPLE_Y = -40;
    /** Bounding search radius for the structure search -- generous but not unbounded. */
    private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 100;
    /** Tag grouping the vanilla structures this search treats as "showcase-worthy" underground content. */
    private static final TagKey<Structure> SHOWCASE_STRUCTURES = TagKey.create(
        Registries.STRUCTURE, Identifier.parse(WorldzCommon.MOD_ID + ":chunk_island_showcase")
    );

    private ChunkIslandShowcaseSearch() {
    }

    /**
     * Runs every showcase search (three cave biomes, one structure category) and returns
     * whichever chunks were found, converted to origin-relative coordinates -- the shape {@code
     * EnvelopedChunkGenerator.chunkIslandHitAt} expects. Missing a target is not an error: this
     * only prefers naturally-qualifying chunks when nearby, per DESIGN §29.6.
     *
     * @param overworld the Overworld server level
     * @param noiseGenerator the world's real noise generator (for {@code RandomState})
     * @param chunkIsland the world's resolved Overworld chunk island plan (for its exclusion zone)
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @return found showcase chunks, relative to the origin chunk
     */
    static Set<ChunkPos> findShowcaseCells(
        ServerLevel overworld,
        NoiseBasedChunkGenerator noiseGenerator,
        ChunkIslandPlan chunkIsland,
        int originX,
        int originZ
    ) {
        int minRadiusBlocks = chunkIsland.exclusionZone().enabled() ? chunkIsland.exclusionZone().radiusBlocks() : 0;
        Set<ChunkPos> found = new HashSet<>();
        for (ResourceKey<Biome> target : List.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK)) {
            findCaveBiomeChunk(overworld, noiseGenerator, target, originX, originZ, minRadiusBlocks)
                .ifPresent(cell -> found.add(toRelative(cell, originX, originZ)));
        }
        findShowcaseStructureChunk(overworld, originX, originZ, minRadiusBlocks)
            .ifPresent(cell -> found.add(toRelative(cell, originX, originZ)));
        if (!found.isEmpty()) {
            WorldzCommon.LOGGER.info("Found {} GOALS 37 underground-content showcase chunk(s).", found.size());
        }
        return Set.copyOf(found);
    }

    private static ChunkPos toRelative(ChunkPos absolute, int originX, int originZ) {
        return new ChunkPos(
            absolute.x() - Math.floorDiv(originX, 16),
            absolute.z() - Math.floorDiv(originZ, 16)
        );
    }

    /**
     * Finds the nearest chunk (beyond {@code minRadiusBlocks}) whose real, unmodified seed
     * biome at {@link #CAVE_BIOME_SAMPLE_Y} matches {@code targetBiome}.
     *
     * @param overworld the Overworld server level
     * @param noiseGenerator the world's real noise generator (for {@code RandomState})
     * @param targetBiome the cave biome to search for
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param minRadiusBlocks search starts beyond this radius (the exclusion zone)
     * @return the found chunk's coordinates, if any within the bounded search
     */
    static Optional<ChunkPos> findCaveBiomeChunk(
        ServerLevel overworld,
        NoiseBasedChunkGenerator noiseGenerator,
        ResourceKey<Biome> targetBiome,
        int originX,
        int originZ,
        int minRadiusBlocks
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
        int sampleQuartY = QuartPos.fromBlock(CAVE_BIOME_SAMPLE_Y);

        for (SpawnSearchPlan.Offset offset : SpawnSearchPlan.defaults().offsetsInSearchOrder()) {
            if (Math.hypot(offset.x(), offset.z()) < minRadiusBlocks) {
                continue;
            }
            int x = originX + offset.x();
            int z = originZ + offset.z();
            Holder<Biome> biome = naturalView.getNoiseBiome(QuartPos.fromBlock(x), sampleQuartY, QuartPos.fromBlock(z), sampler);
            if (biome.is(targetBiome)) {
                return Optional.of(new ChunkPos(x >> 4, z >> 4));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the nearest real vanilla structure among {@link #SHOWCASE_STRUCTURES} beyond {@code
     * minRadiusBlocks}, reusing the same {@code findNearestMapStructure} query {@code /locate
     * structure} itself uses.
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param minRadiusBlocks the found structure must be at least this far from the origin
     * @return the found structure's chunk coordinates, if any within the search radius
     */
    static Optional<ChunkPos> findShowcaseStructureChunk(ServerLevel overworld, int originX, int originZ, int minRadiusBlocks) {
        BlockPos found = overworld.findNearestMapStructure(
            SHOWCASE_STRUCTURES, new BlockPos(originX, 0, originZ), STRUCTURE_SEARCH_RADIUS_CHUNKS, false
        );
        if (found == null || Math.hypot(found.getX() - originX, found.getZ() - originZ) < minRadiusBlocks) {
            return Optional.empty();
        }
        return Optional.of(ChunkPos.containing(found));
    }
}
