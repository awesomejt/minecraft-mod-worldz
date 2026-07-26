package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherStartPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        NetherStartPlan disabled = NetherStartPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(NetherStartPlan.DEFAULT_SPAWN_Y, disabled.spawnY());
        assertEquals(StarterKitTier.MEDIUM, disabled.chestTier());
        assertFalse(disabled.forceCapsule());
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_SIZE_BLOCKS, disabled.capsuleSizeBlocks());
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_HEIGHT_BLOCKS, disabled.capsuleHeightBlocks());
        assertEquals(LightSource.GLOWSTONE, disabled.capsuleLightSource());
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS, disabled.capsuleLightSpacingBlocks());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        NetherStartConfig config = new NetherStartConfig();
        config.spawnY = 64;
        config.chestTier = StarterKitTier.HARD;
        config.forceCapsule = true;
        config.capsule.sizeBlocks = 7;
        config.capsule.heightBlocks = 4;
        config.capsule.lightSource = LightSource.SOUL_LANTERN;
        config.capsule.lightSpacingBlocks = 3;

        NetherStartPlan plan = NetherStartPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(64, plan.spawnY());
        assertEquals(StarterKitTier.HARD, plan.chestTier());
        assertTrue(plan.forceCapsule());
        assertEquals(7, plan.capsuleSizeBlocks());
        assertEquals(4, plan.capsuleHeightBlocks());
        assertEquals(LightSource.SOUL_LANTERN, plan.capsuleLightSource());
        assertEquals(3, plan.capsuleLightSpacingBlocks());
    }

    @Test
    void invalidSpawnYIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(NetherStartPlan.MIN_SPAWN_Y - 1, 5, 3));
        assertThrows(IllegalArgumentException.class, () -> plan(NetherStartPlan.MAX_SPAWN_Y + 1, 5, 3));
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, 32, null, false, 5, 3, LightSource.GLOWSTONE, 5
        ));
    }

    @Test
    void invalidCapsuleSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(32, NetherStartPlan.MIN_CAPSULE_SIZE_BLOCKS - 1, 3));
        assertThrows(IllegalArgumentException.class, () -> plan(32, NetherStartPlan.MAX_CAPSULE_SIZE_BLOCKS + 1, 3));
    }

    @Test
    void evenCapsuleSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(32, 6, 3));
    }

    @Test
    void invalidCapsuleHeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(32, 5, NetherStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS - 1));
        assertThrows(IllegalArgumentException.class, () -> plan(32, 5, NetherStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS + 1));
    }

    @Test
    void invalidCapsuleLightSourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, 32, StarterKitTier.MEDIUM, false, 5, 3, null, 5
        ));
    }

    @Test
    void invalidCapsuleLightSpacingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, 32, StarterKitTier.MEDIUM, false, 5, 3, LightSource.GLOWSTONE, NetherStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS - 1
        ));
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, 32, StarterKitTier.MEDIUM, false, 5, 3, LightSource.GLOWSTONE, NetherStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS + 1
        ));
    }

    @Test
    void defaultSpawnYIsComfortablyAwayFromEitherBoundary() {
        assertFalse(plan(32, 5, 3).spawnYTooCloseToBoundary(0, 128, 16));
    }

    @Test
    void spawnYNearTheFloorPrefersTheGuaranteedCapsule() {
        // Config 62's own scenario: spawnY 4, well within the tolerance of the Nether's Y-0 floor.
        assertTrue(plan(4, 5, 3).spawnYTooCloseToBoundary(0, 128, 16));
    }

    @Test
    void spawnYNearTheCeilingPrefersTheGuaranteedCapsuleToo() {
        assertTrue(plan(120, 5, 3).spawnYTooCloseToBoundary(0, 128, 16));
    }

    @Test
    void spawnYExactlyAtTheToleranceBoundaryStillSearches() {
        // 16 - 16 == 0 == levelMinY, not < it -- the window is not truncated, so this stays false.
        assertFalse(plan(16, 5, 3).spawnYTooCloseToBoundary(0, 128, 16));
        assertFalse(plan(112, 5, 3).spawnYTooCloseToBoundary(0, 128, 16));
    }

    @Test
    void centeredCapsuleOffsetsAlwaysIncludesTheCenter() {
        // The default capsule (interior half-width 2) with the default spacing (5): only one
        // fixture fits, dead center on the wall -- Jason's own "light source in the middle".
        assertEquals(List.of(0), NetherStartPlan.centeredCapsuleOffsets(2, 5));
    }

    @Test
    void centeredCapsuleOffsetsAddSymmetricPairsWithinBounds() {
        assertEquals(List.of(0, -5, 5), NetherStartPlan.centeredCapsuleOffsets(5, 5));
        assertEquals(List.of(0, -5, 5, -10, 10), NetherStartPlan.centeredCapsuleOffsets(11, 5));
    }

    @Test
    void centeredCapsuleOffsetsHandleAZeroHalfWidth() {
        // The smallest capsule (a single interior column) -- no room for anything but the center.
        assertEquals(List.of(0), NetherStartPlan.centeredCapsuleOffsets(0, 5));
    }

    private static NetherStartPlan plan(int spawnY, int capsuleSizeBlocks, int capsuleHeightBlocks) {
        return new NetherStartPlan(
            true, spawnY, StarterKitTier.MEDIUM, false, capsuleSizeBlocks, capsuleHeightBlocks, LightSource.GLOWSTONE, 5
        );
    }
}
