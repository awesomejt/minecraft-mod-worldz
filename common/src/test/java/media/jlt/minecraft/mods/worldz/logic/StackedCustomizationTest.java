package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackedCustomizationTest {
    private static WorldzCustomization.BorderSettings defaultBorder() {
        return new WorldzCustomization.BorderSettings(false, 512, 512, 0, true);
    }

    @Test
    void fromConfigDerivesAVoidExteriorFromWorldSizeChunksByDefaultWithNoBorder() {
        WorldzConfig config = new WorldzConfig();

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertFalse(customization.overworldBorder().enabled());
        assertEquals(ExteriorMode.VOID, customization.overworldExterior().mode());
        assertEquals(64, customization.overworldExterior().boundaryRadiusBlocks());
    }

    @Test
    void fromConfigStillHonorsAnExplicitlyEnabledSharedBorderAlongsideTheDefaultVoidExteriorWhenFieldsAreSetDirectly() {
        // Direct field assignment (not WorldzConfig.parse) never marks anything present (TODO
        // 25.3/25.5), so this reproduces the pre-25.5 value-only behavior on purpose: worldSizeChunks
        // alone still decides, exactly like every other test in this class that builds a config by
        // hand rather than parsing real YAML. Presence-driven coverage (an explicitly *parsed*
        // overworldBorder/overworldExterior winning over the derived void wall, TODO 25.5's own
        // fix) lives in config.StackedConfigTest instead, since WorldzConfig.parse/sanitize are
        // package-private to config.
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertEquals(ExteriorMode.VOID, customization.overworldExterior().mode());
    }

    @Test
    void fromConfigForwardsTheSharedGlobalBorderWhenWorldSizeChunksIsZero() {
        WorldzConfig config = new WorldzConfig();
        config.stacked.worldSizeChunks = 0;
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertEquals(ExteriorMode.NORMAL, customization.overworldExterior().mode());
    }

    @Test
    void fromConfigLeavesTheWorldUnboundedWhenWorldSizeChunksIsZeroAndNoGlobalBorderIsSet() {
        WorldzConfig config = new WorldzConfig();
        config.stacked.worldSizeChunks = 0;

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertFalse(customization.overworldBorder().enabled());
        assertEquals(ExteriorMode.NORMAL, customization.overworldExterior().mode());
    }

    @Test
    void fromConfigCopiesReliefBlocks() {
        WorldzConfig config = new WorldzConfig();
        config.stacked.reliefBlocks = 7;

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertEquals(7, customization.reliefBlocks());
    }

    @Test
    void stackedPlanCarriesReliefBlocks() {
        StackedCustomization customization = new StackedCustomization(
            List.of(new StackedLayerSpec("minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 20)), 0)),
            false, 5, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals(5, customization.stackedPlan().reliefBlocks());
    }

    @Test
    void stackedPlanCarriesForceTopVillage() {
        StackedCustomization customization = new StackedCustomization(
            List.of(new StackedLayerSpec("minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 20)), 0)),
            false, 0, true,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );

        assertTrue(customization.stackedPlan().forceTopVillage());
    }

    @Test
    void fromConfigCopiesForceTopVillage() {
        WorldzConfig config = new WorldzConfig();
        config.stacked.forceTopVillage = true;

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertTrue(customization.forceTopVillage());
    }

    @Test
    void fromTextRejectsNonNumericReliefBlocks() {
        assertThrows(IllegalArgumentException.class, () -> StackedCustomization.fromText(
            "minecraft:plains;minecraft:stone:20;0", false, "not-a-number", false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void negativeReliefBlocksIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StackedCustomization(
            List.of(new StackedLayerSpec("minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 20)), 0)),
            false, -1, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        ));
    }
}
