package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
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
 * Force-generates the guaranteed village (GOALS 07, DESIGN §28.3) beyond a sky island's floating-
 * islands exclusion zone, once per world. Reuses the exact real vanilla structure-generation API
 * {@code /place structure} uses ({@code net.minecraft.server.commands.PlaceCommand.placeStructure},
 * verified against the real 26.2 decompiled sources during 11.1's design pass) rather than hand-
 * building a village the way the fallback End-portal/blaze vaults stand in for structures this
 * project can't risk depending on -- unlike those, a real vanilla village is exactly what GOALS 07
 * asks for, and forcing one with vanilla's own jigsaw machinery is both correct and low-risk.
 */
final class FloatingIslandsDeployment {
    private FloatingIslandsDeployment() {
    }

    /**
     * Places the guaranteed village, once, only for a world with scattered floating islands
     * enabled. No-op (with a logged warning) if the target structure id is unknown or the jigsaw
     * search fails to find a valid placement -- never crashes world startup.
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param skyIsland the world's resolved sky island plan
     */
    static void placeGuaranteedVillage(ServerLevel overworld, int originX, int originZ, SkyIslandPlan skyIsland) {
        FloatingIslandsPlan floatingIslands = skyIsland.floatingIslands();
        long seed = overworld.getSeed();
        FloatingIslandsPlan.VillageSite site = floatingIslands.guaranteedVillageSite(seed);
        int siteX = (int) Math.round(site.centerX()) + originX;
        int siteZ = (int) Math.round(site.centerZ()) + originZ;

        Holder.Reference<Structure> structureHolder = overworld.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(site.structureId())))
            .orElse(null);
        if (structureHolder == null) {
            WorldzCommon.LOGGER.warn("Unknown GOALS 07 guaranteed-village structure '{}'; skipping.", site.structureId());
            return;
        }

        BlockPos pos = new BlockPos(siteX, skyIsland.surfaceY(), siteZ);
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
            WorldzCommon.LOGGER.warn("GOALS 07 guaranteed-village placement failed to find a valid site near ({}, {}).", siteX, siteZ);
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
        WorldzCommon.LOGGER.info("Placed the GOALS 07 guaranteed village ('{}') near ({}, {}).", site.structureId(), siteX, siteZ);
    }
}
