package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.DeepFlatConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepFlatPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        DeepFlatPlan disabled = DeepFlatPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(DeepFlatPlan.DEFAULT_SURFACE_Y, disabled.surfaceY());
        assertFalse(disabled.capLayers().isEmpty());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        DeepFlatConfig config = new DeepFlatConfig();
        config.surfaceY = 80;
        config.capLayers = List.of("minecraft:dirt:2", "minecraft:grass_block:1");
        config.riversEnabled = false;
        config.riverExclusionRadiusBlocks = 256;

        DeepFlatPlan plan = DeepFlatPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(80, plan.surfaceY());
        assertEquals(3, plan.capThicknessBlocks());
        assertFalse(plan.riversEnabled());
        assertEquals(256, plan.riverExclusionRadiusBlocks());
    }

    @Test
    void emptyCapLayerListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeepFlatPlan(true, 64, List.of(), true, 512));
    }

    @Test
    void invalidSurfaceYIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeepFlatPlan(
            true, DeepFlatPlan.MIN_SURFACE_Y - 1, List.of(new FlatLayerSpec("minecraft:grass_block", 1)), true, 512
        ));
        assertThrows(IllegalArgumentException.class, () -> new DeepFlatPlan(
            true, DeepFlatPlan.MAX_SURFACE_Y + 1, List.of(new FlatLayerSpec("minecraft:grass_block", 1)), true, 512
        ));
    }

    @Test
    void negativeExclusionRadiusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeepFlatPlan(
            true, 64, List.of(new FlatLayerSpec("minecraft:grass_block", 1)), true, -1
        ));
    }
}
