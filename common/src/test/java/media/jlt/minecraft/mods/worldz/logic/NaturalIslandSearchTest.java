package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalIslandSearchTest {
    @Test
    void centerBeingOceanIsNeverIsolatedLand() {
        assertFalse(NaturalIslandSearch.isIsolatedLand(0, 0, 64, true, (x, z) -> true));
    }

    @Test
    void fullySurroundedByOceanIsIsolatedLand() {
        assertTrue(NaturalIslandSearch.isIsolatedLand(0, 0, 64, false, (x, z) -> true));
    }

    @Test
    void notSurroundedByOceanAtAllIsNotIsolated() {
        assertFalse(NaturalIslandSearch.isIsolatedLand(0, 0, 64, false, (x, z) -> false));
    }

    @Test
    void mostlySurroundedMeetsTheIsolationThreshold() {
        // 6 of 8 ring samples ocean (the 75% threshold) should qualify.
        int[] counter = {0};
        assertTrue(NaturalIslandSearch.isIsolatedLand(0, 0, 64, false, (x, z) -> {
            counter[0]++;
            return counter[0] <= 6;
        }));
    }

    @Test
    void justBelowTheIsolationThresholdFails() {
        int[] counter = {0};
        assertFalse(NaturalIslandSearch.isIsolatedLand(0, 0, 64, false, (x, z) -> {
            counter[0]++;
            return counter[0] <= 5;
        }));
    }

    @Test
    void samplesTheExpectedRingPointCount() {
        int[] calls = {0};
        NaturalIslandSearch.isIsolatedLand(0, 0, 64, false, (x, z) -> {
            calls[0]++;
            return true;
        });
        assertEquals(NaturalIslandSearch.RING_SAMPLE_COUNT, calls[0]);
    }
}
