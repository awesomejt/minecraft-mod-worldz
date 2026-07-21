package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Force-generates the guaranteed portal room (GOALS 09, DESIGN §29.4) on one reserved chunk-
 * island cell, once per world. Reuses the exact real vanilla structure-generation API
 * {@code /place structure} uses ({@code net.minecraft.server.commands.PlaceCommand.placeStructure},
 * verified against the real 26.2 decompiled sources during 11.1's design pass, reused here
 * unchanged in 12.1) rather than hand-building a stronghold -- mirrors {@code
 * FloatingIslandsDeployment.placeGuaranteedVillage} exactly, just forcing
 * {@code minecraft:stronghold} instead of a village variant. Every vanilla stronghold's own
 * generation loop already retries until it contains a portal-room piece (confirmed by reading
 * {@code StrongholdStructure.generatePieces}), so "guarantee a portal room" reduces exactly to
 * "guarantee a stronghold" -- no piece-targeting logic is needed here at all.
 */
final class ChunkIslandDeployment {
    private static final String STRONGHOLD_STRUCTURE_ID = "minecraft:stronghold";

    private ChunkIslandDeployment() {
    }

    /**
     * Places the guaranteed portal room, once, only for a world with chunk islands enabled. No-op
     * (with a logged warning) if the stronghold structure id is unknown -- never crashes world
     * startup. Unlike the guaranteed village, stronghold placement has no realistic failure path
     * ({@code StrongholdStructure.findGenerationPoint} always returns a stub and its own
     * piece-placement loop always eventually succeeds), so no {@code start.isValid()} guard is
     * needed here the way the village deployment needs one.
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param chunkIsland the world's resolved Overworld chunk island plan
     */
    static void placeGuaranteedPortalRoom(ServerLevel overworld, int originX, int originZ, ChunkIslandPlan chunkIsland) {
        long seed = overworld.getSeed();
        ChunkIslandPlan.PortalCell cell = chunkIsland.reservedPortalCell(seed);
        int[] center = cell.centerBlock(chunkIsland.cellSizeChunks());
        int siteX = center[0] + originX;
        int siteZ = center[1] + originZ;

        Holder.Reference<Structure> structureHolder = overworld.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(STRONGHOLD_STRUCTURE_ID)))
            .orElse(null);
        if (structureHolder == null) {
            WorldzCommon.LOGGER.warn("Unknown GOALS 09 guaranteed-portal-room structure '{}'; skipping.", STRONGHOLD_STRUCTURE_ID);
            return;
        }

        BlockPos pos = new BlockPos(siteX, overworld.getSeaLevel(), siteZ);
        overworld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        Structure structure = structureHolder.value();
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        StructureStart start = structure.generate(
            structureHolder,
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
            WorldzCommon.LOGGER.warn("GOALS 09 guaranteed-portal-room placement failed to find a valid site near ({}, {}).", siteX, siteZ);
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
        WorldzCommon.LOGGER.info("Placed the GOALS 09 guaranteed portal room (stronghold) near ({}, {}).", siteX, siteZ);
    }
}
