package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatLayerSpecTest {
    @Test
    void parsesBlockIdOnlyAsHeightOne() {
        FlatLayerSpec spec = FlatLayerSpec.parse("minecraft:bedrock");
        assertEquals("minecraft:bedrock", spec.blockId());
        assertEquals(1, spec.heightBlocks());
    }

    @Test
    void parsesBlockIdWithHeight() {
        FlatLayerSpec spec = FlatLayerSpec.parse("minecraft:stone:59");
        assertEquals("minecraft:stone", spec.blockId());
        assertEquals(59, spec.heightBlocks());
    }

    @Test
    void formatRoundTripsThroughParse() {
        FlatLayerSpec spec = new FlatLayerSpec("minecraft:dirt", 3);
        assertEquals("minecraft:dirt:3", spec.format());
        assertEquals(spec, FlatLayerSpec.parse(spec.format()));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlatLayerSpec("", 1));
        assertThrows(IllegalArgumentException.class, () -> new FlatLayerSpec(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new FlatLayerSpec("minecraft:dirt", 0));
        assertThrows(IllegalArgumentException.class, () -> new FlatLayerSpec("minecraft:dirt", -1));
    }
}
