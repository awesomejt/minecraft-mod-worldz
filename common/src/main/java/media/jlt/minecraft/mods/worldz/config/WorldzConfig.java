package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.ParseContext;
import media.jlt.minecraft.mods.worldz.config.schema.SanitizeContext;
import media.jlt.minecraft.mods.worldz.config.schema.WorldzRootSchema;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Configuration baked into newly created Worldz worlds. */
public final class WorldzConfig {
    /** Smallest supported starter-zone radius. */
    public static final int MIN_STARTER_RADIUS_BLOCKS = 64;
    /** Largest supported starter-zone radius. */
    public static final int MAX_STARTER_RADIUS_BLOCKS = 4096;
    /** Largest supported natural-terrain blend beyond the starter zone. */
    public static final int MAX_STARTER_LAND_TRANSITION_BLOCKS = 4096;
    /** Largest supported repair depth beneath the natural ocean floor. */
    public static final int MAX_STARTER_LAND_FOUNDATION_DEPTH_BLOCKS = 384;
    /**
     * Smallest supported world-border half-width. Deliberately {@code 1}, not a
     * larger "safe" floor (Jason, 2026-07-18): a very small starting/final/End
     * radius is allowed on purpose for the expanding/collapsing challenges
     * (GOALS 19-20), and keeping the world beatable at that size is the user's
     * responsibility, not something this floor should enforce.
     */
    public static final int MIN_BORDER_RADIUS_BLOCKS = 1;
    /** Largest half-width accepted by vanilla's world border. */
    public static final int MAX_BORDER_RADIUS_BLOCKS = 14_999_992;
    /** Longest supported border transition in in-game days. */
    public static final int MAX_BORDER_RESIZE_DAYS = 1_000_000;
    /** Largest supported rate distance. */
    public static final int MAX_BORDER_RATE_BLOCKS = MAX_BORDER_RADIUS_BLOCKS;
    /** Smallest supported layout grid-cell edge length. */
    public static final int MIN_LAYOUT_REGION_SCALE_BLOCKS = 16;
    /** Largest supported layout grid-cell edge length. */
    public static final int MAX_LAYOUT_REGION_SCALE_BLOCKS = 8192;
    /**
     * Smallest supported ocean-island radius. Deliberately far below
     * {@link #MIN_STARTER_RADIUS_BLOCKS}: that bound was tuned for a starter zone inside an
     * otherwise-normal world, not the entire visible island GOALS 01 explicitly wants
     * configurable down to "16 blocks/1 chunk" (DESIGN §24.8).
     */
    public static final int MIN_ISLAND_RADIUS_BLOCKS = 8;
    /** Largest supported ocean-island radius -- a generous "huge" ceiling, not a tuned limit. */
    public static final int MAX_ISLAND_RADIUS_BLOCKS = 65536;
    /** Largest supported per-column relief bump for a {@code stacked} layer's own surface
     * (DESIGN §34.7) -- a small fraction of the 30-block minimum default air gap. */
    public static final int MAX_STACKED_RELIEF_BLOCKS = 16;

    static final String YAML_EXTENSION = ".yaml";
    static final String REFERENCE_SUFFIX = ".reference" + YAML_EXTENSION;

    /** Fixed comment header prepended to every generated reference file. */
    private static final String REFERENCE_HEADER = """
        # jlt_worldz reference config -- GENERATED, do not edit.
        # Rewritten from the mod's schema on every launch; the mod never reads this file.
        # Every setting is shown at its built-in default. Copy the parts you want into
        # config/jlt_worldz/ (per-world-type files) or as a single
        # config/jlt_worldz/all.yaml bundle -- the mod never rewrites what you put there.
        """;

    /**
     * The root schema (DESIGN §41.7, TODO 25.2g): declares every top-level scalar and section, in
     * {@link #toYaml()}'s exact emit order, and backs {@link #parse}/{@link #sanitize}/
     * {@link #toYaml}/{@link #summary} directly. One shared instance, since a {@code
     * SchemaSection} carries no per-call state (its {@code declare()} result is cached).
     */
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();

