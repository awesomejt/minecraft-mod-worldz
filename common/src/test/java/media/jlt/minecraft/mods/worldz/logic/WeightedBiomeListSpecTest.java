package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedBiomeListSpecTest {
    @Test
    void parsesIdsWithAndWithoutExplicitWeight() {
        WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(List.of("plains", "example:desert@3", " minecraft:savanna@0.5 "));

        assertEquals(List.of(
            new WeightedBiomeListSpec.Entry("minecraft:plains", 1.0),
            new WeightedBiomeListSpec.Entry("example:desert", 3.0),
            new WeightedBiomeListSpec.Entry("minecraft:savanna", 0.5)
        ), spec.entries());
        assertTrue(spec.invalidEntries().isEmpty());
    }

    @Test
    void rejectsTagsZeroAndNegativeAndMalformedWeights() {
        WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(List.of(
            "#minecraft:is_overworld",
            "minecraft:plains@0",
            "minecraft:desert@-1",
            "minecraft:savanna@not-a-number",
            "Uppercase:plains@1"
        ));

        assertTrue(spec.entries().isEmpty());
        assertEquals(5, spec.invalidEntries().size());
    }

    @Test
    void firstOccurrenceWinsOnDuplicateIds() {
        WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(List.of("minecraft:plains@2", "minecraft:plains@5"));

        assertEquals(List.of(new WeightedBiomeListSpec.Entry("minecraft:plains", 2.0)), spec.entries());
    }

    @Test
    void entryRendersConfigValueOmittingDefaultWeight() {
        assertEquals("minecraft:plains", new WeightedBiomeListSpec.Entry("minecraft:plains", 1.0).configValue());
        assertEquals("minecraft:plains@3.0", new WeightedBiomeListSpec.Entry("minecraft:plains", 3.0).configValue());
    }

    @Test
    void nullListIsReportedWithoutThrowing() {
        WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(null);

        assertTrue(spec.entries().isEmpty());
        assertEquals(List.of("null"), spec.invalidEntries());
    }

    @Test
    void resultCollectionsAreImmutableSnapshots() {
        WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(List.of("plains", "bad value"));

        assertThrows(UnsupportedOperationException.class, () -> spec.entries().clear());
        assertThrows(UnsupportedOperationException.class, () -> spec.invalidEntries().clear());
    }
}
