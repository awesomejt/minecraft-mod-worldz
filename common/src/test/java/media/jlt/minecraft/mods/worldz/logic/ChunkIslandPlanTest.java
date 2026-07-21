package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkIslandPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        ChunkIslandPlan disabled = ChunkIslandPlan.disabled();
        assertFalse(disabled.enabled());
        assertFalse(disabled.exclusionZone().enabled());
    }

    @Test
    void disabledPlanNeverHitsRegardlessOfSpawnChance() {
        ChunkIslandPlan plan = new ChunkIslandPlan(false, 1.0, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        for (int chunkX = -50; chunkX <= 50; chunkX++) {
            assertFalse(plan.at(chunkX, 0, 42L).present());
        }
    }

    @Test
    void starterCellIsAlwaysPresentRegardlessOfSpawnChance() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 0.0, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        assertTrue(plan.at(0, 0, 42L).present());
    }

    @Test
    void zeroSpawnChanceNeverHitsOutsideTheStarterCell() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 0.0, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        for (int chunkX = 1; chunkX <= 200; chunkX++) {
            assertFalse(plan.at(chunkX, 5, 42L).present(), "expected no hit at chunk " + chunkX);
        }
    }

    @Test
    void fullSpawnChanceAlwaysHits() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 1.0, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        for (int chunkX = -50; chunkX <= 50; chunkX += 3) {
            assertTrue(plan.at(chunkX, 7, 42L).present(), "expected a hit at chunk " + chunkX);
        }
    }

    @Test
    void exclusionZoneSuppressesIslandsInsideItsRadius() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 1.0, 1, false, 5, new IslandPlan.ExclusionZone(true, 100_000));
        // Chunk (1, 0) is well within a 100,000-block exclusion zone but is not the starter cell.
        assertFalse(plan.at(1, 0, 42L).present());
    }

    @Test
    void resultsAreDeterministicForTheSameSeed() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 0.5, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        for (int chunkX = -30; chunkX <= 30; chunkX++) {
            assertEquals(plan.at(chunkX, 3, 99L).present(), plan.at(chunkX, 3, 99L).present());
        }
    }

    @Test
    void largerCellSizeGroupsChunksTogether() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 1.0, 4, false, 5, new IslandPlan.ExclusionZone(false, 0));
        // Every chunk in the same 4x4-chunk cell resolves to the same present/absent verdict.
        boolean first = plan.at(20, 20, 7L).present();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                assertEquals(first, plan.at(20 + dx, 20 + dz, 7L).present());
            }
        }
    }

    @Test
    void hitCarriesThePlanWideDepthMode() {
        ChunkIslandPlan topOnly = new ChunkIslandPlan(true, 1.0, 1, true, 7, new IslandPlan.ExclusionZone(false, 0));
        ChunkIslandPlan.Hit hit = topOnly.at(3, 3, 5L);
        assertTrue(hit.present());
        assertTrue(hit.topOnly());
        assertEquals(7, hit.topOnlyDepthBlocks());
    }

    @Test
    void reservedPortalCellIsAlwaysBeyondTheExclusionZone() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 1.0, 2, false, 5, new IslandPlan.ExclusionZone(true, 500));
        for (long seed = 0; seed < 50; seed++) {
            ChunkIslandPlan.PortalCell cell = plan.reservedPortalCell(seed);
            int[] center = cell.centerBlock(plan.cellSizeChunks());
            double distance = Math.hypot(center[0], center[1]);
            assertTrue(distance >= 500, "expected the portal cell beyond the exclusion zone at seed " + seed);
        }
    }

    @Test
    void reservedPortalCellIsDeterministicAndAlwaysPresentInTheGrid() {
        ChunkIslandPlan plan = new ChunkIslandPlan(true, 0.0, 1, false, 5, new IslandPlan.ExclusionZone(false, 0));
        ChunkIslandPlan.PortalCell cell = plan.reservedPortalCell(123L);
        assertEquals(cell, plan.reservedPortalCell(123L));
        assertTrue(plan.at((int) cell.cellX(), (int) cell.cellZ(), 123L).present());
    }

    @Test
    void invalidSpawnChanceRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ChunkIslandPlan(true, 1.5, 1, false, 5, new IslandPlan.ExclusionZone(false, 0))
        );
    }

    @Test
    void invalidCellSizeRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ChunkIslandPlan(true, 0.5, 0, false, 5, new IslandPlan.ExclusionZone(false, 0))
        );
    }

    @Test
    void invalidTopOnlyDepthRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ChunkIslandPlan(true, 0.5, 1, true, 0, new IslandPlan.ExclusionZone(false, 0))
        );
    }
}
