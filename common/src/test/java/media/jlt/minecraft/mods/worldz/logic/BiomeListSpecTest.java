package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeListSpecTest {
    @Test
    void parsesBiomeAndTagIdsWithDefaultNamespaces() {
        BiomeListSpec spec = BiomeListSpec.parse(List.of(
            "plains",
            "example:sky/islands",
            "#is_overworld",
            "#example:climate/cold"
        ));

        assertEquals(List.of(
            new BiomeListSpec.Entry("minecraft:plains", false),
            new BiomeListSpec.Entry("example:sky/islands", false),
            new BiomeListSpec.Entry("minecraft:is_overworld", true),
            new BiomeListSpec.Entry("example:climate/cold", true)
        ), spec.entries());
        assertTrue(spec.invalidEntries().isEmpty());
    }

    @Test
    void trimsAndDeduplicatesCanonicalEntries() {
        BiomeListSpec spec = BiomeListSpec.parse(List.of(" plains ", "minecraft:plains", "#foo", " #minecraft:foo "));

        assertEquals(List.of(
            new BiomeListSpec.Entry("minecraft:plains", false),
            new BiomeListSpec.Entry("minecraft:foo", true)
        ), spec.entries());
    }

    @Test
    void reportsEveryInvalidEntry() {
        BiomeListSpec spec = BiomeListSpec.parse(List.of(
            "",
            "#",
            "Uppercase:plains",
            "minecraft:",
            ":plains",
            "minecraft:has space"
        ));

        assertTrue(spec.entries().isEmpty());
        assertEquals(6, spec.invalidEntries().size());
    }

    @Test
    void entryRendersConfigPrefixForTagsOnly() {
        assertEquals("minecraft:plains", new BiomeListSpec.Entry("minecraft:plains", false).configValue());
        assertEquals("#minecraft:is_overworld", new BiomeListSpec.Entry("minecraft:is_overworld", true).configValue());
        assertFalse(new BiomeListSpec.Entry("minecraft:plains", false).tag());
    }
}
