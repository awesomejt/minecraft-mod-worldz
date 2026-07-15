package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedEntryFilterTest {
    @Test
    void preservesCandidateOrderAndDuplicateClimateEntries() {
        List<Entry> candidates = List.of(
            new Entry("hot-dry", "desert"),
            new Entry("temperate", "plains"),
            new Entry("hot-wet", "jungle"),
            new Entry("windswept", "plains")
        );

        AllowedEntryFilter.Result<Entry, String> result = AllowedEntryFilter.filter(
            candidates,
            Entry::biome,
            Set.of("plains", "desert")
        );

        assertEquals(List.of(candidates.get(0), candidates.get(1), candidates.get(3)), result.entries());
        assertEquals(List.of("desert", "plains"), List.copyOf(result.matchedValues()));
    }

    @Test
    void returnsEmptyResultWhenNothingIsAllowedOrMatched() {
        AllowedEntryFilter.Result<Entry, String> noAllowed = AllowedEntryFilter.filter(
            List.of(new Entry("temperate", "plains")),
            Entry::biome,
            Set.of()
        );
        AllowedEntryFilter.Result<Entry, String> noMatches = AllowedEntryFilter.filter(
            List.of(new Entry("temperate", "plains")),
            Entry::biome,
            Set.of("desert")
        );

        assertTrue(noAllowed.entries().isEmpty());
        assertTrue(noAllowed.matchedValues().isEmpty());
        assertTrue(noMatches.entries().isEmpty());
        assertTrue(noMatches.matchedValues().isEmpty());
    }

    @Test
    void resultIsAnImmutableDefensiveSnapshot() {
        List<Entry> entries = new ArrayList<>(List.of(new Entry("temperate", "plains")));
        Set<String> matches = new LinkedHashSet<>(Set.of("plains"));
        AllowedEntryFilter.Result<Entry, String> result = new AllowedEntryFilter.Result<>(entries, matches);

        entries.clear();
        matches.clear();

        assertEquals(1, result.entries().size());
        assertEquals(Set.of("plains"), result.matchedValues());
        assertThrows(UnsupportedOperationException.class, () -> result.entries().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.matchedValues().clear());
    }

    private record Entry(String climate, String biome) {
    }
}
