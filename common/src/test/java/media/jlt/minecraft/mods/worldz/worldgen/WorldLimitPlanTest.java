package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLimitPlanTest {
    @Test
    void configIsSnapshottedWithoutSharingMutableBorderObjects() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.initialRadiusBlocks = 512;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.overworldBorder.resizeDays = 100;
        config.overworldBorder.resizeDelayDays = 9;
        config.overworldBorder.resizeRateBlocks = 128;
        config.overworldBorder.resizeRateDays = 4;
        config.overworldBorder.resizeStyle = ResizeStyle.STEPPED;
        config.netherBorder.enabled = true;
        config.netherBorder.finalRadiusBlocks = 256;
        WorldLimitPlan plan = WorldLimitPlan.fromConfig(config);
        config.overworldBorder.finalRadiusBlocks = 9999;

        assertTrue(plan.enabled());
        assertEquals(2048, plan.overworld().finalRadiusBlocks());
        assertEquals(1_152_000L, plan.overworld().schedule().durationTicks());
        assertEquals(216_000L, plan.overworld().schedule().delayTicks());
        assertTrue(plan.overworld().schedule().usesRate());
        assertEquals(ResizeStyle.STEPPED, plan.overworld().resizeStyle());
        assertEquals(ResizeStyle.STEPPED, plan.overworld().schedule().style());
        assertEquals(256, plan.nether().finalRadiusBlocks());
        assertEquals(ResizeStyle.CONTINUOUS, plan.nether().resizeStyle());
    }

    @Test
    void disabledPlanLeavesBothDimensionsUnlimited() {
        WorldLimitPlan plan = WorldLimitPlan.disabled();

        assertFalse(plan.enabled());
        assertFalse(plan.overworld().enabled());
        assertFalse(plan.nether().enabled());
    }

    @Test
    void endLimitCarriesTheOverworldsFinalRadiusWhenLargerThanTheFloor() {
        WorldLimitPlan.EndLimit end = new WorldLimitPlan.EndLimit(true, 256);
        WorldLimitPlan.DimensionLimit overworld = new WorldLimitPlan.DimensionLimit(true, 512, 2048, 0, true);

        assertEquals(OptionalInt.of(2048), end.resolveRadiusBlocks(overworld));
    }

    @Test
    void endLimitFloorsAVerySmallCarriedRadiusSoTheDragonFightStaysWinnable() {
        WorldLimitPlan.EndLimit end = new WorldLimitPlan.EndLimit(true, 256);
        WorldLimitPlan.DimensionLimit overworld = new WorldLimitPlan.DimensionLimit(true, 64, 64, 0, true);

        assertEquals(OptionalInt.of(256), end.resolveRadiusBlocks(overworld));
    }

    @Test
    void endLimitIsEmptyWhenNotCarrying() {
        WorldLimitPlan.EndLimit end = new WorldLimitPlan.EndLimit(false, 256);
        WorldLimitPlan.DimensionLimit overworld = new WorldLimitPlan.DimensionLimit(true, 512, 2048, 0, true);

        assertEquals(OptionalInt.empty(), end.resolveRadiusBlocks(overworld));
    }

    @Test
    void endLimitIsEmptyWhenTheOverworldHasNothingToCarry() {
        WorldLimitPlan.EndLimit end = new WorldLimitPlan.EndLimit(true, 256);

        assertEquals(OptionalInt.empty(), end.resolveRadiusBlocks(WorldLimitPlan.DimensionLimit.disabled()));
    }

    @Test
    void endConfigIsSnapshottedIntoThePlan() {
        WorldzConfig config = new WorldzConfig();
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        WorldLimitPlan plan = WorldLimitPlan.fromConfig(config);
        config.endBorder.minimumRadiusBlocks = 9999;

        assertTrue(plan.end().carryFromOverworld());
        assertEquals(320, plan.end().minimumRadiusBlocks());
    }

    @Test
    void safeSpawnOffsetIsThePreferredValueWhenNoBorderIsEnabled() {
        assertEquals(8, WorldLimitPlan.DimensionLimit.disabled().safeSpawnOffsetBlocks());
    }

    @Test
    void safeSpawnOffsetIsThePreferredValueWhenTheBorderIsComfortablyLarge() {
        WorldLimitPlan.DimensionLimit limit = new WorldLimitPlan.DimensionLimit(true, 64, 64, 0, false);

        assertEquals(8, limit.safeSpawnOffsetBlocks());
    }

    @Test
    void safeSpawnOffsetShrinksToStayInsideATinyBorder() {
        WorldLimitPlan.DimensionLimit limit = new WorldLimitPlan.DimensionLimit(true, 2, 2, 0, false);

        assertEquals(1, limit.safeSpawnOffsetBlocks());
    }

    @Test
    void safeSpawnOffsetNeverGoesNegativeAtTheSmallestPossibleBorder() {
        WorldLimitPlan.DimensionLimit limit = new WorldLimitPlan.DimensionLimit(true, 1, 1, 0, false);

        assertEquals(0, limit.safeSpawnOffsetBlocks());
    }

    @Test
    void legacyConstructorsDefaultToContinuousResizeStyle() {
        assertEquals(ResizeStyle.CONTINUOUS, new WorldLimitPlan.DimensionLimit(true, 512, 2048, 0, true).resizeStyle());
        assertEquals(ResizeStyle.CONTINUOUS, WorldLimitPlan.DimensionLimit.disabled().resizeStyle());
    }

    @Test
    void steppedDimensionLimitWithoutARateIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new WorldLimitPlan.DimensionLimit(true, 8, 1024, 0, 0, 0, 0, true, ResizeStyle.STEPPED)
        );
    }

}
