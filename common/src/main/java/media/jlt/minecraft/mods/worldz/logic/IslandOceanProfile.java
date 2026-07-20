package media.jlt.minecraft.mods.worldz.logic;

import java.util.List;

/**
 * Pure shallow-to-deep gradient for the ocean surrounding a natural island (GOALS 01, DESIGN
 * §24.5). Both the seabed depth and the biome pool widen with distance beyond the shore ring,
 * so nearby water reads as shallow warm/lukewarm ocean and the water reachable further out
 * draws from the complete vanilla ocean-biome set.
 */
public final class IslandOceanProfile {
    private static final List<String> SHALLOW_OCEAN_IDS = List.of(
        "minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:ocean"
    );
    private static final List<String> SHORE_IDS = List.of("minecraft:beach", "minecraft:stony_shore");
    /** Approximate arc length in blocks each contiguous beach/stony-shore stretch targets. */
    private static final double SHORE_ARC_TARGET_LENGTH_BLOCKS = 32.0;
    private static final int SHORE_ARC_MIN_SEGMENTS = 4;
    /** Distinguishes the shore-arc jitter/pick hashes from the ocean-region ones above them. */
    private static final long SHORE_ARC_SALT = 0x53484F5245415243L;

    private IslandOceanProfile() {
    }

    /**
     * Selects a deterministic beach/stony-shore biome at one column in the shore ring (GOALS
     * 01: "a combination of beach and/or stony shore"). Sampled as jittered 1D Voronoi arcs
     * around the island's angle, not per-block, so the shoreline reads as alternating
     * contiguous stretches of each biome (naturally varying in length, since jittered Voronoi
     * cells are not uniform) rather than a speckled block-by-block mix.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param baseRadiusBlocks configured (unperturbed) island radius, used only to scale how
     *     many arc segments fit around the coastline so stretch length stays roughly constant
     *     in blocks regardless of island size
     * @param seed sampling seed
     * @return {@code minecraft:beach} or {@code minecraft:stony_shore}
     */
    public static String shoreBiomeAt(int x, int z, double baseRadiusBlocks, long seed) {
        double angle = Math.atan2(z, x);
        long segment = nearestShoreArcSegment(angle, baseRadiusBlocks, seed);
        int index = (int) Math.floorMod(hashIndex(seed ^ SHORE_ARC_SALT, segment, 0), (long) SHORE_IDS.size());
        return SHORE_IDS.get(index);
    }

