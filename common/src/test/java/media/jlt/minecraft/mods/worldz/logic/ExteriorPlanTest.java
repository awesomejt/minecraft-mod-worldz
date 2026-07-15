package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExteriorPlanTest {
    @Test
    void normalPlanNeverChangesTerrainEvenAtExtremeCoordinates() {
        ExteriorPlan.DimensionEnvelope normal = ExteriorPlan.DimensionEnvelope.normal();

        assertEquals(ExteriorMode.NORMAL, normal.modeAt(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void voidUsesAnInclusiveOverflowSafeSquareBoundary() {
        ExteriorPlan.DimensionEnvelope envelope = new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, 512, 0);

        assertEquals(ExteriorMode.NORMAL, envelope.modeAt(512, -512));
        assertEquals(ExteriorMode.VOID, envelope.modeAt(513, 0));
        assertEquals(ExteriorMode.VOID, envelope.modeAt(Integer.MIN_VALUE, 0));
    }

    @Test
    void oceanTransitionBeginsInsideOuterBoundaryAndContinuesForever() {
        ExteriorPlan.DimensionEnvelope envelope = new ExteriorPlan.DimensionEnvelope(ExteriorMode.OCEAN, 512, 128);

        assertEquals(384, envelope.solidRadiusBlocks());
        assertEquals(ExteriorMode.NORMAL, envelope.modeAt(384, 0));
        assertEquals(ExteriorMode.OCEAN, envelope.modeAt(385, 0));
        assertEquals(ExteriorMode.OCEAN, envelope.modeAt(10_000, 10_000));
    }

    @Test
    void transitionIsClampedToTheResolvedBoundary() {
        ExteriorPlan.DimensionEnvelope envelope = new ExteriorPlan.DimensionEnvelope(ExteriorMode.OCEAN, 64, 128);

        assertEquals(64, envelope.oceanTransitionWidthBlocks());
        assertEquals(0, envelope.solidRadiusBlocks());
    }

    @Test
    void configAutoBoundaryUsesLargestScheduledBorderRadius() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.initialRadiusBlocks = 2048;
        config.overworldBorder.finalRadiusBlocks = 512;
        config.overworldExterior.mode = ExteriorMode.OCEAN;
        config.overworldExterior.oceanTransitionWidthBlocks = 256;

        ExteriorPlan plan = ExteriorPlan.fromConfig(config);

        assertEquals(2048, plan.overworld().boundaryRadiusBlocks());
        assertEquals(1792, plan.overworld().solidRadiusBlocks());
    }

    @Test
    void invalidPersistedEnvelopesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
            new ExteriorPlan.DimensionEnvelope(ExteriorMode.OCEAN, -1, 0));
    }
}
