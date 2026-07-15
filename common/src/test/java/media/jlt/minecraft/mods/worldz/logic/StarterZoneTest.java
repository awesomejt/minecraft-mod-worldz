package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterZoneTest {
    @Test
    void convertsBlocksToQuartCoordinatesUsingFloorDivision() {
        assertEquals(128, StarterZone.quartRadius(512));
        assertEquals(1, StarterZone.quartRadius(7));
        assertEquals(-1, StarterZone.quartRadius(-1));
    }

    @Test
    void includesOriginAndCircularBoundary() {
        assertTrue(StarterZone.containsQuart(0, 0, 512));
        assertTrue(StarterZone.containsQuart(128, 0, 512));
        assertTrue(StarterZone.containsQuart(-128, 0, 512));
        assertTrue(StarterZone.containsQuart(0, 128, 512));
    }

    @Test
    void excludesPositionsBeyondBoundaryAndSquareCorners() {
        assertFalse(StarterZone.containsQuart(129, 0, 512));
        assertFalse(StarterZone.containsQuart(128, 1, 512));
        assertFalse(StarterZone.containsQuart(100, 100, 512));
    }

    @Test
    void radiusConversionMatchesQuartPrecision() {
        assertTrue(StarterZone.containsQuart(1, 0, 7));
        assertFalse(StarterZone.containsQuart(2, 0, 7));
    }

    @Test
    void rejectsNegativeRadiusAndHandlesExtremeCoordinatesWithoutOverflow() {
        assertFalse(StarterZone.containsQuart(0, 0, -1));
        assertFalse(StarterZone.containsQuart(Integer.MIN_VALUE, Integer.MIN_VALUE, 4096));
        assertFalse(StarterZone.containsQuart(Integer.MAX_VALUE, Integer.MAX_VALUE, 4096));
    }

    @Test
    void includesExactDiagonalBoundaryAndIsSymmetric() {
        assertTrue(StarterZone.containsQuart(3, 4, 20));
        assertTrue(StarterZone.containsQuart(-3, 4, 20));
        assertTrue(StarterZone.containsQuart(3, -4, 20));
        assertTrue(StarterZone.containsQuart(-3, -4, 20));
        assertFalse(StarterZone.containsQuart(4, 4, 20));
    }

    @Test
    void zeroRadiusIncludesOnlyTheOriginQuart() {
        assertTrue(StarterZone.containsQuart(0, 0, 0));
        assertFalse(StarterZone.containsQuart(1, 0, 0));
        assertFalse(StarterZone.containsQuart(0, -1, 0));
    }

    @Test
    void ringExcludesInnerZoneAndIncludesOuterBand() {
        assertFalse(StarterZone.inRingQuart(0, 0, 512, 640));
        assertFalse(StarterZone.inRingQuart(128, 0, 512, 640));
        assertTrue(StarterZone.inRingQuart(129, 0, 512, 640));
        assertTrue(StarterZone.inRingQuart(160, 0, 512, 640));
        assertFalse(StarterZone.inRingQuart(161, 0, 512, 640));
    }

    @Test
    void ringIsEmptyWhenOuterRadiusDoesNotExceedInner() {
        assertFalse(StarterZone.inRingQuart(0, 0, 512, 512));
        assertFalse(StarterZone.inRingQuart(0, 0, 512, 256));
    }
}
