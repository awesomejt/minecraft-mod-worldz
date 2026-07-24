package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StackedLayerSpecTest {
    @Test
    void parsesBiomeBlocksAndAirGap() {
        StackedLayerSpec spec = StackedLayerSpec.parse("minecraft:plains;minecraft:stone:20,minecraft:dirt:3,minecraft:grass_block:1;6");
        assertEquals("minecraft:plains", spec.biome());
        assertEquals(3, spec.blocks().size());
        assertEquals(new FlatLayerSpec("minecraft:stone", 20), spec.blocks().get(0));
        assertEquals(new FlatLayerSpec("minecraft:dirt", 3), spec.blocks().get(1));
        assertEquals(new FlatLayerSpec("minecraft:grass_block", 1), spec.blocks().get(2));
        assertEquals(6, spec.airGapBlocks());
    }

    @Test
    void totalHeightIsBlocksPlusAirGap() {
        StackedLayerSpec spec = new StackedLayerSpec(
            "minecraft:desert", List.of(new FlatLayerSpec("minecraft:sandstone", 20), new FlatLayerSpec("minecraft:sand", 3)), 6
        );
        assertEquals(23, spec.blocksHeightBlocks());
        assertEquals(29, spec.totalHeightBlocks());
    }

    @Test
    void formatRoundTripsThroughParse() {
        StackedLayerSpec spec = new StackedLayerSpec(
            "minecraft:taiga", List.of(new FlatLayerSpec("minecraft:stone", 40), new FlatLayerSpec("minecraft:podzol", 2)), 6
        );
        assertEquals("minecraft:taiga;minecraft:stone:40,minecraft:podzol:2;6", spec.format());
        assertEquals(spec, StackedLayerSpec.parse(spec.format()));
    }

    @Test
    void malformedShorthandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StackedLayerSpec.parse("minecraft:plains;minecraft:stone:20"));
        assertThrows(IllegalArgumentException.class, () -> StackedLayerSpec.parse("minecraft:plains;minecraft:stone:20;not-a-number"));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StackedLayerSpec("", List.of(new FlatLayerSpec("minecraft:stone", 1)), 0));
        assertThrows(IllegalArgumentException.class, () -> new StackedLayerSpec("minecraft:plains", List.of(), 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StackedLayerSpec("minecraft:plains", List.of(new FlatLayerSpec("minecraft:stone", 1)), -1)
        );
    }
}
