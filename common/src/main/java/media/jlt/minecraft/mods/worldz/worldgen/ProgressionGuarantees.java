package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.datafixers.util.Pair;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ObjectiveSite;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.OptionalInt;

/** Creates compact progression sites when vanilla structures do not fit. */
final class ProgressionGuarantees {
    private static final int NATURAL_STRUCTURE_MARGIN = 128;

    private ProgressionGuarantees() {
    }

    static void ensureEndPortal(
        ServerLevel overworld,
        WorldLimitPlan.DimensionLimit limit,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        IslandPlan island,
        WorldLayoutPlan layoutPlan,
        int originX,
        int originZ
    ) {
        if (!limit.ensureObjective()) {
            return;
        }
        OptionalInt supportiveRadius = ObjectiveSite.supportiveRadius(
            limit.enabled(), limit.finalRadiusBlocks(), envelope, island
        );
        if (supportiveRadius.isEmpty()) {
            return;
        }
        int radius = supportiveRadius.getAsInt();
        int zRadius = ObjectiveSite.narrowForStrip(radius, strip);

        BlockPos natural = overworld.findNearestMapStructure(
            StructureTags.EYE_OF_ENDER_LOCATED, new BlockPos(originX, 0, originZ), 100, false
        );
        if (natural != null
            && ObjectiveSite.fitsInside(natural.getX() - originX, natural.getZ() - originZ, radius, zRadius, NATURAL_STRUCTURE_MARGIN)
            && ObjectiveSite.isSupportiveColumn(layoutPlan, natural.getX() - originX, natural.getZ() - originZ)) {
            WorldzCommon.LOGGER.info("Natural stronghold at {} fits inside the Worldz supportive radius {}.", natural, radius);
            return;
        }

        int relativeX = ObjectiveSite.fallbackX(radius);
        int relativeZ = ObjectiveSite.supportiveFallbackZ(layoutPlan, relativeX, radius, zRadius, NATURAL_STRUCTURE_MARGIN);
        int x = originX + relativeX;
        int z = originZ + relativeZ;
        // Level.getHeight silently returns getMinY() for a chunk that has not loaded yet
        // (LevelReader never forces generation for a plain height query) -- this runs at
        // world creation, before the fallback site's own chunk has ever loaded, so the
        // portal must force it to generate first or it always lands on the world floor.
        overworld.getChunk(x >> 4, z >> 4);
        int surfaceY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos center = new BlockPos(x, surfaceY, z);
        buildEndPortalSite(overworld, center);
        WorldzCommon.LOGGER.info(
            "Created compact End portal site at {} because no natural stronghold safely fits radius {}.", center, radius
        );
    }

    static void ensureBlazeAccess(
        ServerLevel nether,
        WorldLimitPlan.DimensionLimit limit,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip
    ) {
        if (!limit.ensureObjective()) {
            return;
        }
        OptionalInt supportiveRadius = ObjectiveSite.supportiveRadius(
            limit.enabled(), limit.finalRadiusBlocks(), envelope
        );
        if (supportiveRadius.isEmpty()) {
            return;
        }
        int radius = supportiveRadius.getAsInt();
        int zRadius = ObjectiveSite.narrowForStrip(radius, strip);

        Holder<Structure> fortress = nether.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .getOrThrow(BuiltinStructures.FORTRESS);
        int searchRadius = Math.min(100, Math.max(1, (radius + 15) / 16));
        Pair<BlockPos, Holder<Structure>> natural = nether.getChunkSource()
            .getGenerator()
            .findNearestMapStructure(nether, HolderSet.direct(fortress), BlockPos.ZERO, searchRadius, false);
        if (natural != null && ObjectiveSite.fitsInside(
            natural.getFirst().getX(), natural.getFirst().getZ(), radius, zRadius, NATURAL_STRUCTURE_MARGIN
        )) {
            WorldzCommon.LOGGER.info(
                "Natural Nether fortress at {} fits inside the Worldz supportive radius {}.", natural.getFirst(), radius
            );
            return;
        }

        BlockPos spawner = new BlockPos(ObjectiveSite.fallbackX(radius), 64, 0);
        buildBlazeSite(nether, spawner);
        WorldzCommon.LOGGER.info(
            "Created compact blaze-spawner site at {} because no natural fortress safely fits radius {}.", spawner, radius
        );
    }

    private static void buildEndPortalSite(ServerLevel level, BlockPos center) {
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlock(center.offset(dx, -1, dz), bricks, Block.UPDATE_ALL);
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        placePortalFrames(level, center, Direction.NORTH, 0, -2, true);
        placePortalFrames(level, center, Direction.SOUTH, 0, 2, true);
        placePortalFrames(level, center, Direction.EAST, -2, 0, false);
        placePortalFrames(level, center, Direction.WEST, 2, 0, false);

        for (int dx : new int[]{-5, 5}) {
            for (int dz : new int[]{-5, 5}) {
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlock(center.offset(dx, dy, dz), bricks, Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void placePortalFrames(
        ServerLevel level,
        BlockPos center,
        Direction facing,
        int fixedX,
        int fixedZ,
        boolean varyX
    ) {
        BlockState frame = Blocks.END_PORTAL_FRAME.defaultBlockState()
            .setValue(EndPortalFrameBlock.FACING, facing)
            .setValue(EndPortalFrameBlock.HAS_EYE, false);
        for (int offset = -1; offset <= 1; offset++) {
            int x = varyX ? offset : fixedX;
            int z = varyX ? fixedZ : offset;
            level.setBlock(center.offset(x, 0, z), frame, Block.UPDATE_ALL);
        }
    }

    private static void buildBlazeSite(ServerLevel level, BlockPos spawnerPos) {
        BlockState bricks = Blocks.NETHER_BRICKS.defaultBlockState();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 6; dy++) {
                    boolean shell = dy == -1 || dy == 6 || Math.abs(dx) == 6 || Math.abs(dz) == 6;
                    level.setBlock(
                        spawnerPos.offset(dx, dy, dz),
                        shell ? bricks : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                    );
                }
            }
        }

        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
                level.setBlock(spawnerPos.offset(-6, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity blockEntity = level.getBlockEntity(spawnerPos);
        if (blockEntity instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(EntityTypes.BLAZE, level.getRandom());
        } else {
            throw new IllegalStateException("Could not create blaze spawner at " + spawnerPos);
        }
    }
}