    /** Biome ids and biome-tag ids allowed in new Worldz worlds. */
    public List<String> allowedBiomes = new ArrayList<>(List.of(
        "minecraft:desert",
        "minecraft:beach",
        "minecraft:river",
        "minecraft:badlands",
        "minecraft:eroded_badlands",
        "minecraft:wooded_badlands",
        "minecraft:stony_shore",
        "minecraft:dripstone_caves",
        "minecraft:lush_caves",
        "minecraft:deep_dark",
        "minecraft:sulfur_caves"
    ));
    /** Optional biome id forced around the origin. */
    public String starterBiome = "minecraft:plains";
    /** Starter-zone radius measured in blocks. */
    public int starterRadiusBlocks = 256;
    /** Whether low terrain beneath a starter biome is raised into usable land. */
    public boolean ensureStarterLand = true;
    /** Outward distance used to blend reinforced land into natural terrain. */
    public int starterLandTransitionBlocks = 128;
    /** Depth below the natural ocean floor repaired as solid foundation. */
    public int starterLandFoundationDepthBlocks = 48;
    /** Optional overworld border and End-portal reachability settings. */
    public BorderConfig overworldBorder = new BorderConfig();
    /** Optional Nether border and blaze-access settings. */
    public BorderConfig netherBorder = new BorderConfig();
    /** Optional End border carried from the Overworld's final radius (GOALS 17). */
    public EndBorderConfig endBorder = new EndBorderConfig();
    /** Optional Overworld terrain outside a central square. */
    public ExteriorConfig overworldExterior = new ExteriorConfig();
    /** Optional Nether terrain outside a central square. */
    public ExteriorConfig netherExterior = new ExteriorConfig();
    /** Optional narrow strip-world corridor (GOALS 32). */
    public StripConfig strip = new StripConfig();
    /** Optional coordinated terrain-layout composition. */
    public LayoutConfig layout = new LayoutConfig();
    /** Layout-origin and initial-spawn strategy. */
    public SpawnConfig spawn = new SpawnConfig();
    /** Defaults for the {@code jlt_worldz:single_biome} typed preset (DESIGN §20.2). */
    public SingleBiomeConfig singleBiome = new SingleBiomeConfig();
    /** Defaults for the {@code jlt_worldz:chaos_biomes} typed preset (DESIGN §20.11). */
    public ChaosBiomesConfig chaosBiomes = new ChaosBiomesConfig();
    /** Defaults for the {@code jlt_worldz:strip_world} typed preset (GOALS 32, DESIGN §23). */
    public StripWorldConfig stripWorld = new StripWorldConfig();
    /** Defaults for the {@code jlt_worldz:ocean_island} typed preset (GOALS 01, 04; DESIGN §24). */
    public OceanIslandConfig oceanIsland = new OceanIslandConfig();
    /** Defaults for the {@code jlt_worldz:sky_island} typed preset (GOALS 05; DESIGN §27). */
    public SkyIslandConfig skyIsland = new SkyIslandConfig();
    /** Defaults for the {@code jlt_worldz:sky_chunk} typed preset (GOALS 09/37; DESIGN §29). */
    public ChunkIslandConfig chunkIsland = new ChunkIslandConfig();
    /** Defaults for the {@code jlt_worldz:cave} typed preset (GOALS 25-26; DESIGN §30). */
    public CaveConfig cave = new CaveConfig();
    /** Defaults for the {@code jlt_worldz:nether_start} typed preset (GOALS 27; DESIGN §31). */
    public NetherStartConfig netherStart = new NetherStartConfig();
    /** Defaults for the {@code jlt_worldz:end_start} typed preset (GOALS 34; DESIGN §32). */
    public EndStartConfig endStart = new EndStartConfig();
    /** Defaults for the {@code jlt_worldz:flat} typed preset (GOAL 15; DESIGN §33.2). */
    public FlatConfig flat = new FlatConfig();
    /** Defaults for the {@code jlt_worldz:deep_flat} typed preset (GOAL 16; DESIGN §33.4). */
    public DeepFlatConfig deepFlat = new DeepFlatConfig();
    /** Defaults for the {@code jlt_worldz:stacked} typed preset (GOAL 35; DESIGN §34.1). */
    public StackedConfig stacked = new StackedConfig();
    /** World-hazard "forever night" module (GOAL 30; DESIGN §35.1) -- composes with any world type. */
    public ForeverNightConfig foreverNight = new ForeverNightConfig();
    /** World-hazard "rising lava floor" module (GOAL 29; DESIGN §35.2) -- composes with any world type. */
    public RisingLavaConfig risingLava = new RisingLavaConfig();
    /** "Structures far from spawn" module (GOAL 24; DESIGN §36) -- composes with any world type. */
    public StructureDistanceConfig structureDistance = new StructureDistanceConfig();
    /** Let vanilla's own river biomes generate naturally on the generic preset (GOALS 13). */
    public boolean allowRivers = false;
    /** Let vanilla's own river/ocean-family biomes generate naturally on the generic preset (GOALS 14). */
    public boolean allowOceans = false;

