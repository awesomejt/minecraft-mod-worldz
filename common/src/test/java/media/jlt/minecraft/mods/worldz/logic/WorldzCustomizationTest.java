package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldzCustomizationTest {
    @Test
    void configDefaultsAreCopiedIntoAnImmutableSnapshot() {
        WorldzConfig config = new WorldzConfig();
        config.allowedBiomes = new ArrayList<>(List.of("plains", "#is_overworld"));
        config.starterBiome = "desert";
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 1024;
        config.overworldBorder.resizeDelayDays = 8;
        config.overworldBorder.resizeRateBlocks = 64;
        config.overworldBorder.resizeRateDays = 2;
        config.overworldExterior.mode = ExteriorMode.OCEAN;
        config.overworldExterior.oceanTransitionWidthBlocks = 128;

        WorldzCustomization customization = WorldzCustomization.fromConfig(config);
        config.allowedBiomes.clear();

        assertEquals(List.of("minecraft:plains", "#minecraft:is_overworld"), customization.allowedBiomes());
        assertEquals("minecraft:desert", customization.starterBiome());
        assertTrue(customization.starterLandPlan().enabled());
        assertEquals(128, customization.starterLandPlan().transitionWidthBlocks());
        assertTrue(customization.overworldBorder().enabled());
        assertEquals(1024, customization.overworldBorder().finalRadiusBlocks());
        assertEquals(64, customization.overworldBorder().resizeRateBlocks());
        assertEquals(8, customization.overworldBorder().resizeDelayDays());
        assertEquals(ExteriorMode.OCEAN, customization.overworldExterior().mode());
        assertEquals(1024, customization.exteriorPlan().overworld().boundaryRadiusBlocks());
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, customization.spawnStrategy());
        assertThrows(UnsupportedOperationException.class, () -> customization.allowedBiomes().clear());
    }

    @Test
    void configSpawnStrategyIsCopiedIntoTheSnapshot() {
        WorldzConfig config = new WorldzConfig();
        config.spawn.strategy = SpawnStrategy.PREFERRED_NATURAL_BIOME;

        WorldzCustomization customization = WorldzCustomization.fromConfig(config);

        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, customization.spawnStrategy());
    }

    @Test
    void fromTextWithExplicitSpawnStrategyRoundTrips() {
        WorldzCustomization customization = WorldzCustomization.fromText(
            "plains", "", "512", StarterLandPlan.disabled(), border(false), border(false),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal(),
            WorldzCustomization.LayoutSettings.legacy(), SpawnStrategy.VANILLA_SPAWN
        );

        assertEquals(SpawnStrategy.VANILLA_SPAWN, customization.spawnStrategy());
    }

    @Test
    void spawnStrategyIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new WorldzCustomization(
            List.of("plains"), "", 512, StarterLandPlan.disabled(), border(false), border(false),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal(),
            WorldzCustomization.LayoutSettings.legacy(), null
        ));
    }

    @Test
    void editableBiomeTextAcceptsCommasNewlinesShortIdsAndTags() {
        WorldzCustomization customization = WorldzCustomization.fromText(
            " plains, minecraft:desert\n#is_overworld ",
            " cherry_grove ",
            "768",
            border(false),
            border(true)
        );

        assertEquals(
            List.of("minecraft:plains", "minecraft:desert", "#minecraft:is_overworld"),
            customization.allowedBiomes()
        );
        assertEquals("minecraft:cherry_grove", customization.starterBiome());
        assertEquals(768, customization.starterRadiusBlocks());
        assertEquals("minecraft:plains\nminecraft:desert\n#minecraft:is_overworld", customization.allowedBiomesText());
    }

    @Test
    void acceptsEmptyButRejectsMalformedAllowedBiomeLists() {
        assertTrue(WorldzCustomization.fromText(
            "  \n ", "", "512", border(false), border(false)
        ).allowedBiomes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "minecraft:has space", "", "512", border(false), border(false)
        ));
    }

    @Test
    void starterMustBeADirectBiomeId() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "#is_overworld", "512", border(false), border(false)
        ));
    }

    @Test
    void numericFieldsMustBeWholeNumbersWithinDocumentedRanges() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "", "63", border(false), border(false)
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "", "five", border(false), border(false)
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.BorderSettings.fromText(
            true, "512", "15000000", "0", true
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.BorderSettings.fromText(
            true, "512", "512", "-1", true
        ));
    }

    @Test
    void borderSettingsBecomeAnIndependentPersistedPlan() {
        WorldzCustomization customization = new WorldzCustomization(
            List.of("plains"),
            "",
            512,
            new WorldzCustomization.BorderSettings(true, 256, 2048, 100, true),
            new WorldzCustomization.BorderSettings(false, 512, 512, 0, false)
        );

        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertEquals(2048, customization.worldLimitPlan().overworld().finalRadiusBlocks());
        assertFalse(customization.worldLimitPlan().nether().enabled());
    }

    @Test
    void editableRatesAndExteriorValuesAreValidatedAndResolved() {
        WorldzCustomization.BorderSettings border = WorldzCustomization.BorderSettings.fromText(
            true, "512", "2048", "100", "128", "5", true
        );
        WorldzCustomization.ExteriorSettings exterior = WorldzCustomization.ExteriorSettings.fromText(
            "ocean", "auto", "256"
        );
        WorldzCustomization customization = WorldzCustomization.fromText(
            "plains", "", "512", border, border(false), exterior, WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals(128, customization.worldLimitPlan().overworld().resizeRateBlocks());
        assertEquals(2048, customization.exteriorPlan().overworld().boundaryRadiusBlocks());
        assertEquals(1792, customization.exteriorPlan().overworld().solidRadiusBlocks());
    }

    @Test
    void editableDelayIsPersistedIndependentlyFromDurationAndRate() {
        WorldzCustomization.BorderSettings border = WorldzCustomization.BorderSettings.fromText(
            true, "512", "2048", "100", "12", "128", "5", true
        );
        WorldzCustomization customization = new WorldzCustomization(
            List.of("plains"), "", 512, border, border(false)
        );

        assertEquals(12, border.resizeDelayDays());
        assertEquals(12, customization.worldLimitPlan().overworld().resizeDelayDays());
        assertEquals(100, customization.worldLimitPlan().overworld().resizeDays());
        assertEquals(128, customization.worldLimitPlan().overworld().resizeRateBlocks());
    }

    @Test
    void automaticExteriorNeedsAnEnabledBorderAndNetherRejectsOcean() {
        var automaticVoid = new WorldzCustomization.ExteriorSettings(ExteriorMode.VOID, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new WorldzCustomization(
            List.of("plains"), "", 512, border(false), border(false), automaticVoid,
            WorldzCustomization.ExteriorSettings.normal()
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorldzCustomization(
            List.of("plains"), "", 512, border(false), border(false),
            WorldzCustomization.ExteriorSettings.normal(),
            new WorldzCustomization.ExteriorSettings(ExteriorMode.OCEAN, 512, 128)
        ));
    }

    private static WorldzCustomization.BorderSettings border(boolean enabled) {
        return new WorldzCustomization.BorderSettings(enabled, 512, 512, 0, true);
    }

    @Test
    void layoutSettingsCanonicalizeWeightedBiomesAndRoleOverrides() {
        WorldzCustomization.LayoutSettings settings = WorldzCustomization.LayoutSettings.fromText(
            "ocean",
            "plains@3, desert",
            "300",
            " plains ",
            "swamp=ocean"
        );

        assertEquals(LayoutMode.OCEAN, settings.mode());
        assertEquals("minecraft:plains", settings.singleBiome());
        assertEquals("ocean", settings.roleOverrides().get("minecraft:swamp"));
        assertEquals(300, settings.regionScaleBlocks());
    }

    @Test
    void layoutSettingsRejectsTagsInWeightedBiomesAndOverrides() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "#is_overworld", "512", "", ""
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "", "512", "#is_overworld", ""
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "", "512", "", "#is_overworld=ocean"
        ));
    }

    @Test
    void layoutSettingsRejectsOutOfRangeAndMalformedFields() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "", "1", "", ""
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "", "not-a-number", "", ""
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.LayoutSettings.fromText(
            "ocean", "", "512", "", "malformed-no-equals"
        ));
    }

    @Test
    void legacyLayoutSettingsMatchWorldLayoutPlanDefaults() {
        WorldzCustomization.LayoutSettings legacy = WorldzCustomization.LayoutSettings.legacy();

        assertEquals(LayoutMode.LEGACY, legacy.mode());
        assertTrue(legacy.biomes().isEmpty());
        assertEquals(WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS, legacy.regionScaleBlocks());
    }

    @Test
    void worldLayoutPlanResolvesRoleOverridesAndUsesTheSuppliedSeed() {
        WorldzCustomization.LayoutSettings settings = WorldzCustomization.LayoutSettings.fromText(
            "ocean", "plains@3, desert, ocean, swamp", "300", "", "swamp=ocean"
        );
        WorldzCustomization customization = new WorldzCustomization(
            List.of("plains"), "", 512, StarterLandPlan.disabled(), border(false), border(false),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal(), settings,
            SpawnStrategy.STARTER_AT_ORIGIN
        );

        WorldLayoutPlan plan = customization.worldLayoutPlan(42L);

        assertEquals(42L, plan.seed());
        assertEquals(
            List.of("minecraft:ocean", "minecraft:swamp"),
            plan.oceanBiomes().stream().map(WorldLayoutPlan.BiomeWeight::biomeId).toList()
        );
    }
}
