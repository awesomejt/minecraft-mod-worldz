package media.jlt.minecraft.mods.worldz.logic;

import java.util.OptionalInt;

/** Pure placement bounds for compact progression-objective fallback sites. */
public final class ObjectiveSite {
    /** Preferred X coordinate keeps fallback sites near and visible from the origin. */
    public static final int PREFERRED_X = 32;
    /** Space reserved between a fallback site center and the border. */
    public static final int FALLBACK_MARGIN = 16;
    /** Deterministic Z offsets tried, in order, when the preferred column is not layout-supportive. */
    private static final int[] FALLBACK_Z_CANDIDATES = {0, 64, -64, 128, -128};

    private ObjectiveSite() {
    }

    /**
     * Tests whether an Overworld layout (if active) classifies a column as
     * land-supportive rather than open ocean. Modes the wrapper does not
     * terrain-adjust ({@code LEGACY}, and {@code VOID} — bounded by its own
     * sky-island exterior instead) are always considered supportive here.
     *
     * @param plan the world's coordinated-layout plan
     * @param x block X
     * @param z block Z
     * @return whether the column is safe to place a progression objective on
     */
    public static boolean isSupportiveColumn(WorldLayoutPlan plan, int x, int z) {
        if (plan.mode() == LayoutMode.LEGACY || plan.mode() == LayoutMode.VOID) {
            return true;
        }
        return plan.sampleAt(x, z).role() != BiomeRole.OCEAN;
    }

    /**
     * Chooses a fallback Z offset at the given X: the preferred {@code 0} when
     * it is layout-supportive and fits, else the first nearby deterministic
     * candidate that is both supportive and inside the supportive radius,
     * else {@code 0} unchanged so a site is still placed somewhere.
     *
     * @param plan the world's coordinated-layout plan
     * @param x chosen fallback X coordinate
     * @param radiusBlocks supportive radius (see {@link #supportiveRadius})
     * @param structureMarginBlocks required structure extent around the point
     * @return a Z offset that fits inside the radius, preferring a supportive column
     */
    public static int supportiveFallbackZ(WorldLayoutPlan plan, int x, int radiusBlocks, int structureMarginBlocks) {
        for (int z : FALLBACK_Z_CANDIDATES) {
            if (fitsInside(x, z, radiusBlocks, structureMarginBlocks) && isSupportiveColumn(plan, x, z)) {
                return z;
            }
        }
        return 0;
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

    /**
     * Finds the tightest square that can support a progression objective.
     *
     * @param borderEnabled whether the final border is a reachability bound
     * @param finalBorderRadius final border half-width
     * @param envelope exterior terrain bound
     * @return effective supportive radius, or empty for an unlimited normal world
     */
    public static OptionalInt supportiveRadius(
        boolean borderEnabled,
        int finalBorderRadius,
        ExteriorPlan.DimensionEnvelope envelope
    ) {
        if (!borderEnabled && envelope.mode() == ExteriorMode.NORMAL) {
            return OptionalInt.empty();
        }
        int radius = borderEnabled ? finalBorderRadius : Integer.MAX_VALUE;
        if (envelope.mode() != ExteriorMode.NORMAL) {
            radius = Math.min(radius, envelope.solidRadiusBlocks());
        }
        return OptionalInt.of(radius);
    }
}