    /**
     * The dotted paths the user explicitly wrote in the source config (TODO 25.3); empty for any
     * instance built via {@code new WorldzConfig()}, since no file was ever parsed.
     */
    private Set<String> presentKeys = Set.of();

    /** Creates a config populated with defaults. */
    public WorldzConfig() {
    }

    /**
     * Loads {@code <configDir>/<modId>/} (DESIGN §43.4.2, TODO 25.7b) when present. Two shapes are
     * accepted: a single {@code all.yaml} bundle (DESIGN §43.4.5), which wins wholesale over every
     * split file when it exists, or the 15 split files {@link ConfigLayout#FILES} describes, merged
     * into one root map before the schema ever sees them. Both shapes -- and an absent directory --
     * are optional: when nothing is found, code defaults apply directly and no file is ever created.
     * A file the mod did read is never rewritten -- comments and omitted settings survive every
     * launch unchanged. See {@code config/jlt_worldz.example.yaml} for the documented,
     * comment-annotated reference to copy from when a reusable custom config is wanted; a sibling
     * {@code <modId>.reference.yaml}, regenerated on every load from the mod's own schema, is an
     * equivalent always-current source (and itself a valid {@code all.yaml} bundle).
     *
     * @param configDir loader-provided configuration directory
     * @param modId stable mod id used as the config subdirectory name
     * @param logger destination for validation diagnostics
     * @return sanitized configuration, or defaults when nothing is found or nothing loads cleanly
     */
    public static WorldzConfig load(Path configDir, String modId, Logger logger) {
        Path dir = configDir.resolve(modId);
        Path bundle = dir.resolve(ConfigLayout.BUNDLE_FILE);
        List<String> loadedFiles = new ArrayList<>();
        WorldzConfig config;
        try {
            Map<?, ?> root = Files.exists(bundle) ? readBundle(bundle, dir, loadedFiles, logger) : readSplit(dir, loadedFiles, logger);
            config = root.isEmpty() ? new WorldzConfig().sanitize(logger) : parseMap(root, logger).sanitize(logger);
        } catch (Exception exception) {
            logger.warn("Could not load config from {}: {}. Using defaults.", loadedFiles, exception.getMessage());
            config = new WorldzConfig().sanitize(logger);
        }
        writeReference(configDir, modId, logger);
        return config;
    }

    /**
     * Reads {@code all.yaml} exactly like today's single-file load always has: the whole file must
     * parse to a YAML mapping, or the caller's one catch (DESIGN §43.4.4) falls back to defaults.
     * Also WARNs, once, if any split file also exists alongside the bundle -- the bundle wins
     * wholesale (DESIGN §43.4.5), so an also-present split file is silently ignored otherwise.
     */
    private static Map<?, ?> readBundle(Path bundle, Path dir, List<String> loadedFiles, Logger logger) throws IOException {
        warnIfSplitFilesAlsoPresent(dir, bundle, logger);
        loadedFiles.add(ConfigLayout.BUNDLE_FILE);
        return loadYamlMap(Files.readString(bundle));
    }

    private static void warnIfSplitFilesAlsoPresent(Path dir, Path bundle, Logger logger) {
        List<String> alsoPresent = ConfigLayout.FILES.stream()
            .map(ConfigFile::relativePath)
            .filter(relativePath -> Files.exists(dir.resolve(relativePath)))
            .toList();
        if (!alsoPresent.isEmpty()) {
            logger.warn("Using {}, ignoring the following also-present split config files: {}", bundle, alsoPresent);
        }
    }

