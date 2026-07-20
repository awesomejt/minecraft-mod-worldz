package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.OceanIslandConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        IslandPlan disabled = IslandPlan.disabled();
        assertFalse(disabled.enabled());
        assertFalse(disabled.exclusionZoneEnabled());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.islandBiome = "minecraft:desert";
        config.radiusBlocks = 64;

        IslandPlan plan = IslandPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals("minecraft:desert", plan.islandBiome());
        assertEquals(64, plan.radiusBlocks());
    }

    @Test
    void invalidRadiusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(7, 0.3, "minecraft:plains", 12));
        assertThrows(IllegalArgumentException.class, () -> plan(100_000, 0.3, "minecraft:plains", 12));
    }

    @Test
    void invalidAmplitudeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(128, -0.1, "minecraft:plains", 12));
        assertThrows(IllegalArgumentException.class, () -> plan(128, 0.7, "minecraft:plains", 12));
    }

    @Test
    void invalidIslandBiomeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(128, 0.3, "", 12));
        assertThrows(IllegalArgumentException.class, () -> plan(128, 0.3, null, 12));
    }

    @Test
    void invalidShoreWidthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(128, 0.3, "minecraft:plains", 0));
        assertThrows(IllegalArgumentException.class, () -> plan(128, 0.3, "minecraft:plains", -1));
    }

    @Test
    void withinExclusionZoneIsAlwaysTrueWhenDisabled() {
        IslandPlan disabled = plan(128, 0.3, "minecraft:plains", 12);
        assertTrue(disabled.withinExclusionZone(1_000_000, 1_000_000));
    }

    @Test
    void withinExclusionZoneRespectsTheConfiguredRadiusWhenEnabled() {
        IslandPlan plan = new IslandPlan(
            true, 128, 0.3, "minecraft:plains", 12, 64, 128, 8, 32, 128, true, true,
            new IslandPlan.ExclusionZone(true, 2000), IslandFluid.WATER
        );
        assertTrue(plan.withinExclusionZone(2000, 0));
        assertTrue(plan.withinExclusionZone(-2000, 2000));
        assertFalse(plan.withinExclusionZone(2001, 0));
        assertFalse(plan.withinExclusionZone(0, -2001));
    }

    @Test
    void distanceFromShoreDelegatesToIslandShapeProfile() {
        IslandPlan plan = plan(128, 0.0, "minecraft:plains", 12);
        assertEquals(
            IslandShapeProfile.distanceFromShore(200, 0, 128, 0.0, 42L),
            plan.distanceFromShore(200, 0, 42L),
            0.000_001
        );
    }

    private static IslandPlan plan(int radiusBlocks, double shapeAmplitude, String islandBiome, int shoreWidthBlocks) {
        return new IslandPlan(
            true, radiusBlocks, shapeAmplitude, islandBiome, shoreWidthBlocks, 64, 128, 8, 32, 128, true, true,
            new IslandPlan.ExclusionZone(false, 2000), IslandFluid.WATER
        );
    }

    @Test
    void hasLandDefaultsTrueForFromConfig() {
        assertTrue(IslandPlan.fromConfig(new OceanIslandConfig()).hasLand());
    }

    @Test
    void syntheticLandDefaultsTrueForFromConfig() {
        assertTrue(IslandPlan.fromConfig(new OceanIslandConfig()).syntheticLand());
    }

    @Test
    void fromConfigWithoutLandResolvesWithHasLandFalse() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.shoreWidthBlocks = 5;

        IslandPlan plan = IslandPlan.fromConfigWithoutLand(config);

        assertTrue(plan.enabled());
        assertFalse(plan.hasLand());
        assertTrue(plan.syntheticLand());
        assertEquals(5, plan.shoreWidthBlocks());
    }

    @Test
    void fromConfigNaturalResolvesWithSyntheticLandFalse() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.radiusBlocks = 96;
        config.shoreWidthBlocks = 5;

        IslandPlan plan = IslandPlan.fromConfigNatural(config);

        assertTrue(plan.enabled());
        assertTrue(plan.hasLand());
        assertFalse(plan.syntheticLand());
        assertEquals(96, plan.radiusBlocks());
        assertEquals(5, plan.shoreWidthBlocks());
    }

    @Test
    void fluidDefaultsToWaterForFromConfig() {
        assertEquals(IslandFluid.WATER, IslandPlan.fromConfig(new OceanIslandConfig()).fluid());
    }

    @Test
    void fromConfigCarriesLavaFluidThrough() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.fluid = IslandFluid.LAVA;
        assertEquals(IslandFluid.LAVA, IslandPlan.fromConfig(config).fluid());
    }

    @Test
    void fromConfigWithoutLandCarriesFluidThrough() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.fluid = IslandFluid.NONE;
        assertEquals(IslandFluid.NONE, IslandPlan.fromConfigWithoutLand(config).fluid());
    }

    @Test
    void fromConfigNaturalCarriesFluidThrough() {
        OceanIslandConfig config = new OceanIslandConfig();
        config.fluid = IslandFluid.NONE;
        assertEquals(IslandFluid.NONE, IslandPlan.fromConfigNatural(config).fluid());
    }
}
