package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyChunkCustomizationTest {
    private static SkyChunkCustomization create(double spawnChance, int cellSizeChunks, boolean applyToNether, boolean applyToEnd) {
        return new SkyChunkCustomization(
            spawnChance, cellSizeChunks, false, 5, false, 256, applyToNether, applyToEnd,
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

        SkyChunkCustomization customization = SkyChunkCustomization.fromConfig(config);

        assertEquals(config.chunkIsland.spawnChance, customization.spawnChance());
        assertEquals(config.chunkIsland.cellSizeChunks, customization.cellSizeChunks());
        assertFalse(customization.topOnly());
        assertFalse(customization.applyToNether());
        assertFalse(customization.applyToEnd());
    }

    @Test
    void chunkIslandPlanIsAlwaysEnabled() {
        SkyChunkCustomization customization = create(0.5, 2, false, false);

        ChunkIslandPlan plan = customization.chunkIslandPlan();

        assertTrue(plan.enabled());
        assertEquals(0.5, plan.spawnChance());
        assertEquals(2, plan.cellSizeChunks());
    }

    @Test
    void netherChunkIslandPlanDisabledUnlessApplyToNether() {
        SkyChunkCustomization off = create(0.5, 1, false, false);
        SkyChunkCustomization on = create(0.5, 1, true, false);

        assertFalse(off.netherChunkIslandPlan().enabled());
        assertTrue(on.netherChunkIslandPlan().enabled());
    }

    @Test
    void endChunkIslandPlanDisabledUnlessApplyToEnd() {
        SkyChunkCustomization off = create(0.5, 1, false, false);
        SkyChunkCustomization on = create(0.5, 1, false, true);

        assertFalse(off.endChunkIslandPlan().enabled());
        assertTrue(on.endChunkIslandPlan().enabled());
    }

    @Test
    void exteriorPlanKeepsOverworldNormal() {
        SkyChunkCustomization customization = create(0.5, 1, false, false);

        ExteriorPlan exterior = customization.exteriorPlan();

        assertEquals(ExteriorMode.NORMAL, exterior.overworld().mode());
    }

    @Test
    void invalidSpawnChanceRejected() {
        assertThrows(IllegalArgumentException.class, () -> create(1.5, 1, false, false));
    }

    @Test
    void invalidCellSizeRejected() {
        assertThrows(IllegalArgumentException.class, () -> create(0.5, 0, false, false));
    }

    @Test
    void oceanExteriorRejectedForNether() {
        assertThrows(IllegalArgumentException.class, () -> new SkyChunkCustomization(
            0.5, 1, false, 5, false, 256, false, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            new WorldzCustomization.ExteriorSettings(ExteriorMode.OCEAN, 512, 64)
        ));
    }
}
