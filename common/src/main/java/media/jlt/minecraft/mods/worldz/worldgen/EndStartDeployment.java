package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.LightSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;

import java.util.List;

/**
 * Builds a guaranteed safe End spawn platform (GOALS 34, DESIGN §32.4) and redirects the world's
 * default spawn to it (mirrors {@code NetherStartDeployment.resolveAndRedirectSpawn}, DESIGN
 * §31.2) -- called once, only for a new {@code end_start} world, from {@code WorldLimitManager.
 * onServerStarted}. Unlike {@code NetherStartDeployment}, there is no natural-site search: Jason's
 * decision (DESIGN §32.2) is to always build the guaranteed platform, since the End's outer
 * regions are mostly void and a bounded force-generation search would rarely find real terrain
 * within budget.
 *
 * <p>Configurable size/height/lighting and off-center chest placement (GOALS 41, generalized here
 * 2026-07-25 after Jason's first real in-game test of config 63 found the original fixed
 * 3x3-footprint/1x1-interior platform "too small", the chest invisible underfoot, and asked for
 * "the starter base thing from the nether" here too) mirror {@code
 * NetherStartDeployment.buildNetherStartCapsule}/{@code placeCapsuleLighting} closely -- end stone
 * instead of nether bricks, no furnace/crafting table (End-start's chest tiers need no smelting or
 * crafting to begin bridging/hand-mining, unlike Nether-start's portal-building), duplicated rather
 * than shared per this project's own "revisit true cross-preset sharing later" precedent (GOALS
 * 41.1).
 */
final class EndStartDeployment {
    /**
     * Fixed outer-island-belt site (DESIGN §32.4): far enough from the origin to be unambiguously
     * the "outer islands" GOALS 34 asks for (vanilla's own outer-island generation starts
     * noticeably populating past roughly 1000 blocks from center), comfortably inside the End's
     * Y range (0-255) with headroom either direction, and never {@code (0, *, 0)} -- the main
     * island GOALS 34 explicitly says is not the spawn point.
     */
    private static final BlockPos SITE = new BlockPos(1200, 64, 0);
    /** Interior footprint (either dimension) at or above which ceiling/floor lights join the wall lights, mirrors Nether-start's own threshold. */
    private static final int DENSE_ROOM_INTERIOR_THRESHOLD = 6;

    private EndStartDeployment() {
    }

    /**
     * The player's spawn column, plus where the starter chest goes -- distinct positions (the
     * chest lines the platform's south wall, GOALS 41 follow-up) since a chest sitting directly
     * underfoot left no room to stand and was easy to miss entirely once the spawn-placement fix
     * (see {@code PlayerSpawnFinderMixin}) actually lands the player inside the sealed room.
     */
    record Site(BlockPos spawnPos, BlockPos chestPos) {
    }

    /**
     * Builds the guaranteed platform and overwrites the world's stored default spawn to point at
     * it -- covers both a brand-new player's first join and any future death (beds/anchors are
     * both impossible in the End, DESIGN §32.1, so every death reads this same stored value).
     *
     * @param overworld the Overworld server level (holds the world's stored default spawn)
     * @param end the End server level (where the platform itself is built)
     * @param endStart the resolved End-start plan (platform size/height/lighting)
     * @return the resolved site, for the caller to place the starter chest at
     */
    static Site buildAndRedirectSpawn(ServerLevel overworld, ServerLevel end, EndStartPlan endStart) {
        Site site = buildEndPlatform(end, endStart);
        overworld.getServer().setRespawnData(LevelData.RespawnData.of(Level.END, site.spawnPos(), 0.0F, 0.0F));
        WorldzCommon.LOGGER.info("Set the GOALS 34 End-start world spawn at {}.", site.spawnPos());
        return site;
    }

