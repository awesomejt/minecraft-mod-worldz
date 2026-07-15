package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure parser for weighted layout-biome entries of the form {@code id} or
 * {@code id@weight}. Unlike {@link BiomeListSpec}, tags are not accepted:
 * layout roles are resolved per concrete biome id ({@link BiomeRoles}) without
 * a registry to expand a tag against.
 *
 * @param entries canonical valid entries, first occurrence wins on duplicate ids
 * @param invalidEntries original values rejected by syntax or weight validation
 */
public record WeightedBiomeListSpec(List<Entry> entries, List<String> invalidEntries) {
    private static final Pattern RESOURCE_LOCATION =
        Pattern.compile("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?");

    /** Makes both result collections immutable. */
    public WeightedBiomeListSpec {
        entries = List.copyOf(entries);
        invalidEntries = List.copyOf(invalidEntries);
    }

    /**
     * Parses, canonicalizes, and deduplicates the configured values.
     *
     * @param configuredEntries raw config values
     * @return separated valid and invalid entries
     */
    public static WeightedBiomeListSpec parse(List<String> configuredEntries) {
        List<Entry> valid = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        if (configuredEntries == null) {
            return new WeightedBiomeListSpec(valid, List.of("null"));
        }

        for (String configured : configuredEntries) {
            String trimmed = configured == null ? "" : configured.trim();
            if (trimmed.startsWith("#")) {
                invalid.add(String.valueOf(configured));
                continue;
            }
            int at = trimmed.lastIndexOf('@');
            String idPart = at < 0 ? trimmed : trimmed.substring(0, at);
            String weightPart = at < 0 ? "1" : trimmed.substring(at + 1);
            if (!RESOURCE_LOCATION.matcher(idPart).matches()) {
                invalid.add(String.valueOf(configured));
                continue;
            }
            double weight;
            try {
                weight = Double.parseDouble(weightPart.trim());
            } catch (NumberFormatException | NullPointerException exception) {
                invalid.add(String.valueOf(configured));
                continue;
            }
            if (!(weight > 0)) {
                invalid.add(String.valueOf(configured));
                continue;
            }
            String id = idPart.contains(":") ? idPart : "minecraft:" + idPart;
            if (seenIds.add(id)) {
                valid.add(new Entry(id, weight));
            }
        }
        return new WeightedBiomeListSpec(valid, invalid);
    }

    /**
     * A canonical resource id and its positive selection weight.
     *
     * @param id resource id with an explicit namespace
     * @param weight positive selection weight
     */
    public record Entry(String id, double weight) {
        /**
         * Renders the canonical value in config syntax.
         *
         * @return {@code id}, or {@code id@weight} when the weight is not {@code 1}
         */
        public String configValue() {
            return weight == 1.0 ? id : id + "@" + weight;
        }
    }
}
