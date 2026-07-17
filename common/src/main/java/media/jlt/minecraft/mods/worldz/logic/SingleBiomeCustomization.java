package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

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
 */
public record SingleBiomeCustomization(
    String landBiome,
    String starterBiome,
    int starterRadiusBlocks,
    SpawnStrategy spawnStrategy
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
        if (spawnStrategy == null) {
            throw new IllegalArgumentException("Spawn strategy is required.");
        }
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
            config.singleBiome.spawn.strategy
        );
    }

    /**
     * Parses client text fields into validated customization values.
     *
     * @param landBiome the one biome that fills the world
     * @param starterBiome optional different biome forced around spawn, or empty
     * @param starterRadiusBlocks decimal starter radius
     * @param spawnStrategy layout-origin and spawn strategy
     * @return canonical immutable customization values
     */
    public static SingleBiomeCustomization fromText(
        String landBiome,
        String starterBiome,
        String starterRadiusBlocks,
        SpawnStrategy spawnStrategy
    ) {
        return new SingleBiomeCustomization(landBiome, starterBiome, parseInteger(starterRadiusBlocks), spawnStrategy);
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
