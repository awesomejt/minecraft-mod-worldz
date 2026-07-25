package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.CaveConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CavePlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        CavePlan disabled = CavePlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(CavePlan.DEFAULT_SPAWN_DEPTH_Y, disabled.spawnDepthY());
        assertFalse(disabled.sealedSurface());
        assertFalse(disabled.cavernEnabled());
        assertFalse(disabled.chestEnabled());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        CaveConfig config = new CaveConfig();
        config.spawnDepthY = -40;
        config.sealedSurface = true;
        config.sealedSurfaceY = 100;
        config.cavernEnabled = true;
        config.cavernRadiusBlocks = 64;
        config.cavernHeightBlocks = 32;
        config.chestEnabled = true;
        config.chestTier = StarterKitTier.HARD;

        CavePlan plan = CavePlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(-40, plan.spawnDepthY());
        assertTrue(plan.sealedSurface());
        assertEquals(100, plan.sealedSurfaceY());
        assertTrue(plan.cavernEnabled());
        assertEquals(64, plan.cavernRadiusBlocks());
        assertEquals(32, plan.cavernHeightBlocks());
        assertTrue(plan.chestEnabled());
        assertEquals(StarterKitTier.HARD, plan.chestTier());
    }

    @Test
    void invalidSealedSurfaceYIsRejectedOnlyWhenSealedSurfaceIsEnabled() {
        assertThrows(IllegalArgumentException.class, () -> plan(true, CaveConfig.MIN_SEALED_SURFACE_Y - 1));
        plan(false, CaveConfig.MIN_SEALED_SURFACE_Y - 1);
    }

    @Test
    void invalidCavernRadiusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, 5, true, CavePlan.MIN_CAVERN_BLOCKS - 1, 24, false,
            StarterKitTier.MEDIUM
        ));
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, 5, true, CavePlan.MAX_CAVERN_BLOCKS + 1, 24, false,
            StarterKitTier.MEDIUM
        ));
    }

    @Test
    void invalidCavernHeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, 5, true, 48, CavePlan.MIN_CAVERN_BLOCKS - 1, false,
            StarterKitTier.MEDIUM
        ));
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, 5, true, 48, CavePlan.MAX_CAVERN_BLOCKS + 1, false,
            StarterKitTier.MEDIUM
        ));
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, 5, false, 48, 24, false, null
        ));
    }

    @Test
    void invalidSealedSurfaceThicknessIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, CavePlan.MIN_SEALED_SURFACE_THICKNESS_BLOCKS - 1,
            false, 48, 24, false, StarterKitTier.MEDIUM
        ));
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, SealedSurfaceBlock.STONE, CavePlan.MAX_SEALED_SURFACE_THICKNESS_BLOCKS + 1,
            false, 48, 24, false, StarterKitTier.MEDIUM
        ));
    }

    @Test
    void invalidSealedSurfaceBlockIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CavePlan(
            true, -32, false, 128, null, 5, false, 48, 24, false, StarterKitTier.MEDIUM
        ));
    }

    private static CavePlan plan(boolean sealedSurface, int sealedSurfaceY) {
        return new CavePlan(
            true, -32, sealedSurface, sealedSurfaceY, SealedSurfaceBlock.STONE, 5, false, 48, 24, false,
            StarterKitTier.MEDIUM
        );
    }
}
