package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.LightSource;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnSearchPlan;
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

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Natural-first, guaranteed-capsule-fallback search (DESIGN §31.4) -- unless {@link
     * NetherStartPlan#forceCapsule()} is set (GOALS 41.1: "explicit, requestable option", not only
     * a fallback), or {@code spawnY} itself is close enough to the Nether's own hard floor/ceiling
     * that a natural pocket is inherently unlikely (GOALS 41 follow-up, 2026-07-26: "using the
     * capsule should be the default behavior in cases like [config 62]... spawn low in the world
     * with rare access to caves") -- in either case the capsule is built directly and the (real,
     * forced-chunk-generation-costly) natural search never runs at all.
     */
    private static BlockPos resolveSite(ServerLevel nether, NetherStartPlan netherStart) {
        if (netherStart.forceCapsule()) {
            WorldzCommon.LOGGER.info("Nether-start capsule explicitly requested (forceCapsule); skipping the natural safe-site search.");
            return buildNetherStartCapsule(nether, netherStart);
        }
        if (netherStart.spawnYTooCloseToBoundary(nether.getMinY(), nether.getMaxY(), SEARCH_VERTICAL_TOLERANCE)) {
            WorldzCommon.LOGGER.info(
                "Nether-start spawnY {} is within {} blocks of the Nether's own floor/ceiling, where natural pockets are rare;"
                    + " building the guaranteed capsule directly instead of searching.",
                netherStart.spawnY(), SEARCH_VERTICAL_TOLERANCE
            );
            return buildNetherStartCapsule(nether, netherStart);
        }
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
     * Carves a safe capsule/starter base directly into already-generated Nether terrain when no
     * natural site was found within budget (or unconditionally when {@link
     * NetherStartPlan#forceCapsule()} is set), so world creation can never fail to produce a safe
     * spawn -- reuses {@code SpawnOriginManager.buildCaveCapsule}'s fully-enclosed-shell principle
     * (the Phase 7 test-2 lesson: corner posts alone aren't a real shell), swapping stone for
     * nether bricks -- the same material {@code ProgressionGuarantees.buildBlazeSite} already
     * uses for its own guaranteed Nether room. Unlike the original 1x1-interior shape, this now
     * builds a "decent sized enclosure" (GOALS 41), lit by default so the player never spawns in
     * darkness (GOALS 41.2), with a chest (placed by the caller, {@code
     * StarterKitDeployment.spawnNetherStartChest}, directly beneath the returned site as before),
     * plus an always-present furnace and crafting table (GOALS 41) once the room is large enough
     * to hold them without crowding the player's own spawn column.
     */
    private static BlockPos buildNetherStartCapsule(ServerLevel nether, NetherStartPlan netherStart) {
        BlockPos center = new BlockPos(0, netherStart.spawnY(), 0);
        nether.getChunk(0, 0);
        int radius = (netherStart.capsuleSizeBlocks() - 1) / 2;
        int height = netherStart.capsuleHeightBlocks();
        BlockState wall = Blocks.NETHER_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= height; dy++) {
                    boolean shell = dy == -1 || dy == height || Math.abs(dx) == radius || Math.abs(dz) == radius;
                    nether.setBlock(center.offset(dx, dy, dz), shell ? wall : air, Block.UPDATE_ALL);
                }
            }
        }
        int interiorHalfWidth = radius - 1;
        if (interiorHalfWidth >= 1) {
            nether.setBlock(center.offset(1, 0, 0), Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
            nether.setBlock(center.offset(-1, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        }
        placeCapsuleLighting(nether, netherStart, center, radius, height);
        return center;
    }

    /**
     * Lights the capsule's interior per {@link NetherStartPlan#capsuleLightSource()} (GOALS 41.2)
     * so it never spawns the player in darkness. Placement rule depends on the source: {@link
     * LightSource#GLOW_LICHEN} coats the entire interior surface; {@link LightSource#LANTERN}/
     * {@link LightSource#SOUL_LANTERN} hang from the ceiling in a grid (Jason, 2026-07-25: "those
     * can be hung from the ceiling"); everything else is spaced every {@code
     * capsuleLightSpacingBlocks} around the room's own wall ring. Deliberately does not attempt to
     * compute an exact light level -- the default spacing/brightness combination keeps a
     * default-sized room comfortably lit throughout. Known, accepted limitation (GOALS 41.2,
     * carried over from Jason's own acceptance note): Nether mobs that ignore light level
     * entirely when choosing a spawn location (zombified piglins in particular) can still spawn
     * inside a fully lit capsule -- no amount of interior lighting prevents that.
     */
    private static void placeCapsuleLighting(ServerLevel nether, NetherStartPlan netherStart, BlockPos center, int radius, int height) {
        LightSource source = netherStart.capsuleLightSource();
        int spacing = netherStart.capsuleLightSpacingBlocks();
        if (source == LightSource.GLOW_LICHEN) {
            coatInteriorWithGlowLichen(nether, center, radius, height);
            return;
        }
        if (source == LightSource.LANTERN || source == LightSource.SOUL_LANTERN) {
            hangLanternGrid(nether, center, radius, height, spacing, source == LightSource.SOUL_LANTERN ? Blocks.SOUL_LANTERN : Blocks.LANTERN);
            return;
        }
        int midY = (height - 1) / 2;
        if (source == LightSource.TORCH) {
            for (PerimeterPoint point : wallPerimeter(Math.max(radius - 1, 0), spacing)) {
                BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, point.outward());
                nether.setBlock(center.offset(point.dx(), midY, point.dz()), torch, Block.UPDATE_ALL);
            }
            return;
        }
        Block embedded = source == LightSource.SHROOMLIGHT ? Blocks.SHROOMLIGHT : Blocks.GLOWSTONE;
        for (PerimeterPoint point : wallPerimeter(radius, spacing)) {
            nether.setBlock(center.offset(point.dx(), midY, point.dz()), embedded.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void hangLanternGrid(ServerLevel nether, BlockPos center, int radius, int height, int spacing, Block lanternBlock) {
        int half = Math.max(radius - 1, 0);
        BlockState lantern = lanternBlock.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int dx = -half; dx <= half; dx += spacing) {
            for (int dz = -half; dz <= half; dz += spacing) {
                nether.setBlock(center.offset(dx, height - 1, dz), lantern, Block.UPDATE_ALL);
            }
        }
    }

    private static void coatInteriorWithGlowLichen(ServerLevel nether, BlockPos center, int radius, int height) {
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
                        nether.setBlock(center.offset(dx, dy, dz), state, Block.UPDATE_ALL);
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

    /** One spaced wall-ring placement point, tagged with the direction pointing into the room. */
    private record PerimeterPoint(int dx, int dz, Direction outward) {
    }

    /**
     * Walks the square ring at the given radius (clockwise from the northwest corner) and returns
     * every {@code spacing}-th point, each tagged with the {@link Direction} a fixture mounted
     * there should face to point into the room. A ring radius of 0 or less (the smallest supported
     * capsule, a single interior column) degenerates to one single point -- there is no room for a
     * real ring, so exactly one fixture is placed regardless of {@code spacing}.
     */
    private static List<PerimeterPoint> wallPerimeter(int radius, int spacing) {
        List<PerimeterPoint> ring = new ArrayList<>();
        if (radius <= 0) {
            ring.add(new PerimeterPoint(0, 0, Direction.SOUTH));
            return ring;
        }
        for (int dx = -radius; dx < radius; dx++) {
            ring.add(new PerimeterPoint(dx, -radius, Direction.SOUTH));
        }
        for (int dz = -radius; dz < radius; dz++) {
            ring.add(new PerimeterPoint(radius, dz, Direction.WEST));
        }
        for (int dx = radius; dx > -radius; dx--) {
            ring.add(new PerimeterPoint(dx, radius, Direction.NORTH));
        }
        for (int dz = radius; dz > -radius; dz--) {
            ring.add(new PerimeterPoint(-radius, dz, Direction.EAST));
        }
        List<PerimeterPoint> spaced = new ArrayList<>();
        for (int i = 0; i < ring.size(); i += spacing) {
            spaced.add(ring.get(i));
        }
        return spaced;
    }
}
