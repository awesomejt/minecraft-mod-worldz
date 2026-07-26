package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndStartPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        EndStartPlan disabled = EndStartPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(StarterKitTier.MEDIUM, disabled.chestTier());
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_SIZE_BLOCKS, disabled.capsuleSizeBlocks());
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_HEIGHT_BLOCKS, disabled.capsuleHeightBlocks());
        assertEquals(LightSource.GLOWSTONE, disabled.capsuleLightSource());
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS, disabled.capsuleLightSpacingBlocks());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        EndStartConfig config = new EndStartConfig();
        config.chestTier = StarterKitTier.HARD;
        config.capsule.sizeBlocks = 9;
        config.capsule.heightBlocks = 4;
        config.capsule.lightSource = LightSource.SOUL_LANTERN;
        config.capsule.lightSpacingBlocks = 3;

        EndStartPlan plan = EndStartPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(StarterKitTier.HARD, plan.chestTier());
        assertEquals(9, plan.capsuleSizeBlocks());
        assertEquals(4, plan.capsuleHeightBlocks());
        assertEquals(LightSource.SOUL_LANTERN, plan.capsuleLightSource());
        assertEquals(3, plan.capsuleLightSpacingBlocks());
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(null, 7, 3));
    }

    @Test
    void invalidCapsuleSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(StarterKitTier.MEDIUM, EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS - 1, 3));
        assertThrows(IllegalArgumentException.class, () -> plan(StarterKitTier.MEDIUM, EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS + 1, 3));
    }

    @Test
    void evenCapsuleSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(StarterKitTier.MEDIUM, 6, 3));
    }

    @Test
    void invalidCapsuleHeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plan(StarterKitTier.MEDIUM, 7, EndStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS - 1));
        assertThrows(IllegalArgumentException.class, () -> plan(StarterKitTier.MEDIUM, 7, EndStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS + 1));
    }

    @Test
    void invalidCapsuleLightSourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EndStartPlan(true, StarterKitTier.MEDIUM, 7, 3, null, 5));
    }

    @Test
    void invalidCapsuleLightSpacingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EndStartPlan(
            true, StarterKitTier.MEDIUM, 7, 3, LightSource.GLOWSTONE, EndStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS - 1
        ));
        assertThrows(IllegalArgumentException.class, () -> new EndStartPlan(
            true, StarterKitTier.MEDIUM, 7, 3, LightSource.GLOWSTONE, EndStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS + 1
        ));
    }

    @Test
    void centeredCapsuleOffsetsAlwaysIncludesTheCenter() {
        assertEquals(List.of(0), EndStartPlan.centeredCapsuleOffsets(2, 5));
    }

    @Test
    void centeredCapsuleOffsetsAddSymmetricPairsWithinBounds() {
        assertEquals(List.of(0, -5, 5), EndStartPlan.centeredCapsuleOffsets(5, 5));
        assertEquals(List.of(0, -5, 5, -10, 10), EndStartPlan.centeredCapsuleOffsets(11, 5));
    }

    @Test
    void centeredCapsuleOffsetsHandleAZeroHalfWidth() {
        assertEquals(List.of(0), EndStartPlan.centeredCapsuleOffsets(0, 5));
    }

    private static EndStartPlan plan(StarterKitTier chestTier, int capsuleSizeBlocks, int capsuleHeightBlocks) {
        return new EndStartPlan(true, chestTier, capsuleSizeBlocks, capsuleHeightBlocks, LightSource.GLOWSTONE, 5);
    }
}
