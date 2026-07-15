package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarterLandProfileTest {
    @Test
    void coreRaisesLowTerrainButPreservesNaturalHighGround() {
        assertEquals(65, StarterLandProfile.targetHeight(0, 0, 64, 128, 40, 63));
        assertEquals(92, StarterLandProfile.targetHeight(32, 32, 64, 128, 92, 63));
    }

    @Test
    void transitionUsesSmoothstepAndEndsAtNaturalTerrain() {
        assertEquals(1.0, StarterLandProfile.strengthAt(64, 0, 64, 128), 0.000_001);
        assertEquals(0.5, StarterLandProfile.strengthAt(128, 0, 64, 128), 0.000_001);
        assertEquals(0.0, StarterLandProfile.strengthAt(192, 0, 64, 128), 0.000_001);
        assertEquals(53, StarterLandProfile.targetHeight(128, 0, 64, 128, 40, 63));
        assertEquals(40, StarterLandProfile.targetHeight(192, 0, 64, 128, 40, 63));
    }

    @Test
    void zeroWidthHasAnExactBoundary() {
        assertEquals(65, StarterLandProfile.targetHeight(64, 0, 64, 0, 20, 63));
        assertEquals(20, StarterLandProfile.targetHeight(65, 0, 64, 0, 20, 63));
    }

    @Test
    void extremeCoordinatesAndFoundationArithmeticStaySafe() {
        assertEquals(0.0, StarterLandProfile.strengthAt(Integer.MIN_VALUE, Integer.MAX_VALUE, 4096, 4096));
        assertEquals(-64, StarterLandProfile.foundationMinY(-63, 32, -64));
        assertEquals(-20, StarterLandProfile.foundationMinY(12, 32, -64));
        assertEquals(Integer.MIN_VALUE, StarterLandProfile.foundationMinY(Integer.MIN_VALUE, 384, Integer.MIN_VALUE));
    }
}
