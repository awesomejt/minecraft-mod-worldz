package media.jlt.minecraft.mods.worldz.logic;

/** Pure placement bounds for compact progression-objective fallback sites. */
public final class ObjectiveSite {
    /** Preferred X coordinate keeps fallback sites near and visible from the origin. */
    public static final int PREFERRED_X = 32;
    /** Space reserved between a fallback site center and the border. */
    public static final int FALLBACK_MARGIN = 16;

    private ObjectiveSite() {
    }

    /**
     * Tests whether a structure reference point plus safety margin fits inside
     * the final square border.
     *
     * @param x structure X coordinate
     * @param z structure Z coordinate
     * @param radiusBlocks border center-to-side distance
     * @param structureMarginBlocks required structure extent around the point
     * @return whether the structure is safely reachable
     */
    public static boolean fitsInside(int x, int z, int radiusBlocks, int structureMarginBlocks) {
        long usableRadius = (long)radiusBlocks - structureMarginBlocks;
        return usableRadius >= 0L && Math.abs((long)x) <= usableRadius && Math.abs((long)z) <= usableRadius;
    }

    /**
     * Selects a deterministic fallback X coordinate that fits even the minimum
     * supported border while remaining close enough to discover naturally.
     *
     * @param radiusBlocks final border radius
     * @return positive X coordinate for the objective site
     */
    public static int fallbackX(int radiusBlocks) {
        return Math.min(PREFERRED_X, Math.max(0, radiusBlocks - FALLBACK_MARGIN));
    }
}
