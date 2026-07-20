package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanIslandCustomizationTest {
    private static OceanIslandCustomization create(String islandBiome, int radiusBlocks, double shapeAmplitude, int shoreWidthBlocks) {
        return create(IslandSource.ARTIFICIAL, islandBiome, radiusBlocks, shapeAmplitude, shoreWidthBlocks);
    }

    private static OceanIslandCustomization create(
        IslandSource islandSource, String islandBiome, int radiusBlocks, double shapeAmplitude, int shoreWidthBlocks
    ) {
        return new OceanIslandCustomization(
            islandSource, islandBiome, radiusBlocks, shapeAmplitude, shoreWidthBlocks, 64, 128, 8, 32, 128, false, 2000,
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

        OceanIslandCustomization customization = OceanIslandCustomization.fromConfig(config);

        assertEquals(config.oceanIsland.islandBiome, customization.islandBiome());
        assertEquals(config.oceanIsland.radiusBlocks, customization.radiusBlocks());
        assertEquals(config.oceanIsland.shapeAmplitude, customization.shapeAmplitude());
        assertFalse(customization.exclusionZoneEnabled());
    }

    @Test
    void fromConfigCopiesBorderAndEndBorderSettings() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        OceanIslandCustomization customization = OceanIslandCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertTrue(customization.worldLimitPlan().end().carryFromOverworld());
    }

    @Test
    void constructorRejectsRadiusOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 7, 0.3, 12));
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 100_000, 0.3, 12));
    }

    @Test
    void constructorRejectsAmplitudeOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 128, -0.1, 12));
        assertThrows(IllegalArgumentException.class, () -> create("minecraft:plains", 128, 0.7, 12));
    }

    @Test
    void constructorRejectsIslandBiomeTag() {
        assertThrows(IllegalArgumentException.class, () -> create("#minecraft:is_overworld", 128, 0.3, 12));
    }

    @Test
    void islandPlanIsAlwaysEnabled() {
        OceanIslandCustomization customization = create("minecraft:desert", 128, 0.3, 12);
        IslandPlan plan = customization.islandPlan();

        assertTrue(plan.enabled());
        assertEquals("minecraft:desert", plan.islandBiome());
        assertEquals(128, plan.radiusBlocks());
        assertEquals(0.3, plan.shapeAmplitude());
        assertEquals(12, plan.shoreWidthBlocks());
    }

    @Test
    void exteriorPlanAlwaysKeepsTheOverworldNormal() {
        OceanIslandCustomization customization = create("minecraft:plains", 128, 0.3, 12);
        assertEquals(ExteriorMode.NORMAL, customization.exteriorPlan().overworld().mode());
    }

    @Test
    void fromTextParsesDecimalAndDoubleFields() {
        OceanIslandCustomization customization = OceanIslandCustomization.fromText(
            IslandSource.ARTIFICIAL, "minecraft:desert", "256", "0.4", "16", "64", "128", "8", "32", "128", true, "1500",
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals("minecraft:desert", customization.islandBiome());
        assertEquals(256, customization.radiusBlocks());
        assertEquals(0.4, customization.shapeAmplitude());
        assertEquals(16, customization.shoreWidthBlocks());
        assertTrue(customization.exclusionZoneEnabled());
        assertEquals(1500, customization.exclusionZoneRadiusBlocks());
    }

    @Test
    void fromTextRejectsNonNumericRadius() {
        assertThrows(IllegalArgumentException.class, () -> OceanIslandCustomization.fromText(
            IslandSource.ARTIFICIAL, "minecraft:plains", "not-a-number", "0.3", "12", "64", "128", "8", "32", "128", false, "2000",
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void islandPlanHasLandForArtificialSource() {
        OceanIslandCustomization customization = create(IslandSource.ARTIFICIAL, "minecraft:desert", 128, 0.3, 12);
        assertTrue(customization.islandPlan().hasLand());
    }

    @Test
    void islandPlanHasNoLandForChestBoatSource() {
        OceanIslandCustomization customization = create(IslandSource.CHEST_BOAT, "minecraft:desert", 128, 0.3, 12);
        IslandPlan plan = customization.islandPlan();

        assertTrue(plan.enabled());
        assertFalse(plan.hasLand());
    }
}
