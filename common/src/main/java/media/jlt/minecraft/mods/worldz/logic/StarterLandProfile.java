package media.jlt.minecraft.mods.worldz.logic;

/** Pure column-height calculations for blending guaranteed starter land into natural terrain. */
public final class StarterLandProfile {
    /** Required first-free block height relative to sea level. */
    public static final int BLOCKS_ABOVE_SEA_LEVEL = 2;
    /** Coordinate scale applied to seeded vanilla surface noise for broad relief. */
    public static final double RELIEF_NOISE_SCALE = 1.0 / 64.0;
    private static final int NATURAL_RELIEF_REFERENCE_DEPTH = 32;
    private static final int NATURAL_RELIEF_DIVISOR = 4;
    private static final int MAX_NATURAL_RELIEF_BLOCKS = 8;
    private static final int MAX_NOISE_RELIEF_BLOCKS = 8;

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
     * @param profileVersion persisted terrain-shaping revision
     * @param reliefNoise seeded broad noise sample, normally in the range -1 through 1
     * @return natural or smoothly raised first-free height
     */
    public static int targetHeight(
        int x,
        int z,
        int starterRadiusBlocks,
        int transitionWidthBlocks,
        int naturalHeight,
        int seaLevel,
        int profileVersion,
        double reliefNoise
    ) {
        int minimumHeight = seaLevel + BLOCKS_ABOVE_SEA_LEVEL;
        int shapedMinimum = profileVersion <= StarterLandPlan.LEGACY_PROFILE_VERSION
            ? minimumHeight
            : reliefMinimumHeight(naturalHeight, seaLevel, reliefNoise);
        if (naturalHeight >= shapedMinimum) {
            return naturalHeight;
        }
        double strength = strengthAt(x, z, starterRadiusBlocks, transitionWidthBlocks);
        double lift = (shapedMinimum - naturalHeight) * strength;
        int roundedLift = profileVersion <= StarterLandPlan.LEGACY_PROFILE_VERSION
            ? (int) Math.ceil(lift)
            : (int) Math.round(lift);
        return naturalHeight + roundedLift;
    }

    private static int reliefMinimumHeight(int naturalHeight, int seaLevel, double reliefNoise) {
        int referenceHeight = seaLevel - NATURAL_RELIEF_REFERENCE_DEPTH;
        int naturalRelief = Math.clamp(
            Math.floorDiv(naturalHeight - referenceHeight, NATURAL_RELIEF_DIVISOR),
            0,
            MAX_NATURAL_RELIEF_BLOCKS
        );
        double normalizedNoise = Math.clamp((reliefNoise + 1.0) * 0.5, 0.0, 1.0);
        int noiseRelief = (int) Math.round(normalizedNoise * MAX_NOISE_RELIEF_BLOCKS);
        return seaLevel + BLOCKS_ABOVE_SEA_LEVEL + naturalRelief + noiseRelief;
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
