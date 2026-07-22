package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        FlatPlan disabled = FlatPlan.disabled();
        assertFalse(disabled.enabled());
        assertFalse(disabled.layers().isEmpty());
        assertEquals("minecraft:plains", disabled.biome());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        FlatConfig config = new FlatConfig();
        config.layers = List.of("minecraft:bedrock:1", "minecraft:dirt:2", "minecraft:grass_block:1");
        config.biome = "minecraft:desert";
        config.decoration = true;
        config.structureOverrides = List.of("minecraft:villages");

        FlatPlan plan = FlatPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(3, plan.layers().size());
        assertEquals(4, plan.totalHeightBlocks());
        assertEquals("minecraft:desert", plan.biome());
        assertTrue(plan.decoration());
        assertEquals(List.of("minecraft:villages"), plan.structureOverrides());
    }

    @Test
    void emptyLayerListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlatPlan(true, List.of(), "minecraft:plains", false, List.of()));
    }

    @Test
    void blankBiomeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlatPlan(
            true, List.of(new FlatLayerSpec("minecraft:bedrock", 1)), "", false, List.of()
        ));
    }

    @Test
    void tooTallLayerStackIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlatPlan(
            true, List.of(new FlatLayerSpec("minecraft:stone", FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS + 1)),
            "minecraft:plains", false, List.of()
        ));
    }
}
