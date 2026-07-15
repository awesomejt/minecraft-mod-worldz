package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterLandPlanTest {
    @Test
    void configValuesAreSnapshotted() {
        WorldzConfig config = new WorldzConfig();
        config.ensureStarterLand = true;
        config.starterLandTransitionBlocks = 96;
        config.starterLandFoundationDepthBlocks = 48;

        StarterLandPlan plan = StarterLandPlan.fromConfig(config);
        config.starterLandTransitionBlocks = 5;

        assertTrue(plan.enabled());
        assertEquals(96, plan.transitionWidthBlocks());
        assertEquals(48, plan.foundationDepthBlocks());
    }

    @Test
    void disabledCompatibilityPlanAndRangesAreStable() {
        assertFalse(StarterLandPlan.disabled().enabled());
        assertThrows(IllegalArgumentException.class, () -> new StarterLandPlan(true, -1, 32));
        assertThrows(IllegalArgumentException.class, () -> new StarterLandPlan(true, 128, 385));
    }

    @Test
    void editableValuesRequireWholeNumbersInRange() {
        assertEquals(new StarterLandPlan(true, 256, 64), StarterLandPlan.fromText(true, " 256 ", "64"));
        assertThrows(IllegalArgumentException.class, () -> StarterLandPlan.fromText(true, "wide", "32"));
        assertThrows(IllegalArgumentException.class, () -> StarterLandPlan.fromText(true, "128", "999"));
    }
}
