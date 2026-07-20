package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyIslandCustomizationTest {
    private static SkyIslandCustomization create(String islandBiome, int radiusBlocks, double shapeAmplitude, int surfaceY, int thicknessBlocks) {
        return new SkyIslandCustomization(
            islandBiome, radiusBlocks, shapeAmplitude, surfaceY, thicknessBlocks, StarterKitTier.MEDIUM, false,
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

        SkyIslandCustomization customization = SkyIslandCustomization.fromConfig(config);

        assertEquals(config.skyIsland.islandBiome, customization.islandBiome());
        assertEquals(config.skyIsland.radiusBlocks, customization.radiusBlocks());
        assertEquals(config.skyIsland.shapeAmplitude, customization.shapeAmplitude());
        assertEquals(config.skyIsland.surfaceY, customization.surfaceY());
        assertEquals(config.skyIsland.thicknessBlocks, customization.thicknessBlocks());
        assertEquals(config.skyIsland.chestTier, customization.chestTier());
    }

    @Test
    void fromConfigCopiesBorderAndEndBorderSettings() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        SkyIslandCustomization customization = SkyIslandCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertTrue(customization.worldLimitPlan().end().carryFromOverworld());
    }

    @Test
    void constructorRejectsRadiusOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 7, 0.3, 64, 6));
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 100_000, 0.3, 64, 6));
    }

    @Test
    void constructorRejectsAmplitudeOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 16, -0.1, 64, 6));
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 16, 0.7, 64, 6));
    }

    @Test
    void constructorRejectsThicknessOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 16, 0.3, 64, 0));
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 16, 0.3, 64, 65));
    }

    @Test
    void constructorRejectsIslandBiomeTag() {
        assertThrows(IllegalArgumentException.class, () -> create("#minecraft:is_overworld", 16, 0.3, 64, 6));
    }

    @Test
    void skyIslandPlanIsAlwaysEnabled() {
        SkyIslandCustomization customization = create("minecraft:desert", 32, 0.3, 80, 10);
        SkyIslandPlan plan = customization.skyIslandPlan();

        assertTrue(plan.enabled());
        assertEquals("minecraft:desert", plan.islandBiome());
        assertEquals(32, plan.radiusBlocks());
        assertEquals(0.3, plan.shapeAmplitude());
        assertEquals(80, plan.surfaceY());
        assertEquals(10, plan.thicknessBlocks());
    }

    @Test
    void exteriorPlanAlwaysKeepsTheOverworldNormal() {
        SkyIslandCustomization customization = create("minecraft:plains", 16, 0.3, 64, 6);
        assertEquals(ExteriorMode.NORMAL, customization.exteriorPlan().overworld().mode());
    }

    @Test
    void fromTextParsesDecimalAndDoubleFields() {
        SkyIslandCustomization customization = SkyIslandCustomization.fromText(
            "minecraft:desert", "256", "0.4", "72", "8", StarterKitTier.HARD, true,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals("minecraft:desert", customization.islandBiome());
        assertEquals(256, customization.radiusBlocks());
        assertEquals(0.4, customization.shapeAmplitude());
        assertEquals(72, customization.surfaceY());
        assertEquals(8, customization.thicknessBlocks());
        assertEquals(StarterKitTier.HARD, customization.chestTier());
        assertTrue(customization.applyToNether());
    }

    @Test
    void fromTextRejectsNonNumericRadius() {
        assertThrows(IllegalArgumentException.class, () -> SkyIslandCustomization.fromText(
            "minecraft:plains", "not-a-number", "0.3", "64", "6", StarterKitTier.MEDIUM, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void netherSkyIslandPlanDisabledUnlessApplyToNether() {
        SkyIslandCustomization off = new SkyIslandCustomization(
            "minecraft:plains", 16, 0.3, 64, 6, StarterKitTier.MEDIUM, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );
        SkyIslandCustomization on = new SkyIslandCustomization(
            "minecraft:plains", 16, 0.3, 64, 6, StarterKitTier.MEDIUM, true,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals(SkyIslandPlan.disabled(), off.netherSkyIslandPlan());
        assertTrue(on.netherSkyIslandPlan().enabled());
        assertEquals(16, on.netherSkyIslandPlan().radiusBlocks());
    }
}
