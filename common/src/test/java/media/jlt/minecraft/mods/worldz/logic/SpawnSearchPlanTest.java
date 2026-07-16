package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnSearchPlanTest {
    @Test
    void firstCandidateIsAlwaysTheOrigin() {
        SpawnSearchPlan plan = SpawnSearchPlan.defaults();

        assertEquals(new SpawnSearchPlan.Offset(0, 0), plan.offsetsInSearchOrder().getFirst());
    }

    @Test
    void cardinalRingProducesExactCompassPoints() {
        SpawnSearchPlan plan = new SpawnSearchPlan(64, 32, 4);

        List<SpawnSearchPlan.Offset> offsets = plan.offsetsInSearchOrder();

        assertEquals(List.of(
            new SpawnSearchPlan.Offset(0, 0),
            new SpawnSearchPlan.Offset(0, -32),
            new SpawnSearchPlan.Offset(32, 0),
            new SpawnSearchPlan.Offset(0, 32),
            new SpawnSearchPlan.Offset(-32, 0),
            new SpawnSearchPlan.Offset(0, -64),
            new SpawnSearchPlan.Offset(64, 0),
            new SpawnSearchPlan.Offset(0, 64),
            new SpawnSearchPlan.Offset(-64, 0)
        ), offsets);
    }

    @Test
    void offsetCountMatchesGeneratedListSize() {
        SpawnSearchPlan plan = new SpawnSearchPlan(256, 32, 8);

        assertEquals(plan.offsetCount(), plan.offsetsInSearchOrder().size());
        assertEquals(1 + 8 * 8, plan.offsetCount());
    }

    @Test
    void everyNonOriginOffsetLandsCloseToItsRingRadius() {
        SpawnSearchPlan plan = new SpawnSearchPlan(2048, 32, 8);

        for (SpawnSearchPlan.Offset offset : plan.offsetsInSearchOrder()) {
            if (offset.x() == 0 && offset.z() == 0) {
                continue;
            }
            double distance = Math.hypot(offset.x(), offset.z());
            double nearestRingMultiple = Math.round(distance / 32.0) * 32.0;
            assertTrue(Math.abs(distance - nearestRingMultiple) < 1.0, "offset " + offset + " is off-ring: distance " + distance);
        }
    }

    @Test
    void resultIsDeterministicAcrossCalls() {
        SpawnSearchPlan plan = SpawnSearchPlan.defaults();

        assertEquals(plan.offsetsInSearchOrder(), plan.offsetsInSearchOrder());
    }

    @Test
    void rejectsNonPositiveOrInconsistentBounds() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnSearchPlan(0, 32, 8));
        assertThrows(IllegalArgumentException.class, () -> new SpawnSearchPlan(2048, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> new SpawnSearchPlan(2048, 32, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnSearchPlan(16, 32, 8));
    }
}
