package media.jlt.minecraft.mods.worldz.logic;

/**
 * Pure math coordinating vanilla local relief with a {@link WorldLayoutPlan}
 * land/ocean classification (DESIGN §17). Rather than replacing terrain
 * outright, it computes one smooth vertical target per column: blending
 * between a raised land floor and a capped ocean ceiling by the sampled
 * {@code landFactor} preserves natural relief shape while nudging the whole
 * column above or below sea level as its role requires.
 */
public final class LayoutTerrainProfile {
    /** Baseline height above sea level guaranteed for fully land-classified columns. */
    public static final int LAND_FLOOR_OFFSET_ABOVE_SEA_LEVEL = 2;
    /** Baseline depth below sea level enforced for fully ocean-classified columns. */
    public static final int OCEAN_CEILING_OFFSET_BELOW_SEA_LEVEL = 3;

    private LayoutTerrainProfile() {
    }

    /**
     * Blends a raised land target and a capped ocean target by land factor.
     *
     * @param naturalFloorHeight the delegate's natural ocean-floor height at this column
     * @param landFactor {@code 0} fully ocean-shaped, {@code 1} fully land-shaped
     * @param seaLevel the dimension's sea level
     * @return the coordinated target floor height
     */
    public static int targetHeight(int naturalFloorHeight, double landFactor, int seaLevel) {
        double clampedFactor = Math.clamp(landFactor, 0.0, 1.0);
        int landFloor = seaLevel + LAND_FLOOR_OFFSET_ABOVE_SEA_LEVEL;
        int oceanCeiling = seaLevel - OCEAN_CEILING_OFFSET_BELOW_SEA_LEVEL;
        int raised = Math.max(naturalFloorHeight, landFloor);
        int capped = Math.min(naturalFloorHeight, oceanCeiling);
        return (int) Math.round(capped + (raised - capped) * clampedFactor);
    }

    /**
     * Raises only clearly deep-ocean floors, leaving shallow natural depressions
     * (rivers, ponds) untouched so {@code LAND_ONLY} layouts keep permitting
     * small inland water instead of flattening every column to the land floor.
     *
     * @param naturalFloorHeight the delegate's natural ocean-floor height at this column
     * @param seaLevel the dimension's sea level
     * @return {@code naturalFloorHeight} unchanged, or the land floor if it was deep-ocean-shaped
     */
    public static int landOnlyTarget(int naturalFloorHeight, int seaLevel) {
        int oceanCeiling = seaLevel - OCEAN_CEILING_OFFSET_BELOW_SEA_LEVEL;
        if (naturalFloorHeight >= oceanCeiling) {
            return naturalFloorHeight;
        }
        return seaLevel + LAND_FLOOR_OFFSET_ABOVE_SEA_LEVEL;
    }
}
