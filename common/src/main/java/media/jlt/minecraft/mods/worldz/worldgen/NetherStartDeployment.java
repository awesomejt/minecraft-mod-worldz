package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnSearchPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;

/**
 * Resolves a guaranteed safe Nether spawn site (GOALS 27, DESIGN §31.4) and redirects the world's
 * default spawn to it (DESIGN §31.2) -- called once, only for a new {@code nether_start} world,
 * from {@code WorldLimitManager.onServerStarted} (the first hook where the Nether {@code
 * ServerLevel} actually exists, DESIGN §31.1). Mirrors {@code SpawnOriginManager}'s cave-cavity
 * search/capsule-fallback shape (DESIGN §30.3) closely enough to justify the naming parallel, but
 * lives in its own class rather than extending {@code SpawnOriginManager} -- that class's two
 * entry points are scoped to the early Overworld-spawn-search hooks, which fire before the Nether
 * exists (DESIGN §31.1) and are therefore never usable here.
 */
final class NetherStartDeployment {
    /**
     * Same reduced-budget reasoning as {@code SpawnOriginManager}'s own cave search (DESIGN
     * §30.3): each candidate costs a real, forced chunk generation, so this is nowhere near
     * {@link SpawnSearchPlan#defaults()}'s ordinary free-climate-sampling budget.
     */
    private static final SpawnSearchPlan SEARCH_PLAN = new SpawnSearchPlan(320, 16, 8);
    /** How far above/below {@link NetherStartPlan#spawnY()} the site search scans per column. */
    private static final int SEARCH_VERTICAL_TOLERANCE = 16;

    private NetherStartDeployment() {
    }

    /**
     * Resolves the safe Nether spawn site and overwrites the world's stored default spawn to
     * point at it (DESIGN §31.2) -- covers both a brand-new player's first join and any future
     * death without a personal bed/respawn-anchor, since both read the same stored value.
     *
     * @param overworld the Overworld server level (holds the world's stored default spawn)
     * @param nether the Nether server level (where the site itself is resolved)
     * @param netherStart the enabled Nether-start plan
     * @return the resolved safe site, for the caller to place the starter chest at (TODO 14.2b)
     */
    static BlockPos resolveAndRedirectSpawn(ServerLevel overworld, ServerLevel nether, NetherStartPlan netherStart) {
        BlockPos site = resolveSite(nether, netherStart);
        overworld.getServer().setRespawnData(LevelData.RespawnData.of(Level.NETHER, site, 0.0F, 0.0F));
        WorldzCommon.LOGGER.info("Set the GOALS 27 Nether-start world spawn at {}.", site);
        return site;
    }

    private static BlockPos resolveSite(ServerLevel nether, NetherStartPlan netherStart) {
        for (SpawnSearchPlan.Offset offset : SEARCH_PLAN.offsetsInSearchOrder()) {
            Optional<BlockPos> found = searchNetherStartSite(nether, netherStart, offset.x(), offset.z());
            if (found.isPresent()) {
                return found.get();
            }
        }
        WorldzCommon.LOGGER.warn(
            "Nether-start safe-site search found no natural pocket near Y{} within {} blocks of the origin; carving a safe capsule instead.",
            netherStart.spawnY(), SEARCH_PLAN.maxRadiusBlocks()
        );
        return buildNetherStartCapsule(nether, netherStart);
    }

    /**
     * Force-generates the chunk at one candidate column and scans a vertical window around
     * {@link NetherStartPlan#spawnY()} for a solid, non-fluid floor with two clear blocks above
     * it -- the same bar {@code SpawnOriginManager.searchCaveCavity} applies -- plus a check that
     * the floor's four horizontal neighbors aren't lava, since an unbounded lava sea (not cave
     * cavities) is the dominant Nether hazard a cave-style search never had to consider. Reads
     * through the level rather than the raw candidate chunk for every check, since a neighbor
     * check can legitimately cross into an adjacent, not-yet-forced chunk.
     */
    private static Optional<BlockPos> searchNetherStartSite(ServerLevel nether, NetherStartPlan netherStart, int x, int z) {
        nether.getChunk(x >> 4, z >> 4);
        int minY = Math.max(nether.getMinY() + 1, netherStart.spawnY() - SEARCH_VERTICAL_TOLERANCE);
        int maxY = Math.min(nether.getMaxY() - 1, netherStart.spawnY() + SEARCH_VERTICAL_TOLERANCE);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            BlockState floor = nether.getBlockState(pos.set(x, y - 1, z));
            if (floor.isAir() || !floor.getFluidState().isEmpty()) {
                continue;
            }
            BlockState feet = nether.getBlockState(pos.set(x, y, z));
            BlockState head = nether.getBlockState(pos.set(x, y + 1, z));
            if (!feet.isAir() || !head.isAir()) {
                continue;
            }
            if (hasAdjacentLava(nether, x, y - 1, z)) {
                continue;
            }
            return Optional.of(new BlockPos(x, y, z));
        }
        return Optional.empty();
    }

    private static boolean hasAdjacentLava(ServerLevel nether, int x, int y, int z) {
        return isLava(nether, x + 1, y, z) || isLava(nether, x - 1, y, z)
            || isLava(nether, x, y, z + 1) || isLava(nether, x, y, z - 1);
    }

    private static boolean isLava(ServerLevel nether, int x, int y, int z) {
        return !nether.getFluidState(new BlockPos(x, y, z)).isEmpty();
    }

    /**
     * Carves a small safe capsule directly into already-generated Nether terrain when no natural
     * site was found within budget, so world creation can never fail to produce a safe spawn --
     * reuses {@code SpawnOriginManager.buildCaveCapsule}'s exact fully-enclosed-shell shape (the
     * Phase 7 test-2 lesson: corner posts alone aren't a real shell), swapping stone for
     * nether bricks -- the same material {@code ProgressionGuarantees.buildBlazeSite} already
     * uses for its own guaranteed Nether room.
     */
    private static BlockPos buildNetherStartCapsule(ServerLevel nether, NetherStartPlan netherStart) {
        BlockPos center = new BlockPos(0, netherStart.spawnY(), 0);
        nether.getChunk(0, 0);
        BlockState brick = Blocks.NETHER_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    boolean shell = dy == -1 || dy == 2 || Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    nether.setBlock(center.offset(dx, dy, dz), shell ? brick : air, 3);
                }
            }
        }
        return center;
    }
}
