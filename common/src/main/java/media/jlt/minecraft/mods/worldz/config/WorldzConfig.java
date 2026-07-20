package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.BiomeRole;
import media.jlt.minecraft.mods.worldz.logic.BiomeRoles;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.WeightedBiomeListSpec;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String YAML_EXTENSION = ".yaml";

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
        "minecraft:deep_dark"
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
    /** Let vanilla's own river biomes generate naturally on the generic preset (GOALS 13). */
    public boolean allowRivers = false;
    /** Let vanilla's own river/ocean-family biomes generate naturally on the generic preset (GOALS 14). */
    public boolean allowOceans = false;

    /** Creates a config populated with defaults. */
    public WorldzConfig() {
    }

    /**
     * Loads {@code <configDir>/<modId>.yaml} when present. The mod-level config file is
     * optional: when absent, code defaults apply directly and no file is created. See
     * {@code config/jlt_worldz.example.yaml} for the documented, comment-annotated
     * reference to copy from when a reusable custom config is wanted.
     *
     * @param configDir loader-provided configuration directory
     * @param modId stable mod id used as the filename
     * @param logger destination for validation diagnostics
     * @return sanitized configuration, or defaults when the file is absent or unreadable
     */
    public static WorldzConfig load(Path configDir, String modId, Logger logger) {
        Path configFile = configDir.resolve(modId + YAML_EXTENSION);
        if (!Files.exists(configFile)) {
            return new WorldzConfig().sanitize(logger);
        }
        return loadExisting(configFile, logger);
    }

    private static WorldzConfig loadExisting(Path configFile, Logger logger) {
        try {
            WorldzConfig config = parse(Files.readString(configFile), logger).sanitize(logger);
            config.save(configFile);
            return config;
        } catch (Exception exception) {
            logger.warn("Could not load config {}: {}. Using defaults without changing the file.",
                configFile, exception.getMessage());
            return new WorldzConfig().sanitize(logger);
        }
    }

    static WorldzConfig parse(String yaml, Logger logger) {
        Object loaded = createYaml().load(yaml);
        if (!(loaded instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("root value must be a YAML mapping");
        }

        WorldzConfig config = new WorldzConfig();
        if (object.containsKey("allowedBiomes")) {
            config.allowedBiomes = readStringList(object.get("allowedBiomes"), "allowedBiomes", logger);
        }
        if (object.containsKey("starterBiome")) {
            config.starterBiome = readString(object.get("starterBiome"), "starterBiome");
        }
        if (object.containsKey("starterRadiusBlocks")) {
            config.starterRadiusBlocks = readInt(object.get("starterRadiusBlocks"), "starterRadiusBlocks");
        }
        if (object.containsKey("ensureStarterLand")) {
            config.ensureStarterLand = readBoolean(object.get("ensureStarterLand"), "ensureStarterLand");
        }
        if (object.containsKey("starterLandTransitionBlocks")) {
            config.starterLandTransitionBlocks = readInt(
                object.get("starterLandTransitionBlocks"), "starterLandTransitionBlocks"
            );
        }
        if (object.containsKey("starterLandFoundationDepthBlocks")) {
            config.starterLandFoundationDepthBlocks = readInt(
                object.get("starterLandFoundationDepthBlocks"), "starterLandFoundationDepthBlocks"
            );
        }
        if (object.containsKey("overworldBorder")) {
            config.overworldBorder = readBorderConfig(object.get("overworldBorder"), "overworldBorder", "ensureEndPortal");
        }
        if (object.containsKey("netherBorder")) {
            config.netherBorder = readBorderConfig(object.get("netherBorder"), "netherBorder", "ensureBlazeAccess");
        }
        if (object.containsKey("endBorder")) {
            config.endBorder = readEndBorderConfig(object.get("endBorder"), "endBorder");
        }
        if (object.containsKey("overworldExterior")) {
            config.overworldExterior = readExteriorConfig(object.get("overworldExterior"), "overworldExterior");
        }
        if (object.containsKey("netherExterior")) {
            config.netherExterior = readExteriorConfig(object.get("netherExterior"), "netherExterior");
        }
        if (object.containsKey("strip")) {
            config.strip = readStripConfig(object.get("strip"), "strip");
        }
        if (object.containsKey("layout")) {
            config.layout = readLayoutConfig(object.get("layout"), "layout", logger);
        }
        if (object.containsKey("spawn")) {
            config.spawn = readSpawnConfig(object.get("spawn"), "spawn");
        }
        if (object.containsKey("singleBiome")) {
            config.singleBiome = readSingleBiomeConfig(object.get("singleBiome"), "singleBiome");
        }
        if (object.containsKey("chaosBiomes")) {
            config.chaosBiomes = readChaosBiomesConfig(object.get("chaosBiomes"), "chaosBiomes", logger);
        }
        if (object.containsKey("stripWorld")) {
            config.stripWorld = readStripWorldConfig(object.get("stripWorld"), "stripWorld", logger);
        }
        if (object.containsKey("oceanIsland")) {
            config.oceanIsland = readOceanIslandConfig(object.get("oceanIsland"), "oceanIsland", logger);
        }
        if (object.containsKey("allowRivers")) {
            config.allowRivers = readBoolean(object.get("allowRivers"), "allowRivers");
        }
        if (object.containsKey("allowOceans")) {
            config.allowOceans = readBoolean(object.get("allowOceans"), "allowOceans");
        }
        return config;
    }

    WorldzConfig sanitize(Logger logger) {
        BiomeListSpec allowedSpec = BiomeListSpec.parse(allowedBiomes);
        for (String invalid : allowedSpec.invalidEntries()) {
            logger.warn("Ignoring invalid allowed biome or tag '{}'.", invalid);
        }
        allowedBiomes = new ArrayList<>(allowedSpec.entries().stream().map(BiomeListSpec.Entry::configValue).toList());

        starterBiome = starterBiome == null ? "" : starterBiome.trim();
        if (!starterBiome.isEmpty()) {
            BiomeListSpec starterSpec = BiomeListSpec.parse(List.of(starterBiome));
            if (starterSpec.entries().size() != 1 || starterSpec.entries().getFirst().tag()) {
                logger.warn("Ignoring invalid starter biome '{}'.", starterBiome);
                starterBiome = "";
            } else {
                starterBiome = starterSpec.entries().getFirst().id();
            }
        }

        int originalRadius = starterRadiusBlocks;
        starterRadiusBlocks = Math.clamp(starterRadiusBlocks, MIN_STARTER_RADIUS_BLOCKS, MAX_STARTER_RADIUS_BLOCKS);
        if (starterRadiusBlocks != originalRadius) {
            logger.warn("Clamped starterRadiusBlocks from {} to {}.", originalRadius, starterRadiusBlocks);
        }
        starterLandTransitionBlocks = clampWithWarning(
            starterLandTransitionBlocks, 0, MAX_STARTER_LAND_TRANSITION_BLOCKS,
            "starterLandTransitionBlocks", logger
        );
        starterLandFoundationDepthBlocks = clampWithWarning(
            starterLandFoundationDepthBlocks, 0, MAX_STARTER_LAND_FOUNDATION_DEPTH_BLOCKS,
            "starterLandFoundationDepthBlocks", logger
        );

        overworldBorder = sanitizeBorder(overworldBorder, "overworldBorder", logger);
        netherBorder = sanitizeBorder(netherBorder, "netherBorder", logger);
        endBorder = sanitizeEndBorder(endBorder, logger);
        overworldExterior = sanitizeExterior(overworldExterior, overworldBorder, true, "overworldExterior", logger);
        netherExterior = sanitizeExterior(netherExterior, netherBorder, false, "netherExterior", logger);
        strip = sanitizeStrip(strip, logger);
        layout = sanitizeLayout(layout, logger);
        spawn = spawn == null ? new SpawnConfig() : spawn;
        if (spawn.strategy == null) {
            spawn.strategy = SpawnStrategy.STARTER_AT_ORIGIN;
        }
        singleBiome = sanitizeSingleBiome(singleBiome, logger);
        chaosBiomes = sanitizeChaosBiomes(chaosBiomes, logger);
        stripWorld = sanitizeStripWorld(stripWorld, logger);
        oceanIsland = sanitizeOceanIsland(oceanIsland, logger);
        return this;
    }

    private static SingleBiomeConfig sanitizeSingleBiome(SingleBiomeConfig config, Logger logger) {
        SingleBiomeConfig sanitized = config == null ? new SingleBiomeConfig() : config;

        sanitized.landBiome = sanitizeSingleBiomeId(sanitized.landBiome, "singleBiome.landBiome", logger);
        if (sanitized.landBiome.isEmpty()) {
            logger.warn("singleBiome.landBiome must be one biome ID; using minecraft:plains instead.");
            sanitized.landBiome = "minecraft:plains";
        }

        sanitized.starterBiome = sanitizeSingleBiomeId(sanitized.starterBiome, "singleBiome.starterBiome", logger);

        int originalRadius = sanitized.starterRadiusBlocks;
        sanitized.starterRadiusBlocks = Math.clamp(
            sanitized.starterRadiusBlocks, MIN_STARTER_RADIUS_BLOCKS, MAX_STARTER_RADIUS_BLOCKS
        );
        if (sanitized.starterRadiusBlocks != originalRadius) {
            logger.warn("Clamped singleBiome.starterRadiusBlocks from {} to {}.", originalRadius, sanitized.starterRadiusBlocks);
        }

        sanitized.spawn = sanitized.spawn == null ? new SpawnConfig() : sanitized.spawn;
        if (sanitized.spawn.strategy == null) {
            sanitized.spawn.strategy = SpawnStrategy.STARTER_AT_ORIGIN;
        }
        return sanitized;
    }

    private static ChaosBiomesConfig sanitizeChaosBiomes(ChaosBiomesConfig config, Logger logger) {
        ChaosBiomesConfig sanitized = config == null ? new ChaosBiomesConfig() : config;

        WeightedBiomeListSpec biomeSpec = WeightedBiomeListSpec.parse(sanitized.biomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            logger.warn("Ignoring invalid chaosBiomes biome '{}'.", invalid);
        }
        sanitized.biomes = new ArrayList<>(
            biomeSpec.entries().stream().map(WeightedBiomeListSpec.Entry::configValue).toList()
        );
        if (sanitized.biomes.isEmpty()) {
            logger.warn("chaosBiomes.biomes has no usable entries; using the default biome list instead.");
            sanitized.biomes = new ChaosBiomesConfig().biomes;
        }

        sanitized.regionScaleBlocks = clampWithWarning(
            sanitized.regionScaleBlocks, MIN_LAYOUT_REGION_SCALE_BLOCKS, MAX_LAYOUT_REGION_SCALE_BLOCKS,
            "chaosBiomes.regionScaleBlocks", logger
        );

        sanitized.starterBiome = sanitizeSingleBiomeId(sanitized.starterBiome, "chaosBiomes.starterBiome", logger);

        int originalRadius = sanitized.starterRadiusBlocks;
        sanitized.starterRadiusBlocks = Math.clamp(
            sanitized.starterRadiusBlocks, MIN_STARTER_RADIUS_BLOCKS, MAX_STARTER_RADIUS_BLOCKS
        );
        if (sanitized.starterRadiusBlocks != originalRadius) {
            logger.warn("Clamped chaosBiomes.starterRadiusBlocks from {} to {}.", originalRadius, sanitized.starterRadiusBlocks);
        }

        sanitized.spawn = sanitized.spawn == null ? new SpawnConfig() : sanitized.spawn;
        if (sanitized.spawn.strategy == null) {
            sanitized.spawn.strategy = SpawnStrategy.STARTER_AT_ORIGIN;
        }
        return sanitized;
    }

    private static StripWorldConfig sanitizeStripWorld(StripWorldConfig config, Logger logger) {
        StripWorldConfig sanitized = config == null ? new StripWorldConfig() : config;
        sanitized.spawn = sanitized.spawn == null ? new SpawnConfig() : sanitized.spawn;
        if (sanitized.spawn.strategy == null) {
            sanitized.spawn.strategy = SpawnStrategy.STARTER_AT_ORIGIN;
        }
        sanitized.bands = sanitizeStripBands(sanitized.bands, logger);
        return sanitized;
    }

    private static OceanIslandConfig sanitizeOceanIsland(OceanIslandConfig config, Logger logger) {
        OceanIslandConfig sanitized = config == null ? new OceanIslandConfig() : config;

        String originalBiome = sanitized.islandBiome;
        sanitized.islandBiome = sanitizeSingleBiomeId(sanitized.islandBiome, "oceanIsland.islandBiome", logger);
        if (sanitized.islandBiome.isEmpty()) {
            sanitized.islandBiome = new OceanIslandConfig().islandBiome;
            logger.warn("Invalid oceanIsland.islandBiome '{}'; using the default instead.", originalBiome);
        }

        sanitized.radiusBlocks = clampWithWarning(
            sanitized.radiusBlocks, MIN_ISLAND_RADIUS_BLOCKS, MAX_ISLAND_RADIUS_BLOCKS, "oceanIsland.radiusBlocks", logger
        );
        double clampedAmplitude = Math.clamp(sanitized.shapeAmplitude, 0.0, IslandShapeProfile.MAX_AMPLITUDE);
        if (clampedAmplitude != sanitized.shapeAmplitude) {
            logger.warn("Clamped oceanIsland.shapeAmplitude from {} to {}.", sanitized.shapeAmplitude, clampedAmplitude);
            sanitized.shapeAmplitude = clampedAmplitude;
        }
        if (sanitized.shoreWidthBlocks < 1) {
            logger.warn("Clamped oceanIsland.shoreWidthBlocks from {} to 1.", sanitized.shoreWidthBlocks);
            sanitized.shoreWidthBlocks = 1;
        }
        if (sanitized.oceanShallowWidthBlocks < 0) {
            logger.warn("Clamped oceanIsland.oceanShallowWidthBlocks from {} to 0.", sanitized.oceanShallowWidthBlocks);
            sanitized.oceanShallowWidthBlocks = 0;
        }
        if (sanitized.oceanDeepenWidthBlocks < 0) {
            logger.warn("Clamped oceanIsland.oceanDeepenWidthBlocks from {} to 0.", sanitized.oceanDeepenWidthBlocks);
            sanitized.oceanDeepenWidthBlocks = 0;
        }
        if (sanitized.oceanShallowDepthBlocks < 1) {
            logger.warn("Clamped oceanIsland.oceanShallowDepthBlocks from {} to 1.", sanitized.oceanShallowDepthBlocks);
            sanitized.oceanShallowDepthBlocks = 1;
        }
        if (sanitized.oceanDeepDepthBlocks < 1) {
            logger.warn("Clamped oceanIsland.oceanDeepDepthBlocks from {} to 1.", sanitized.oceanDeepDepthBlocks);
            sanitized.oceanDeepDepthBlocks = 1;
        }
        if (sanitized.oceanRegionScaleBlocks < 1) {
            logger.warn("Clamped oceanIsland.oceanRegionScaleBlocks from {} to 1.", sanitized.oceanRegionScaleBlocks);
            sanitized.oceanRegionScaleBlocks = 1;
        }
        if (sanitized.exclusionZoneRadiusBlocks < 1) {
            logger.warn("Clamped oceanIsland.exclusionZoneRadiusBlocks from {} to 1.", sanitized.exclusionZoneRadiusBlocks);
            sanitized.exclusionZoneRadiusBlocks = 1;
        }
        sanitized.starterKit = sanitizeStarterKit(sanitized.starterKit, logger);
        return sanitized;
    }

    private static StarterKitConfig sanitizeStarterKit(StarterKitConfig config, Logger logger) {
        StarterKitConfig sanitized = config == null ? new StarterKitConfig() : config;
        if (sanitized.essentials == null) {
            sanitized.essentials = new StarterKitConfig().essentials;
        }
        if (sanitized.extras == null) {
            sanitized.extras = new StarterKitConfig().extras;
        }
        if (sanitized.extrasCount < 0) {
            logger.warn("Clamped oceanIsland.starterKit.extrasCount from {} to 0.", sanitized.extrasCount);
            sanitized.extrasCount = 0;
        }
        if (sanitized.extrasCount > 0 && sanitized.extras.isEmpty()) {
            logger.warn(
                "Clamped oceanIsland.starterKit.extrasCount from {} to 0 because the extras pool is empty.",
                sanitized.extrasCount
            );
            sanitized.extrasCount = 0;
        }
        return sanitized;
    }

    private static StripBandsConfig sanitizeStripBands(StripBandsConfig config, Logger logger) {
        StripBandsConfig sanitized = config == null ? new StripBandsConfig() : config;

        BiomeListSpec biomeSpec = BiomeListSpec.parse(sanitized.biomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            logger.warn("Ignoring invalid stripWorld.bands biome '{}'.", invalid);
        }
        for (BiomeListSpec.Entry entry : biomeSpec.entries()) {
            if (entry.tag()) {
                logger.warn("Ignoring stripWorld.bands biome tag '#{}'; bands require concrete biome ids.", entry.id());
            }
        }
        sanitized.biomes = new ArrayList<>(
            biomeSpec.entries().stream().filter(entry -> !entry.tag()).map(BiomeListSpec.Entry::id).toList()
        );
        if (sanitized.enabled && sanitized.biomes.isEmpty()) {
            logger.warn("stripWorld.bands.enabled is set but has no usable biomes; disabling biome bands.");
            sanitized.enabled = false;
        }

        sanitized.widthBlocks = clampWithWarning(
            sanitized.widthBlocks, MIN_LAYOUT_REGION_SCALE_BLOCKS, MAX_LAYOUT_REGION_SCALE_BLOCKS,
            "stripWorld.bands.widthBlocks", logger
        );
        return sanitized;
    }

    private static String sanitizeSingleBiomeId(String value, String name, Logger logger) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        BiomeListSpec spec = BiomeListSpec.parse(List.of(trimmed));
        if (spec.entries().size() != 1 || spec.entries().getFirst().tag()) {
            logger.warn("Ignoring invalid {} '{}'.", name, trimmed);
            return "";
        }
        return spec.entries().getFirst().id();
    }

    /**
     * Returns a compact representation suitable for startup logging.
     *
     * @return config summary
     */
    public String summary() {
        return "allowedBiomes=" + allowedBiomes
            + ", starterBiome=" + (starterBiome.isEmpty() ? "<none>" : starterBiome)
            + ", starterRadiusBlocks=" + starterRadiusBlocks
            + ", starterLand=" + (ensureStarterLand
                ? "transition=" + starterLandTransitionBlocks + ", foundation=" + starterLandFoundationDepthBlocks
                : "<disabled>")
            + ", overworldBorder=" + borderSummary(overworldBorder, "endPortal")
            + ", netherBorder=" + borderSummary(netherBorder, "blazeAccess")
            + ", endBorder=" + endBorderSummary(endBorder)
            + ", overworldExterior=" + exteriorSummary(overworldExterior)
            + ", netherExterior=" + exteriorSummary(netherExterior)
            + ", strip=" + stripSummary(strip)
            + ", layout=" + layoutSummary(layout)
            + ", spawn=" + spawn.strategy.serializedName()
            + ", singleBiome=" + singleBiomeSummary(singleBiome)
            + ", chaosBiomes=" + chaosBiomesSummary(chaosBiomes)
            + ", stripWorld=" + stripWorldSummary(stripWorld)
            + ", oceanIsland=" + oceanIslandSummary(oceanIsland)
            + ", allowRivers=" + allowRivers
            + ", allowOceans=" + allowOceans;
    }

    String toYaml() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedBiomes", allowedBiomes);
        values.put("starterBiome", starterBiome);
        values.put("starterRadiusBlocks", starterRadiusBlocks);
        values.put("ensureStarterLand", ensureStarterLand);
        values.put("starterLandTransitionBlocks", starterLandTransitionBlocks);
        values.put("starterLandFoundationDepthBlocks", starterLandFoundationDepthBlocks);
        values.put("overworldBorder", borderMap(overworldBorder, "ensureEndPortal"));
        values.put("netherBorder", borderMap(netherBorder, "ensureBlazeAccess"));
        values.put("endBorder", endBorderMap(endBorder));
        values.put("overworldExterior", exteriorMap(overworldExterior));
        values.put("netherExterior", exteriorMap(netherExterior));
        values.put("strip", stripMap(strip));
        values.put("layout", layoutMap(layout));
        values.put("spawn", spawnMap(spawn));
        values.put("singleBiome", singleBiomeMap(singleBiome));
        values.put("chaosBiomes", chaosBiomesMap(chaosBiomes));
        values.put("stripWorld", stripWorldMap(stripWorld));
        values.put("oceanIsland", oceanIslandMap(oceanIsland));
        values.put("allowRivers", allowRivers);
        values.put("allowOceans", allowOceans);
        return createYaml().dump(values);
    }

    private void save(Path configFile) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        Files.writeString(temporary, toYaml());
        try {
            Files.move(temporary, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> readStringList(Object value, String name, Logger logger) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(name + " must be a sequence");
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Object entry = list.get(index);
            if (entry instanceof String string) {
                values.add(string);
            } else {
                logger.warn("Ignoring non-string {} entry at index {}.", name, index);
            }
        }
        return values;
    }

    private static String readString(Object value, String name) {
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return string;
    }

    private static boolean readBoolean(Object value, String name) {
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException(name + " must be a boolean");
        }
        return booleanValue;
    }

    private static BorderConfig readBorderConfig(Object value, String name, String objectiveKey) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        BorderConfig config = new BorderConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("initialRadiusBlocks")) {
            config.initialRadiusBlocks = readInt(map.get("initialRadiusBlocks"), name + ".initialRadiusBlocks");
        }
        if (map.containsKey("finalRadiusBlocks")) {
            config.finalRadiusBlocks = readInt(map.get("finalRadiusBlocks"), name + ".finalRadiusBlocks");
        }
        if (map.containsKey("resizeDays")) {
            config.resizeDays = readInt(map.get("resizeDays"), name + ".resizeDays");
        }
        if (map.containsKey("resizeDelayDays")) {
            config.resizeDelayDays = readInt(map.get("resizeDelayDays"), name + ".resizeDelayDays");
        }
        if (map.containsKey("resizeRateBlocks")) {
            config.resizeRateBlocks = readInt(map.get("resizeRateBlocks"), name + ".resizeRateBlocks");
        }
        if (map.containsKey("resizeRateDays")) {
            config.resizeRateDays = readInt(map.get("resizeRateDays"), name + ".resizeRateDays");
        }
        if (map.containsKey(objectiveKey)) {
            config.ensureObjective = readBoolean(map.get(objectiveKey), name + "." + objectiveKey);
        }
        if (map.containsKey("resizeStyle")) {
            config.resizeStyle = ResizeStyle.parse(readString(map.get("resizeStyle"), name + ".resizeStyle"));
        }
        return config;
    }

    private static EndBorderConfig readEndBorderConfig(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        EndBorderConfig config = new EndBorderConfig();
        if (map.containsKey("carryFromOverworld")) {
            config.carryFromOverworld = readBoolean(map.get("carryFromOverworld"), name + ".carryFromOverworld");
        }
        if (map.containsKey("minimumRadiusBlocks")) {
            config.minimumRadiusBlocks = readInt(map.get("minimumRadiusBlocks"), name + ".minimumRadiusBlocks");
        }
        return config;
    }

    private static ExteriorConfig readExteriorConfig(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        ExteriorConfig config = new ExteriorConfig();
        if (map.containsKey("mode")) {
            config.mode = ExteriorMode.parse(readString(map.get("mode"), name + ".mode"));
        }
        if (map.containsKey("boundaryRadiusBlocks")) {
            config.boundaryRadiusBlocks = readInt(map.get("boundaryRadiusBlocks"), name + ".boundaryRadiusBlocks");
        }
        if (map.containsKey("oceanTransitionWidthBlocks")) {
            config.oceanTransitionWidthBlocks = readInt(
                map.get("oceanTransitionWidthBlocks"), name + ".oceanTransitionWidthBlocks"
            );
        }
        return config;
    }

    private static StripConfig readStripConfig(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StripConfig config = new StripConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("widthRadiusBlocks")) {
            config.widthRadiusBlocks = readInt(map.get("widthRadiusBlocks"), name + ".widthRadiusBlocks");
        }
        if (map.containsKey("widthMode")) {
            config.widthMode = ExteriorMode.parse(readString(map.get("widthMode"), name + ".widthMode"));
        }
        if (map.containsKey("applyToNether")) {
            config.applyToNether = readBoolean(map.get("applyToNether"), name + ".applyToNether");
        }
        return config;
    }

    private static LayoutConfig readLayoutConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        LayoutConfig config = new LayoutConfig();
        if (map.containsKey("mode")) {
            config.mode = LayoutMode.parse(readString(map.get("mode"), name + ".mode"));
        }
        if (map.containsKey("biomes")) {
            config.biomes = readStringList(map.get("biomes"), name + ".biomes", logger);
        }
        if (map.containsKey("regionScaleBlocks")) {
            config.regionScaleBlocks = readInt(map.get("regionScaleBlocks"), name + ".regionScaleBlocks");
        }
        if (map.containsKey("singleBiome")) {
            config.singleBiome = readString(map.get("singleBiome"), name + ".singleBiome");
        }
        if (map.containsKey("roleOverrides")) {
            config.roleOverrides = readStringMap(map.get("roleOverrides"), name + ".roleOverrides");
        }
        return config;
    }

    private static SpawnConfig readSpawnConfig(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        SpawnConfig config = new SpawnConfig();
        if (map.containsKey("strategy")) {
            config.strategy = SpawnStrategy.parse(readString(map.get("strategy"), name + ".strategy"));
        }
        return config;
    }

    private static SingleBiomeConfig readSingleBiomeConfig(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        SingleBiomeConfig config = new SingleBiomeConfig();
        if (map.containsKey("landBiome")) {
            config.landBiome = readString(map.get("landBiome"), name + ".landBiome");
        }
        if (map.containsKey("starterBiome")) {
            config.starterBiome = readString(map.get("starterBiome"), name + ".starterBiome");
        }
        if (map.containsKey("starterRadiusBlocks")) {
            config.starterRadiusBlocks = readInt(map.get("starterRadiusBlocks"), name + ".starterRadiusBlocks");
        }
        if (map.containsKey("spawn")) {
            config.spawn = readSpawnConfig(map.get("spawn"), name + ".spawn");
        }
        if (map.containsKey("allowRivers")) {
            config.allowRivers = readBoolean(map.get("allowRivers"), name + ".allowRivers");
        }
        if (map.containsKey("allowOceans")) {
            config.allowOceans = readBoolean(map.get("allowOceans"), name + ".allowOceans");
        }
        if (map.containsKey("allowBeaches")) {
            config.allowBeaches = readBoolean(map.get("allowBeaches"), name + ".allowBeaches");
        }
        return config;
    }

    private static ChaosBiomesConfig readChaosBiomesConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        ChaosBiomesConfig config = new ChaosBiomesConfig();
        if (map.containsKey("biomes")) {
            config.biomes = readStringList(map.get("biomes"), name + ".biomes", logger);
        }
        if (map.containsKey("regionScaleBlocks")) {
            config.regionScaleBlocks = readInt(map.get("regionScaleBlocks"), name + ".regionScaleBlocks");
        }
        if (map.containsKey("starterBiome")) {
            config.starterBiome = readString(map.get("starterBiome"), name + ".starterBiome");
        }
        if (map.containsKey("starterRadiusBlocks")) {
            config.starterRadiusBlocks = readInt(map.get("starterRadiusBlocks"), name + ".starterRadiusBlocks");
        }
        if (map.containsKey("spawn")) {
            config.spawn = readSpawnConfig(map.get("spawn"), name + ".spawn");
        }
        if (map.containsKey("allowRivers")) {
            config.allowRivers = readBoolean(map.get("allowRivers"), name + ".allowRivers");
        }
        if (map.containsKey("allowOceans")) {
            config.allowOceans = readBoolean(map.get("allowOceans"), name + ".allowOceans");
        }
        if (map.containsKey("allowBeaches")) {
            config.allowBeaches = readBoolean(map.get("allowBeaches"), name + ".allowBeaches");
        }
        return config;
    }

    private static StripWorldConfig readStripWorldConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StripWorldConfig config = new StripWorldConfig();
        if (map.containsKey("spawn")) {
            config.spawn = readSpawnConfig(map.get("spawn"), name + ".spawn");
        }
        if (map.containsKey("bands")) {
            config.bands = readStripBandsConfig(map.get("bands"), name + ".bands", logger);
        }
        return config;
    }

    private static OceanIslandConfig readOceanIslandConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        OceanIslandConfig config = new OceanIslandConfig();
        if (map.containsKey("islandSource")) {
            config.islandSource = IslandSource.parse(readString(map.get("islandSource"), name + ".islandSource"));
        }
        if (map.containsKey("fluid")) {
            config.fluid = IslandFluid.parse(readString(map.get("fluid"), name + ".fluid"));
        }
        if (map.containsKey("islandBiome")) {
            config.islandBiome = readString(map.get("islandBiome"), name + ".islandBiome");
        }
        if (map.containsKey("radiusBlocks")) {
            config.radiusBlocks = readInt(map.get("radiusBlocks"), name + ".radiusBlocks");
        }
        if (map.containsKey("shapeAmplitude")) {
            config.shapeAmplitude = readDouble(map.get("shapeAmplitude"), name + ".shapeAmplitude");
        }
        if (map.containsKey("shoreWidthBlocks")) {
            config.shoreWidthBlocks = readInt(map.get("shoreWidthBlocks"), name + ".shoreWidthBlocks");
        }
        if (map.containsKey("oceanShallowWidthBlocks")) {
            config.oceanShallowWidthBlocks = readInt(map.get("oceanShallowWidthBlocks"), name + ".oceanShallowWidthBlocks");
        }
        if (map.containsKey("oceanDeepenWidthBlocks")) {
            config.oceanDeepenWidthBlocks = readInt(map.get("oceanDeepenWidthBlocks"), name + ".oceanDeepenWidthBlocks");
        }
        if (map.containsKey("oceanShallowDepthBlocks")) {
            config.oceanShallowDepthBlocks = readInt(map.get("oceanShallowDepthBlocks"), name + ".oceanShallowDepthBlocks");
        }
        if (map.containsKey("oceanDeepDepthBlocks")) {
            config.oceanDeepDepthBlocks = readInt(map.get("oceanDeepDepthBlocks"), name + ".oceanDeepDepthBlocks");
        }
        if (map.containsKey("oceanRegionScaleBlocks")) {
            config.oceanRegionScaleBlocks = readInt(map.get("oceanRegionScaleBlocks"), name + ".oceanRegionScaleBlocks");
        }
        if (map.containsKey("exclusionZoneEnabled")) {
            config.exclusionZoneEnabled = readBoolean(map.get("exclusionZoneEnabled"), name + ".exclusionZoneEnabled");
        }
        if (map.containsKey("exclusionZoneRadiusBlocks")) {
            config.exclusionZoneRadiusBlocks = readInt(map.get("exclusionZoneRadiusBlocks"), name + ".exclusionZoneRadiusBlocks");
        }
        if (map.containsKey("starterKit")) {
            config.starterKit = readStarterKitConfig(map.get("starterKit"), name + ".starterKit", logger);
        }
        return config;
    }

    private static StarterKitConfig readStarterKitConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StarterKitConfig config = new StarterKitConfig();
        if (map.containsKey("essentials")) {
            config.essentials = readStringList(map.get("essentials"), name + ".essentials", logger);
        }
        if (map.containsKey("extras")) {
            config.extras = readStringList(map.get("extras"), name + ".extras", logger);
        }
        if (map.containsKey("extrasCount")) {
            config.extrasCount = readInt(map.get("extrasCount"), name + ".extrasCount");
        }
        return config;
    }

    private static StripBandsConfig readStripBandsConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StripBandsConfig config = new StripBandsConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("biomes")) {
            config.biomes = readStringList(map.get("biomes"), name + ".biomes", logger);
        }
        if (map.containsKey("widthBlocks")) {
            config.widthBlocks = readInt(map.get("widthBlocks"), name + ".widthBlocks");
        }
        if (map.containsKey("seedRandomOrder")) {
            config.seedRandomOrder = readBoolean(map.get("seedRandomOrder"), name + ".seedRandomOrder");
        }
        if (map.containsKey("allowRivers")) {
            config.allowRivers = readBoolean(map.get("allowRivers"), name + ".allowRivers");
        }
        if (map.containsKey("allowOceans")) {
            config.allowOceans = readBoolean(map.get("allowOceans"), name + ".allowOceans");
        }
        if (map.containsKey("allowBeaches")) {
            config.allowBeaches = readBoolean(map.get("allowBeaches"), name + ".allowBeaches");
        }
        return config;
    }

    private static Map<String, String> readStringMap(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String stringValue)) {
                throw new IllegalArgumentException(name + " keys and values must be strings");
            }
            values.put(key, stringValue);
        }
        return values;
    }

    private static int readInt(Object value, String name) {
        try {
            return switch (value) {
                case Integer integer -> integer;
                case Byte byteValue -> byteValue.intValue();
                case Short shortValue -> shortValue.intValue();
                case Long longValue -> Math.toIntExact(longValue);
                case BigInteger bigInteger -> bigInteger.intValueExact();
                case BigDecimal bigDecimal -> bigDecimal.intValueExact();
                default -> throw new IllegalArgumentException(name + " must be an integer");
            };
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static double readDouble(Object value, String name) {
        return switch (value) {
            case Double doubleValue -> doubleValue;
            case Float floatValue -> floatValue.doubleValue();
            case Integer integer -> integer.doubleValue();
            case Byte byteValue -> byteValue.doubleValue();
            case Short shortValue -> shortValue.doubleValue();
            case Long longValue -> longValue.doubleValue();
            case BigInteger bigInteger -> bigInteger.doubleValue();
            case BigDecimal bigDecimal -> bigDecimal.doubleValue();
            default -> throw new IllegalArgumentException(name + " must be a number");
        };
    }

    private static BorderConfig sanitizeBorder(BorderConfig config, String name, Logger logger) {
        BorderConfig sanitized = config == null ? new BorderConfig() : config;
        sanitized.initialRadiusBlocks = clampWithWarning(
            sanitized.initialRadiusBlocks, MIN_BORDER_RADIUS_BLOCKS, MAX_BORDER_RADIUS_BLOCKS,
            name + ".initialRadiusBlocks", logger
        );
        sanitized.finalRadiusBlocks = clampWithWarning(
            sanitized.finalRadiusBlocks, MIN_BORDER_RADIUS_BLOCKS, MAX_BORDER_RADIUS_BLOCKS,
            name + ".finalRadiusBlocks", logger
        );
        sanitized.resizeDays = clampWithWarning(
            sanitized.resizeDays, 0, MAX_BORDER_RESIZE_DAYS, name + ".resizeDays", logger
        );
        sanitized.resizeDelayDays = clampWithWarning(
            sanitized.resizeDelayDays, 0, MAX_BORDER_RESIZE_DAYS, name + ".resizeDelayDays", logger
        );
        sanitized.resizeRateBlocks = clampWithWarning(
            sanitized.resizeRateBlocks, 0, MAX_BORDER_RATE_BLOCKS, name + ".resizeRateBlocks", logger
        );
        sanitized.resizeRateDays = clampWithWarning(
            sanitized.resizeRateDays, 0, MAX_BORDER_RESIZE_DAYS, name + ".resizeRateDays", logger
        );
        if ((sanitized.resizeRateBlocks == 0) != (sanitized.resizeRateDays == 0)) {
            logger.warn("Ignoring incomplete {} resize rate; both resizeRateBlocks and resizeRateDays must be positive.", name);
            sanitized.resizeRateBlocks = 0;
            sanitized.resizeRateDays = 0;
        }
        sanitized.resizeStyle = sanitized.resizeStyle == null ? ResizeStyle.CONTINUOUS : sanitized.resizeStyle;
        if (sanitized.resizeStyle == ResizeStyle.STEPPED && sanitized.resizeRateBlocks == 0) {
            logger.warn("{} resizeStyle 'stepped' needs resizeRateBlocks/resizeRateDays; using 'continuous' instead.", name);
            sanitized.resizeStyle = ResizeStyle.CONTINUOUS;
        }
        return sanitized;
    }

    private static EndBorderConfig sanitizeEndBorder(EndBorderConfig config, Logger logger) {
        EndBorderConfig sanitized = config == null ? new EndBorderConfig() : config;
        sanitized.minimumRadiusBlocks = clampWithWarning(
            sanitized.minimumRadiusBlocks, MIN_BORDER_RADIUS_BLOCKS, MAX_BORDER_RADIUS_BLOCKS,
            "endBorder.minimumRadiusBlocks", logger
        );
        return sanitized;
    }

    private static StripConfig sanitizeStrip(StripConfig config, Logger logger) {
        StripConfig sanitized = config == null ? new StripConfig() : config;
        sanitized.widthRadiusBlocks = clampWithWarning(
            sanitized.widthRadiusBlocks, 1, MAX_BORDER_RADIUS_BLOCKS, "strip.widthRadiusBlocks", logger
        );
        sanitized.widthMode = sanitized.widthMode == null ? ExteriorMode.VOID : sanitized.widthMode;
        if (sanitized.widthMode == ExteriorMode.NORMAL) {
            logger.warn("strip.widthMode cannot be normal; using void instead.");
            sanitized.widthMode = ExteriorMode.VOID;
        }
        return sanitized;
    }

    private static ExteriorConfig sanitizeExterior(
        ExteriorConfig config,
        BorderConfig border,
        boolean oceanAllowed,
        String name,
        Logger logger
    ) {
        ExteriorConfig sanitized = config == null ? new ExteriorConfig() : config;
        sanitized.mode = sanitized.mode == null ? ExteriorMode.NORMAL : sanitized.mode;
        if (!oceanAllowed && sanitized.mode == ExteriorMode.OCEAN) {
            logger.warn("Ignoring unsupported ocean mode for {}; using normal terrain.", name);
            sanitized.mode = ExteriorMode.NORMAL;
        }
        sanitized.boundaryRadiusBlocks = clampWithWarning(
            sanitized.boundaryRadiusBlocks, 0, MAX_BORDER_RADIUS_BLOCKS, name + ".boundaryRadiusBlocks", logger
        );
        sanitized.oceanTransitionWidthBlocks = clampWithWarning(
            sanitized.oceanTransitionWidthBlocks, 0, MAX_BORDER_RADIUS_BLOCKS,
            name + ".oceanTransitionWidthBlocks", logger
        );
        if (sanitized.mode != ExteriorMode.NORMAL && sanitized.boundaryRadiusBlocks == 0 && !border.enabled) {
            logger.warn("{} requires an explicit boundary or an enabled border; using normal terrain.", name);
            sanitized.mode = ExteriorMode.NORMAL;
        }
        int resolvedBoundary = sanitized.boundaryRadiusBlocks == 0
            ? Math.max(border.initialRadiusBlocks, border.finalRadiusBlocks)
            : sanitized.boundaryRadiusBlocks;
        if (sanitized.mode == ExteriorMode.OCEAN && sanitized.oceanTransitionWidthBlocks > resolvedBoundary) {
            logger.warn("Clamped {}.oceanTransitionWidthBlocks from {} to {}.",
                name, sanitized.oceanTransitionWidthBlocks, resolvedBoundary);
            sanitized.oceanTransitionWidthBlocks = resolvedBoundary;
        }
        return sanitized;
    }

    private static LayoutConfig sanitizeLayout(LayoutConfig config, Logger logger) {
        LayoutConfig sanitized = config == null ? new LayoutConfig() : config;
        sanitized.mode = sanitized.mode == null ? LayoutMode.LEGACY : sanitized.mode;

        WeightedBiomeListSpec biomeSpec = WeightedBiomeListSpec.parse(sanitized.biomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            logger.warn("Ignoring invalid layout biome '{}'.", invalid);
        }
        sanitized.biomes = new ArrayList<>(
            biomeSpec.entries().stream().map(WeightedBiomeListSpec.Entry::configValue).toList()
        );

        Map<String, String> validOverrides = new LinkedHashMap<>();
        if (sanitized.roleOverrides != null) {
            sanitized.roleOverrides.forEach((rawId, rawRole) -> {
                BiomeListSpec idSpec = BiomeListSpec.parse(List.of(rawId == null ? "" : rawId));
                if (idSpec.entries().size() != 1 || idSpec.entries().getFirst().tag()) {
                    logger.warn("Ignoring layout roleOverrides entry with an invalid biome id '{}'.", rawId);
                    return;
                }
                try {
                    BiomeRole role = BiomeRole.parse(rawRole);
                    validOverrides.put(idSpec.entries().getFirst().id(), role.serializedName());
                } catch (IllegalArgumentException exception) {
                    logger.warn("Ignoring layout roleOverrides entry for '{}' with an invalid role '{}'.", rawId, rawRole);
                }
            });
        }
        sanitized.roleOverrides = validOverrides;

        sanitized.regionScaleBlocks = clampWithWarning(
            sanitized.regionScaleBlocks, MIN_LAYOUT_REGION_SCALE_BLOCKS, MAX_LAYOUT_REGION_SCALE_BLOCKS,
            "layout.regionScaleBlocks", logger
        );

        sanitized.singleBiome = sanitized.singleBiome == null ? "" : sanitized.singleBiome.trim();
        if (!sanitized.singleBiome.isEmpty()) {
            BiomeListSpec singleSpec = BiomeListSpec.parse(List.of(sanitized.singleBiome));
            if (singleSpec.entries().size() != 1 || singleSpec.entries().getFirst().tag()) {
                logger.warn("Ignoring invalid layout singleBiome '{}'.", sanitized.singleBiome);
                sanitized.singleBiome = "";
            } else {
                sanitized.singleBiome = singleSpec.entries().getFirst().id();
            }
        }

        Map<String, BiomeRole> overrides = new LinkedHashMap<>();
        sanitized.roleOverrides.forEach((id, role) -> overrides.put(id, BiomeRole.parse(role)));
        boolean hasOcean = sanitized.biomes.stream()
            .anyMatch(entry -> BiomeRoles.resolve(stripWeight(entry), overrides) == BiomeRole.OCEAN);
        boolean hasLand = sanitized.biomes.stream()
            .anyMatch(entry -> BiomeRoles.resolve(stripWeight(entry), overrides) == BiomeRole.LAND);
        boolean unsupported = switch (sanitized.mode) {
            case OCEAN -> !hasOcean;
            case SINGLE_BIOME -> sanitized.singleBiome.isEmpty();
            case CHAOS -> !hasLand;
            case VOID, LEGACY -> false;
            // STRIP_BANDS is strip_world-only (GOALS 36): it needs an ordered, unweighted
            // band sequence the generic preset's layout: section has no field for, so it can
            // never resolve here -- always fall back rather than risk WorldLayoutPlan's own
            // "at least one band biome" validation throwing during world creation.
            case STRIP_BANDS -> true;
        };
        if (unsupported) {
            logger.warn(
                "Layout mode '{}' has no usable biomes for its required role(s); using legacy mode instead.",
                sanitized.mode.serializedName()
            );
            sanitized.mode = LayoutMode.LEGACY;
        }
        return sanitized;
    }

    private static String stripWeight(String configValue) {
        int at = configValue.lastIndexOf('@');
        return at < 0 ? configValue : configValue.substring(0, at);
    }

    private static int clampWithWarning(int value, int minimum, int maximum, String name, Logger logger) {
        int clamped = Math.clamp(value, minimum, maximum);
        if (clamped != value) {
            logger.warn("Clamped {} from {} to {}.", name, value, clamped);
        }
        return clamped;
    }

    private static Map<String, Object> borderMap(BorderConfig config, String objectiveKey) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("initialRadiusBlocks", config.initialRadiusBlocks);
        values.put("finalRadiusBlocks", config.finalRadiusBlocks);
        values.put("resizeDays", config.resizeDays);
        values.put("resizeDelayDays", config.resizeDelayDays);
        values.put("resizeRateBlocks", config.resizeRateBlocks);
        values.put("resizeRateDays", config.resizeRateDays);
        values.put(objectiveKey, config.ensureObjective);
        values.put("resizeStyle", config.resizeStyle.serializedName());
        return values;
    }

    private static Map<String, Object> endBorderMap(EndBorderConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("carryFromOverworld", config.carryFromOverworld);
        values.put("minimumRadiusBlocks", config.minimumRadiusBlocks);
        return values;
    }

    private static Map<String, Object> exteriorMap(ExteriorConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", config.mode.serializedName());
        values.put("boundaryRadiusBlocks", config.boundaryRadiusBlocks);
        values.put("oceanTransitionWidthBlocks", config.oceanTransitionWidthBlocks);
        return values;
    }

    private static Map<String, Object> stripMap(StripConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("widthRadiusBlocks", config.widthRadiusBlocks);
        values.put("widthMode", config.widthMode.serializedName());
        values.put("applyToNether", config.applyToNether);
        return values;
    }

    private static Map<String, Object> layoutMap(LayoutConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", config.mode.serializedName());
        values.put("biomes", config.biomes);
        values.put("regionScaleBlocks", config.regionScaleBlocks);
        values.put("singleBiome", config.singleBiome);
        values.put("roleOverrides", config.roleOverrides);
        return values;
    }

    private static Map<String, Object> spawnMap(SpawnConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("strategy", config.strategy.serializedName());
        return values;
    }

    private static Map<String, Object> singleBiomeMap(SingleBiomeConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("landBiome", config.landBiome);
        values.put("starterBiome", config.starterBiome);
        values.put("starterRadiusBlocks", config.starterRadiusBlocks);
        values.put("spawn", spawnMap(config.spawn));
        values.put("allowRivers", config.allowRivers);
        values.put("allowOceans", config.allowOceans);
        values.put("allowBeaches", config.allowBeaches);
        return values;
    }

    private static Map<String, Object> chaosBiomesMap(ChaosBiomesConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("biomes", config.biomes);
        values.put("regionScaleBlocks", config.regionScaleBlocks);
        values.put("starterBiome", config.starterBiome);
        values.put("starterRadiusBlocks", config.starterRadiusBlocks);
        values.put("spawn", spawnMap(config.spawn));
        values.put("allowRivers", config.allowRivers);
        values.put("allowOceans", config.allowOceans);
        values.put("allowBeaches", config.allowBeaches);
        return values;
    }

    private static Map<String, Object> stripWorldMap(StripWorldConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spawn", spawnMap(config.spawn));
        values.put("bands", stripBandsMap(config.bands));
        return values;
    }

    private static Map<String, Object> oceanIslandMap(OceanIslandConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("islandSource", config.islandSource.serializedName());
        values.put("fluid", config.fluid.serializedName());
        values.put("islandBiome", config.islandBiome);
        values.put("radiusBlocks", config.radiusBlocks);
        values.put("shapeAmplitude", config.shapeAmplitude);
        values.put("shoreWidthBlocks", config.shoreWidthBlocks);
        values.put("oceanShallowWidthBlocks", config.oceanShallowWidthBlocks);
        values.put("oceanDeepenWidthBlocks", config.oceanDeepenWidthBlocks);
        values.put("oceanShallowDepthBlocks", config.oceanShallowDepthBlocks);
        values.put("oceanDeepDepthBlocks", config.oceanDeepDepthBlocks);
        values.put("oceanRegionScaleBlocks", config.oceanRegionScaleBlocks);
        values.put("exclusionZoneEnabled", config.exclusionZoneEnabled);
        values.put("exclusionZoneRadiusBlocks", config.exclusionZoneRadiusBlocks);
        values.put("starterKit", starterKitMap(config.starterKit));
        return values;
    }

    private static Map<String, Object> starterKitMap(StarterKitConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("essentials", config.essentials);
        values.put("extras", config.extras);
        values.put("extrasCount", config.extrasCount);
        return values;
    }

    private static Map<String, Object> stripBandsMap(StripBandsConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("biomes", config.biomes);
        values.put("widthBlocks", config.widthBlocks);
        values.put("seedRandomOrder", config.seedRandomOrder);
        values.put("allowRivers", config.allowRivers);
        values.put("allowOceans", config.allowOceans);
        values.put("allowBeaches", config.allowBeaches);
        return values;
    }

    private static String borderSummary(BorderConfig config, String objectiveName) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "initial=" + config.initialRadiusBlocks
            + ", final=" + config.finalRadiusBlocks
            + ", days=" + config.resizeDays
            + ", delayDays=" + config.resizeDelayDays
            + ", rate=" + (config.resizeRateBlocks == 0
                ? "<total-days>"
                : config.resizeRateBlocks + " blocks/" + config.resizeRateDays + " days")
            + ", style=" + config.resizeStyle.serializedName()
            + ", " + objectiveName + "=" + config.ensureObjective;
    }

    private static String endBorderSummary(EndBorderConfig config) {
        return config.carryFromOverworld
            ? "carried, minimum=" + config.minimumRadiusBlocks
            : "<disabled>";
    }

    private static String exteriorSummary(ExteriorConfig config) {
        if (config.mode == ExteriorMode.NORMAL) {
            return "<normal>";
        }
        return config.mode.serializedName() + ", boundary="
            + (config.boundaryRadiusBlocks == 0 ? "auto" : config.boundaryRadiusBlocks)
            + (config.mode == ExteriorMode.OCEAN ? ", transition=" + config.oceanTransitionWidthBlocks : "");
    }

    private static String stripSummary(StripConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "widthRadius=" + config.widthRadiusBlocks
            + ", widthMode=" + config.widthMode.serializedName()
            + ", applyToNether=" + config.applyToNether;
    }

    private static String singleBiomeSummary(SingleBiomeConfig config) {
        return "landBiome=" + config.landBiome
            + ", starterBiome=" + (config.starterBiome.isEmpty() ? "<none>" : config.starterBiome)
            + ", starterRadiusBlocks=" + config.starterRadiusBlocks
            + ", spawn=" + config.spawn.strategy.serializedName()
            + ", allowRivers=" + config.allowRivers
            + ", allowOceans=" + config.allowOceans
            + ", allowBeaches=" + config.allowBeaches;
    }

    private static String chaosBiomesSummary(ChaosBiomesConfig config) {
        return "biomes=" + config.biomes
            + ", regionScaleBlocks=" + config.regionScaleBlocks
            + ", starterBiome=" + (config.starterBiome.isEmpty() ? "<none>" : config.starterBiome)
            + ", starterRadiusBlocks=" + config.starterRadiusBlocks
            + ", spawn=" + config.spawn.strategy.serializedName()
            + ", allowRivers=" + config.allowRivers
            + ", allowOceans=" + config.allowOceans
            + ", allowBeaches=" + config.allowBeaches;
    }

    private static String stripWorldSummary(StripWorldConfig config) {
        return "spawn=" + config.spawn.strategy.serializedName()
            + ", bands=" + stripBandsSummary(config.bands);
    }

    private static String oceanIslandSummary(OceanIslandConfig config) {
        return "islandSource=" + config.islandSource.serializedName()
            + ", fluid=" + config.fluid.serializedName()
            + ", islandBiome=" + config.islandBiome
            + ", radiusBlocks=" + config.radiusBlocks
            + ", shapeAmplitude=" + config.shapeAmplitude
            + ", shoreWidthBlocks=" + config.shoreWidthBlocks
            + ", oceanShallowWidthBlocks=" + config.oceanShallowWidthBlocks
            + ", oceanDeepenWidthBlocks=" + config.oceanDeepenWidthBlocks
            + ", oceanShallowDepthBlocks=" + config.oceanShallowDepthBlocks
            + ", oceanDeepDepthBlocks=" + config.oceanDeepDepthBlocks
            + ", oceanRegionScaleBlocks=" + config.oceanRegionScaleBlocks
            + ", exclusionZone=" + (config.exclusionZoneEnabled
                ? "radius=" + config.exclusionZoneRadiusBlocks
                : "<disabled>")
            + ", starterKit=" + starterKitSummary(config.starterKit);
    }

    private static String starterKitSummary(StarterKitConfig config) {
        return "essentials=" + config.essentials + ", extras=" + config.extras + ", extrasCount=" + config.extrasCount;
    }

    private static String stripBandsSummary(StripBandsConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "biomes=" + config.biomes
            + ", widthBlocks=" + config.widthBlocks
            + ", seedRandomOrder=" + config.seedRandomOrder
            + ", allowRivers=" + config.allowRivers
            + ", allowOceans=" + config.allowOceans
            + ", allowBeaches=" + config.allowBeaches;
    }

    private static String layoutSummary(LayoutConfig config) {
        if (config.mode == LayoutMode.LEGACY) {
            return "<legacy>";
        }
        return config.mode.serializedName()
            + ", biomes=" + config.biomes
            + ", regionScaleBlocks=" + config.regionScaleBlocks
            + (config.singleBiome.isEmpty() ? "" : ", singleBiome=" + config.singleBiome);
    }

    private static Yaml createYaml() {
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