    /**
     * Carves a small, fully enclosed end-stone capsule at the fixed site (DESIGN §32.4) -- reuses
     * {@code NetherStartDeployment.buildNetherStartCapsule}'s exact fully-enclosed-shell shape
     * (not corner-posts-only, the Phase 7 test-2 lesson) and configurable size/height (GOALS 41),
     * swapping nether bricks for end stone -- the material the End's own natural islands are made
     * of, the same "visual consistency within the dimension" reasoning DESIGN §31.4 already used
     * for the Nether. Always run, unconditionally (DESIGN §32.2: no natural search), so the site is
     * guaranteed to occupy real terrain -- the void-stranding risk DESIGN §32.1 flags for the
     * End's own respawn landing search. The chest lines the south wall, centered, same as
     * Nether-start's own furniture wall (2026-07-27 follow-up there; applied here from day one) --
     * only placed off-center once the room is big enough to have a real interior; the original
     * 1x1-interior shape has nowhere else to put it, so the chest stays underfoot there.
     */
    private static Site buildEndPlatform(ServerLevel end, EndStartPlan endStart) {
        BlockPos center = SITE;
        end.getChunk(center.getX() >> 4, center.getZ() >> 4);
        int radius = (endStart.capsuleSizeBlocks() - 1) / 2;
        int height = endStart.capsuleHeightBlocks();
        BlockState endStone = Blocks.END_STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= height; dy++) {
                    boolean shell = dy == -1 || dy == height || Math.abs(dx) == radius || Math.abs(dz) == radius;
                    end.setBlock(center.offset(dx, dy, dz), shell ? endStone : air, Block.UPDATE_ALL);
                }
            }
        }
        placeCapsuleLighting(end, endStart, center, radius, height);
        int interiorHalfWidth = radius - 1;
        // The smallest (1x1-interior) room has no side wall to line -- the original underfoot
        // placement (embedded in the real floor, one below the player's own spawn column) is the
        // only option there and matches GOALS 34's original acceptance wording exactly. Anything
        // bigger gets its own dedicated interior tile off to one side (south wall), same as
        // Nether-start's own furniture-wall fix, so the player isn't standing on top of the chest.
        BlockPos chestPos = interiorHalfWidth >= 1 ? center.offset(0, 0, interiorHalfWidth) : center.below();
        return new Site(center, chestPos);
    }

    /**
     * Lights the platform's interior per {@link EndStartPlan#capsuleLightSource()} (GOALS 41.2) so
     * it never spawns the player in darkness -- identical dispatch/placement logic to {@code
     * NetherStartDeployment.placeCapsuleLighting}, duplicated for the same self-sufficiency
     * reasoning as {@link EndStartPlan}'s own capsule fields. The north/east/west walls each get
     * one fixture centered on that wall (or several, symmetric about the center, for a wall long
     * enough to need more at the configured spacing); the south wall is skipped (the chest wall,
     * {@link #buildEndPlatform}). A room with either interior dimension at or above {@link
     * #DENSE_ROOM_INTERIOR_THRESHOLD} also gets ceiling/floor lights in addition to the walls.
     */
    private static void placeCapsuleLighting(ServerLevel end, EndStartPlan endStart, BlockPos center, int radius, int height) {
        LightSource source = endStart.capsuleLightSource();
        int spacing = endStart.capsuleLightSpacingBlocks();
        if (source == LightSource.GLOW_LICHEN) {
            coatInteriorWithGlowLichen(end, center, radius, height);
            return;
        }
        int interiorHalfWidth = Math.max(radius - 1, 0);
        boolean denseRoom = (2 * interiorHalfWidth + 1) >= DENSE_ROOM_INTERIOR_THRESHOLD;
        if (source == LightSource.LANTERN || source == LightSource.SOUL_LANTERN) {
            Block lanternBlock = source == LightSource.SOUL_LANTERN ? Blocks.SOUL_LANTERN : Blocks.LANTERN;
            placeGrid(end, center, interiorHalfWidth, spacing, height - 1, lanternBlock.defaultBlockState().setValue(LanternBlock.HANGING, true), false);
            if (denseRoom) {
                placeGrid(end, center, interiorHalfWidth, spacing, 0, lanternBlock.defaultBlockState().setValue(LanternBlock.HANGING, false), true);
            }
            return;
        }
        int midY = (height - 1) / 2;
        List<Integer> offsets = EndStartPlan.centeredCapsuleOffsets(interiorHalfWidth, spacing);
        if (source == LightSource.TORCH) {
            int inner = Math.max(radius - 1, 0);
            for (int offset : offsets) {
                place(end, center, offset, midY, -inner, wallTorch(Direction.SOUTH));
                place(end, center, inner, midY, offset, wallTorch(Direction.WEST));
                place(end, center, -inner, midY, offset, wallTorch(Direction.EAST));
            }
            if (denseRoom) {
                placeGrid(end, center, interiorHalfWidth, spacing, 0, Blocks.TORCH.defaultBlockState(), true);
            }
            return;
        }
        Block embedded = source == LightSource.SHROOMLIGHT ? Blocks.SHROOMLIGHT : Blocks.GLOWSTONE;
        BlockState embeddedState = embedded.defaultBlockState();
        for (int offset : offsets) {
            place(end, center, offset, midY, -radius, embeddedState);
            place(end, center, radius, midY, offset, embeddedState);
            place(end, center, -radius, midY, offset, embeddedState);
        }
        if (denseRoom) {
            placeGrid(end, center, interiorHalfWidth, spacing, -1, embeddedState, false);
            placeGrid(end, center, interiorHalfWidth, spacing, height, embeddedState, false);
        }
    }

    private static BlockState wallTorch(Direction facing) {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }

    private static void place(ServerLevel end, BlockPos center, int dx, int dy, int dz, BlockState state) {
        end.setBlock(center.offset(dx, dy, dz), state, Block.UPDATE_ALL);
    }

    /**
     * Places {@code state} at every point of a symmetric, centered {@code dx}/{@code dz} grid
     * (see {@link EndStartPlan#centeredCapsuleOffsets}) at the fixed interior height {@code dy}.
     * When {@code skipCenter} is set, the exact center point ({@code dx = dz = 0}) is left alone,
     * so a floor-standing fixture never lands on the player's own spawn column.
     */
    private static void placeGrid(ServerLevel end, BlockPos center, int half, int spacing, int dy, BlockState state, boolean skipCenter) {
        for (int dx : EndStartPlan.centeredCapsuleOffsets(half, spacing)) {
            for (int dz : EndStartPlan.centeredCapsuleOffsets(half, spacing)) {
                if (skipCenter && dx == 0 && dz == 0) {
                    continue;
                }
                place(end, center, dx, dy, dz, state);
            }
        }
    }

    private static void coatInteriorWithGlowLichen(ServerLevel end, BlockPos center, int radius, int height) {
        int half = Math.max(radius - 1, 0);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    BlockState state = Blocks.GLOW_LICHEN.defaultBlockState();
                    boolean anyFace = false;
                    for (Direction direction : Direction.values()) {
                        if (isShellNeighbor(dx, dy, dz, direction, radius, height)) {
                            state = state.setValue(MultifaceBlock.getFaceProperty(direction), true);
                            anyFace = true;
                        }
                    }
                    if (anyFace) {
                        end.setBlock(center.offset(dx, dy, dz), state, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static boolean isShellNeighbor(int dx, int dy, int dz, Direction direction, int radius, int height) {
        int nx = dx + direction.getStepX();
        int ny = dy + direction.getStepY();
        int nz = dz + direction.getStepZ();
        return ny == -1 || ny == height || Math.abs(nx) == radius || Math.abs(nz) == radius;
    }
}
