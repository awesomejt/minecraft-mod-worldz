package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.ExteriorConfig;
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
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 */
public record WorldzCustomization(
    List<String> allowedBiomes,
    String starterBiome,
    int starterRadiusBlocks,
    BorderSettings overworldBorder,
    BorderSettings netherBorder,
    ExteriorSettings overworldExterior,
    ExteriorSettings netherExterior
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
        if (overworldBorder == null || netherBorder == null || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
        validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates customization values with backward-compatible normal exteriors.
     *
     * @param allowedBiomes biome ids and biome-tag ids
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks starter-zone radius
     * @param overworldBorder overworld border selection
     * @param netherBorder Nether border selection
     */
    public WorldzCustomization(
        List<String> allowedBiomes,
        String starterBiome,
        int starterRadiusBlocks,
        BorderSettings overworldBorder,
        BorderSettings netherBorder
    ) {
        this(
            allowedBiomes,
            starterBiome,
            starterRadiusBlocks,
            overworldBorder,
            netherBorder,
            ExteriorSettings.normal(),
            ExteriorSettings.normal()
        );
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
            BorderSettings.fromConfig(config.netherBorder),
            ExteriorSettings.fromConfig(config.overworldExterior),
            ExteriorSettings.fromConfig(config.netherExterior)
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
        return fromText(
            allowedBiomes,
            starterBiome,
            starterRadiusBlocks,
            overworldBorder,
            netherBorder,
            ExteriorSettings.normal(),
            ExteriorSettings.normal()
        );
    }

    /**
     * Parses the editable biome fields while retaining dimension settings.
     *
     * @param allowedBiomes newline- or comma-separated biome ids and tags
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks decimal starter radius
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static WorldzCustomization fromText(
        String allowedBiomes,
        String starterBiome,
        String starterRadiusBlocks,
        BorderSettings overworldBorder,
        BorderSettings netherBorder,
        ExteriorSettings overworldExterior,
        ExteriorSettings netherExterior
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
            netherBorder,
            overworldExterior,
            netherExterior
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
     * Converts both exterior selections to resolved, persisted envelopes.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(
            overworldExterior.toPlan(overworldBorder),
            netherExterior.toPlan(netherBorder)
        );
    }

    /**
     * One dimension's editable world-border values.
     *
     * @param enabled whether this dimension is limited
     * @param initialRadiusBlocks border half-width at creation
     * @param finalRadiusBlocks border half-width after resizing
     * @param resizeDays transition duration in Minecraft days
     * @param resizeDelayDays wait before resizing begins
     * @param resizeRateBlocks radius blocks traversed per rate interval
     * @param resizeRateDays days per rate interval
     * @param ensureObjective whether progression access is guaranteed
     */
    public record BorderSettings(
        boolean enabled,
        int initialRadiusBlocks,
        int finalRadiusBlocks,
        int resizeDays,
        int resizeDelayDays,
        int resizeRateBlocks,
        int resizeRateDays,
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
            requireRange(resizeDelayDays, 0, WorldzConfig.MAX_BORDER_RESIZE_DAYS, "Resize delay days");
            requireRange(resizeRateBlocks, 0, WorldzConfig.MAX_BORDER_RATE_BLOCKS, "Resize rate blocks");
            requireRange(resizeRateDays, 0, WorldzConfig.MAX_BORDER_RESIZE_DAYS, "Resize rate days");
            if ((resizeRateBlocks == 0) != (resizeRateDays == 0)) {
                throw new IllegalArgumentException("Both resize rate fields must be zero or positive.");
            }
        }

        /**
         * Creates legacy total-duration values without a resize rate.
         *
         * @param enabled whether this dimension is limited
         * @param initialRadiusBlocks border half-width at creation
         * @param finalRadiusBlocks border half-width after resizing
         * @param resizeDays total transition duration in Minecraft days
         * @param ensureObjective whether progression access is guaranteed
         */
        public BorderSettings(
            boolean enabled,
            int initialRadiusBlocks,
            int finalRadiusBlocks,
            int resizeDays,
            boolean ensureObjective
        ) {
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, 0, 0, ensureObjective);
        }

        /**
         * Creates rate-based values without an initial delay.
         *
         * @param enabled whether this dimension is limited
         * @param initialRadiusBlocks border half-width at creation
         * @param finalRadiusBlocks border half-width after resizing
         * @param resizeDays legacy total transition duration
         * @param resizeRateBlocks radius blocks per interval
         * @param resizeRateDays Minecraft days per interval
         * @param ensureObjective whether progression access is guaranteed
         */
        public BorderSettings(
            boolean enabled,
            int initialRadiusBlocks,
            int finalRadiusBlocks,
            int resizeDays,
            int resizeRateBlocks,
            int resizeRateDays,
            boolean ensureObjective
        ) {
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, resizeRateBlocks, resizeRateDays, ensureObjective);
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
                config.resizeDelayDays,
                config.resizeRateBlocks,
                config.resizeRateDays,
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
            return fromText(
                enabled,
                initialRadiusBlocks,
                finalRadiusBlocks,
                resizeDays,
                "0",
                "0",
                "0",
                ensureObjective
            );
        }

        /**
         * Parses total-duration and optional rate fields from the client screen.
         *
         * @param enabled whether this dimension is limited
         * @param initialRadiusBlocks decimal initial radius
         * @param finalRadiusBlocks decimal final radius
         * @param resizeDays decimal total transition duration
         * @param resizeRateBlocks decimal radius blocks per interval
         * @param resizeRateDays decimal Minecraft days per interval
         * @param ensureObjective whether progression access is guaranteed
         * @return validated immutable border values
         */
        public static BorderSettings fromText(
            boolean enabled,
            String initialRadiusBlocks,
            String finalRadiusBlocks,
            String resizeDays,
            String resizeRateBlocks,
            String resizeRateDays,
            boolean ensureObjective
        ) {
            return fromText(
                enabled,
                initialRadiusBlocks,
                finalRadiusBlocks,
                resizeDays,
                "0",
                resizeRateBlocks,
                resizeRateDays,
                ensureObjective
            );
        }

        /**
         * Parses total-duration, initial delay, and optional rate fields.
         *
         * @param enabled whether this dimension is limited
         * @param initialRadiusBlocks decimal initial radius
         * @param finalRadiusBlocks decimal final radius
         * @param resizeDays decimal total transition duration
         * @param resizeDelayDays decimal wait before resizing
         * @param resizeRateBlocks decimal radius blocks per interval
         * @param resizeRateDays decimal Minecraft days per interval
         * @param ensureObjective whether progression access is guaranteed
         * @return validated immutable border values
         */
        public static BorderSettings fromText(
            boolean enabled,
            String initialRadiusBlocks,
            String finalRadiusBlocks,
            String resizeDays,
            String resizeDelayDays,
            String resizeRateBlocks,
            String resizeRateDays,
            boolean ensureObjective
        ) {
            return new BorderSettings(
                enabled,
                parseInteger(initialRadiusBlocks, "Initial border radius"),
                parseInteger(finalRadiusBlocks, "Final border radius"),
                parseInteger(resizeDays, "Resize days"),
                parseInteger(resizeDelayDays, "Resize delay days"),
                parseInteger(resizeRateBlocks, "Resize rate blocks"),
                parseInteger(resizeRateDays, "Resize rate days"),
                ensureObjective
            );
        }

        private WorldLimitPlan.DimensionLimit toPlan() {
            return new WorldLimitPlan.DimensionLimit(
                enabled,
                initialRadiusBlocks,
                finalRadiusBlocks,
                resizeDays,
                resizeDelayDays,
                resizeRateBlocks,
                resizeRateDays,
                ensureObjective
            );
        }
    }

    /**
     * One dimension's editable exterior values.
     *
     * @param mode terrain outside the central square
     * @param boundaryRadiusBlocks explicit outer radius, or zero for border-derived
     * @param oceanTransitionWidthBlocks ocean width inside the outer radius
     */
    public record ExteriorSettings(ExteriorMode mode, int boundaryRadiusBlocks, int oceanTransitionWidthBlocks) {
        /** Validates editable exterior ranges. */
        public ExteriorSettings {
            if (mode == null) {
                throw new IllegalArgumentException("Exterior mode is required.");
            }
            requireRange(boundaryRadiusBlocks, 0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS, "Exterior boundary");
            requireRange(
                oceanTransitionWidthBlocks,
                0,
                WorldzConfig.MAX_BORDER_RADIUS_BLOCKS,
                "Ocean transition width"
            );
        }

        /**
         * Returns normal delegated terrain.
         *
         * @return normal editable exterior values
         */
        public static ExteriorSettings normal() {
            return new ExteriorSettings(ExteriorMode.NORMAL, 0, 0);
        }

        /**
         * Copies sanitized YAML values without resolving an automatic boundary.
         *
         * @param config sanitized exterior configuration
         * @return immutable editable values
         */
        public static ExteriorSettings fromConfig(ExteriorConfig config) {
            return new ExteriorSettings(config.mode, config.boundaryRadiusBlocks, config.oceanTransitionWidthBlocks);
        }

        /**
         * Parses a client mode, explicit/auto boundary, and ocean transition.
         *
         * @param mode normal, ocean, or void
         * @param boundaryRadiusBlocks decimal radius or {@code auto}
         * @param oceanTransitionWidthBlocks decimal transition width
         * @return validated immutable exterior values
         */
        public static ExteriorSettings fromText(String mode, String boundaryRadiusBlocks, String oceanTransitionWidthBlocks) {
            int boundary = boundaryRadiusBlocks.trim().equalsIgnoreCase("auto")
                ? 0
                : parseInteger(boundaryRadiusBlocks, "Exterior boundary");
            return new ExteriorSettings(
                ExteriorMode.parse(mode),
                boundary,
                parseInteger(oceanTransitionWidthBlocks, "Ocean transition width")
            );
        }

        private ExteriorPlan.DimensionEnvelope toPlan(BorderSettings border) {
            if (mode == ExteriorMode.NORMAL) {
                return ExteriorPlan.DimensionEnvelope.normal();
            }
            int resolvedBoundary = boundaryRadiusBlocks == 0
                ? Math.max(border.initialRadiusBlocks, border.finalRadiusBlocks)
                : boundaryRadiusBlocks;
            return new ExteriorPlan.DimensionEnvelope(mode, resolvedBoundary, oceanTransitionWidthBlocks);
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

    private static void validateAutomaticBoundary(ExteriorSettings exterior, BorderSettings border, String dimension) {
        if (exterior.mode() != ExteriorMode.NORMAL && exterior.boundaryRadiusBlocks() == 0 && !border.enabled()) {
            throw new IllegalArgumentException(dimension + " exterior needs a boundary or an enabled border.");
        }
    }
}
