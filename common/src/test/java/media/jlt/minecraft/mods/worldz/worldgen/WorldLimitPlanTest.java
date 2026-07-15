package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLimitPlanTest {
    @Test
    void configIsSnapshottedWithoutSharingMutableBorderObjects() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.initialRadiusBlocks = 512;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.overworldBorder.resizeDays = 100;
        config.overworldBorder.resizeRateBlocks = 128;
        config.overworldBorder.resizeRateDays = 4;
        config.netherBorder.enabled = true;
        config.netherBorder.finalRadiusBlocks = 256;
        WorldLimitPlan plan = WorldLimitPlan.fromConfig(config);
        config.overworldBorder.finalRadiusBlocks = 9999;

        assertTrue(plan.enabled());
        assertEquals(2048, plan.overworld().finalRadiusBlocks());
        assertEquals(1_152_000L, plan.overworld().schedule().durationTicks());
        assertTrue(plan.overworld().schedule().usesRate());
        assertEquals(256, plan.nether().finalRadiusBlocks());
    }

    @Test
    void disabledPlanLeavesBothDimensionsUnlimited() {
        WorldLimitPlan plan = WorldLimitPlan.disabled();

        assertFalse(plan.enabled());
        assertFalse(plan.overworld().enabled());
        assertFalse(plan.nether().enabled());
    }

}
