package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripWorldCustomizationTest {
    private static StripWorldCustomization create(int widthRadiusBlocks, ExteriorMode widthMode, boolean applyToNether) {
        return new StripWorldCustomization(
            widthRadiusBlocks, widthMode, applyToNether, false, List.of(), 128, false, false, false, false,
            SpawnStrategy.STARTER_AT_ORIGIN,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );
    }

    private static StripWorldCustomization createWithBands(boolean bandsEnabled, List<String> bandBiomes, int bandWidthBlocks) {
        return new StripWorldCustomization(
            32, ExteriorMode.VOID, false, bandsEnabled, bandBiomes, bandWidthBlocks, false, false, false, false,
            SpawnStrategy.STARTER_AT_ORIGIN,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );
    }

    private static WorldzCustomization.BorderSettings defaultBorder() {
        return new WorldzCustomization.BorderSettings(false, 512, 512, 0, true);
    }

    @Test
    void fromConfigCopiesSanitizedDefaults() {
        WorldzConfig config = new WorldzConfig();

        StripWorldCustomization customization = StripWorldCustomization.fromConfig(config);

        assertEquals(config.strip.widthRadiusBlocks, customization.widthRadiusBlocks());
        assertEquals(ExteriorMode.VOID, customization.widthMode());
        assertFalse(customization.applyToNether());
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, customization.spawnStrategy());
    }

    @Test
    void fromConfigCopiesBorderExteriorAndEndBorderSettings() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        StripWorldCustomization customization = StripWorldCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertTrue(customization.worldLimitPlan().end().carryFromOverworld());
    }

    @Test
    void constructorRejectsNonPositiveWidthRadius() {
        assertThrows(IllegalArgumentException.class, () -> create(0, ExteriorMode.VOID, false));
        assertThrows(IllegalArgumentException.class, () -> create(-1, ExteriorMode.VOID, false));
    }

    @Test
    void constructorRejectsNormalWidthMode() {
        assertThrows(IllegalArgumentException.class, () -> create(32, ExteriorMode.NORMAL, false));
    }

    @Test
    void stripPlanAlwaysAppliesToTheOverworld() {
        StripWorldCustomization customization = create(32, ExteriorMode.VOID, false);

        assertTrue(customization.stripPlan(true).enabled());
        assertEquals(32, customization.stripPlan(true).widthRadiusBlocks());
        assertEquals(ExteriorMode.VOID, customization.stripPlan(true).widthMode());
    }

    @Test
    void stripPlanOnlyAppliesToTheNetherWhenApplyToNetherIsSet() {
        StripWorldCustomization disabled = create(32, ExteriorMode.VOID, false);
        assertFalse(disabled.stripPlan(false).enabled());

        StripWorldCustomization enabled = create(32, ExteriorMode.VOID, true);
        assertTrue(enabled.stripPlan(false).enabled());
        assertEquals(32, enabled.stripPlan(false).widthRadiusBlocks());
    }

    @Test
    void fromTextParsesDecimalWidthAndMode() {
        StripWorldCustomization customization = StripWorldCustomization.fromText(
            "48", "ocean", true, false, "", "128", false, false, false, false,
            SpawnStrategy.PREFERRED_NATURAL_BIOME,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals(48, customization.widthRadiusBlocks());
        assertEquals(ExteriorMode.OCEAN, customization.widthMode());
        assertTrue(customization.applyToNether());
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, customization.spawnStrategy());
    }

    @Test
    void fromTextRejectsNonNumericWidth() {
        assertThrows(IllegalArgumentException.class, () -> StripWorldCustomization.fromText(
            "not-a-number", "void", false, false, "", "128", false, false, false, false,
            SpawnStrategy.STARTER_AT_ORIGIN,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void fromTextParsesBandFields() {
        StripWorldCustomization customization = StripWorldCustomization.fromText(
            "48", "void", false, true, "minecraft:desert\nminecraft:jungle", "256", true, true, true, true,
            SpawnStrategy.STARTER_AT_ORIGIN,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );

        assertTrue(customization.bandsEnabled());
        assertEquals(List.of("minecraft:desert", "minecraft:jungle"), customization.bandBiomes());
        assertEquals(256, customization.bandWidthBlocks());
        assertTrue(customization.bandSeedRandomOrder());
        assertTrue(customization.bandAllowRivers());
        assertTrue(customization.bandAllowOceans());
        assertTrue(customization.bandAllowBeaches());
    }

    @Test
    void fromConfigCopiesBandPassThroughDefaults() {
        WorldzConfig config = new WorldzConfig();

        StripWorldCustomization customization = StripWorldCustomization.fromConfig(config);

        assertTrue(customization.bandAllowRivers());
        assertTrue(customization.bandAllowOceans());
        assertTrue(customization.bandAllowBeaches());
    }

    @Test
    void constructorAllowsBandsDisabledWithNoBiomes() {
        StripWorldCustomization customization = createWithBands(false, List.of(), 128);
        assertFalse(customization.bandsEnabled());
        assertTrue(customization.bandBiomes().isEmpty());
    }

    @Test
    void constructorRejectsEnabledBandsWithNoBiomes() {
        assertThrows(IllegalArgumentException.class, () -> createWithBands(true, List.of(), 128));
    }

    @Test
    void constructorRejectsBandTags() {
        assertThrows(
            IllegalArgumentException.class,
            () -> createWithBands(true, List.of("#minecraft:is_overworld"), 128)
        );
    }

    @Test
    void constructorRejectsBandWidthOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> createWithBands(true, List.of("minecraft:desert"), 4));
    }

    @Test
    void layoutPlanIsLegacyWhenBandsAreDisabled() {
        StripWorldCustomization customization = createWithBands(false, List.of(), 128);
        assertEquals(LayoutMode.LEGACY, customization.layoutPlan(1L).mode());
    }

    @Test
    void layoutPlanResolvesBandsWhenEnabled() {
        StripWorldCustomization customization = createWithBands(true, List.of("minecraft:desert", "minecraft:jungle"), 256);
        WorldLayoutPlan plan = customization.layoutPlan(1L);
        assertEquals(LayoutMode.STRIP_BANDS, plan.mode());
        assertEquals(List.of("minecraft:desert", "minecraft:jungle"), plan.bandBiomes());
        assertEquals(256, plan.regionScaleBlocks());
    }
}
