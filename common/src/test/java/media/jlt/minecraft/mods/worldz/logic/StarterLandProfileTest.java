package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarterLandProfileTest {
    @Test
    void coreRaisesLowTerrainButPreservesNaturalHighGround() {
        assertEquals(65, target(0, 0, 40, StarterLandPlan.LEGACY_PROFILE_VERSION, 0.0));
        assertEquals(92, target(32, 32, 92, StarterLandPlan.RELIEF_PROFILE_VERSION, 0.0));
    }

    @Test
    void reliefProfileDoesNotFlattenDifferentNaturalFloorHeights() {
        assertEquals(65, target(0, 0, 20, StarterLandPlan.RELIEF_PROFILE_VERSION, -1.0));
        assertEquals(67, target(0, 0, 40, StarterLandPlan.RELIEF_PROFILE_VERSION, -1.0));
        assertEquals(75, target(0, 0, 40, StarterLandPlan.RELIEF_PROFILE_VERSION, 1.0));
    }

    @Test
    void transitionUsesSmoothstepAndEndsAtNaturalTerrain() {
        assertEquals(1.0, StarterLandProfile.strengthAt(64, 0, 64, 128), 0.000_001);
        assertEquals(0.5, StarterLandProfile.strengthAt(128, 0, 64, 128), 0.000_001);
        assertEquals(0.0, StarterLandProfile.strengthAt(192, 0, 64, 128), 0.000_001);
        assertEquals(53, target(128, 0, 40, StarterLandPlan.LEGACY_PROFILE_VERSION, 0.0));
        assertEquals(40, target(192, 0, 40, StarterLandPlan.RELIEF_PROFILE_VERSION, 1.0));
        assertEquals(40, target(191, 0, 40, StarterLandPlan.RELIEF_PROFILE_VERSION, 1.0));
    }

    @Test
    void zeroWidthHasAnExactBoundary() {
        assertEquals(65, target(64, 0, 20, StarterLandPlan.RELIEF_PROFILE_VERSION, -1.0, 0));
        assertEquals(20, target(65, 0, 20, StarterLandPlan.RELIEF_PROFILE_VERSION, -1.0, 0));
    }

    @Test
    void extremeCoordinatesAndFoundationArithmeticStaySafe() {
        assertEquals(0.0, StarterLandProfile.strengthAt(Integer.MIN_VALUE, Integer.MAX_VALUE, 4096, 4096));
        assertEquals(-64, StarterLandProfile.foundationMinY(-63, 32, -64));
        assertEquals(-20, StarterLandProfile.foundationMinY(12, 32, -64));
        assertEquals(Integer.MIN_VALUE, StarterLandProfile.foundationMinY(Integer.MIN_VALUE, 384, Integer.MIN_VALUE));
    }

    private static int target(int x, int z, int naturalHeight, int profileVersion, double noise) {
        return target(x, z, naturalHeight, profileVersion, noise, 128);
    }

    private static int target(int x, int z, int naturalHeight, int profileVersion, double noise, int transition) {
        return StarterLandProfile.targetHeight(x, z, 64, transition, naturalHeight, 63, profileVersion, noise);
    }
}
