package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackedConfigTest {
    @Test
    void effectiveOverworldBorderDerivesFromWorldSizeChunksByDefault() {
        StackedConfig config = new StackedConfig();
        BorderConfig configured = new BorderConfig();

        BorderConfig effective = config.effectiveOverworldBorder(configured);

        assertTrue(effective.enabled);
        assertEquals(64, effective.initialRadiusBlocks);
        assertEquals(64, effective.finalRadiusBlocks);
        assertTrue(effective.ensureObjective);
    }

    @Test
    void effectiveOverworldBorderScalesWithWorldSizeChunks() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 8;

        BorderConfig effective = config.effectiveOverworldBorder(new BorderConfig());

        assertEquals(128, effective.initialRadiusBlocks);
        assertEquals(128, effective.finalRadiusBlocks);
    }

    @Test
    void effectiveOverworldBorderPassesThroughUnchangedWhenWorldSizeChunksIsZero() {
        StackedConfig config = new StackedConfig();
        config.worldSizeChunks = 0;
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
