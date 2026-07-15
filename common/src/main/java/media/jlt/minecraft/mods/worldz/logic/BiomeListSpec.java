package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure parser result for configured biome ids and biome-tag ids.
 *
 * @param entries canonical valid entries
 * @param invalidEntries original values rejected by syntax validation
 */
public record BiomeListSpec(List<Entry> entries, List<String> invalidEntries) {
    private static final Pattern RESOURCE_LOCATION =
        Pattern.compile("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?");

    /** Makes both result collections immutable. */
    public BiomeListSpec {
        entries = List.copyOf(entries);
        invalidEntries = List.copyOf(invalidEntries);
    }

    /**
     * Parses, canonicalizes, and deduplicates the configured values.
     *
     * @param configuredEntries raw config values
     * @return separated valid and invalid entries
     */
    public static BiomeListSpec parse(List<String> configuredEntries) {
        List<Entry> valid = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        Set<Entry> seen = new LinkedHashSet<>();
        if (configuredEntries == null) {
            return new BiomeListSpec(valid, List.of("null"));
        }

        for (String configured : configuredEntries) {
            String trimmed = configured == null ? "" : configured.trim();
            boolean tag = trimmed.startsWith("#");
            String value = tag ? trimmed.substring(1) : trimmed;
            if (!RESOURCE_LOCATION.matcher(value).matches()) {
                invalid.add(String.valueOf(configured));
                continue;
            }
            String id = value.contains(":") ? value : "minecraft:" + value;
            Entry entry = new Entry(id, tag);
            if (seen.add(entry)) {
                valid.add(entry);
            }
        }
        return new BiomeListSpec(valid, invalid);
    }

    /**
     * A canonical resource id and whether it denotes a tag.
     *
     * @param id resource id with an explicit namespace
     * @param tag whether the id identifies a biome tag
     */
    public record Entry(String id, boolean tag) {
        /**
         * Renders the canonical value in config syntax.
         *
         * @return biome id, prefixed with {@code #} for tags
         */
        public String configValue() {
            return tag ? "#" + id : id;
        }
    }
}
