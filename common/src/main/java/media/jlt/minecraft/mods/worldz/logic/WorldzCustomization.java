package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.ExteriorConfig;
import media.jlt.minecraft.mods.worldz.config.LayoutConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, loader-neutral values selected for one new Worldz world.
 *
 * @param allowedBiomes biome ids and biome-tag ids
 * @param starterBiome optional direct biome id
 * @param starterRadiusBlocks starter-zone radius
 * @param starterLandPlan starter terrain guarantee
 * @param overworldBorder overworld border selection
 * @param netherBorder Nether border selection
 * @param overworldExterior Overworld exterior-terrain selection
 * @param netherExterior Nether exterior-terrain selection
 * @param worldLayout coordinated world-layout selection
 * @param spawnStrategy layout-origin and initial-spawn strategy
 */
public record WorldzCustomization(
    List<String> allowedBiomes,
    String starterBiome,
    int starterRadiusBlocks,
    StarterLandPlan starterLandPlan,
    BorderSettings overworldBorder,
    BorderSettings netherBorder,
    ExteriorSettings overworldExterior,
    ExteriorSettings netherExterior,
    LayoutSettings worldLayout,
    SpawnStrategy spawnStrategy
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
        if (starterLandPlan == null || overworldBorder == null || netherBorder == null
            || overworldExterior == null || netherExterior == null || worldLayout == null || spawnStrategy == null) {
            throw new IllegalArgumentException("Starter-land, border, exterior, layout, and spawn settings are required.");
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
            StarterLandPlan.disabled(),
            overworldBorder,
            netherBorder,
            ExteriorSettings.normal(),
            ExteriorSettings.normal(),
            LayoutSettings.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN
        );
    }

    /**
     * Creates customization values with an explicit exterior and a disabled starter-land compatibility plan.
     *
     * @param allowedBiomes biome ids and biome-tag ids
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks starter-zone radius
     * @param overworldBorder overworld border selection
     * @param netherBorder Nether border selection
     * @param overworldExterior Overworld exterior selection
     * @param netherExterior Nether exterior selection
     */
    public WorldzCustomization(
        List<String> allowedBiomes,
        String starterBiome,
        int starterRadiusBlocks,
        BorderSettings overworldBorder,
        BorderSettings netherBorder,
        ExteriorSettings overworldExterior,
        ExteriorSettings netherExterior
    ) {
        this(
            allowedBiomes,
            starterBiome,
            starterRadiusBlocks,
            StarterLandPlan.disabled(),
            overworldBorder,
            netherBorder,
            overworldExterior,
            netherExterior,
            LayoutSettings.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN
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
            StarterLandPlan.fromConfig(config),
            BorderSettings.fromConfig(config.overworldBorder),
            BorderSettings.fromConfig(config.netherBorder),
            ExteriorSettings.fromConfig(config.overworldExterior),
            ExteriorSettings.fromConfig(config.netherExterior),
            LayoutSettings.fromConfig(config),
            config.spawn.strategy
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
            StarterLandPlan.disabled(),
            overworldBorder,
            netherBorder,
            overworldExterior,
            netherExterior,
            LayoutSettings.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN
        );
    }

    /**
     * Parses editable biome fields while preserving an explicit starter-land plan.
     *
     * @param allowedBiomes newline- or comma-separated biome ids and tags
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks decimal starter radius
     * @param starterLandPlan validated starter-land values
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
        StarterLandPlan starterLandPlan,
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
            starterLandPlan,
            overworldBorder,
            netherBorder,
            overworldExterior,
            netherExterior,
            LayoutSettings.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN
        );
    }

    /**
     * Parses editable biome fields while preserving explicit starter-land and layout plans.
     *
     * @param allowedBiomes newline- or comma-separated biome ids and tags
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks decimal starter radius
     * @param starterLandPlan validated starter-land values
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @param worldLayout validated layout values
     * @return canonical immutable customization values
     */
    public static WorldzCustomization fromText(
        String allowedBiomes,
        String starterBiome,
        String starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        BorderSettings overworldBorder,
        BorderSettings netherBorder,
        ExteriorSettings overworldExterior,
        ExteriorSettings netherExterior,
        LayoutSettings worldLayout
    ) {
        List<String> allowed = Arrays.stream(allowedBiomes.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new WorldzCustomization(
            allowed,
            starterBiome,
            parseInteger(starterRadiusBlocks, "Starter radius"),
            starterLandPlan,
            overworldBorder,
            netherBorder,
            overworldExterior,
            netherExterior,
            worldLayout,
            SpawnStrategy.STARTER_AT_ORIGIN
        );
    }

    /**
     * Parses editable biome fields while preserving explicit starter-land, layout, and spawn settings.
     *
     * @param allowedBiomes newline- or comma-separated biome ids and tags
     * @param starterBiome optional direct biome id
     * @param starterRadiusBlocks decimal starter radius
     * @param starterLandPlan validated starter-land values
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @param worldLayout validated layout values
     * @param spawnStrategy layout-origin and spawn strategy
     * @return canonical immutable customization values
     */
    public static WorldzCustomization fromText(
        String allowedBiomes,
        String starterBiome,
        String starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        BorderSettings overworldBorder,
        BorderSettings netherBorder,
        ExteriorSettings overworldExterior,
        ExteriorSettings netherExterior,
        LayoutSettings worldLayout,
        SpawnStrategy spawnStrategy
    ) {
        List<String> allowed = Arrays.stream(allowedBiomes.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new WorldzCustomization(
            allowed,
            starterBiome,
            parseInteger(starterRadiusBlocks, "Starter radius"),
            starterLandPlan,
            overworldBorder,
            netherBorder,
            overworldExterior,
            netherExterior,
            worldLayout,
            spawnStrategy
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
     * Resolves the layout selection into a persisted plan with a caller-supplied seed.
     * A fresh seed should be generated once, at world-creation time.
     *
     * @param seed sampling seed (see {@link WorldLayoutPlan#seed()})
     * @return immutable resolved layout plan
     */
    public WorldLayoutPlan worldLayoutPlan(long seed) {
        return worldLayout.toPlan(seed);
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

    /**
     * Editable coordinated world-layout values.
     *
     * @param mode layout mode
     * @param biomes weighted {@code id}/{@code id@weight} entries; tags are not accepted
     * @param regionScaleBlocks grid-cell edge length in blocks
     * @param singleBiome {@code SINGLE_BIOME} biome id, or empty when unused
     * @param roleOverrides biome id to role-name overrides
     */
    public record LayoutSettings(
        LayoutMode mode,
        List<String> biomes,
        int regionScaleBlocks,
        String singleBiome,
        Map<String, String> roleOverrides
    ) {
        /** Validates and canonicalizes editable layout values. */
        public LayoutSettings {
            if (mode == null) {
                throw new IllegalArgumentException("Layout mode is required.");
            }
            WeightedBiomeListSpec biomeSpec = WeightedBiomeListSpec.parse(biomes);
            if (!biomeSpec.invalidEntries().isEmpty()) {
                throw new IllegalArgumentException("Invalid layout biome: " + biomeSpec.invalidEntries().getFirst());
            }
            biomes = biomeSpec.entries().stream().map(WeightedBiomeListSpec.Entry::configValue).toList();

            singleBiome = singleBiome == null ? "" : singleBiome.trim();
            if (!singleBiome.isEmpty()) {
                BiomeListSpec singleSpec = BiomeListSpec.parse(List.of(singleBiome));
                if (singleSpec.entries().size() != 1 || singleSpec.entries().getFirst().tag()) {
                    throw new IllegalArgumentException("Single biome must be one biome ID, not a tag.");
                }
                singleBiome = singleSpec.entries().getFirst().id();
            }

            Map<String, String> canonicalOverrides = new LinkedHashMap<>();
            if (roleOverrides != null) {
                roleOverrides.forEach((rawId, rawRole) -> {
                    BiomeListSpec idSpec = BiomeListSpec.parse(List.of(rawId == null ? "" : rawId));
                    if (idSpec.entries().size() != 1 || idSpec.entries().getFirst().tag()) {
                        throw new IllegalArgumentException("Role override id must be one biome ID, not a tag: " + rawId);
                    }
                    BiomeRole role = BiomeRole.parse(rawRole);
                    canonicalOverrides.put(idSpec.entries().getFirst().id(), role.serializedName());
                });
            }
            roleOverrides = canonicalOverrides;

            requireRange(
                regionScaleBlocks,
                WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS,
                WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS,
                "Region scale"
            );
        }

        /**
         * Returns the backward-compatible legacy default (today's climate-filter-only behavior).
         *
         * @return disabled layout selection
         */
        public static LayoutSettings legacy() {
            return new LayoutSettings(
                LayoutMode.LEGACY,
                List.of(),
                WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS,
                "",
                Map.of()
            );
        }

        /**
         * Copies sanitized YAML values.
         *
         * @param config sanitized startup configuration
         * @return immutable editable values
         */
        public static LayoutSettings fromConfig(WorldzConfig config) {
            LayoutConfig layout = config.layout;
            return new LayoutSettings(
                layout.mode,
                layout.biomes,
                layout.regionScaleBlocks,
                layout.singleBiome,
                layout.roleOverrides
            );
        }

        /**
         * Parses client text fields into validated layout values.
         *
         * @param mode serialized layout mode name
         * @param biomesText newline- or comma-separated weighted biome entries
         * @param regionScaleBlocks decimal region scale
         * @param singleBiome single-biome-mode biome id, or empty
         * @param roleOverridesText newline- or comma-separated {@code id=role} entries
         * @return validated immutable layout values
         */
        public static LayoutSettings fromText(
            String mode,
            String biomesText,
            String regionScaleBlocks,
            String singleBiome,
            String roleOverridesText
        ) {
            List<String> biomes = Arrays.stream(biomesText.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
            Map<String, String> overrides = new LinkedHashMap<>();
            for (String line : roleOverridesText.split("[,\\r\\n]+")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int at = trimmed.indexOf('=');
                if (at < 0) {
                    throw new IllegalArgumentException("Role override '" + trimmed + "' must be formatted as id=role.");
                }
                overrides.put(trimmed.substring(0, at).trim(), trimmed.substring(at + 1).trim());
            }
            return new LayoutSettings(
                LayoutMode.parse(mode),
                biomes,
                parseInteger(regionScaleBlocks, "Region scale"),
                singleBiome,
                overrides
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
         * Renders role overrides one per line for the multi-line editor.
         *
         * @return newline-separated {@code id=role} entries
         */
        public String roleOverridesText() {
            List<String> lines = new ArrayList<>();
            roleOverrides.forEach((id, role) -> lines.add(id + "=" + role));
            return String.join("\n", lines);
        }

        private WorldLayoutPlan toPlan(long seed) {
            return WorldLayoutPlan.resolve(mode, biomes, roleOverrides, regionScaleBlocks, singleBiome, seed);
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
