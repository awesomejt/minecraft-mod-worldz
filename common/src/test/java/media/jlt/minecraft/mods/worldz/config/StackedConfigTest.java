package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.StackedCustomization;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackedConfigTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void effectiveOverworldBorderAlwaysPassesThroughTheSharedConfig() {
        StackedConfig config = new StackedConfig();
        BorderConfig configured = new BorderConfig();

        BorderConfig effective = config.effectiveOverworldBorder(configured);

        assertSame(configured, effective);
        assertFalse(effective.enabled);
    }

    @Test
    void effectiveOverworldBorderIgnoresWorldSizeChunks() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 8;
        BorderConfig configured = new BorderConfig();
        configured.enabled = true;
        configured.initialRadiusBlocks = 2048;
        configured.finalRadiusBlocks = 4096;

        BorderConfig effective = config.effectiveOverworldBorder(configured);

        assertSame(configured, effective);
    }

    @Test
    void effectiveOverworldExteriorIsVoidByDefaultWhenSharedSectionsAreUntouched() {
        StackedConfig config = new StackedConfig();

        ExteriorConfig effective = config.effectiveOverworldExterior(new ExteriorConfig(), false);

        assertEquals(ExteriorMode.VOID, effective.mode);
        assertEquals(64, effective.boundaryRadiusBlocks);
    }

    @Test
    void effectiveOverworldExteriorBoundaryScalesWithWorldSizeChunks() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 8;

        ExteriorConfig effective = config.effectiveOverworldExterior(new ExteriorConfig(), false);

        assertEquals(ExteriorMode.VOID, effective.mode);
        assertEquals(128, effective.boundaryRadiusBlocks);
    }

    @Test
    void effectiveOverworldExteriorPassesThroughUnchangedWhenWorldSizeChunksIsZero() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 0;
        ExteriorConfig configured = new ExteriorConfig();
        configured.mode = ExteriorMode.OCEAN;

        ExteriorConfig effective = config.effectiveOverworldExterior(configured, false);

        assertSame(configured, effective);
        assertFalse(effective.mode == ExteriorMode.VOID);
    }

    @Test
    void effectiveOverworldExteriorPassesThroughUnchangedWhenSharedSectionsAreExplicitlyConfigured() {
        // TODO 25.5: worldSizeChunks is left at its own nonzero default here on purpose -- this is
        // exactly config 73-76's own shape once their old `worldSizeChunks: 0` opt-out boilerplate
        // is deleted, relying instead on their own explicit overworldBorder/overworldExterior
        // section to win over the derived void wall.
        StackedConfig config = new StackedConfig();
        ExteriorConfig configured = new ExteriorConfig();
        configured.mode = ExteriorMode.OCEAN;
        configured.oceanTransitionWidthBlocks = 32;

        ExteriorConfig effective = config.effectiveOverworldExterior(configured, true);

        assertSame(configured, effective);
        assertFalse(effective.mode == ExteriorMode.VOID);
    }

    @Test
    void effectiveOverworldExteriorHonorsLiteralZeroWorldSizeChunksEvenWhenSharedSectionsAreUntouched() {
        // Zero remains a full opt-out regardless of presence (StackedSchema's own "zero opts out"
        // doc) -- a literal zero-radius void wall would never be valid anyway
        // (ExteriorPlan.DimensionEnvelope's compact constructor rejects it), so this is the one
        // sentinel value TODO 25.5 deliberately keeps honoring the old way.
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 0;

        ExteriorConfig effective = config.effectiveOverworldExterior(new ExteriorConfig(), false);

        assertEquals(ExteriorMode.NORMAL, effective.mode);
    }

    @Test
    void stackedCustomizationHonorsAnExplicitlyParsedOverworldBorderOverTheDerivedVoidExteriorEvenWithWorldSizeChunksAtItsDefault() {
        // TODO 25.5: this is config 73-76's own shape once their `worldSizeChunks: 0` opt-out
        // boilerplate is deleted -- real YAML parsing (unlike direct field assignment,
        // StackedCustomizationTest's own coverage) marks overworldBorder present, so the derived
        // void wall backs off even though worldSizeChunks is left at its own nonzero default (4).
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              finalRadius: 2048
            stacked:
              layers:
                - "minecraft:plains;minecraft:stone:20;0"
            """, LOGGER).sanitize(LOGGER);

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertEquals(ExteriorMode.NORMAL, customization.overworldExterior().mode());
    }

    @Test
    void stackedCustomizationHonorsAnExplicitlyParsedOverworldExteriorOverTheDerivedVoidExteriorEvenWithWorldSizeChunksAtItsDefault() {
        // Same fix, but presence on overworldExterior alone (no overworldBorder at all) --
        // config 76's own shape once its worldSizeChunks: 0 opt-out is deleted.
        WorldzConfig config = WorldzConfig.parse("""
            overworldExterior:
              mode: ocean
              boundaryRadius: 64
            stacked:
              layers:
                - "minecraft:plains;minecraft:stone:20;0"
            """, LOGGER).sanitize(LOGGER);

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertEquals(ExteriorMode.OCEAN, customization.overworldExterior().mode());
        assertEquals(64, customization.overworldExterior().boundaryRadiusBlocks());
    }

    @Test
    void stackedCustomizationStillDerivesTheVoidExteriorWhenNeitherSharedSectionIsParsed() {
        // Config 72's own shape (and 99's) -- confirms this fix does *not* regress the
        // never-customized default (F4/D5's own "left at default" case), which must keep deriving
        // the bounded void wall purely from worldSizeChunks's own nonzero default.
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              layers:
                - "minecraft:plains;minecraft:stone:20;0"
            """, LOGGER).sanitize(LOGGER);

        StackedCustomization customization = StackedCustomization.fromConfig(config);

        assertFalse(customization.overworldBorder().enabled());
        assertEquals(ExteriorMode.VOID, customization.overworldExterior().mode());
        assertEquals(64, customization.overworldExterior().boundaryRadiusBlocks());
    }
}
