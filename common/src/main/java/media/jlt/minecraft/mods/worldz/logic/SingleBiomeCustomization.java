package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:single_biome}
 * world (DESIGN §20.2's Phase 2.1 subsection): the small per-type field set, not the
 * full generic Customize screen's.
 *
 * @param landBiome the one biome that fills the world (GOALS 10)
 * @param starterBiome optional different biome forced around spawn, or empty (GOALS 11)
 * @param starterRadiusBlocks starter-zone radius, meaningful only when {@code starterBiome} is set
 * @param spawnStrategy layout-origin and initial-spawn strategy (GOALS 12 uses {@code preferred_natural_biome})
 * @param allowRivers let vanilla's own river biomes generate naturally (GOALS 13)
 * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally (GOALS 14, additive over {@code allowRivers})
 * @param overworldBorder overworld border selection (GOALS 17-20, TODO 5.3)
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record SingleBiomeCustomization(
    String landBiome,
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
    public SingleBiomeCustomization {
        landBiome = requireSingleBiomeId(landBiome, "Land biome");

        starterBiome = starterBiome == null ? "" : starterBiome.trim();
        if (!starterBiome.isEmpty()) {
            starterBiome = requireSingleBiomeId(starterBiome, "Starter biome");
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
    public static SingleBiomeCustomization fromConfig(WorldzConfig config) {
        return new SingleBiomeCustomization(
            config.singleBiome.landBiome,
            config.singleBiome.starterBiome,
            config.singleBiome.starterRadiusBlocks,
            config.singleBiome.spawn.strategy,
            config.singleBiome.allowRivers,
            config.singleBiome.allowOceans,
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
     * @param landBiome the one biome that fills the world
     * @param starterBiome optional different biome forced around spawn, or empty
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
    public static SingleBiomeCustomization fromText(
        String landBiome,
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
        return new SingleBiomeCustomization(
            landBiome, starterBiome, parseInteger(starterRadiusBlocks), spawnStrategy, allowRivers, allowOceans,
            overworldBorder, netherBorder, endBorder, overworldExterior, netherExterior
        );
    }

    /**
     * Returns the auto-derived {@code allowedBiomes} set: the land biome, plus the
     * starter biome when it differs (DESIGN §20.2: never user-edited directly for
     * this type, unlike the generic preset's {@code allowedBiomes} field).
     *
     * @return canonical biome ids, in a stable order
     */
    public List<String> allowedBiomeIds() {
        List<String> ids = new ArrayList<>();
        ids.add(landBiome);
        if (!starterBiome.isEmpty() && !starterBiome.equals(landBiome)) {
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

    private static String requireSingleBiomeId(String value, String name) {
        BiomeListSpec spec = BiomeListSpec.parse(List.of(value == null ? "" : value));
        if (spec.entries().size() != 1 || spec.entries().getFirst().tag()) {
            throw new IllegalArgumentException(name + " must be one biome ID, not a tag.");
        }
        return spec.entries().getFirst().id();
    }

    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException("Starter radius must be a whole number.", exception);
        }
    }
}
