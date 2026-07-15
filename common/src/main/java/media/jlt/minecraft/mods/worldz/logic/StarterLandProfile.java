package media.jlt.minecraft.mods.worldz.logic;

/** Pure column-height calculations for blending guaranteed starter land into natural terrain. */
public final class StarterLandProfile {
    /** Required first-free block height relative to sea level. */
    public static final int BLOCKS_ABOVE_SEA_LEVEL = 2;

    private StarterLandProfile() {
    }

    /**
     * Calculates the first-free height after applying the starter-land floor.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param starterRadiusBlocks radius receiving the full guarantee
     * @param transitionWidthBlocks width beyond the radius blended to natural terrain
     * @param naturalHeight delegate generator's first-free height
     * @param seaLevel generator sea level
     * @return natural or smoothly raised first-free height
     */
    public static int targetHeight(
        int x,
        int z,
        int starterRadiusBlocks,
        int transitionWidthBlocks,
        int naturalHeight,
        int seaLevel
    ) {
        int minimumHeight = seaLevel + BLOCKS_ABOVE_SEA_LEVEL;
        if (naturalHeight >= minimumHeight) {
            return naturalHeight;
        }
        double strength = strengthAt(x, z, starterRadiusBlocks, transitionWidthBlocks);
        return naturalHeight + (int) Math.ceil((minimumHeight - naturalHeight) * strength);
    }

    /**
     * Returns a smooth terrain-raising strength from one in the starter zone to zero beyond its transition.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param starterRadiusBlocks full-strength radius
     * @param transitionWidthBlocks outward blend width
     * @return value in the inclusive range zero through one
     */
    public static double strengthAt(int x, int z, int starterRadiusBlocks, int transitionWidthBlocks) {
        if (starterRadiusBlocks < 0 || transitionWidthBlocks < 0) {
            return 0.0;
        }
        double distance = Math.hypot((double) x, (double) z);
        if (distance <= starterRadiusBlocks) {
            return 1.0;
        }
        if (transitionWidthBlocks == 0 || distance >= (double) starterRadiusBlocks + transitionWidthBlocks) {
            return 0.0;
        }
        double progress = (distance - starterRadiusBlocks) / transitionWidthBlocks;
        double smoothstep = progress * progress * (3.0 - 2.0 * progress);
        return 1.0 - smoothstep;
    }

    /**
     * Chooses the lowest block repaired beneath a natural ocean floor.
     *
     * @param naturalOceanFloorHeight first-free height of the natural ocean floor
     * @param foundationDepthBlocks number of blocks repaired below that floor
     * @param minY dimension build minimum
     * @return inclusive minimum fill Y
     */
    public static int foundationMinY(int naturalOceanFloorHeight, int foundationDepthBlocks, int minY) {
        long requested = (long) naturalOceanFloorHeight - foundationDepthBlocks;
        return (int) Math.max(minY, requested);
    }
}
