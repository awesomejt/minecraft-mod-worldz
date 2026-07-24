package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import media.jlt.minecraft.mods.worldz.config.StackedConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackedPlanTest {
    private static final StackedLayerSpec TAIGA = new StackedLayerSpec(
        "minecraft:taiga", List.of(new FlatLayerSpec("minecraft:stone", 40)), 6
    );
    private static final StackedLayerSpec DESERT = new StackedLayerSpec(
        "minecraft:desert", List.of(new FlatLayerSpec("minecraft:sandstone", 20)), 6
    );
    private static final StackedLayerSpec PLAINS = new StackedLayerSpec(
        "minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 20), new FlatLayerSpec("minecraft:grass_block", 1)), 0
    );

    @Test
    void disabledPlanHasSafePlaceholderValues() {
        StackedPlan disabled = StackedPlan.disabled();
        assertFalse(disabled.enabled());
        assertFalse(disabled.layers().isEmpty());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        StackedConfig config = new StackedConfig();
        config.layers = List.of(
            "minecraft:taiga;minecraft:stone:40;6",
            "minecraft:plains;minecraft:stone:20,minecraft:grass_block:1;0"
        );
        config.seedRandomizedOrder = true;

        StackedPlan plan = StackedPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(2, plan.layers().size());
        assertTrue(plan.seedRandomizedOrder());
    }

    @Test
    void emptyLayerListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StackedPlan(true, List.of(), false));
    }

    @Test
    void tooTallLayerStackIsRejected() {
        StackedLayerSpec tooTall = new StackedLayerSpec(
            "minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS + 1)), 0
        );
        assertThrows(IllegalArgumentException.class, () -> new StackedPlan(true, List.of(tooTall), false));
    }

    @Test
    void resolvedLayersKeepsConfiguredOrderWhenNotRandomized() {
        StackedPlan plan = new StackedPlan(true, List.of(TAIGA, DESERT, PLAINS), false);
        assertSame(plan.layers(), plan.resolvedLayers(12345L));
    }

    @Test
    void resolvedLayersIsDeterministicForTheSameSeedWhenRandomized() {
        StackedPlan plan = new StackedPlan(true, List.of(TAIGA, DESERT, PLAINS), true);
        List<StackedLayerSpec> first = plan.resolvedLayers(42L);
        List<StackedLayerSpec> second = plan.resolvedLayers(42L);
        assertEquals(first, second);
        assertEquals(3, first.size());
        assertTrue(first.containsAll(List.of(TAIGA, DESERT, PLAINS)));
    }

    @Test
    void totalHeightBlocksSumsEveryLayer() {
        List<StackedLayerSpec> layers = List.of(TAIGA, DESERT, PLAINS);
        // taiga: 40 + 6 = 46; desert: 20 + 6 = 26; plains: 21 + 0 = 21
        assertEquals(93, StackedPlan.totalHeightBlocks(layers));
    }

    @Test
    void layerAtWalksBottomToTopStartingAtOverworldMinY() {
        List<StackedLayerSpec> layers = List.of(TAIGA, DESERT, PLAINS);
        int minY = FlatConfig.OVERWORLD_MIN_Y;

        assertEquals(TAIGA, StackedPlan.layerAt(layers, minY));
        assertEquals(TAIGA, StackedPlan.layerAt(layers, minY + 45));
        assertEquals(DESERT, StackedPlan.layerAt(layers, minY + 46));
        assertEquals(DESERT, StackedPlan.layerAt(layers, minY + 71));
        assertEquals(PLAINS, StackedPlan.layerAt(layers, minY + 72));
        assertEquals(PLAINS, StackedPlan.layerAt(layers, minY + 92));
    }

    @Test
    void layerAtClampsOutOfRangeYToTheNearestEndLayer() {
        List<StackedLayerSpec> layers = List.of(TAIGA, DESERT, PLAINS);
        assertEquals(TAIGA, StackedPlan.layerAt(layers, FlatConfig.OVERWORLD_MIN_Y - 10));
        assertEquals(PLAINS, StackedPlan.layerAt(layers, FlatConfig.OVERWORLD_MIN_Y + 1000));
    }
}
