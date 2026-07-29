package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ObjectiveSite;
import media.jlt.minecraft.mods.worldz.logic.StackedLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.StackedPlan;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.OptionalInt;

/**
 * Force-generates a real vanilla village on the {@code jlt_worldz:stacked} preset's top layer
 * (GOAL 35 follow-up, DESIGN §34.9), once per world, provided the top layer's own biome is
 * village-compatible. Mirrors {@link FloatingIslandsDeployment#placeGuaranteedVillage} exactly
 * (same real vanilla {@code Structure.generate}/{@code placeInChunk} mechanism, GOALS 07's own
 * precedent) -- always forced at a fixed, deterministic site, never a natural-search-first
 * attempt, since natural random-spread placement isn't reliable to land anywhere reachable in a
 * small, bounded stacked world.
 *
 * <p>Unlike {@link FloatingIslandsDeployment}/{@link ChunkIslandDeployment}, which each store a
 * hardcoded biome-to-structure-id table, biome eligibility here is derived entirely from real
 * vanilla data: {@code minecraft:villages}' own {@link StructureSet#structures()} entries are
 * walked for the first one whose {@link Structure#biomes()} accepts the top layer's biome --
 * exactly the same real-data-driven check {@code EnvelopedChunkGenerator
 * .warnUnreachableFlatStructureOverrides} already uses, just resolving which structure to force
 * rather than warning about one that won't ever place. This makes the two-array-drifting-out-of-
 * sync bug class {@link media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan}'s own table
 * documents structurally impossible here.
 */
final class StackedVillageDeployment {
    private StackedVillageDeployment() {
    }

    /**
     * Places the guaranteed top-layer village, once, for a stacked world with {@code
     * forceTopVillage} enabled. No-op (with an {@code INFO} log, not a warning) if the top layer's
     * biome doesn't support villages -- an expected, valid outcome, not a misconfiguration. No-op
     * (with a {@code WARN} log) if the top biome id is unresolvable or the jigsaw search itself
     * fails to find a valid placement -- never crashes world startup.
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param stacked the world's resolved stacked plan
     * @param limit the Overworld's resolved border/limit plan
     * @param envelope the Overworld's resolved exterior envelope
     * @param strip the Overworld's resolved strip-world plan
     * @param layoutPlan the world's coordinated-layout plan
     */
    static void placeGuaranteedTopVillage(
        ServerLevel overworld,
        int originX,
        int originZ,
        StackedPlan stacked,
        WorldLimitPlan.DimensionLimit limit,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        WorldLayoutPlan layoutPlan
    ) {
        long seed = overworld.getSeed();
        List<StackedLayerSpec> resolved = stacked.resolvedLayers(seed);
        String topBiomeId = resolved.get(resolved.size() - 1).biome();

        Holder<Biome> topBiome = overworld.registryAccess().lookupOrThrow(Registries.BIOME)
            .get(ResourceKey.create(Registries.BIOME, Identifier.parse(topBiomeId)))
            .orElse(null);
        if (topBiome == null) {
            WorldzCommon.LOGGER.warn("Unknown stacked top-layer biome '{}'; skipping forced top-layer village.", topBiomeId);
            return;
        }

        Holder<StructureSet> villages = overworld.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
            .get(BuiltinStructureSets.VILLAGES)
            .orElse(null);
        if (villages == null) {
            WorldzCommon.LOGGER.warn("Could not find the real 'minecraft:villages' structure set; skipping forced top-layer village.");
            return;
        }

        Holder<Structure> matched = villages.value().structures().stream()
            .map(StructureSet.StructureSelectionEntry::structure)
            .filter(structureHolder -> structureHolder.value().biomes().contains(topBiome))
            .findFirst()
            .orElse(null);
        if (matched == null) {
            WorldzCommon.LOGGER.info(
                "Stacked top layer's biome '{}' is not village-compatible; forceTopVillage stays a no-op this world.", topBiomeId
            );
            return;
        }

        // Unbounded (worldSizeChunks: 0) worlds report an empty supportive radius -- unlike
        // ensureEndPortal's fallback-of-last-resort posture, this feature always forces a
        // village regardless of boundedness, so a large synthetic radius stands in; fallbackX/
        // supportiveFallbackZ naturally degrade to their own small PREFERRED_X-scale default
        // rather than picking something degenerate.
        OptionalInt supportiveRadius = ObjectiveSite.supportiveRadius(limit.enabled(), limit.finalRadiusBlocks(), envelope);
        int radius = supportiveRadius.orElse(Integer.MAX_VALUE);
        ObjectiveSite.ZBounds zBounds = ObjectiveSite.narrowForStrip(radius, strip);
        int relativeX = ObjectiveSite.fallbackX(radius);
        int relativeZ = ObjectiveSite.supportiveFallbackZ(
            layoutPlan, relativeX, radius, zBounds, VILLAGE_MARGIN_BLOCKS
        );
        int siteX = originX + relativeX;
        int siteZ = originZ + relativeZ;
        int surfaceY = StackedPlan.surfaceY(resolved, overworld.getMinY(), overworld.getHeight());

        BlockPos pos = new BlockPos(siteX, surfaceY, siteZ);
        overworld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        Structure structure = matched.value();
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        StructureStart start = structure.generate(
            matched,
            overworld.dimension(),
            overworld.registryAccess(),
            generator,
            generator.getBiomeSource(),
            overworld.getChunkSource().randomState(),
            overworld.getStructureManager(),
            seed,
            ChunkPos.containing(pos),
            0,
            overworld,
            biome -> true
        );
        if (!start.isValid()) {
            WorldzCommon.LOGGER.warn("Stacked forceTopVillage placement failed to find a valid site near ({}, {}).", siteX, siteZ);
            return;
        }

        BoundingBox boundingBox = start.getBoundingBox();
        ChunkPos chunkMin = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()));
        ChunkPos chunkMax = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));
        ChunkPos.rangeClosed(chunkMin, chunkMax).forEach(chunkPos -> {
            overworld.getChunk(chunkPos.x(), chunkPos.z());
            start.placeInChunk(
                overworld,
                overworld.structureManager(),
                generator,
                overworld.getRandom(),
                new BoundingBox(
                    chunkPos.getMinBlockX(), overworld.getMinY(), chunkPos.getMinBlockZ(),
                    chunkPos.getMaxBlockX(), overworld.getMaxY() + 1, chunkPos.getMaxBlockZ()
                ),
                chunkPos
            );
        });
        WorldzCommon.LOGGER.info("Placed the stacked top-layer guaranteed village (biome '{}') near ({}, {}).", topBiomeId, siteX, siteZ);
    }

    /**
     * Structure-fit margin for {@link ObjectiveSite#supportiveFallbackZ} -- no existing constant
     * to reuse cleanly ({@code ProgressionGuarantees.NATURAL_STRUCTURE_MARGIN} is private to a
     * sibling class), so this is a documented first-pass estimate of the same order of magnitude,
     * not verified against a real village's true footprint (DESIGN §34.9's own flagged risk: a
     * real village is very commonly wider than this on a small stacked world's default border).
     */
    private static final int VILLAGE_MARGIN_BLOCKS = 128;
}
