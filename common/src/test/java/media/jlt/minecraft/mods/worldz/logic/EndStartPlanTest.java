package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndStartPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        EndStartPlan disabled = EndStartPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(StarterKitTier.MEDIUM, disabled.chestTier());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        EndStartConfig config = new EndStartConfig();
        config.chestTier = StarterKitTier.HARD;

        EndStartPlan plan = EndStartPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(StarterKitTier.HARD, plan.chestTier());
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EndStartPlan(true, null));
    }
}
