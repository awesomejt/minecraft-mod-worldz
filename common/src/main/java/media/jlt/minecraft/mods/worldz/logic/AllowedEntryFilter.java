package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Pure filtering logic shared by the Minecraft-facing climate adapter. */
public final class AllowedEntryFilter {
    private AllowedEntryFilter() {
    }

    /**
     * Keeps every candidate whose selected value is allowed and tracks the
     * distinct values that matched. Candidate order and duplicate candidates
     * are preserved because a biome can own several climate parameter points.
     *
     * @param candidates ordered source entries
     * @param valueSelector extracts the value checked against {@code allowedValues}
     * @param allowedValues values permitted in the result
     * @param <E> entry type
     * @param <V> selected value type
     * @return immutable selected entries and matched values
     */
    public static <E, V> Result<E, V> filter(
        List<E> candidates,
        Function<? super E, ? extends V> valueSelector,
        Set<V> allowedValues
    ) {
        List<E> selectedEntries = new ArrayList<>();
        Set<V> matchedValues = new LinkedHashSet<>();
        for (E candidate : candidates) {
            V value = valueSelector.apply(candidate);
            if (allowedValues.contains(value)) {
                selectedEntries.add(candidate);
                matchedValues.add(value);
            }
        }
        return new Result<>(selectedEntries, matchedValues);
    }

    /**
     * Immutable result of filtering ordered entries by allowed values.
     *
     * @param entries selected entries, including duplicates
     * @param matchedValues distinct selected values in first-match order
     * @param <E> entry type
     * @param <V> selected value type
     */
    public record Result<E, V>(List<E> entries, Set<V> matchedValues) {
        /** Makes defensive, unmodifiable snapshots of both collections. */
        public Result {
            entries = List.copyOf(entries);
            matchedValues = Collections.unmodifiableSet(new LinkedHashSet<>(matchedValues));
        }
    }
}
