package media.jlt.minecraft.mods.worldz.logic;

import java.util.function.BiPredicate;

/**
 * Pure geometry for GOALS 02's natural-island isolation check (DESIGN §25.4): a candidate point
 * plausibly reads as a small, isolated landmass when it isn't itself ocean and most of a ring of
 * sample points around it, at the island's own radius, are ocean.
 */
public final class NaturalIslandSearch {
    /** Sample points taken around the candidate's ring. */
    public static final int RING_SAMPLE_COUNT = 8;
    /** Fraction of ring samples that must be ocean for a candidate to count as isolated. */
    private static final double ISOLATION_FRACTION = 0.75;

    private NaturalIslandSearch() {
    }

    /**
     * Tests whether a candidate point is plausibly a small, isolated landmass.
     *
     * @param x candidate block X
     * @param z candidate block Z
     * @param radiusBlocks ring sample radius (the configured island radius)
     * @param centerIsOcean whether the candidate point itself is ocean-family
     * @param isOcean tests whether the biome at a given (x, z) is ocean-family
     * @return whether the candidate qualifies as an isolated natural island
     */
    public static boolean isIsolatedLand(
        int x, int z, int radiusBlocks, boolean centerIsOcean, BiPredicate<Integer, Integer> isOcean
    ) {
        if (centerIsOcean) {
            return false;
        }
        int oceanSamples = 0;
        for (int index = 0; index < RING_SAMPLE_COUNT; index++) {
            double angle = 2.0 * Math.PI * index / RING_SAMPLE_COUNT;
            int ringX = x + (int) Math.round(radiusBlocks * Math.cos(angle));
            int ringZ = z + (int) Math.round(radiusBlocks * Math.sin(angle));
            if (isOcean.test(ringX, ringZ)) {
                oceanSamples++;
            }
        }
        return oceanSamples >= Math.ceil(RING_SAMPLE_COUNT * ISOLATION_FRACTION);
    }
}
