package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripPlanTest {
    @Test
    void disabledPlanNeverClassifiesAnyColumn() {
        assertEquals(ExteriorMode.NORMAL, StripPlan.disabled().modeAt(Integer.MIN_VALUE));
        assertEquals(ExteriorMode.NORMAL, StripPlan.disabled().modeAt(Integer.MAX_VALUE));
    }

    @Test
    void absoluteWidthsHaveTheRequiredOddAndEvenRanges() {
        assertRange(1, 0, 0);
        assertRange(2, 0, 1);
        assertRange(3, -1, 1);
        assertRange(4, -1, 2);
        assertRange(16, -7, 8);
    }

    @Test
    void invalidPlansAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 0, ExteriorMode.VOID));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, -1, ExteriorMode.VOID));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 32, ExteriorMode.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> new StripPlan(true, 32, null));
    }

    @Test
    void fromConfigResolvesTheGenericOverworldWheneverEnabled() {
        StripWorldConfig config = new StripWorldConfig();
        config.enabled = true;
        config.width = 48;
        config.widthMode = ExteriorMode.OCEAN;

        StripPlan overworld = StripPlan.fromConfig(config, true);
        assertEquals(true, overworld.enabled());
        assertEquals(48, overworld.width());
        assertEquals(ExteriorMode.OCEAN, overworld.widthMode());
    }

    @Test
    void fromConfigOnlyResolvesTheNetherWhenApplyToNetherIsSet() {
        StripWorldConfig config = new StripWorldConfig();
        config.enabled = true;

        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, false));

        config.applyToNether = true;
        assertEquals(true, StripPlan.fromConfig(config, false).enabled());
    }

    @Test
    void fromConfigIsDisabledWhenTheGenericOptInIsDisabled() {
        StripWorldConfig config = new StripWorldConfig();
        config.applyToNether = true;

        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, true));
        assertEquals(StripPlan.disabled(), StripPlan.fromConfig(config, false));
    }

    @Test
    void dedicatedPresetAlwaysEnablesOverworldButStillHonorsNetherToggle() {
        StripWorldConfig config = new StripWorldConfig();
        assertEquals(true, StripPlan.fromDedicatedConfig(config, true).enabled());
        assertEquals(65, StripPlan.fromDedicatedConfig(config, true).width());
        assertEquals(StripPlan.disabled(), StripPlan.fromDedicatedConfig(config, false));

        config.applyToNether = true;
        assertEquals(true, StripPlan.fromDedicatedConfig(config, false).enabled());
    }

    @Test
    void fieldlessPresetResolutionIsExplicitlyIsolatedByWorldType() {
        StripWorldConfig config = new StripWorldConfig();
        config.enabled = true;
        config.applyToNether = true;
        config.width = 7;

        assertEquals(new StripPlan(true, 7, ExteriorMode.VOID), StripPlan.fromPresetConfig(config, true, "worldz"));
        assertEquals(new StripPlan(true, 7, ExteriorMode.VOID), StripPlan.fromPresetConfig(config, false, "worldz"));
        assertEquals(new StripPlan(true, 7, ExteriorMode.VOID), StripPlan.fromPresetConfig(config, true, "strip_world"));
        assertEquals(new StripPlan(true, 7, ExteriorMode.VOID), StripPlan.fromPresetConfig(config, false, "strip_world"));

        for (String unrelated : new String[] {"single_biome", "chaos_biomes", "ocean_island", "cave", ""}) {
            assertEquals(StripPlan.disabled(), StripPlan.fromPresetConfig(config, true, unrelated), unrelated);
            assertEquals(StripPlan.disabled(), StripPlan.fromPresetConfig(config, false, unrelated), unrelated);
        }
    }

    private static void assertRange(int width, int minZ, int maxZ) {
        StripPlan strip = new StripPlan(true, width, ExteriorMode.VOID);
        assertEquals(minZ, strip.minZ());
        assertEquals(maxZ, strip.maxZ());
        assertEquals(ExteriorMode.VOID, strip.modeAt(minZ - 1));
        assertEquals(ExteriorMode.NORMAL, strip.modeAt(minZ));
        assertEquals(ExteriorMode.NORMAL, strip.modeAt(maxZ));
        assertEquals(ExteriorMode.VOID, strip.modeAt(maxZ + 1));
    }
}
