package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;

/**
 * Builds a guaranteed safe End spawn platform (GOALS 34, DESIGN §32.4) and redirects the world's
 * default spawn to it (mirrors {@code NetherStartDeployment.resolveAndRedirectSpawn}, DESIGN
 * §31.2) -- called once, only for a new {@code end_start} world, from {@code WorldLimitManager.
 * onServerStarted}. Unlike {@code NetherStartDeployment}, there is no natural-site search: Jason's
 * decision (DESIGN §32.2) is to always build the guaranteed platform, since the End's outer
 * regions are mostly void and a bounded force-generation search would rarely find real terrain
 * within budget.
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

    private EndStartDeployment() {
    }

    /**
     * Builds the guaranteed platform and overwrites the world's stored default spawn to point at
     * it -- covers both a brand-new player's first join and any future death (beds/anchors are
     * both impossible in the End, DESIGN §32.1, so every death reads this same stored value).
     *
     * @param overworld the Overworld server level (holds the world's stored default spawn)
     * @param end the End server level (where the platform itself is built)
     * @return the platform's site, for the caller to place the starter chest at (TODO 15.2b)
     */
    static BlockPos buildAndRedirectSpawn(ServerLevel overworld, ServerLevel end) {
        buildEndPlatform(end);
        overworld.getServer().setRespawnData(LevelData.RespawnData.of(Level.END, SITE, 0.0F, 0.0F));
        WorldzCommon.LOGGER.info("Set the GOALS 34 End-start world spawn at {}.", SITE);
        return SITE;
    }

    /**
     * Carves a small, fully enclosed end-stone capsule at the fixed site (DESIGN §32.4) -- reuses
     * {@code NetherStartDeployment.buildNetherStartCapsule}'s exact fully-enclosed-shell shape
     * (not corner-posts-only, the Phase 7 test-2 lesson), swapping nether bricks for end stone --
     * the material the End's own natural islands are made of, the same "visual consistency within
     * the dimension" reasoning DESIGN §31.4 already used for the Nether. Always run, unconditionally
     * (DESIGN §32.2: no natural search), so the site is guaranteed to occupy real terrain -- the
     * void-stranding risk DESIGN §32.1 flags for the End's own respawn landing search.
     */
    private static void buildEndPlatform(ServerLevel end) {
        end.getChunk(SITE.getX() >> 4, SITE.getZ() >> 4);
        BlockState endStone = Blocks.END_STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    boolean shell = dy == -1 || dy == 2 || Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    end.setBlock(SITE.offset(dx, dy, dz), shell ? endStone : air, 3);
                }
            }
        }
    }
}
