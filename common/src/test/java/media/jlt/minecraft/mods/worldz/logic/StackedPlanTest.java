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
        config.reliefBlocks = 4;
        config.forceTopVillage = true;

        StackedPlan plan = StackedPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(2, plan.layers().size());
        assertTrue(plan.seedRandomizedOrder());
        assertEquals(4, plan.reliefBlocks());
        assertTrue(plan.forceTopVillage());
    }

    @Test
    void defaultStackedConfigResolvesToTheDocumentedTotalHeight() {
        // Regression guard for DESIGN §34.7/§34.8's own documented arithmetic (8 layers, 324
        // blocks total, Y98 center) -- the shipped default mixes full-shorthand (bottom/top) and
        // simplified bare-biome (the six middle layers) entries; this confirms the simplified
        // entries still expand to exactly the same composition/height the full shorthand had
        // before §34.8, not silently drifting if StackedBiomeDefaults is ever retuned.
        StackedPlan plan = StackedPlan.fromConfig(new StackedConfig());

        assertEquals(8, plan.layers().size());
        assertEquals(324, StackedPlan.totalHeightBlocks(plan.layers()));
    }

    @Test
    void emptyLayerListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StackedPlan(true, List.of(), false, 0, false));
    }

    @Test
    void tooTallLayerStackIsRejected() {
        StackedLayerSpec tooTall = new StackedLayerSpec(
            "minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS + 1)), 0
        );
        assertThrows(IllegalArgumentException.class, () -> new StackedPlan(true, List.of(tooTall), false, 0, false));
    }

    @Test
    void negativeReliefIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StackedPlan(true, List.of(TAIGA, DESERT, PLAINS), false, -1, false));
    }

    @Test
    void resolvedLayersKeepsConfiguredOrderWhenNotRandomized() {
        StackedPlan plan = new StackedPlan(true, List.of(TAIGA, DESERT, PLAINS), false, 0, false);
        assertSame(plan.layers(), plan.resolvedLayers(12345L));
    }

    @Test
    void resolvedLayersIsDeterministicForTheSameSeedWhenRandomized() {
        StackedPlan plan = new StackedPlan(true, List.of(TAIGA, DESERT, PLAINS), true, 0, false);
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
    void surfaceYExcludesTheTopLayersOwnTrailingAirGap() {
        List<StackedLayerSpec> layers = List.of(TAIGA, DESERT, PLAINS);
        // total 93, PLAINS (top) has a 0-block air gap, so surfaceY sits at the very top of the stack.
        assertEquals(FlatConfig.OVERWORLD_MIN_Y + 93, StackedPlan.surfaceY(layers, FlatConfig.OVERWORLD_MIN_Y, 384));
    }

    @Test
    void surfaceYClampsToTheAvailableHeight() {
        List<StackedLayerSpec> layers = List.of(TAIGA, DESERT, PLAINS);
        assertEquals(FlatConfig.OVERWORLD_MIN_Y + 10, StackedPlan.surfaceY(layers, FlatConfig.OVERWORLD_MIN_Y, 10));
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

    @Test
    void reliefBlocksAtIsZeroWhenDisabled() {
        assertEquals(0, StackedPlan.reliefBlocksAt(42L, 0, 100, 200, 0));
        assertEquals(0, StackedPlan.reliefBlocksAt(42L, 0, 100, 200, -1));
    }

    @Test
    void reliefBlocksAtIsBounded() {
        for (int blockX = 0; blockX < 64; blockX += 4) {
            for (int blockZ = 0; blockZ < 64; blockZ += 4) {
                int bump = StackedPlan.reliefBlocksAt(42L, 3, blockX, blockZ, 6);
                assertTrue(bump >= 0 && bump <= 6, "bump " + bump + " out of [0,6] at " + blockX + "," + blockZ);
            }
        }
    }

    @Test
    void reliefBlocksAtIsDeterministicForTheSameInputs() {
        assertEquals(
            StackedPlan.reliefBlocksAt(42L, 2, 17, -33, 8),
            StackedPlan.reliefBlocksAt(42L, 2, 17, -33, 8)
        );
    }

    @Test
    void reliefBlocksAtVariesAcrossColumnsAndLayers() {
        int base = StackedPlan.reliefBlocksAt(42L, 0, 0, 0, 16);
        boolean differsByColumn = false;
        boolean differsByLayer = false;
        for (int cell = 4; cell < 200; cell += 4) {
            if (StackedPlan.reliefBlocksAt(42L, 0, cell, 0, 16) != base) {
                differsByColumn = true;
            }
            if (StackedPlan.reliefBlocksAt(42L, cell, 0, 0, 16) != base) {
                differsByLayer = true;
            }
        }
        assertTrue(differsByColumn, "expected relief to vary across columns");
        assertTrue(differsByLayer, "expected relief to vary across layer indices at the same column");
    }
}
