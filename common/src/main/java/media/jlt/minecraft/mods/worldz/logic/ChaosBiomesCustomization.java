package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:chaos_biomes}
 * world (DESIGN §20.11's Phase 4.1 subsection): the small per-type field set, not the
 * full generic Customize screen's.
 *
 * @param biomes weighted land biome entries ({@code id} or {@code id@weight}) shuffled per region (GOALS 33)
 * @param regionScaleBlocks grid-cell edge length in blocks
 * @param starterBiome optional biome forced around spawn, or empty (chaos starts at spawn)
 * @param starterRadiusBlocks starter-zone radius, meaningful only when {@code starterBiome} is set
 * @param spawnStrategy layout-origin and initial-spawn strategy
 * @param allowRivers let vanilla's own river biomes generate naturally
 * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
 * @param overworldBorder overworld border selection (GOALS 17-20, TODO 5.3)
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record ChaosBiomesCustomization(
    List<String> biomes,
    int regionScaleBlocks,
    String starterBiome,
    int starterRadiusBlocks,
    SpawnStrategy spawnStrategy,
    boolean allowRivers,
    boolean allowOceans,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public ChaosBiomesCustomization {
        WeightedBiomeListSpec biomeSpec = WeightedBiomeListSpec.parse(biomes);
        if (!biomeSpec.invalidEntries().isEmpty()) {
            throw new IllegalArgumentException("Invalid chaos biome: " + biomeSpec.invalidEntries().getFirst());
        }
        if (biomeSpec.entries().isEmpty()) {
            throw new IllegalArgumentException("At least one biome is required.");
        }
        biomes = biomeSpec.entries().stream().map(WeightedBiomeListSpec.Entry::configValue).toList();

        if (regionScaleBlocks < WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
            || regionScaleBlocks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS) {
            throw new IllegalArgumentException(
                "Region scale must be between " + WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
                    + " and " + WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS + "."
            );
        }

        starterBiome = starterBiome == null ? "" : starterBiome.trim();
        if (!starterBiome.isEmpty()) {
            BiomeListSpec spec = BiomeListSpec.parse(List.of(starterBiome));
            if (spec.entries().size() != 1 || spec.entries().getFirst().tag()) {
                throw new IllegalArgumentException("Starter biome must be one biome ID, not a tag.");
            }
            starterBiome = spec.entries().getFirst().id();
        }

        if (starterRadiusBlocks < WorldzConfig.MIN_STARTER_RADIUS_BLOCKS
            || starterRadiusBlocks > WorldzConfig.MAX_STARTER_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Starter radius must be between " + WorldzConfig.MIN_STARTER_RADIUS_BLOCKS
                    + " and " + WorldzConfig.MAX_STARTER_RADIUS_BLOCKS + "."
            );
        }
        if (spawnStrategy == null || overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Spawn strategy, border, and exterior settings are required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static ChaosBiomesCustomization fromConfig(WorldzConfig config) {
        return new ChaosBiomesCustomization(
            config.chaosBiomes.biomes,
            config.chaosBiomes.regionScaleBlocks,
            config.chaosBiomes.starterBiome,
            config.chaosBiomes.starterRadiusBlocks,
            config.chaosBiomes.spawn.strategy,
            config.chaosBiomes.allowRivers,
            config.chaosBiomes.allowOceans,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.overworldExterior),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text fields into validated customization values.
     *
     * @param biomesText newline- or comma-separated weighted biome entries
     * @param regionScaleBlocks decimal region scale
     * @param starterBiome optional biome forced around spawn, or empty
     * @param starterRadiusBlocks decimal starter radius
     * @param spawnStrategy layout-origin and spawn strategy
     * @param allowRivers let vanilla's own river biomes generate naturally
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static ChaosBiomesCustomization fromText(
        String biomesText,
        String regionScaleBlocks,
        String starterBiome,
        String starterRadiusBlocks,
        SpawnStrategy spawnStrategy,
        boolean allowRivers,
        boolean allowOceans,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        List<String> biomes = Arrays.stream(biomesText.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new ChaosBiomesCustomization(
            biomes,
            parseInteger(regionScaleBlocks, "Region scale"),
            starterBiome,
            parseInteger(starterRadiusBlocks, "Starter radius"),
            spawnStrategy,
            allowRivers,
            allowOceans,
            overworldBorder,
            netherBorder,
            endBorder,
            overworldExterior,
            netherExterior
        );
    }

    /**
     * Renders weighted biome entries one per line for the multi-line editor.
     *
     * @return newline-separated canonical values
     */
    public String biomesText() {
        return String.join("\n", biomes);
    }

    /**
     * Returns the resolved {@code allowedBiomes} set: every configured biome, plus the
     * starter biome when set (DESIGN §20.11: never user-edited directly beyond this list,
     * matching {@code single_biome}'s auto-derivation).
     *
     * @return canonical biome ids, in a stable order
     */
    public List<String> allowedBiomeIds() {
        List<String> ids = new ArrayList<>();
        for (String entry : biomes) {
            int at = entry.lastIndexOf('@');
            ids.add(at < 0 ? entry : entry.substring(0, at));
        }
        if (!starterBiome.isEmpty() && !ids.contains(starterBiome)) {
            ids.add(starterBiome);
        }
        return ids;
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
     * Converts both exterior selections to resolved, persisted envelopes.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(overworldExterior.toPlan(overworldBorder), netherExterior.toPlan(netherBorder));
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }
}
