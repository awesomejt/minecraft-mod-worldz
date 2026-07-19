package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StripConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripPlanTest {
    @Test
    void disabledPlanNeverClassifiesAnyColumn() {
        assertEquals(ExteriorMode.NORMAL, StripPlan.disabled().modeAt(0));
        assertEquals(ExteriorMode.NORMAL, StripPlan.disabled().modeAt(999_999));
    }

    @Test
    void enabledPlanClassifiesOnlyBeyondTheWidthRadius() {
        StripPlan strip = new StripPlan(true, 32, ExteriorMode.VOID);

        assertEquals(ExteriorMode.NORMAL, strip.modeAt(0));
        assertEquals(ExteriorMode.NORMAL, strip.modeAt(32));
        assertEquals(ExteriorMode.NORMAL, strip.modeAt(-32));
        assertEquals(ExteriorMode.VOID, strip.modeAt(33));
        assertEquals(ExteriorMode.VOID, strip.modeAt(-33));
    }

    @Test
    void invalidPlansAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 0, ExteriorMode.VOID));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, -1, ExteriorMode.VOID));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 32, ExteriorMode.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 32, null));
    }

    @Test
    void fromConfigResolvesTheOverworldWheneverEnabled() {
        StripConfig config = new StripConfig();
        config.enabled = true;
        config.widthRadiusBlocks = 48;
        config.widthMode = ExteriorMode.OCEAN;
        config.applyToNether = false;

        StripPlan overworld = StripPlan.fromConfig(config, true);
        assertEquals(true, overworld.enabled());
        assertEquals(48, overworld.widthRadiusBlocks());
        assertEquals(ExteriorMode.OCEAN, overworld.widthMode());
    }

    @Test
    void fromConfigOnlyResolvesTheNetherWhenApplyToNetherIsSet() {
        StripConfig config = new StripConfig();
        config.enabled = true;

        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, false));

        config.applyToNether = true;
        assertEquals(true, StripPlan.fromConfig(config, false).enabled());
    }

    @Test
    void fromConfigIsDisabledWhenTheConfigItselfIsDisabled() {
        StripConfig config = new StripConfig();
        config.applyToNether = true;

        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, true));
        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, false));
    }
}
