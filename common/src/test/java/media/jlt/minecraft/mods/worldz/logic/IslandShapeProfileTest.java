package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandShapeProfileTest {
    @Test
    void zeroAmplitudeIsAnExactCircle() {
        assertEquals(128.0, IslandShapeProfile.radiusAt(128.0, 0.0, 0.0, 42L), 0.000_001);
        assertEquals(128.0, IslandShapeProfile.radiusAt(128.0, 0.0, 1.7, 42L), 0.000_001);
        assertEquals(0.0, IslandShapeProfile.distanceFromShore(128, 0, 128.0, 0.0, 42L), 0.000_001);
        assertEquals(-28.0, IslandShapeProfile.distanceFromShore(100, 0, 128.0, 0.0, 42L), 0.000_001);
        assertEquals(72.0, IslandShapeProfile.distanceFromShore(200, 0, 128.0, 0.0, 42L), 0.000_001);
    }

    @Test
    void perturbedRadiusStaysWithinTheAmplitudeBound() {
        for (double angle = 0; angle < Math.PI * 2; angle += 0.3) {
            double radius = IslandShapeProfile.radiusAt(128.0, 0.3, angle, 42L);
            assertTrue(radius >= 128.0 * 0.7 && radius <= 128.0 * 1.3, "radius " + radius + " out of bounds at angle " + angle);
        }
    }

    @Test
    void amplitudeIsClampedToTheMaximum() {
        double overAmplitude = IslandShapeProfile.radiusAt(128.0, 5.0, 0.4, 42L);
        double atMax = IslandShapeProfile.radiusAt(128.0, IslandShapeProfile.MAX_AMPLITUDE, 0.4, 42L);
        assertEquals(atMax, overAmplitude, 0.000_001);
    }

    @Test
    void differentSeedsProduceDifferentShapes() {
        double first = IslandShapeProfile.radiusAt(128.0, 0.3, 0.7, 1L);
        double second = IslandShapeProfile.radiusAt(128.0, 0.3, 0.7, 2L);
        assertNotEquals(first, second);
    }

    @Test
    void sameSeedIsDeterministic() {
        double first = IslandShapeProfile.radiusAt(128.0, 0.3, 0.7, 42L);
        double second = IslandShapeProfile.radiusAt(128.0, 0.3, 0.7, 42L);
        assertEquals(first, second, 0.0);
    }

    @Test
    void strengthIsFullInsideAndZeroAtTheShoreEdge() {
        assertEquals(1.0, IslandShapeProfile.strengthAt(-10.0, 12), 0.000_001);
        assertEquals(1.0, IslandShapeProfile.strengthAt(0.0, 12), 0.000_001);
        assertEquals(0.5, IslandShapeProfile.strengthAt(6.0, 12), 0.000_001);
        assertEquals(0.0, IslandShapeProfile.strengthAt(12.0, 12), 0.000_001);
        assertEquals(0.0, IslandShapeProfile.strengthAt(20.0, 12), 0.000_001);
    }

    @Test
    void targetHeightRaisesLowTerrainButPreservesNaturalHighGround() {
        assertEquals(71, IslandShapeProfile.targetHeight(-10.0, 12, 40, 63, 0.0));
        assertEquals(92, IslandShapeProfile.targetHeight(-10.0, 12, 92, 63, 0.0));
    }

    @Test
    void targetHeightTapersToTheBlendBaselineAtTheShoreEdge() {
        assertEquals(40, IslandShapeProfile.targetHeight(12.0, 12, 40, 63, 1.0));
        assertEquals(40, IslandShapeProfile.targetHeight(20.0, 12, 40, 63, 1.0));
    }
}
