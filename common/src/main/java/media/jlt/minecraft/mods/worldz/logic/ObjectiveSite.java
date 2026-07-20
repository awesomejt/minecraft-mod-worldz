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
        return supportiveFallbackZ(plan, x, radiusBlocks, radiusBlocks, structureMarginBlocks);
    }

    /**
     * Chooses a fallback Z offset at the given X, bounded independently on each axis --
     * needed for a strip world (GOALS 32), whose Z bound (the corridor width) is normally far
     * smaller than its X bound (the corridor length, from the border).
     *
     * @param plan the world's coordinated-layout plan
     * @param x chosen fallback X coordinate
     * @param xRadiusBlocks supportive X bound (see {@link #supportiveRadius})
     * @param zRadiusBlocks supportive Z bound -- a strip's width radius, or the same as
     *     {@code xRadiusBlocks} for a square world
     * @param structureMarginBlocks required structure extent around the point
     * @return a Z offset that fits inside the bounds, preferring a supportive column
     */
    public static int supportiveFallbackZ(
        WorldLayoutPlan plan, int x, int xRadiusBlocks, int zRadiusBlocks, int structureMarginBlocks
    ) {
        for (int z : FALLBACK_Z_CANDIDATES) {
            if (fitsInside(x, z, xRadiusBlocks, zRadiusBlocks, structureMarginBlocks) && isSupportiveColumn(plan, x, z)) {
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
        return fitsInside(x, z, radiusBlocks, radiusBlocks, structureMarginBlocks);
    }

    /**
     * Tests whether a structure reference point plus safety margin fits inside independent
     * X/Z bounds -- needed for a strip world (GOALS 32), whose Z bound (corridor width) is
     * normally far smaller than its X bound (corridor length, from the border).
     *
     * @param x structure X coordinate
     * @param z structure Z coordinate
     * @param xRadiusBlocks X bound (a square border's radius, or a strip's length)
     * @param zRadiusBlocks Z bound -- a strip's width radius, or the same as
     *     {@code xRadiusBlocks} for a square world
     * @param structureMarginBlocks required structure extent around the point
     * @return whether the structure is safely reachable
     */
    public static boolean fitsInside(int x, int z, int xRadiusBlocks, int zRadiusBlocks, int structureMarginBlocks) {
        long usableXRadius = (long)xRadiusBlocks - structureMarginBlocks;
        long usableZRadius = (long)zRadiusBlocks - structureMarginBlocks;
        return usableXRadius >= 0L && usableZRadius >= 0L
            && Math.abs((long)x) <= usableXRadius && Math.abs((long)z) <= usableZRadius;
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

    /**
     * Narrows a supportive radius to also respect an ocean island's own radius (GOALS 01,
     * DESIGN §24), which -- unlike every other exterior mechanism -- is deliberately
     * <em>not</em> expressed through {@link ExteriorPlan.DimensionEnvelope} at all (§24.1/24.5:
     * the flat single-depth envelope model can't represent the shallow-to-deep gradient), so
     * the border/envelope-only overload above would otherwise report an ocean-island world as
     * an unlimited normal world and silently skip the fallback End-portal guarantee entirely.
     *
     * @param borderEnabled whether the final border is a reachability bound
     * @param finalBorderRadius final border half-width
     * @param envelope exterior terrain bound
     * @param island the world's ocean-island plan
     * @return effective supportive radius, empty only when border, envelope, and island are
     *     all inactive
     */
    public static OptionalInt supportiveRadius(
        boolean borderEnabled,
        int finalBorderRadius,
        ExteriorPlan.DimensionEnvelope envelope,
        IslandPlan island
    ) {
        if (!island.enabled()) {
            return supportiveRadius(borderEnabled, finalBorderRadius, envelope);
        }
        int radius = borderEnabled ? finalBorderRadius : Integer.MAX_VALUE;
        if (envelope.mode() != ExteriorMode.NORMAL) {
            radius = Math.min(radius, envelope.solidRadiusBlocks());
        }
        // A land-free island (GOALS 03, IslandPlan.hasLand false) has no real size of its own to
        // narrow by -- radiusBlocks is only ever a harmless placeholder in that case (DESIGN
        // §25.2), and treating it as a real bound would wrongly shrink the fallback's search to
        // the configured minimum radius.
        return OptionalInt.of(island.hasLand() ? Math.min(radius, island.radiusBlocks()) : radius);
    }

    /**
     * Narrows an X-axis supportive radius to also respect a strip world's width (GOALS 32),
     * whose Z bound is normally much smaller than the border/envelope's own radius. The
     * strip's length (X axis) already rides the same {@link #supportiveRadius} unmodified.
     *
     * @param radiusBlocks the X-axis supportive radius (see {@link #supportiveRadius})
     * @param strip the world's strip plan
     * @return the tighter of the two bounds when the strip is enabled, otherwise
     *     {@code radiusBlocks} unchanged
     */
    public static int narrowForStrip(int radiusBlocks, StripPlan strip) {
        return strip.enabled() ? Math.min(radiusBlocks, strip.widthRadiusBlocks()) : radiusBlocks;
    }
}
