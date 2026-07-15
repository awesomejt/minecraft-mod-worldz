package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectiveSiteTest {
    @Test
    void structureMarginMustFitOnBothAxes() {
        assertTrue(ObjectiveSite.fitsInside(384, -384, 512, 128));
        assertFalse(ObjectiveSite.fitsInside(385, 0, 512, 128));
        assertFalse(ObjectiveSite.fitsInside(0, -385, 512, 128));
    }

    @Test
    void arithmeticHandlesExtremeNegativeCoordinates() {
        assertFalse(ObjectiveSite.fitsInside(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0));
    }

    @Test
    void fallbackStaysPreferredWhenThereIsRoom() {
        assertEquals(32, ObjectiveSite.fallbackX(512));
        assertEquals(32, ObjectiveSite.fallbackX(64));
    }

    @Test
    void fallbackMovesInwardForTinyDefensiveInputs() {
        assertEquals(4, ObjectiveSite.fallbackX(20));
        assertEquals(0, ObjectiveSite.fallbackX(10));
    }

    @Test
    void supportiveRadiusUsesTheTightestBorderOrTerrainBound() {
        var voidEnvelope = new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, 384, 0);

        assertEquals(384, ObjectiveSite.supportiveRadius(false, 512, voidEnvelope).orElseThrow());
        assertEquals(256, ObjectiveSite.supportiveRadius(true, 256, voidEnvelope).orElseThrow());
        assertEquals(384, ObjectiveSite.supportiveRadius(true, 512, voidEnvelope).orElseThrow());
    }

    @Test
    void oceanTransitionIsExcludedFromSupportiveTerrain() {
        var oceanEnvelope = new ExteriorPlan.DimensionEnvelope(ExteriorMode.OCEAN, 512, 96);

        assertEquals(416, ObjectiveSite.supportiveRadius(false, 512, oceanEnvelope).orElseThrow());
    }

    @Test
    void normalUnlimitedWorldHasNoObjectiveBound() {
        assertTrue(ObjectiveSite.supportiveRadius(
            false,
            512,
            ExteriorPlan.DimensionEnvelope.normal()
        ).isEmpty());
    }

    @Test
    void legacyAndVoidLayoutsAreAlwaysSupportive() {
        assertTrue(ObjectiveSite.isSupportiveColumn(WorldLayoutPlan.legacy(), 12345, -6789));

        WorldLayoutPlan voidPlan = new WorldLayoutPlan(
            LayoutMode.VOID, 1L, 512, 0.0, 128, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
        assertTrue(ObjectiveSite.isSupportiveColumn(voidPlan, 12345, -6789));
    }

    @Test
    void oceanModeColumnsAreNeverSupportive() {
        WorldLayoutPlan oceanPlan = new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, 0.0, 128, List.of(), List.of(new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0)),
            List.of(), Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        assertFalse(ObjectiveSite.isSupportiveColumn(oceanPlan, 12345, -6789));
    }

    @Test
    void supportiveFallbackZPrefersZeroWhenAlreadySupportive() {
        assertEquals(0, ObjectiveSite.supportiveFallbackZ(WorldLayoutPlan.legacy(), 32, 512, 128));
    }

    @Test
    void supportiveFallbackZHonorsItsContractAcrossManySeedsAndOffsets() {
        List<WorldLayoutPlan.BiomeWeight> land = List.of(new WorldLayoutPlan.BiomeWeight("minecraft:plains", 1.0));
        List<WorldLayoutPlan.BiomeWeight> ocean = List.of(new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0));
        boolean everFoundABetterCandidate = false;

        for (long seed = 0; seed < 200; seed++) {
            WorldLayoutPlan plan = new WorldLayoutPlan(
                LayoutMode.MIXED, seed, 100, 0.5, 20, land, ocean, List.of(),
                Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
            );
            int x = 32;
            int z = ObjectiveSite.supportiveFallbackZ(plan, x, 512, 128);

            assertTrue(z == 0 || z == 64 || z == -64 || z == 128 || z == -128, "unexpected candidate " + z);
            if (ObjectiveSite.isSupportiveColumn(plan, x, 0)) {
                assertEquals(0, z, "z=0 was already supportive but a different candidate was chosen");
            } else if (z != 0) {
                assertTrue(ObjectiveSite.isSupportiveColumn(plan, x, z), "chosen candidate was not actually supportive");
                everFoundABetterCandidate = true;
            }
        }

        assertTrue(everFoundABetterCandidate, "no seed in the sample ever needed or found a non-zero fallback candidate");
    }

    @Test
    void supportiveFallbackZFallsBackToZeroWhenNothingIsSupportive() {
        WorldLayoutPlan oceanPlan = new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, 0.0, 128, List.of(), List.of(new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0)),
            List.of(), Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        assertEquals(0, ObjectiveSite.supportiveFallbackZ(oceanPlan, 32, 512, 128));
    }
}
