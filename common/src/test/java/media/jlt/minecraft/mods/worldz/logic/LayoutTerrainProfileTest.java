package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTerrainProfileTest {
    private static final int SEA_LEVEL = 63;

    @Test
    void fullyLandRaisesLowNaturalFloorToTheLandBaseline() {
        int result = LayoutTerrainProfile.targetHeight(SEA_LEVEL - 20, 1.0, SEA_LEVEL);

        assertEquals(SEA_LEVEL + LayoutTerrainProfile.LAND_FLOOR_OFFSET_ABOVE_SEA_LEVEL, result);
    }

    @Test
    void fullyLandLeavesAlreadyHighNaturalFloorUnchanged() {
        int naturalFloor = SEA_LEVEL + 40;

        assertEquals(naturalFloor, LayoutTerrainProfile.targetHeight(naturalFloor, 1.0, SEA_LEVEL));
    }

    @Test
    void fullyOceanCapsHighNaturalFloorToTheOceanCeiling() {
        int result = LayoutTerrainProfile.targetHeight(SEA_LEVEL + 40, 0.0, SEA_LEVEL);

        assertEquals(SEA_LEVEL - LayoutTerrainProfile.OCEAN_CEILING_OFFSET_BELOW_SEA_LEVEL, result);
    }

    @Test
    void fullyOceanLeavesAlreadyLowNaturalFloorUnchanged() {
        int naturalFloor = SEA_LEVEL - 20;

        assertEquals(naturalFloor, LayoutTerrainProfile.targetHeight(naturalFloor, 0.0, SEA_LEVEL));
    }

    @Test
    void midBlendFallsStrictlyBetweenCappedAndRaisedTargets() {
        int naturalFloor = SEA_LEVEL + 40;
        int capped = LayoutTerrainProfile.targetHeight(naturalFloor, 0.0, SEA_LEVEL);
        int raised = LayoutTerrainProfile.targetHeight(naturalFloor, 1.0, SEA_LEVEL);
        int mid = LayoutTerrainProfile.targetHeight(naturalFloor, 0.5, SEA_LEVEL);

        assertTrue(mid > capped && mid < raised, "expected " + capped + " < " + mid + " < " + raised);
    }

    @Test
    void resultIsMonotonicWithIncreasingLandFactor() {
        int naturalFloor = SEA_LEVEL - 5;
        int previous = LayoutTerrainProfile.targetHeight(naturalFloor, 0.0, SEA_LEVEL);
        for (int step = 1; step <= 10; step++) {
            int current = LayoutTerrainProfile.targetHeight(naturalFloor, step / 10.0, SEA_LEVEL);
            assertTrue(current >= previous, "target height decreased as landFactor increased");
            previous = current;
        }
    }

    @Test
    void landOnlyLeavesShallowDepressionsLikeRiversUntouched() {
        int shallowRiverFloor = SEA_LEVEL - 1;

        assertEquals(shallowRiverFloor, LayoutTerrainProfile.landOnlyTarget(shallowRiverFloor, SEA_LEVEL));
        assertEquals(SEA_LEVEL, LayoutTerrainProfile.landOnlyTarget(SEA_LEVEL, SEA_LEVEL));
    }

    @Test
    void landOnlyLeavesOrdinaryDryLandUntouched() {
        int naturalHill = SEA_LEVEL + 40;

        assertEquals(naturalHill, LayoutTerrainProfile.landOnlyTarget(naturalHill, SEA_LEVEL));
    }

    @Test
    void landOnlyRaisesOnlyDeepOceanBasinsToTheLandFloor() {
        int deepOceanFloor = SEA_LEVEL - 20;

        assertEquals(
            SEA_LEVEL + LayoutTerrainProfile.LAND_FLOOR_OFFSET_ABOVE_SEA_LEVEL,
            LayoutTerrainProfile.landOnlyTarget(deepOceanFloor, SEA_LEVEL)
        );
    }

    @Test
    void outOfRangeLandFactorIsClampedRatherThanExtrapolated() {
        int naturalFloor = SEA_LEVEL - 20;

        assertEquals(
            LayoutTerrainProfile.targetHeight(naturalFloor, 1.0, SEA_LEVEL),
            LayoutTerrainProfile.targetHeight(naturalFloor, 5.0, SEA_LEVEL)
        );
        assertEquals(
            LayoutTerrainProfile.targetHeight(naturalFloor, 0.0, SEA_LEVEL),
            LayoutTerrainProfile.targetHeight(naturalFloor, -5.0, SEA_LEVEL)
        );
    }
}
