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
            LayoutMode.VOID, 1L, 512, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
        assertTrue(ObjectiveSite.isSupportiveColumn(voidPlan, 12345, -6789));
    }

    @Test
    void oceanModeColumnsAreNeverSupportive() {
        WorldLayoutPlan oceanPlan = new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, List.of(), List.of(new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0)),
            List.of(), Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        assertFalse(ObjectiveSite.isSupportiveColumn(oceanPlan, 12345, -6789));
    }

    @Test
    void supportiveFallbackZPrefersZeroWhenAlreadySupportive() {
        assertEquals(0, ObjectiveSite.supportiveFallbackZ(WorldLayoutPlan.legacy(), 32, 512, 128));
    }

    @Test
    void supportiveFallbackZFallsBackToZeroWhenNothingIsSupportive() {
        WorldLayoutPlan oceanPlan = new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, List.of(), List.of(new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0)),
            List.of(), Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        assertEquals(0, ObjectiveSite.supportiveFallbackZ(oceanPlan, 32, 512, 128));
    }
}
