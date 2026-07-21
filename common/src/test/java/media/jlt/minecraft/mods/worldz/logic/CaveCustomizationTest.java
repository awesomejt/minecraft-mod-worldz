package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveCustomizationTest {
    private static CaveCustomization create() {
        return new CaveCustomization(
            -32, false, 128, false, 48, 24, false, StarterKitTier.MEDIUM,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );
    }

    private static WorldzCustomization.BorderSettings defaultBorder() {
        return new WorldzCustomization.BorderSettings(false, 512, 512, 0, true);
    }

    @Test
    void fromConfigCopiesSanitizedDefaults() {
        WorldzConfig config = new WorldzConfig();

        CaveCustomization customization = CaveCustomization.fromConfig(config);

        assertEquals(config.cave.spawnDepthY, customization.spawnDepthY());
        assertEquals(config.cave.sealedSurface, customization.sealedSurface());
        assertEquals(config.cave.sealedSurfaceY, customization.sealedSurfaceY());
        assertEquals(config.cave.cavernEnabled, customization.cavernEnabled());
        assertEquals(config.cave.chestEnabled, customization.chestEnabled());
        assertEquals(config.cave.chestTier, customization.chestTier());
    }

    @Test
    void fromConfigCopiesBorderAndEndBorderSettings() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        CaveCustomization customization = CaveCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
    }

    @Test
    void fromTextParsesNumericFields() {
        CaveCustomization customization = CaveCustomization.fromText(
            "-40", true, "100", true, "64", "32", true, StarterKitTier.HARD,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals(-40, customization.spawnDepthY());
        assertTrue(customization.sealedSurface());
        assertEquals(100, customization.sealedSurfaceY());
        assertTrue(customization.cavernEnabled());
        assertEquals(64, customization.cavernRadiusBlocks());
        assertEquals(32, customization.cavernHeightBlocks());
        assertTrue(customization.chestEnabled());
        assertEquals(StarterKitTier.HARD, customization.chestTier());
    }

    @Test
    void fromTextRejectsNonNumericFields() {
        assertThrows(IllegalArgumentException.class, () -> CaveCustomization.fromText(
            "not-a-number", false, "128", false, "48", "24", false, StarterKitTier.MEDIUM,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void cavePlanResolvesAnEnabledPlan() {
        CavePlan plan = create().cavePlan();
        assertTrue(plan.enabled());
        assertEquals(-32, plan.spawnDepthY());
    }

    @Test
    void oceanExteriorIsRejectedForTheNether() {
        assertThrows(IllegalArgumentException.class, () -> new CaveCustomization(
            -32, false, 128, false, 48, 24, false, StarterKitTier.MEDIUM,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            new WorldzCustomization.ExteriorSettings(ExteriorMode.OCEAN, 512, 64)
        ));
    }

    @Test
    void exteriorPlanAlwaysKeepsTheOverworldNormal() {
        ExteriorPlan exterior = create().exteriorPlan();
        assertEquals(ExteriorMode.NORMAL, exterior.overworld().mode());
    }

    @Test
    void missingChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaveCustomization(
            -32, false, 128, false, 48, 24, false, null,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void missingBorderSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaveCustomization(
            -32, false, 128, false, 48, 24, false, StarterKitTier.MEDIUM,
            null, defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        ));
    }
}