    /**
     * Finds the jittered arc segment whose feature angle is nearest {@code angle}, searching
     * the query segment and its two neighbors (the jitter margin guarantees the true nearest
     * feature angle can never fall outside that neighborhood), wrapping around the full circle.
     */
    private static long nearestShoreArcSegment(double angle, double baseRadiusBlocks, long seed) {
        int segmentCount = Math.max(
            SHORE_ARC_MIN_SEGMENTS,
            (int) Math.round(2.0 * Math.PI * Math.max(1.0, baseRadiusBlocks) / SHORE_ARC_TARGET_LENGTH_BLOCKS)
        );
        double segmentWidth = 2.0 * Math.PI / segmentCount;
        double normalizedAngle = angle < 0.0 ? angle + 2.0 * Math.PI : angle;
        long originSegment = (long) Math.floor(normalizedAngle / segmentWidth);
        long bestSegment = originSegment;
        double bestDistance = Double.MAX_VALUE;
        for (int offset = -1; offset <= 1; offset++) {
            long segment = Math.floorMod(originSegment + offset, (long) segmentCount);
            double featureAngle = (segment + JITTER_MARGIN + jitter01(seed ^ SHORE_ARC_SALT, segment, 0, 0) * JITTER_SPAN) * segmentWidth;
            double distance = angularDistance(normalizedAngle, featureAngle);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSegment = segment;
            }
        }
        return bestSegment;
    }

    private static double angularDistance(double a, double b) {
        double diff = Math.abs(a - b) % (2.0 * Math.PI);
        return Math.min(diff, 2.0 * Math.PI - diff);
    }

    /**
     * Computes the seabed depth below sea level at a given distance beyond the shore ring.
     *
     * @param distanceBeyondShoreBlocks distance past the shore ring's outer edge; negative or
     *     zero is treated as the shore band itself
     * @param shallowWidthBlocks width of the shallow band immediately beyond the shore
     * @param deepenWidthBlocks width over which depth ramps from shallow to deep
     * @param shallowDepthBlocks seabed depth below sea level in the shallow band
     * @param deepDepthBlocks seabed depth below sea level once fully deep
     * @return seabed depth below sea level, smoothly interpolated across the deepening band
     */
    public static int floorDepthBelowSeaLevel(
        double distanceBeyondShoreBlocks,
        int shallowWidthBlocks,
        int deepenWidthBlocks,
        int shallowDepthBlocks,
        int deepDepthBlocks
    ) {
        if (distanceBeyondShoreBlocks <= shallowWidthBlocks) {
            return shallowDepthBlocks;
        }
        double deepenEnd = (double) shallowWidthBlocks + deepenWidthBlocks;
        if (deepenWidthBlocks <= 0 || distanceBeyondShoreBlocks >= deepenEnd) {
            return deepDepthBlocks;
        }
        double progress = (distanceBeyondShoreBlocks - shallowWidthBlocks) / deepenWidthBlocks;
        double smoothstep = progress * progress * (3.0 - 2.0 * progress);
        return (int) Math.round(shallowDepthBlocks + (deepDepthBlocks - shallowDepthBlocks) * smoothstep);
    }

    /** Fraction of a cell's edge the jittered feature point is confined to, centered in the cell. */
    private static final double JITTER_MARGIN = 0.2;
    private static final double JITTER_SPAN = 1.0 - 2.0 * JITTER_MARGIN;

    /**
     * Selects a deterministic ocean biome at one column: the shallow warm/lukewarm/ocean pool
     * within the shallow band, otherwise the complete vanilla ocean-biome set (GOALS 01's "all
     * ocean biomes available"). Sampled as jittered-grid Voronoi regions (each grid cell gets a
     * randomly offset feature point; the nearest one wins) rather than a plain axis-aligned
     * grid, so the ocean reads as organic patches of variety instead of a visible checkerboard.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param distanceBeyondShoreBlocks distance past the shore ring's outer edge
     * @param shallowWidthBlocks width of the shallow band immediately beyond the shore
     * @param regionScaleBlocks grid-cell edge length in blocks for the per-region pick
     * @param seed sampling seed
     * @return canonical ocean biome id
     */
    public static String biomeAt(
        int x,
        int z,
        double distanceBeyondShoreBlocks,
        int shallowWidthBlocks,
        int regionScaleBlocks,
        long seed
    ) {
        List<String> pool = distanceBeyondShoreBlocks <= shallowWidthBlocks
            ? SHALLOW_OCEAN_IDS
            : BiomeRoles.oceanIds();
        long[] cell = nearestFeatureCell(x, z, regionScaleBlocks, seed);
        int index = (int) Math.floorMod(hashIndex(seed, cell[0], cell[1]), (long) pool.size());
        return pool.get(index);
    }

    /**
     * Finds the grid cell whose jittered feature point is nearest to {@code (x, z)}, searching
     * the query cell and its eight neighbors (the jitter margin guarantees the true nearest
     * feature point can never fall outside that 3x3 neighborhood).
     *
     * @return {@code {cellX, cellZ}} of the winning cell
     */
    private static long[] nearestFeatureCell(int x, int z, int regionScaleBlocks, long seed) {
        int scale = Math.max(1, regionScaleBlocks);
        long originCellX = Math.floorDiv(x, scale);
        long originCellZ = Math.floorDiv(z, scale);
        long bestCellX = originCellX;
        long bestCellZ = originCellZ;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long cellX = originCellX + dx;
                long cellZ = originCellZ + dz;
                double featureX = (cellX + JITTER_MARGIN + jitter01(seed, cellX, cellZ, 0) * JITTER_SPAN) * scale;
                double featureZ = (cellZ + JITTER_MARGIN + jitter01(seed, cellX, cellZ, 1) * JITTER_SPAN) * scale;
                double deltaX = x - featureX;
                double deltaZ = z - featureZ;
                double distanceSq = deltaX * deltaX + deltaZ * deltaZ;
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    bestCellX = cellX;
                    bestCellZ = cellZ;
                }
            }
        }
        return new long[] {bestCellX, bestCellZ};
    }

    private static double jitter01(long seed, long cellX, long cellZ, int axis) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ cellX);
        h = splitmix64(h ^ cellZ);
        h = splitmix64(h ^ axis);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long hashIndex(long seed, long cellX, long cellZ) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ cellX);
        h = splitmix64(h ^ cellZ);
        return h;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
