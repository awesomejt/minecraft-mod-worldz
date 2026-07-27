package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class StackedConfigTest {
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
    void effectiveOverworldExteriorIsVoidByDefault() {
        StackedConfig config = new StackedConfig();

        ExteriorConfig effective = config.effectiveOverworldExterior(new ExteriorConfig());

        assertEquals(ExteriorMode.VOID, effective.mode);
        assertEquals(64, effective.boundaryRadiusBlocks);
    }

    @Test
    void effectiveOverworldExteriorBoundaryScalesWithWorldSizeChunks() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 8;

        ExteriorConfig effective = config.effectiveOverworldExterior(new ExteriorConfig());

        assertEquals(ExteriorMode.VOID, effective.mode);
        assertEquals(128, effective.boundaryRadiusBlocks);
    }

    @Test
    void effectiveOverworldExteriorPassesThroughUnchangedWhenWorldSizeChunksIsZero() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 0;
        ExteriorConfig configured = new ExteriorConfig();
        configured.mode = ExteriorMode.OCEAN;

        ExteriorConfig effective = config.effectiveOverworldExterior(configured);

        assertSame(configured, effective);
        assertFalse(effective.mode == ExteriorMode.VOID);
    }
}
