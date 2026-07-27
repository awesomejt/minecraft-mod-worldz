package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyIslandPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        SkyIslandPlan disabled = SkyIslandPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(SkyIslandPlan.DEFAULT_SURFACE_Y, disabled.surfaceY());
        assertEquals(SkyIslandPlan.DEFAULT_THICKNESS_BLOCKS, disabled.thicknessBlocks());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        SkyIslandConfig config = new SkyIslandConfig();
        config.islandBiome = "minecraft:desert";
        config.radiusBlocks = 32;
        config.surfaceY = 80;
        config.thicknessBlocks = 10;

        SkyIslandPlan plan = SkyIslandPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals("minecraft:desert", plan.islandBiome());
        assertEquals(32, plan.radiusBlocks());
        assertEquals(80, plan.surfaceY());
        assertEquals(10, plan.thicknessBlocks());
    }

    @Test
    void invalidRadiusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(7, 0.3, "minecraft:plains", 64, 6));
        assertThrows(IllegalArgumentException.class, () -> plan(100_000, 0.3, "minecraft:plains", 64, 6));
    }

    @Test
    void invalidAmplitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(16, -0.1, "minecraft:plains", 64, 6));
        assertThrows(IllegalArgumentException.class, () -> plan(16, 0.7, "minecraft:plains", 64, 6));
    }

    @Test
    void invalidIslandBiomeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(16, 0.3, "", 64, 6));
        assertThrows(IllegalArgumentException.class, () -> plan(16, 0.3, null, 64, 6));
    }

    @Test
    void invalidThicknessIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(16, 0.3, "minecraft:plains", 64, 0));
        assertThrows(IllegalArgumentException.class, () -> plan(16, 0.3, "minecraft:plains", 64, 65));
    }

    @Test
    void distanceFromShoreDelegatesToIslandShapeProfile() {
        SkyIslandPlan plan = plan(16, 0.0, "minecraft:plains", 64, 6);
        assertEquals(
            IslandShapeProfile.distanceFromShore(20, 0, 16, 0.0, 42L),
            plan.distanceFromShore(20, 0, 42L),
            0.000_001
        );
    }

    @Test
    void bottomYIsSurfaceYMinusThickness() {
        SkyIslandPlan plan = plan(16, 0.0, "minecraft:plains", 70, 8);
        assertEquals(62, plan.bottomY());
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SkyIslandPlan(
            true, 16, 0.3, "minecraft:plains", 64, 6, null, FloatingIslandsPlan.disabled()
        ));
    }

    @Test
    void fromConfigCarriesChestTierThrough() {
        SkyIslandConfig config = new SkyIslandConfig();
        config.chestTier = StarterKitTier.HARD;
        assertEquals(StarterKitTier.HARD, SkyIslandPlan.fromConfig(config).chestTier());
    }

    @Test
    void fromConfigCarriesBiomeExclusionZoneThrough() {
        SkyIslandConfig config = new SkyIslandConfig();
        config.exclusionZoneEnabled = false;
        config.exclusionZoneRadiusBlocks = 200;

        SkyIslandPlan plan = SkyIslandPlan.fromConfig(config);

        assertFalse(plan.biomeExclusionZone().enabled());
        assertEquals(200, plan.biomeExclusionZone().radiusBlocks());
    }

    @Test
    void legacyConstructorDefaultsBiomeExclusionZoneToTodaysConfigDefaults() {
        SkyIslandPlan plan = plan(16, 0.0, "minecraft:plains", 64, 6);
        assertTrue(plan.biomeExclusionZone().enabled());
        assertEquals(128, plan.biomeExclusionZone().radiusBlocks());
    }

    @Test
    void disabledPlanHasDisabledBiomeExclusionZone() {
        assertFalse(SkyIslandPlan.disabled().biomeExclusionZone().enabled());
    }

    @Test
    void withinBiomeExclusionZoneRespectsEnabledAndRadius() {
        SkyIslandPlan enabled = new SkyIslandPlan(
            true, 16, 0.0, "minecraft:desert", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(true, 128)
        );
        assertTrue(enabled.withinBiomeExclusionZone(50.0));
        assertTrue(enabled.withinBiomeExclusionZone(128.0));
        assertFalse(enabled.withinBiomeExclusionZone(128.1));

        SkyIslandPlan disabled = new SkyIslandPlan(
            true, 16, 0.0, "minecraft:desert", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(false, 128)
        );
        assertFalse(disabled.withinBiomeExclusionZone(1.0));
    }

    @Test
    void legacyConstructorsDefaultUndergroundBandToDisabled() {
        SkyIslandPlan plan = plan(16, 0.0, "minecraft:plains", 64, 6);
        assertFalse(plan.undergroundEnabled());
        assertEquals("", plan.undergroundBiome());
    }

    @Test
    void disabledPlanHasUndergroundBandDisabled() {
        assertFalse(SkyIslandPlan.disabled().undergroundEnabled());
    }

    @Test
    void negativeUndergroundBelowSurfaceBlocksIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SkyIslandPlan(
            true, 16, 0.3, "minecraft:plains", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(true, 128), "minecraft:dripstone_caves", -1
        ));
    }

    @Test
    void undergroundEnabledRequiresBothABiomeAndAPositiveThreshold() {
        SkyIslandPlan noBiome = new SkyIslandPlan(
            true, 16, 0.3, "minecraft:plains", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(true, 128), "", 10
        );
        assertFalse(noBiome.undergroundEnabled());

        SkyIslandPlan zeroBlocks = new SkyIslandPlan(
            true, 16, 0.3, "minecraft:plains", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(true, 128), "minecraft:dripstone_caves", 0
        );
        assertFalse(zeroBlocks.undergroundEnabled());

        SkyIslandPlan enabled = new SkyIslandPlan(
            true, 16, 0.3, "minecraft:plains", 64, 6, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled(),
            new IslandPlan.ExclusionZone(true, 128), "minecraft:dripstone_caves", 10
        );
        assertTrue(enabled.undergroundEnabled());
    }

    @Test
    void fromConfigResolvesUndergroundFields() {
        SkyIslandConfig config = new SkyIslandConfig();
        config.undergroundBiome = "minecraft:lush_caves";
        config.undergroundBelowSurfaceBlocks = 15;

        SkyIslandPlan plan = SkyIslandPlan.fromConfig(config);

        assertEquals("minecraft:lush_caves", plan.undergroundBiome());
        assertEquals(15, plan.undergroundBelowSurfaceBlocks());
        assertTrue(plan.undergroundEnabled());
    }

    private static SkyIslandPlan plan(int radiusBlocks, double shapeAmplitude, String islandBiome, int surfaceY, int thicknessBlocks) {
        return new SkyIslandPlan(
            true, radiusBlocks, shapeAmplitude, islandBiome, surfaceY, thicknessBlocks, StarterKitTier.MEDIUM, FloatingIslandsPlan.disabled()
        );
    }
}
