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

    private IslandOceanProfile() {
    }

    /**
     * Selects a deterministic beach/stony-shore biome at one column in the shore ring (GOALS
     * 01: "a combination of beach and/or stony shore"). Sampled per-block, unlike the wider
     * ocean gradient, since the shore ring itself is already narrow.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param seed sampling seed
     * @return {@code minecraft:beach} or {@code minecraft:stony_shore}
     */
    public static String shoreBiomeAt(int x, int z, long seed) {
        int index = (int) Math.floorMod(hashIndex(seed, x, z), (long) SHORE_IDS.size());
        return SHORE_IDS.get(index);
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

    /**
     * Selects a deterministic ocean biome at one column: the shallow warm/lukewarm/ocean pool
     * within the shallow band, otherwise the complete vanilla ocean-biome set (GOALS 01's "all
     * ocean biomes available"). Sampled at a coarse region scale so the ocean reads as patches
     * of variety rather than per-block dithering.
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
        int scale = Math.max(1, regionScaleBlocks);
        long cellX = Math.floorDiv(x, scale);
        long cellZ = Math.floorDiv(z, scale);
        int index = (int) Math.floorMod(hashIndex(seed, cellX, cellZ), (long) pool.size());
        return pool.get(index);
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
