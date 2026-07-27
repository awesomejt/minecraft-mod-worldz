package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:flat} world (GOAL 15,
 * DESIGN §33.2): the small per-type field set, not the full generic Customize screen's. No
 * spawn-strategy field -- spawn is always the top of the layer stack (DESIGN §33.3), never
 * vanilla's ordinary strategies.
 *
 * @param layers ordered bottom-to-top layer stack
 * @param biome the single fixed biome for the whole world
 * @param decoration whether ordinary biome decoration (trees, ore veins, etc.) runs
 * @param structureOverrides structure-set ids eligible to place; empty means every registered set
 * @param overworldBorder Overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record FlatCustomization(
    List<FlatLayerSpec> layers,
    String biome,
    boolean decoration,
    List<String> structureOverrides,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public FlatCustomization {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("At least one layer is required.");
        }
        if (biome == null || biome.isBlank()) {
            throw new IllegalArgumentException("Biome is required.");
        }
        if (overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        layers = List.copyOf(layers);
        structureOverrides = List.copyOf(structureOverrides == null ? List.of() : structureOverrides);
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static FlatCustomization fromConfig(WorldzConfig config) {
        List<FlatLayerSpec> layers = new ArrayList<>();
        for (String raw : config.flat.layers) {
            layers.add(FlatLayerSpec.parse(raw));
        }
        return new FlatCustomization(
            layers,
            config.flat.biome,
            config.flat.decoration,
            List.copyOf(config.flat.structureOverrides),
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
     * @param layersText comma/newline-separated {@code block:height} shorthand (DESIGN §33.5)
     * @param biome the single fixed biome id
     * @param decoration whether ordinary biome decoration runs
     * @param structureOverridesText comma/newline-separated structure-set ids
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static FlatCustomization fromText(
        String layersText,
        String biome,
        boolean decoration,
        String structureOverridesText,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        List<FlatLayerSpec> layers = new ArrayList<>();
        for (String entry : splitEntries(layersText)) {
            layers.add(FlatLayerSpec.parse(entry));
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("At least one layer is required.");
        }
        return new FlatCustomization(
            layers, biome.trim(), decoration, splitEntries(structureOverridesText),
            overworldBorder, netherBorder, endBorder, overworldExterior, netherExterior
        );
    }

    private static List<String> splitEntries(String text) {
        List<String> entries = new ArrayList<>();
        if (text == null) {
            return entries;
        }
        for (String line : text.split("[,\\n]")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    /**
     * Resolves this world's flat plan. The underground-biome band (GOAL 42, DESIGN §37.3) isn't
     * yet exposed on the Customize screen -- config-only for now, same deferral this project
     * already uses for other new fields (e.g. the capsule mechanism) -- so a Customize-screen
     * world always gets it disabled; only a config-driven world (never opening Customize) reads
     * {@code flat.undergroundBiome}/{@code undergroundBelowSurfaceBlocks} via
     * {@link FlatPlan#fromConfig}.
     *
     * @return resolved, enabled plan
     */
    public FlatPlan flatPlan() {
        return new FlatPlan(true, layers, biome, decoration, structureOverrides, "", 10);
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
     * @return newline-joined {@code block:height} entries
     */
    public String layersText() {
        StringBuilder builder = new StringBuilder();
        for (FlatLayerSpec layer : layers) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(layer.format());
        }
        return builder.toString();
    }

    /**
     * Renders {@link #structureOverrides} back into the text editor's own shorthand, one per line.
     *
     * @return newline-joined structure-set ids
     */
    public String structureOverridesText() {
        return String.join("\n", structureOverrides);
    }
}
