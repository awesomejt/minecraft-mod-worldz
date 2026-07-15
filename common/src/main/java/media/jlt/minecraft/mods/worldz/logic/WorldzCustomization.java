package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new Worldz world.
 *
 * @param allowedBiomes biome ids and biome-tag ids
 * @param starterBiome optional direct biome id
 * @param starterRadiusBlocks starter-zone radius
 * @param overworldBorder overworld border selection
 * @param netherBorder Nether border selection
 */
public record WorldzCustomization(
    List<String> allowedBiomes,
    String starterBiome,
    int starterRadiusBlocks,
    BorderSettings overworldBorder,
    BorderSettings netherBorder
) {
    /** Validates and snapshots customization values. */
    public WorldzCustomization {
        BiomeListSpec allowed = BiomeListSpec.parse(allowedBiomes);
        if (!allowed.invalidEntries().isEmpty()) {
            throw new IllegalArgumentException("Invalid allowed biome or tag: " + allowed.invalidEntries().getFirst());
        }
        allowedBiomes = allowed.entries().stream().map(BiomeListSpec.Entry::configValue).toList();

        starterBiome = starterBiome == null ? "" : starterBiome.trim();
        if (!starterBiome.isEmpty()) {
            BiomeListSpec starter = BiomeListSpec.parse(List.of(starterBiome));
            if (starter.entries().size() != 1 || starter.entries().getFirst().tag()) {
                throw new IllegalArgumentException("Starter biome must be one biome ID, not a tag.");
            }
            starterBiome = starter.entries().getFirst().id();
        }

        requireRange(
            starterRadiusBlocks,
            WorldzConfig.MIN_STARTER_RADIUS_BLOCKS,
            WorldzConfig.MAX_STARTER_RADIUS_BLOCKS,
            "Starter radius"
        );
        if (overworldBorder == null || netherBorder == null) {
            throw new IllegalArgumentException("Border settings are required.");
        }
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static WorldzCustomization fromConfig(WorldzConfig config) {
        return new WorldzCustomization(
            config.allowedBiomes,
            config.starterBiome,
            config.starterRadiusBlocks,
            BorderSettings.fromConfig(config.overworldBorder),
            BorderSettings.fromConfig(config.netherBorder)
        );
    }

    /**
     * Parses the editable biome list and starter radius used by the client screen.
     *
     * @param allowedBiomes newline- or comma-separated biome ids and tags
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks decimal starter radius
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @return canonical immutable customization values
     */
    public static WorldzCustomization fromText(
        String allowedBiomes,
        String starterBiome,
        String starterRadiusBlocks,
        BorderSettings overworldBorder,
        BorderSettings netherBorder
    ) {
        List<String> allowed = Arrays.stream(allowedBiomes.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new WorldzCustomization(
            allowed,
            starterBiome,
            parseInteger(starterRadiusBlocks, "Starter radius"),
            overworldBorder,
            netherBorder
        );
    }

    /**
     * Renders allowed values one per line for the multi-line editor.
     *
     * @return newline-separated canonical values
     */
    public String allowedBiomesText() {
        return String.join("\n", allowedBiomes);
    }

    /**
     * Converts both border selections to the world-persisted plan.
     *
     * @return immutable codec-backed world-limit plan
     */
    public WorldLimitPlan worldLimitPlan() {
        return new WorldLimitPlan(overworldBorder.toPlan(), netherBorder.toPlan());
    }

    /**
     * One dimension's editable world-border values.
     *
     * @param enabled whether this dimension is limited
     * @param initialRadiusBlocks border half-width at creation
     * @param finalRadiusBlocks border half-width after resizing
     * @param resizeDays transition duration in Minecraft days
     * @param ensureObjective whether progression access is guaranteed
     */
    public record BorderSettings(
        boolean enabled,
        int initialRadiusBlocks,
        int finalRadiusBlocks,
        int resizeDays,
        boolean ensureObjective
    ) {
        /** Validates border values even while the border is disabled. */
        public BorderSettings {
            requireRange(
                initialRadiusBlocks,
                WorldzConfig.MIN_BORDER_RADIUS_BLOCKS,
                WorldzConfig.MAX_BORDER_RADIUS_BLOCKS,
                "Initial border radius"
            );
            requireRange(
                finalRadiusBlocks,
                WorldzConfig.MIN_BORDER_RADIUS_BLOCKS,
                WorldzConfig.MAX_BORDER_RADIUS_BLOCKS,
                "Final border radius"
            );
            requireRange(resizeDays, 0, WorldzConfig.MAX_BORDER_RESIZE_DAYS, "Resize days");
        }

        /**
         * Copies sanitized YAML values.
         *
         * @param config sanitized dimension border configuration
         * @return immutable border values
         */
        public static BorderSettings fromConfig(BorderConfig config) {
            return new BorderSettings(
                config.enabled,
                config.initialRadiusBlocks,
                config.finalRadiusBlocks,
                config.resizeDays,
                config.ensureObjective
            );
        }

        /**
         * Parses the three numeric border fields used by the client screen.
         *
         * @param enabled whether this dimension is limited
         * @param initialRadiusBlocks decimal initial radius
         * @param finalRadiusBlocks decimal final radius
         * @param resizeDays decimal transition duration
         * @param ensureObjective whether progression access is guaranteed
         * @return validated immutable border values
         */
        public static BorderSettings fromText(
            boolean enabled,
            String initialRadiusBlocks,
            String finalRadiusBlocks,
            String resizeDays,
            boolean ensureObjective
        ) {
            return new BorderSettings(
                enabled,
                parseInteger(initialRadiusBlocks, "Initial border radius"),
                parseInteger(finalRadiusBlocks, "Final border radius"),
                parseInteger(resizeDays, "Resize days"),
                ensureObjective
            );
        }

        private WorldLimitPlan.DimensionLimit toPlan() {
            return new WorldLimitPlan.DimensionLimit(
                enabled,
                initialRadiusBlocks,
                finalRadiusBlocks,
                resizeDays,
                ensureObjective
            );
        }
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum + ".");
        }
    }
}
