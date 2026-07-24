package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:stacked} world (GOAL
 * 35, DESIGN §34.1): the small per-type field set, not the full generic Customize screen's. No
 * starter-biome/spawn-strategy field -- spawn is always the top of the layer stack (DESIGN
 * §34.1), never vanilla's ordinary strategies, mirroring {@code FlatCustomization}'s own precedent.
 *
 * @param layers ordered bottom-to-top layer stack
 * @param seedRandomizedOrder whether the configured order is shuffled, seeded off the real world seed
 * @param overworldBorder Overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record StackedCustomization(
    List<StackedLayerSpec> layers,
    boolean seedRandomizedOrder,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public StackedCustomization {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("At least one layer is required.");
        }
        if (overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        layers = List.copyOf(layers);
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static StackedCustomization fromConfig(WorldzConfig config) {
        List<StackedLayerSpec> layers = new ArrayList<>();
        for (String raw : config.stacked.layers) {
            layers.add(StackedLayerSpec.parse(raw));
        }
        return new StackedCustomization(
            layers,
            config.stacked.seedRandomizedOrder,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.overworldExterior),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param layersText comma/newline-separated {@code biome;blocks;air gap} shorthand
     * @param seedRandomizedOrder whether the configured order is shuffled, seeded off the real world seed
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static StackedCustomization fromText(
        String layersText,
        boolean seedRandomizedOrder,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        List<StackedLayerSpec> layers = new ArrayList<>();
        for (String entry : splitEntries(layersText)) {
            layers.add(StackedLayerSpec.parse(entry));
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("At least one layer is required.");
        }
        return new StackedCustomization(
            layers, seedRandomizedOrder, overworldBorder, netherBorder, endBorder, overworldExterior, netherExterior
        );
    }

    private static List<String> splitEntries(String text) {
        // Newline-only (unlike FlatCustomization's own comma-or-newline split): each entry's own
        // blocks sub-list already uses commas internally (StackedLayerSpec's "biome;blocks;gap"
        // shorthand), so a comma-or-newline split would break mid-entry.
        List<String> entries = new ArrayList<>();
        if (text == null) {
            return entries;
        }
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    /**
     * Resolves this world's stacked plan.
     *
     * @return resolved, enabled plan
     */
    public StackedPlan stackedPlan() {
        return new StackedPlan(true, layers, seedRandomizedOrder);
    }

    /**
     * Converts all three border selections to the world-persisted plan.
     *
     * @return immutable codec-backed world-limit plan
     */
    public WorldLimitPlan worldLimitPlan() {
        return new WorldLimitPlan(overworldBorder.toPlan(), netherBorder.toPlan(), endBorder.toPlan());
    }

    /**
     * Converts both exterior selections to a resolved, persisted plan.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(overworldExterior.toPlan(overworldBorder), netherExterior.toPlan(netherBorder));
    }

    /**
     * Renders {@link #layers} back into the text editor's own shorthand, one per line.
     *
     * @return newline-joined {@code biome;blocks;air gap} entries
     */
    public String layersText() {
        StringBuilder builder = new StringBuilder();
        for (StackedLayerSpec layer : layers) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(layer.format());
        }
        return builder.toString();
    }
}
