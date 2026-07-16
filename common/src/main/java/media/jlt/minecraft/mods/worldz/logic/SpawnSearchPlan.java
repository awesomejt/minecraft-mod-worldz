package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, deterministic outward search pattern for the {@code PREFERRED_NATURAL_BIOME}
 * spawn strategy (DESIGN §18). Generates candidate block offsets from the current
 * origin in concentric rings, origin first; the caller drives the actual biome test
 * against each offset in order and stops at the first match, since a real biome/climate
 * sampler is not pure and cannot live in this class.
 *
 * @param maxRadiusBlocks outermost ring radius, inclusive
 * @param stepBlocks spacing between successive rings
 * @param pointsPerRing candidate points sampled per ring, evenly spaced by angle
 */
public record SpawnSearchPlan(int maxRadiusBlocks, int stepBlocks, int pointsPerRing) {
    /** Matches vanilla's own spawn-search radius (see {@code Climate.SpawnFinder}). */
    public static final int DEFAULT_MAX_RADIUS_BLOCKS = 2048;
    /** Default ring spacing. */
    public static final int DEFAULT_STEP_BLOCKS = 32;
    /** Default candidate count per ring. */
    public static final int DEFAULT_POINTS_PER_RING = 8;

    /** Validates the search bounds. */
    public SpawnSearchPlan {
        if (maxRadiusBlocks <= 0) {
            throw new IllegalArgumentException("Max search radius must be positive.");
        }
        if (stepBlocks <= 0) {
            throw new IllegalArgumentException("Search step must be positive.");
        }
        if (stepBlocks > maxRadiusBlocks) {
            throw new IllegalArgumentException("Search step must not exceed the max search radius.");
        }
        if (pointsPerRing <= 0) {
            throw new IllegalArgumentException("Points per ring must be positive.");
        }
    }

    /**
     * Returns the fixture-matched default search plan.
     *
     * @return default search bounds
     */
    public static SpawnSearchPlan defaults() {
        return new SpawnSearchPlan(DEFAULT_MAX_RADIUS_BLOCKS, DEFAULT_STEP_BLOCKS, DEFAULT_POINTS_PER_RING);
    }

    /**
     * Generates every candidate offset in deterministic search order: the origin
     * itself, then each ring outward from {@code stepBlocks} to {@code maxRadiusBlocks}.
     *
     * @return immutable ordered offsets, origin-relative
     */
    public List<Offset> offsetsInSearchOrder() {
        List<Offset> offsets = new ArrayList<>();
        offsets.add(new Offset(0, 0));
        for (int radius = stepBlocks; radius <= maxRadiusBlocks; radius += stepBlocks) {
            for (int index = 0; index < pointsPerRing; index++) {
                double angle = 2.0 * Math.PI * index / pointsPerRing;
                int x = (int) Math.round(radius * Math.sin(angle));
                int z = (int) Math.round(radius * -Math.cos(angle));
                offsets.add(new Offset(x, z));
            }
        }
        return List.copyOf(offsets);
    }

    /**
     * Total candidate count {@link #offsetsInSearchOrder()} will produce, without
     * building the list.
     *
     * @return offset count
     */
    public int offsetCount() {
        return 1 + (maxRadiusBlocks / stepBlocks) * pointsPerRing;
    }

    /**
     * One origin-relative candidate block column.
     *
     * @param x block X offset
     * @param z block Z offset
     */
    public record Offset(int x, int z) {
    }
}