    /**
     * Reads every {@link ConfigLayout#FILES} entry found under {@code dir} and merges their owned
     * keys into one root map (DESIGN §43.4.2). Merge order is irrelevant -- each file owns a
     * disjoint key set ({@code ConfigLayoutTest}) -- so files are simply walked in declaration
     * order. Each file's own YAML-loading step is isolated (DESIGN §43.4.3-4): an absent file is
     * skipped silently (15 optional files; a WARN per absent file would be noise), a blank/{@code
     * null} document is skipped silently (a deliberate per-file refinement -- unlike a blank {@code
     * all.yaml}, which still throws), and a non-mapping root or YAML syntax error costs only that
     * file's sections, WARNing and continuing rather than aborting the whole load. Value-level
     * errors are deliberately <em>not</em> isolated here -- that happens once, later, over the fully
     * merged map, exactly as today (DESIGN §43.4.4).
     */
    private static Map<String, Object> readSplit(Path dir, List<String> loadedFiles, Logger logger) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (ConfigFile file : ConfigLayout.FILES) {
            Path filePath = dir.resolve(file.relativePath());
            if (!Files.exists(filePath)) {
                continue;
            }
            Map<?, ?> fileMap;
            try {
                Object loaded = createYaml().load(Files.readString(filePath));
                if (loaded == null) {
                    continue;
                }
                if (!(loaded instanceof Map<?, ?> map)) {
                    logger.warn("Ignoring config file {}: root value must be a YAML mapping.", filePath);
                    continue;
                }
                fileMap = map;
            } catch (Exception exception) {
                logger.warn("Ignoring config file {}: {}", filePath, exception.getMessage());
                continue;
            }
            loadedFiles.add(file.relativePath());
            mergeFile(root, file, fileMap);
        }
        return root;
    }

    /**
     * Merges one file's already-loaded map into the accumulating root map: an unwrapped file's
     * entire root mapping becomes its one owned key's body; a wrapped file contributes only the
     * entries whose key it actually owns, silently ignoring anything else (a misfiled key WARN is
     * TODO 25.7c's job, not this one's -- just don't let one crash the merge).
     */
    private static void mergeFile(Map<String, Object> root, ConfigFile file, Map<?, ?> fileMap) {
        if (file.unwrapped()) {
            root.put(file.rootKeys().get(0), fileMap);
            return;
        }
        for (Map.Entry<?, ?> entry : fileMap.entrySet()) {
            if (file.rootKeys().contains(entry.getKey())) {
                root.put((String) entry.getKey(), entry.getValue());
            }
        }
    }

    static WorldzConfig parse(String yaml, Logger logger) {
        return parseMap(loadYamlMap(yaml), logger);
    }

    static WorldzConfig parseMap(Map<?, ?> map, Logger logger) {
        LinkedHashSet<String> presentKeys = new LinkedHashSet<>();
        WorldzConfig config = ROOT.read(map, new ParseContext(logger, presentKeys::add));
        config.presentKeys = Set.copyOf(presentKeys);
        return config;
    }

    private static Map<?, ?> loadYamlMap(String yaml) {
        Object loaded = createYaml().load(yaml);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("root value must be a YAML mapping");
        }
        return map;
    }

    WorldzConfig sanitize(Logger logger) {
        return ROOT.sanitize(this, new SanitizeContext(logger, this));
    }

    /**
     * Reports whether the user explicitly wrote a given key in the source config, as opposed to
     * it being left at (or coincidentally set to) its default value. {@code dottedPath} uses the
     * same dotted-path convention as schema warnings and error messages (e.g. {@code
     * "stacked.worldSizeChunks"}, {@code "cave.easyKit.essentials"}).
     *
     * @param dottedPath the full dotted path of the key to check
     * @return {@code true} if the key was present in the parsed source, {@code false} otherwise
     *     (including for any instance never built via {@link #parse})
     */
    public boolean present(String dottedPath) {
        return presentKeys.contains(dottedPath);
    }

    /**
     * Returns a compact representation suitable for startup logging.
     *
     * @return config summary
     */
    public String summary() {
        return ROOT.summary(this);
    }

    String toYaml() {
        return createYaml().dump(ROOT.toMap(this));
    }

    /**
     * Renders the generated reference file's exact contents: fixed comment header + the schema's
     * all-defaults YAML. Deterministic -- no timestamp, no version.
     */
    static String referenceYaml() {
        return REFERENCE_HEADER + new WorldzConfig().sanitize(NOPLogger.NOP_LOGGER).toYaml();
    }

    /**
     * Writes {@code <configDir>/<modId>.reference.yaml}, overwriting any previous copy. Never
     * throws: a failure here must not stop the user's own config from loading. Unchanged by TODO
     * 25.7b's directory split (DESIGN §43.4.2/§43.7): still one file, sibling to {@code
     * <configDir>/<modId>/}, not written inside it -- and, since it is whole-root shaped, it still
     * doubles as a valid {@code all.yaml} bundle to copy in (DESIGN §43.4.5).
     */
    static void writeReference(Path configDir, String modId, Logger logger) {
        Path referenceFile = configDir.resolve(modId + REFERENCE_SUFFIX);
        try {
            Files.createDirectories(configDir);
            Files.writeString(referenceFile, referenceYaml());
            logger.info("Wrote reference config {}", referenceFile);
        } catch (Exception exception) {
            logger.warn("Could not write reference config {}: {}", referenceFile, exception.getMessage());
        }
    }

    static Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setAllowRecursiveKeys(false);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(120);

        return new Yaml(
            new SafeConstructor(loaderOptions),
            new Representer(dumperOptions),
            dumperOptions,
            loaderOptions
        );
    }
}
