package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.ParseContext;
import media.jlt.minecraft.mods.worldz.config.schema.SanitizeContext;
import media.jlt.minecraft.mods.worldz.config.schema.SectionCodec;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Thin {@link SectionCodec} adapters onto the existing, unmodified {@code readXConfig}/{@code
 * sanitizeX}/{@code xMap}/{@code xSummary} method quads on {@link WorldzConfig} (DESIGN §41.8).
 * Every section not yet converted to a declarative {@code SchemaSection} gets one of these, so
 * production code (routed through {@link SchemaSections}) never needs to know which sections are
 * schema-driven and which are still legacy -- that indifference is what makes the differential
 * harness ({@code ConfigSchemaDifferentialTest}) possible.
 *
 * <p>Nothing here touches the wrapped methods' logic; only visibility was widened (from {@code
 * private static} to package-private {@code static}) so this class, in the same package, can
 * call them.
 */
public final class LegacySections {
    private LegacySections() {
    }

    public static SectionCodec<BorderConfig> border(String name, String objectiveKey, String summaryObjectiveName) {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readBorderConfig(raw, name, objectiveKey),
            (value, ctx) -> WorldzConfig.sanitizeBorder(value, name, ctx.logger()),
            value -> WorldzConfig.borderMap(value, objectiveKey),
            value -> WorldzConfig.borderSummary(value, summaryObjectiveName)
        );
    }

    public static SectionCodec<EndBorderConfig> endBorder() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readEndBorderConfig(raw, "endBorder"),
            (value, ctx) -> WorldzConfig.sanitizeEndBorder(value, ctx.logger()),
            WorldzConfig::endBorderMap,
            WorldzConfig::endBorderSummary
        );
    }

    public static SectionCodec<ExteriorConfig> exterior(String name, Function<WorldzConfig, BorderConfig> border, boolean oceanAllowed) {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readExteriorConfig(raw, name),
            (value, ctx) -> WorldzConfig.sanitizeExterior(value, border.apply(ctx.root()), oceanAllowed, name, ctx.logger()),
            WorldzConfig::exteriorMap,
            WorldzConfig::exteriorSummary
        );
    }

    public static SectionCodec<StripConfig> strip() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readStripConfig(raw, "strip"),
            (value, ctx) -> WorldzConfig.sanitizeStrip(value, ctx.logger()),
            WorldzConfig::stripMap,
            WorldzConfig::stripSummary
        );
    }

    public static SectionCodec<LayoutConfig> layout() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readLayoutConfig(raw, "layout", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeLayout(value, ctx.logger()),
            WorldzConfig::layoutMap,
            WorldzConfig::layoutSummary
        );
    }

    public static SectionCodec<SingleBiomeConfig> singleBiome() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readSingleBiomeConfig(raw, "singleBiome"),
            (value, ctx) -> WorldzConfig.sanitizeSingleBiome(value, ctx.logger()),
            WorldzConfig::singleBiomeMap,
            WorldzConfig::singleBiomeSummary
        );
    }

    public static SectionCodec<ChaosBiomesConfig> chaosBiomes() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readChaosBiomesConfig(raw, "chaosBiomes", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeChaosBiomes(value, ctx.logger()),
            WorldzConfig::chaosBiomesMap,
            WorldzConfig::chaosBiomesSummary
        );
    }

    public static SectionCodec<StripWorldConfig> stripWorld() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readStripWorldConfig(raw, "stripWorld", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeStripWorld(value, ctx.logger()),
            WorldzConfig::stripWorldMap,
            WorldzConfig::stripWorldSummary
        );
    }

    /**
     * {@code stripWorld.bands} is only reachable through {@link #stripWorld()} in production, but
     * (like {@link #spawn}/{@link #starterKit}) gets its own adapter so the differential harness
     * can exercise it directly. {@code sanitizeStripBands} takes no {@code name} parameter -- its
     * one real call site always means {@code "stripWorld.bands"}, which is hardcoded here to match.
     */
    public static SectionCodec<StripBandsConfig> stripBands() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readStripBandsConfig(raw, "stripWorld.bands", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeStripBands(value, ctx.logger()),
            WorldzConfig::stripBandsMap,
            WorldzConfig::stripBandsSummary
        );
    }

    public static SectionCodec<OceanIslandConfig> oceanIsland() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readOceanIslandConfig(raw, "oceanIsland", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeOceanIsland(value, ctx.logger()),
            WorldzConfig::oceanIslandMap,
            WorldzConfig::oceanIslandSummary
        );
    }

    public static SectionCodec<SkyIslandConfig> skyIsland() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readSkyIslandConfig(raw, "skyIsland", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeSkyIsland(value, ctx.logger()),
            WorldzConfig::skyIslandMap,
            WorldzConfig::skyIslandSummary
        );
    }

    public static SectionCodec<ChunkIslandConfig> chunkIsland() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readChunkIslandConfig(raw, "chunkIsland", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeChunkIsland(value, ctx.logger()),
            WorldzConfig::chunkIslandMap,
            WorldzConfig::chunkIslandSummary
        );
    }

    public static SectionCodec<CaveConfig> cave() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readCaveConfig(raw, "cave", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeCave(value, ctx.logger()),
            WorldzConfig::caveMap,
            WorldzConfig::caveSummary
        );
    }

    public static SectionCodec<NetherStartConfig> netherStart() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readNetherStartConfig(raw, "netherStart", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeNetherStart(value, ctx.logger()),
            WorldzConfig::netherStartMap,
            WorldzConfig::netherStartSummary
        );
    }

    public static SectionCodec<EndStartConfig> endStart() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readEndStartConfig(raw, "endStart", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeEndStart(value, ctx.logger()),
            WorldzConfig::endStartMap,
            WorldzConfig::endStartSummary
        );
    }

    public static SectionCodec<FlatConfig> flat() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readFlatConfig(raw, "flat", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeFlat(value, ctx.logger()),
            WorldzConfig::flatMap,
            WorldzConfig::flatSummary
        );
    }

    public static SectionCodec<DeepFlatConfig> deepFlat() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readDeepFlatConfig(raw, "deepFlat", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeDeepFlat(value, ctx.logger()),
            WorldzConfig::deepFlatMap,
            WorldzConfig::deepFlatSummary
        );
    }

    public static SectionCodec<StackedConfig> stacked() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readStackedConfig(raw, "stacked", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeStacked(value, ctx.logger()),
            WorldzConfig::stackedMap,
            WorldzConfig::stackedSummary
        );
    }

    public static SectionCodec<ForeverNightConfig> foreverNight() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readForeverNightConfig(raw, "foreverNight", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeForeverNight(value, ctx.logger()),
            WorldzConfig::foreverNightMap,
            WorldzConfig::foreverNightSummary
        );
    }

    public static SectionCodec<RisingLavaConfig> risingLava() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readRisingLavaConfig(raw, "risingLava", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeRisingLava(value, ctx.logger()),
            WorldzConfig::risingLavaMap,
            WorldzConfig::risingLavaSummary
        );
    }

    /**
     * Verbatim copy of the pre-25.2a {@code readSpawnConfig}/(inline sanitize)/{@code
     * spawnMap} logic, kept independent of {@link WorldzConfig}'s own (now schema-delegating)
     * copies so {@code ConfigSchemaDifferentialTest} can compare old behavior against
     * {@code SpawnSchema} directly, not against itself.
     */
    public static SectionCodec<SpawnConfig> spawn(String name) {
        return new Adapter<>(
            (raw, ctx) -> {
                if (!(raw instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException(name + " must be a mapping");
                }
                SpawnConfig config = new SpawnConfig();
                if (map.containsKey("strategy")) {
                    Object value = map.get("strategy");
                    if (!(value instanceof String string)) {
                        throw new IllegalArgumentException(name + ".strategy must be a string");
                    }
                    config.strategy = media.jlt.minecraft.mods.worldz.logic.SpawnStrategy.parse(string);
                }
                return config;
            },
            (value, ctx) -> {
                SpawnConfig sanitized = value == null ? new SpawnConfig() : value;
                if (sanitized.strategy == null) {
                    sanitized.strategy = media.jlt.minecraft.mods.worldz.logic.SpawnStrategy.STARTER_AT_ORIGIN;
                }
                return sanitized;
            },
            config -> {
                Map<String, Object> values = new java.util.LinkedHashMap<>();
                values.put("strategy", config.strategy.serializedName());
                return values;
            },
            config -> config.strategy.serializedName()
        );
    }

    /**
     * Verbatim copy of the pre-25.2a {@code readStarterKitConfig}/{@code sanitizeStarterKit}/
     * {@code starterKitMap}/{@code starterKitSummary} logic; see {@link #spawn} for why this is
     * kept independent of {@link WorldzConfig}'s own copies.
     */
    public static SectionCodec<StarterKitConfig> starterKit(String name) {
        return new Adapter<>(
            (raw, ctx) -> {
                if (!(raw instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException(name + " must be a mapping");
                }
                StarterKitConfig config = new StarterKitConfig();
                if (map.containsKey("essentials")) {
                    config.essentials = WorldzConfig.readStringList(map.get("essentials"), name + ".essentials", ctx.logger());
                }
                if (map.containsKey("extras")) {
                    config.extras = WorldzConfig.readStringList(map.get("extras"), name + ".extras", ctx.logger());
                }
                if (map.containsKey("extrasCount")) {
                    config.extrasCount = WorldzConfig.readInt(map.get("extrasCount"), name + ".extrasCount");
                }
                return config;
            },
            (value, ctx) -> {
                StarterKitConfig sanitized = value == null ? new StarterKitConfig() : value;
                if (sanitized.essentials == null) {
                    sanitized.essentials = new StarterKitConfig().essentials;
                }
                if (sanitized.extras == null) {
                    sanitized.extras = new StarterKitConfig().extras;
                }
                if (sanitized.extrasCount < 0) {
                    ctx.logger().warn("Clamped {}.extrasCount from {} to 0.", name, sanitized.extrasCount);
                    sanitized.extrasCount = 0;
                }
                if (sanitized.extrasCount > 0 && sanitized.extras.isEmpty()) {
                    ctx.logger().warn(
                        "Clamped {}.extrasCount from {} to 0 because the extras pool is empty.", name, sanitized.extrasCount
                    );
                    sanitized.extrasCount = 0;
                }
                return sanitized;
            },
            config -> {
                Map<String, Object> values = new java.util.LinkedHashMap<>();
                values.put("essentials", config.essentials);
                values.put("extras", config.extras);
                values.put("extrasCount", config.extrasCount);
                return values;
            },
            config -> "essentials=" + config.essentials + ", extras=" + config.extras + ", extrasCount=" + config.extrasCount
        );
    }

    /**
     * Verbatim copy of the pre-25.2a {@code readStarterCapsuleConfig}/{@code
     * sanitizeStarterCapsule}/{@code starterCapsuleMap}/{@code starterCapsuleSummary} logic; see
     * {@link #spawn} for why this is kept independent of {@link WorldzConfig}'s own copies.
     */
    public static SectionCodec<StarterCapsuleConfig> starterCapsule(
        String name, int minSize, int maxSize, int minHeight, int maxHeight, int minSpacing, int maxSpacing
    ) {
        return new Adapter<>(
            (raw, ctx) -> {
                if (!(raw instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException(name + " must be a mapping");
                }
                StarterCapsuleConfig config = new StarterCapsuleConfig();
                if (map.containsKey("sizeBlocks")) {
                    config.sizeBlocks = WorldzConfig.readInt(map.get("sizeBlocks"), name + ".sizeBlocks");
                }
                if (map.containsKey("heightBlocks")) {
                    config.heightBlocks = WorldzConfig.readInt(map.get("heightBlocks"), name + ".heightBlocks");
                }
                if (map.containsKey("lightSource")) {
                    Object value = map.get("lightSource");
                    if (!(value instanceof String string)) {
                        throw new IllegalArgumentException(name + ".lightSource must be a string");
                    }
                    config.lightSource = media.jlt.minecraft.mods.worldz.logic.LightSource.parse(string);
                }
                if (map.containsKey("lightSpacingBlocks")) {
                    config.lightSpacingBlocks = WorldzConfig.readInt(map.get("lightSpacingBlocks"), name + ".lightSpacingBlocks");
                }
                return config;
            },
            (value, ctx) -> {
                StarterCapsuleConfig sanitized = value == null ? new StarterCapsuleConfig() : value;
                if (sanitized.sizeBlocks % 2 == 0) {
                    int oddened = sanitized.sizeBlocks + 1;
                    ctx.logger().warn("Rounded {}.sizeBlocks from {} to {} (must be odd).", name, sanitized.sizeBlocks, oddened);
                    sanitized.sizeBlocks = oddened;
                }
                sanitized.sizeBlocks = WorldzConfig.clampWithWarning(sanitized.sizeBlocks, minSize, maxSize, name + ".sizeBlocks", ctx.logger());
                sanitized.heightBlocks = WorldzConfig.clampWithWarning(sanitized.heightBlocks, minHeight, maxHeight, name + ".heightBlocks", ctx.logger());
                sanitized.lightSource = sanitized.lightSource == null
                    ? media.jlt.minecraft.mods.worldz.logic.LightSource.TORCH : sanitized.lightSource;
                sanitized.lightSpacingBlocks = WorldzConfig.clampWithWarning(
                    sanitized.lightSpacingBlocks, minSpacing, maxSpacing, name + ".lightSpacingBlocks", ctx.logger()
                );
                return sanitized;
            },
            config -> {
                Map<String, Object> values = new java.util.LinkedHashMap<>();
                values.put("sizeBlocks", config.sizeBlocks);
                values.put("heightBlocks", config.heightBlocks);
                values.put("lightSource", config.lightSource.serializedName());
                values.put("lightSpacingBlocks", config.lightSpacingBlocks);
                return values;
            },
            config -> "sizeBlocks=" + config.sizeBlocks
                + ", heightBlocks=" + config.heightBlocks
                + ", lightSource=" + config.lightSource.serializedName()
                + ", lightSpacingBlocks=" + config.lightSpacingBlocks
        );
    }

    public static SectionCodec<StructureDistanceConfig> structureDistance() {
        return new Adapter<>(
            (raw, ctx) -> WorldzConfig.readStructureDistanceConfig(raw, "structureDistance", ctx.logger()),
            (value, ctx) -> WorldzConfig.sanitizeStructureDistance(value, ctx.logger()),
            WorldzConfig::structureDistanceMap,
            WorldzConfig::structureDistanceSummary
        );
    }

    /** Generic wrapper so each factory method above is a one-liner. */
    private record Adapter<S>(
        BiFunction<Object, ParseContext, S> reader,
        BiFunction<S, SanitizeContext, S> sanitizer,
        Function<S, Map<String, Object>> mapper,
        Function<S, String> summarizer
    ) implements SectionCodec<S> {
        @Override
        public S read(Object raw, ParseContext ctx) {
            return reader.apply(raw, ctx);
        }

        @Override
        public S sanitize(S value, SanitizeContext ctx) {
            return sanitizer.apply(value, ctx);
        }

        @Override
        public Map<String, Object> toMap(S value) {
            return mapper.apply(value);
        }

        @Override
        public String summary(S value) {
            return summarizer.apply(value);
        }
    }
}
