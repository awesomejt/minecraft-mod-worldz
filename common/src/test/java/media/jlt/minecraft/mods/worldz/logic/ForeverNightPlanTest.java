package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.ForeverNightConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverNightPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        ForeverNightPlan disabled = ForeverNightPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(0, disabled.lockAfterDays());
        assertFalse(disabled.relaxInsomnia());
    }

    @Test
    void fromConfigIsDisabledWhenTheConfigItselfIsDisabled() {
        ForeverNightConfig config = new ForeverNightConfig();
        config.lockAfterDays = 5;
        config.relaxInsomnia = true;

        assertEquals(ForeverNightPlan.disabled(), ForeverNightPlan.fromConfig(config));
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        ForeverNightConfig config = new ForeverNightConfig();
        config.enabled = true;
        config.lockAfterDays = 5;
        config.relaxInsomnia = true;

        ForeverNightPlan plan = ForeverNightPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(5, plan.lockAfterDays());
        assertTrue(plan.relaxInsomnia());
    }

    @Test
    void negativeLockDelayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ForeverNightPlan(true, -1, false));
    }

    @Test
    void lockDelayTicksConvertsDaysToTicks() {
        assertEquals(0L, new ForeverNightPlan(true, 0, false).lockDelayTicks());
        assertEquals(BorderSchedule.TICKS_PER_DAY, new ForeverNightPlan(true, 1, false).lockDelayTicks());
        assertEquals(3 * BorderSchedule.TICKS_PER_DAY, new ForeverNightPlan(true, 3, false).lockDelayTicks());
    }
}
