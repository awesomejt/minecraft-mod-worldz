package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:deep_flat} world (GOAL
 * 16, DESIGN §33.4): the small per-type field set, not the full generic Customize screen's. No
 * spawn-strategy field -- spawn is always the configured {@code surfaceY} (DESIGN §33.4's
 * {@code getSpawnHeight} override), never vanilla's ordinary strategies. Full vanilla biome
 * variety, like {@code cave} -- real caves/cave biomes/rivers below the cap need the real,
 * unrestricted biome source, unlike {@code flat}'s single-biome restriction.
 *
 * @param surfaceY the flat cap height
 * @param capLayers land-cap layer stack, painted immediately below {@code surfaceY}
 * @param riversEnabled whether a river/ocean biome column gets a water cap instead of the land cap
 * @param riverExclusionRadiusBlocks radius around the origin within which river/ocean columns
 *     always get the land cap regardless of {@code riversEnabled}
 * @param overworldBorder Overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record DeepFlatCustomization(
    int surfaceY,
    List<FlatLayerSpec> capLayers,
    boolean riversEnabled,
    int riverExclusionRadiusBlocks,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public DeepFlatCustomization {
        if (capLayers == null || capLayers.isEmpty()) {
            throw new IllegalArgumentException("At least one cap layer is required.");
        }
        if (riverExclusionRadiusBlocks < 0) {
            throw new IllegalArgumentException("River exclusion radius must not be negative.");
        }
        if (overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        capLayers = List.copyOf(capLayers);
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static DeepFlatCustomization fromConfig(WorldzConfig config) {
        List<FlatLayerSpec> layers = new ArrayList<>();
        for (String raw : config.deepFlat.capLayers) {
            layers.add(FlatLayerSpec.parse(raw));
        }
        return new DeepFlatCustomization(
            config.deepFlat.surfaceY,
            layers,
            config.deepFlat.riversEnabled,
            config.deepFlat.riverExclusionRadiusBlocks,
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
     * @param surfaceY decimal flat cap height
     * @param capLayersText comma/newline-separated {@code block:height} shorthand
     * @param riversEnabled whether rivers/oceans get a water cap
     * @param riverExclusionRadiusBlocks decimal exclusion radius
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static DeepFlatCustomization fromText(
        String surfaceY,
        String capLayersText,
        boolean riversEnabled,
        String riverExclusionRadiusBlocks,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        List<FlatLayerSpec> layers = new ArrayList<>();
        for (String entry : splitEntries(capLayersText)) {
            layers.add(FlatLayerSpec.parse(entry));
        }
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("At least one cap layer is required.");
        }
        return new DeepFlatCustomization(
            parseInteger(surfaceY, "Surface Y"), layers, riversEnabled, parseInteger(riverExclusionRadiusBlocks, "River exclusion radius"),
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
     * Resolves this world's deep-flat plan.
     *
     * @return resolved, enabled plan
     */
    public DeepFlatPlan deepFlatPlan() {
        return new DeepFlatPlan(true, surfaceY, capLayers, riversEnabled, riverExclusionRadiusBlocks);
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
     * Renders {@link #capLayers} back into the text editor's own shorthand, one per line.
     *
     * @return newline-joined {@code block:height} entries
     */
    public String capLayersText() {
        StringBuilder builder = new StringBuilder();
        for (FlatLayerSpec layer : capLayers) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(layer.format());
        }
        return builder.toString();
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }
}
