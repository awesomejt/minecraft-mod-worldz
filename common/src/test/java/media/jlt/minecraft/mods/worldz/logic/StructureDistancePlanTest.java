package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StructureDistanceConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureDistancePlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        StructureDistancePlan disabled = StructureDistancePlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(2000, disabled.minDistanceBlocks());
        assertTrue(disabled.exemptStructureSets().isEmpty());
    }

    @Test
    void fromConfigIsDisabledWhenTheConfigItselfIsDisabled() {
        StructureDistanceConfig config = new StructureDistanceConfig();
        config.minDistanceBlocks = 3000;

        assertEquals(StructureDistancePlan.disabled(), StructureDistancePlan.fromConfig(config));
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        StructureDistanceConfig config = new StructureDistanceConfig();
        config.enabled = true;
        config.minDistanceBlocks = 3000;
        config.exemptStructureSets = List.of("minecraft:strongholds");

        StructureDistancePlan plan = StructureDistancePlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(3000, plan.minDistanceBlocks());
        assertEquals(List.of("minecraft:strongholds"), plan.exemptStructureSets());
    }

    @Test
    void negativeMinDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StructureDistancePlan(true, -1, List.of()));
    }

    @Test
    void isRestrictedRequiresEnabledNonZeroDistanceAndNonExemptSet() {
        StructureDistancePlan plan = new StructureDistancePlan(true, 2000, List.of("minecraft:strongholds"));

        assertTrue(plan.isRestricted("minecraft:villages", 0));
        assertTrue(plan.isRestricted("minecraft:villages", 1999));
        assertFalse(plan.isRestricted("minecraft:villages", 2000));
        assertFalse(plan.isRestricted("minecraft:villages", 5000));
        assertFalse(plan.isRestricted("minecraft:strongholds", 0));
    }

    @Test
    void isRestrictedIsAlwaysFalseWhenDisabled() {
        assertFalse(StructureDistancePlan.disabled().isRestricted("minecraft:villages", 0));
    }

    @Test
    void zeroMinDistanceDisablesTheRestrictionEvenWhenEnabled() {
        StructureDistancePlan plan = new StructureDistancePlan(true, 0, List.of());
        assertFalse(plan.isRestricted("minecraft:villages", 0));
    }
}
