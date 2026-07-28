package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.ParseContext;
import media.jlt.minecraft.mods.worldz.config.schema.SanitizeContext;
import media.jlt.minecraft.mods.worldz.config.schema.SchemaSection;
import media.jlt.minecraft.mods.worldz.config.schema.StarterCapsuleSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StarterKitSchema;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.BiomeRole;
import media.jlt.minecraft.mods.worldz.logic.BiomeRoles;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.LightSource;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
import media.jlt.minecraft.mods.worldz.logic.WeightedBiomeListSpec;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
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
    /** Largest supported per-column relief bump for a {@code stacked} layer's own surface
     * (DESIGN §34.7) -- a small fraction of the 30-block minimum default air gap. */
    public static final int MAX_STACKED_RELIEF_BLOCKS = 16;

    static final String YAML_EXTENSION = ".yaml";

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

    static WorldzConfig loadExisting(Path configFile, Logger logger) {
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
        ParseContext ctx = new ParseContext(logger);
        if (object.containsKey("overworldBorder")) {
            config.overworldBorder = SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal")
                .read(object.get("overworldBorder"), ctx);
        }
        if (object.containsKey("netherBorder")) {
            config.netherBorder = SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess")
                .read(object.get("netherBorder"), ctx);
        }
        if (object.containsKey("endBorder")) {
            config.endBorder = SchemaSections.endBorder().read(object.get("endBorder"), ctx);
        }
        if (object.containsKey("overworldExterior")) {
            config.overworldExterior = SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true)
                .read(object.get("overworldExterior"), ctx);
        }
        if (object.containsKey("netherExterior")) {
            config.netherExterior = SchemaSections.exterior("netherExterior", c -> c.netherBorder, false)
                .read(object.get("netherExterior"), ctx);
        }
        if (object.containsKey("strip")) {
            config.strip = SchemaSections.strip().read(object.get("strip"), ctx);
        }
        if (object.containsKey("layout")) {
            config.layout = SchemaSections.layout().read(object.get("layout"), ctx);
        }
        if (object.containsKey("spawn")) {
            config.spawn = SchemaSections.spawn("spawn").read(object.get("spawn"), ctx);
        }
        if (object.containsKey("singleBiome")) {
            config.singleBiome = SchemaSections.singleBiome().read(object.get("singleBiome"), ctx);
        }
        if (object.containsKey("chaosBiomes")) {
            config.chaosBiomes = SchemaSections.chaosBiomes().read(object.get("chaosBiomes"), ctx);
        }
        if (object.containsKey("stripWorld")) {
            config.stripWorld = SchemaSections.stripWorld().read(object.get("stripWorld"), ctx);
        }
        if (object.containsKey("oceanIsland")) {
            config.oceanIsland = SchemaSections.oceanIsland().read(object.get("oceanIsland"), ctx);
        }
        if (object.containsKey("skyIsland")) {
            config.skyIsland = SchemaSections.skyIsland().read(object.get("skyIsland"), ctx);
        }
        if (object.containsKey("chunkIsland")) {
            config.chunkIsland = SchemaSections.chunkIsland().read(object.get("chunkIsland"), ctx);
        }
        if (object.containsKey("cave")) {
            config.cave = SchemaSections.cave().read(object.get("cave"), ctx);
        }
        if (object.containsKey("netherStart")) {
            config.netherStart = SchemaSections.netherStart().read(object.get("netherStart"), ctx);
        }
        if (object.containsKey("endStart")) {
            config.endStart = SchemaSections.endStart().read(object.get("endStart"), ctx);
        }
        if (object.containsKey("flat")) {
            config.flat = SchemaSections.flat().read(object.get("flat"), ctx);
        }
        if (object.containsKey("deepFlat")) {
            config.deepFlat = SchemaSections.deepFlat().read(object.get("deepFlat"), ctx);
        }
        if (object.containsKey("stacked")) {
            config.stacked = SchemaSections.stacked().read(object.get("stacked"), ctx);
        }
        if (object.containsKey("foreverNight")) {
            config.foreverNight = SchemaSections.foreverNight().read(object.get("foreverNight"), ctx);
        }
        if (object.containsKey("risingLava")) {
            config.risingLava = SchemaSections.risingLava().read(object.get("risingLava"), ctx);
        }
        if (object.containsKey("structureDistance")) {
            config.structureDistance = SchemaSections.structureDistance().read(object.get("structureDistance"), ctx);
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

        SanitizeContext ctx = new SanitizeContext(logger, this);
        overworldBorder = SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal").sanitize(overworldBorder, ctx);
        netherBorder = SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess").sanitize(netherBorder, ctx);
        endBorder = SchemaSections.endBorder().sanitize(endBorder, ctx);
        overworldExterior = SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true).sanitize(overworldExterior, ctx);
        netherExterior = SchemaSections.exterior("netherExterior", c -> c.netherBorder, false).sanitize(netherExterior, ctx);
        strip = SchemaSections.strip().sanitize(strip, ctx);
        layout = SchemaSections.layout().sanitize(layout, ctx);
        spawn = SchemaSections.spawn("spawn").sanitize(spawn, ctx);
        singleBiome = SchemaSections.singleBiome().sanitize(singleBiome, ctx);
        chaosBiomes = SchemaSections.chaosBiomes().sanitize(chaosBiomes, ctx);
        stripWorld = SchemaSections.stripWorld().sanitize(stripWorld, ctx);
        oceanIsland = SchemaSections.oceanIsland().sanitize(oceanIsland, ctx);
        skyIsland = SchemaSections.skyIsland().sanitize(skyIsland, ctx);
        chunkIsland = SchemaSections.chunkIsland().sanitize(chunkIsland, ctx);
        cave = SchemaSections.cave().sanitize(cave, ctx);
        netherStart = SchemaSections.netherStart().sanitize(netherStart, ctx);
        endStart = SchemaSections.endStart().sanitize(endStart, ctx);
        flat = SchemaSections.flat().sanitize(flat, ctx);
        deepFlat = SchemaSections.deepFlat().sanitize(deepFlat, ctx);
        stacked = SchemaSections.stacked().sanitize(stacked, ctx);
        foreverNight = SchemaSections.foreverNight().sanitize(foreverNight, ctx);
        risingLava = SchemaSections.risingLava().sanitize(risingLava, ctx);
        structureDistance = SchemaSections.structureDistance().sanitize(structureDistance, ctx);
        return this;
    }

    static SingleBiomeConfig sanitizeSingleBiome(SingleBiomeConfig config, Logger logger) {
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

        sanitized.spawn = SchemaSections.spawn("singleBiome.spawn").sanitize(sanitized.spawn, new SanitizeContext(logger, null));
        return sanitized;
    }

    static ChaosBiomesConfig sanitizeChaosBiomes(ChaosBiomesConfig config, Logger logger) {
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

        sanitized.spawn = SchemaSections.spawn("chaosBiomes.spawn").sanitize(sanitized.spawn, new SanitizeContext(logger, null));
        return sanitized;
    }

    static StripWorldConfig sanitizeStripWorld(StripWorldConfig config, Logger logger) {
        StripWorldConfig sanitized = config == null ? new StripWorldConfig() : config;
        sanitized.spawn = SchemaSections.spawn("stripWorld.spawn").sanitize(sanitized.spawn, new SanitizeContext(logger, null));
        sanitized.bands = sanitizeStripBands(sanitized.bands, logger);
        return sanitized;
    }

    static OceanIslandConfig sanitizeOceanIsland(OceanIslandConfig config, Logger logger) {
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
        sanitized.starterKit = sanitizeStarterKit(sanitized.starterKit, "oceanIsland.starterKit", logger);
        return sanitized;
    }

    static SkyIslandConfig sanitizeSkyIsland(SkyIslandConfig config, Logger logger) {
        SkyIslandConfig sanitized = config == null ? new SkyIslandConfig() : config;

        String originalBiome = sanitized.islandBiome;
        sanitized.islandBiome = sanitizeSingleBiomeId(sanitized.islandBiome, "skyIsland.islandBiome", logger);
        if (sanitized.islandBiome.isEmpty()) {
            sanitized.islandBiome = new SkyIslandConfig().islandBiome;
            logger.warn("Invalid skyIsland.islandBiome '{}'; using the default instead.", originalBiome);
        }

        sanitized.radiusBlocks = clampWithWarning(
            sanitized.radiusBlocks, MIN_ISLAND_RADIUS_BLOCKS, MAX_ISLAND_RADIUS_BLOCKS, "skyIsland.radiusBlocks", logger
        );
        double clampedAmplitude = Math.clamp(sanitized.shapeAmplitude, 0.0, IslandShapeProfile.MAX_AMPLITUDE);
        if (clampedAmplitude != sanitized.shapeAmplitude) {
            logger.warn("Clamped skyIsland.shapeAmplitude from {} to {}.", sanitized.shapeAmplitude, clampedAmplitude);
            sanitized.shapeAmplitude = clampedAmplitude;
        }
        sanitized.thicknessBlocks = clampWithWarning(
            sanitized.thicknessBlocks, SkyIslandPlan.MIN_THICKNESS_BLOCKS, SkyIslandPlan.MAX_THICKNESS_BLOCKS,
            "skyIsland.thicknessBlocks", logger
        );
        sanitized.chestTier = sanitized.chestTier == null ? StarterKitTier.MEDIUM : sanitized.chestTier;
        sanitized.easyKit = sanitizeStarterKit(sanitized.easyKit, "skyIsland.easyKit", logger);
        sanitized.mediumKit = sanitizeStarterKit(sanitized.mediumKit, "skyIsland.mediumKit", logger);
        sanitized.hardKit = sanitizeStarterKit(sanitized.hardKit, "skyIsland.hardKit", logger);
        sanitized.floatingIslands = sanitizeFloatingIslands(sanitized.floatingIslands, logger);
        return sanitized;
    }

    static CaveConfig sanitizeCave(CaveConfig config, Logger logger) {
        CaveConfig sanitized = config == null ? new CaveConfig() : config;

        if (sanitized.sealedSurface && sanitized.sealedSurfaceY < CaveConfig.MIN_SEALED_SURFACE_Y) {
            logger.warn(
                "Clamped cave.sealedSurfaceY from {} to {}.", sanitized.sealedSurfaceY, CaveConfig.MIN_SEALED_SURFACE_Y
            );
            sanitized.sealedSurfaceY = CaveConfig.MIN_SEALED_SURFACE_Y;
        }
        sanitized.sealedSurfaceBlock = sanitized.sealedSurfaceBlock == null
            ? CavePlan.DEFAULT_SEALED_SURFACE_BLOCK : sanitized.sealedSurfaceBlock;
        sanitized.sealedSurfaceThicknessBlocks = clampWithWarning(
            sanitized.sealedSurfaceThicknessBlocks, CavePlan.MIN_SEALED_SURFACE_THICKNESS_BLOCKS,
            CavePlan.MAX_SEALED_SURFACE_THICKNESS_BLOCKS, "cave.sealedSurfaceThicknessBlocks", logger
        );
        sanitized.cavernRadiusBlocks = clampWithWarning(
            sanitized.cavernRadiusBlocks, CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS,
            "cave.cavernRadiusBlocks", logger
        );
        sanitized.cavernHeightBlocks = clampWithWarning(
            sanitized.cavernHeightBlocks, CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS,
            "cave.cavernHeightBlocks", logger
        );
        sanitized.chestTier = sanitized.chestTier == null ? StarterKitTier.MEDIUM : sanitized.chestTier;
        sanitized.easyKit = sanitizeStarterKit(sanitized.easyKit, "cave.easyKit", logger);
        sanitized.mediumKit = sanitizeStarterKit(sanitized.mediumKit, "cave.mediumKit", logger);
        sanitized.hardKit = sanitizeStarterKit(sanitized.hardKit, "cave.hardKit", logger);
        return sanitized;
    }

    static NetherStartConfig sanitizeNetherStart(NetherStartConfig config, Logger logger) {
        NetherStartConfig sanitized = config == null ? new NetherStartConfig() : config;
        sanitized.spawnY = clampWithWarning(
            sanitized.spawnY, NetherStartPlan.MIN_SPAWN_Y, NetherStartPlan.MAX_SPAWN_Y, "netherStart.spawnY", logger
        );
        sanitized.chestTier = sanitized.chestTier == null ? StarterKitTier.MEDIUM : sanitized.chestTier;
        sanitized.easyKit = sanitizeStarterKit(sanitized.easyKit, "netherStart.easyKit", logger);
        sanitized.mediumKit = sanitizeStarterKit(sanitized.mediumKit, "netherStart.mediumKit", logger);
        sanitized.hardKit = sanitizeStarterKit(sanitized.hardKit, "netherStart.hardKit", logger);
        sanitized.capsule = sanitizeStarterCapsule(
            sanitized.capsule, "netherStart.capsule",
            NetherStartPlan.MIN_CAPSULE_SIZE_BLOCKS, NetherStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, NetherStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, NetherStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS,
            logger
        );
        return sanitized;
    }

    static StarterCapsuleConfig sanitizeStarterCapsule(
        StarterCapsuleConfig config, String name,
        int minSize, int maxSize, int minHeight, int maxHeight, int minSpacing, int maxSpacing,
        Logger logger
    ) {
        SchemaSection<StarterCapsuleConfig> schema = new StarterCapsuleSchema(name, minSize, maxSize, minHeight, maxHeight, minSpacing, maxSpacing);
        return schema.sanitize(config, new SanitizeContext(logger, null));
    }

    static EndStartConfig sanitizeEndStart(EndStartConfig config, Logger logger) {
        EndStartConfig sanitized = config == null ? new EndStartConfig() : config;
        sanitized.chestTier = sanitized.chestTier == null ? StarterKitTier.MEDIUM : sanitized.chestTier;
        sanitized.easyKit = sanitizeStarterKit(sanitized.easyKit, "endStart.easyKit", logger);
        sanitized.mediumKit = sanitizeStarterKit(sanitized.mediumKit, "endStart.mediumKit", logger);
        sanitized.hardKit = sanitizeStarterKit(sanitized.hardKit, "endStart.hardKit", logger);
        sanitized.capsule = sanitizeStarterCapsule(
            sanitized.capsule, "endStart.capsule",
            EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS, EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            EndStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, EndStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            EndStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, EndStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS,
            logger
        );
        return sanitized;
    }

    static FlatConfig sanitizeFlat(FlatConfig config, Logger logger) {
        FlatConfig sanitized = config == null ? new FlatConfig() : config;
        if (sanitized.layers == null || sanitized.layers.isEmpty()) {
            logger.warn("flat.layers was empty; using the default layer stack.");
            sanitized.layers = new FlatConfig().layers;
        }
        if (sanitized.biome == null || sanitized.biome.isBlank()) {
            logger.warn("flat.biome was blank; defaulting to minecraft:plains.");
            sanitized.biome = "minecraft:plains";
        }
        if (sanitized.structureOverrides == null) {
            sanitized.structureOverrides = new FlatConfig().structureOverrides;
        }
        return sanitized;
    }

    static DeepFlatConfig sanitizeDeepFlat(DeepFlatConfig config, Logger logger) {
        DeepFlatConfig sanitized = config == null ? new DeepFlatConfig() : config;
        sanitized.surfaceY = clampWithWarning(
            sanitized.surfaceY, DeepFlatPlan.MIN_SURFACE_Y, DeepFlatPlan.MAX_SURFACE_Y, "deepFlat.surfaceY", logger
        );
        if (sanitized.capLayers == null || sanitized.capLayers.isEmpty()) {
            logger.warn("deepFlat.capLayers was empty; using the default cap layer stack.");
            sanitized.capLayers = new DeepFlatConfig().capLayers;
        }
        if (sanitized.riverExclusionRadiusBlocks < 0) {
            logger.warn("Clamped deepFlat.riverExclusionRadiusBlocks from {} to 0.", sanitized.riverExclusionRadiusBlocks);
            sanitized.riverExclusionRadiusBlocks = 0;
        }
        return sanitized;
    }

    static StackedConfig sanitizeStacked(StackedConfig config, Logger logger) {
        StackedConfig sanitized = config == null ? new StackedConfig() : config;
        if (sanitized.layers == null || sanitized.layers.isEmpty()) {
            logger.warn("stacked.layers was empty; using the default layer stack.");
            sanitized.layers = new StackedConfig().layers;
        }
        sanitized.worldSizeChunks = clampWithWarning(
            sanitized.worldSizeChunks, 0, MAX_BORDER_RADIUS_BLOCKS / 16, "stacked.worldSizeChunks", logger
        );
        sanitized.reliefBlocks = clampWithWarning(
            sanitized.reliefBlocks, 0, MAX_STACKED_RELIEF_BLOCKS, "stacked.reliefBlocks", logger
        );
        return sanitized;
    }

    static ForeverNightConfig sanitizeForeverNight(ForeverNightConfig config, Logger logger) {
        ForeverNightConfig sanitized = config == null ? new ForeverNightConfig() : config;
        sanitized.lockAfterDays = clampWithWarning(
            sanitized.lockAfterDays, 0, MAX_BORDER_RESIZE_DAYS, "foreverNight.lockAfterDays", logger
        );
        return sanitized;
    }

    static RisingLavaConfig sanitizeRisingLava(RisingLavaConfig config, Logger logger) {
        RisingLavaConfig sanitized = config == null ? new RisingLavaConfig() : config;
        int minY = FlatConfig.OVERWORLD_MIN_Y;
        int maxBuildY = FlatConfig.OVERWORLD_MIN_Y + FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS - 1;
        sanitized.delayDays = clampWithWarning(sanitized.delayDays, 0, MAX_BORDER_RESIZE_DAYS, "risingLava.delayDays", logger);
        sanitized.startY = clampWithWarning(sanitized.startY, minY, maxBuildY, "risingLava.startY", logger);
        sanitized.maxY = clampWithWarning(sanitized.maxY, minY, maxBuildY, "risingLava.maxY", logger);
        if (sanitized.maxY < sanitized.startY) {
            logger.warn("risingLava.maxY ({}) was below startY ({}); raised to match.", sanitized.maxY, sanitized.startY);
            sanitized.maxY = sanitized.startY;
        }
        sanitized.rateBlocks = clampWithWarning(sanitized.rateBlocks, 1, MAX_BORDER_RATE_BLOCKS, "risingLava.rateBlocks", logger);
        sanitized.rateDays = clampWithWarning(sanitized.rateDays, 1, MAX_BORDER_RESIZE_DAYS, "risingLava.rateDays", logger);
        return sanitized;
    }

    static StructureDistanceConfig sanitizeStructureDistance(StructureDistanceConfig config, Logger logger) {
        StructureDistanceConfig sanitized = config == null ? new StructureDistanceConfig() : config;
        sanitized.minDistanceBlocks = clampWithWarning(
            sanitized.minDistanceBlocks, 0, MAX_BORDER_RADIUS_BLOCKS, "structureDistance.minDistanceBlocks", logger
        );
        if (sanitized.exemptStructureSets == null) {
            sanitized.exemptStructureSets = new StructureDistanceConfig().exemptStructureSets;
        }
        return sanitized;
    }

    static FloatingIslandsConfig sanitizeFloatingIslands(FloatingIslandsConfig config, Logger logger) {
        FloatingIslandsConfig sanitized = config == null ? new FloatingIslandsConfig() : config;

        sanitized.minRadiusBlocks = clampWithWarning(
            sanitized.minRadiusBlocks, MIN_ISLAND_RADIUS_BLOCKS, MAX_ISLAND_RADIUS_BLOCKS, "skyIsland.floatingIslands.minRadiusBlocks", logger
        );
        sanitized.maxRadiusBlocks = clampWithWarning(
            sanitized.maxRadiusBlocks, sanitized.minRadiusBlocks, MAX_ISLAND_RADIUS_BLOCKS,
            "skyIsland.floatingIslands.maxRadiusBlocks", logger
        );
        double clampedAmplitude = Math.clamp(sanitized.shapeAmplitude, 0.0, IslandShapeProfile.MAX_AMPLITUDE);
        if (clampedAmplitude != sanitized.shapeAmplitude) {
            logger.warn("Clamped skyIsland.floatingIslands.shapeAmplitude from {} to {}.", sanitized.shapeAmplitude, clampedAmplitude);
            sanitized.shapeAmplitude = clampedAmplitude;
        }
        sanitized.cellSizeBlocks = clampWithWarning(
            sanitized.cellSizeBlocks, MIN_LAYOUT_REGION_SCALE_BLOCKS, MAX_LAYOUT_REGION_SCALE_BLOCKS,
            "skyIsland.floatingIslands.cellSizeBlocks", logger
        );
        double clampedChance = Math.clamp(sanitized.spawnChance, 0.0, 1.0);
        if (clampedChance != sanitized.spawnChance) {
            logger.warn("Clamped skyIsland.floatingIslands.spawnChance from {} to {}.", sanitized.spawnChance, clampedChance);
            sanitized.spawnChance = clampedChance;
        }

        BiomeListSpec biomeSpec = BiomeListSpec.parse(sanitized.islandBiomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            logger.warn("Ignoring invalid skyIsland.floatingIslands.islandBiomes biome '{}'.", invalid);
        }
        for (BiomeListSpec.Entry entry : biomeSpec.entries()) {
            if (entry.tag()) {
                logger.warn(
                    "Ignoring skyIsland.floatingIslands.islandBiomes biome tag '#{}'; floating islands require concrete biome ids.",
                    entry.id()
                );
            }
        }
        sanitized.islandBiomes = new ArrayList<>(
            biomeSpec.entries().stream().filter(entry -> !entry.tag()).map(BiomeListSpec.Entry::id).toList()
        );
        if (sanitized.biomeVariety && sanitized.islandBiomes.isEmpty()) {
            logger.warn("skyIsland.floatingIslands.biomeVariety is set but has no usable biomes; disabling biome variety.");
            sanitized.biomeVariety = false;
        }
        if (sanitized.naturalBiome && sanitized.biomeVariety) {
            // Not a hard conflict -- naturalBiome deliberately takes precedence (DESIGN §28.4) --
            // but honoring the preferred setting silently makes biomeVariety's own pool look
            // configured yet dead, which is confusing to debug from the outside. Log the
            // limitation instead of just picking a winner quietly.
            logger.warn(
                "skyIsland.floatingIslands.naturalBiome and biomeVariety are both enabled; "
                    + "naturalBiome takes precedence and the islandBiomes pool will be unused."
            );
        }
        if (sanitized.naturalBiome) {
            // Always true whenever floating islands are enabled (the guaranteed village, GOALS 07/
            // DESIGN §28.3, isn't itself optional) -- the village's own island can never honor
            // naturalBiome, since its real vanilla village structure was force-generated for a
            // specific structure-compatible biome (see FloatingIslandsPlan.Hit#village()).
            logger.warn(
                "skyIsland.floatingIslands.naturalBiome is enabled: the guaranteed village's own "
                    + "island always keeps its forced, structure-compatible biome instead, since its "
                    + "real vanilla village structure requires it."
            );
        }

        if (sanitized.exclusionZoneRadiusBlocks < 1) {
            logger.warn("Clamped skyIsland.floatingIslands.exclusionZoneRadiusBlocks from {} to 1.", sanitized.exclusionZoneRadiusBlocks);
            sanitized.exclusionZoneRadiusBlocks = 1;
        }

        sanitized.oreFeatureIds = new ArrayList<>(sanitized.oreFeatureIds.stream().map(String::trim).filter(id -> !id.isEmpty()).toList());
        if (sanitized.oreDepositsEnabled && sanitized.oreFeatureIds.isEmpty()) {
            logger.warn("skyIsland.floatingIslands.oreDepositsEnabled is set but has no usable feature ids; disabling ore deposits.");
            sanitized.oreDepositsEnabled = false;
        }
        sanitized.lootKit = sanitizeStarterKit(sanitized.lootKit, "skyIsland.floatingIslands.lootKit", logger);
        return sanitized;
    }

    static StarterKitConfig sanitizeStarterKit(StarterKitConfig config, String name, Logger logger) {
        return new StarterKitSchema(name).sanitize(config, new SanitizeContext(logger, null));
    }

    static StripBandsConfig sanitizeStripBands(StripBandsConfig config, Logger logger) {
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

    static String sanitizeSingleBiomeId(String value, String name, Logger logger) {
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
            + ", overworldBorder=" + SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal").summary(overworldBorder)
            + ", netherBorder=" + SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess").summary(netherBorder)
            + ", endBorder=" + SchemaSections.endBorder().summary(endBorder)
            + ", overworldExterior=" + SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true).summary(overworldExterior)
            + ", netherExterior=" + SchemaSections.exterior("netherExterior", c -> c.netherBorder, false).summary(netherExterior)
            + ", strip=" + SchemaSections.strip().summary(strip)
            + ", layout=" + SchemaSections.layout().summary(layout)
            + ", spawn=" + SchemaSections.spawn("spawn").summary(spawn)
            + ", singleBiome=" + SchemaSections.singleBiome().summary(singleBiome)
            + ", chaosBiomes=" + SchemaSections.chaosBiomes().summary(chaosBiomes)
            + ", stripWorld=" + SchemaSections.stripWorld().summary(stripWorld)
            + ", oceanIsland=" + SchemaSections.oceanIsland().summary(oceanIsland)
            + ", skyIsland=" + SchemaSections.skyIsland().summary(skyIsland)
            + ", chunkIsland=" + SchemaSections.chunkIsland().summary(chunkIsland)
            + ", cave=" + SchemaSections.cave().summary(cave)
            + ", netherStart=" + SchemaSections.netherStart().summary(netherStart)
            + ", endStart=" + SchemaSections.endStart().summary(endStart)
            + ", flat=" + SchemaSections.flat().summary(flat)
            + ", deepFlat=" + SchemaSections.deepFlat().summary(deepFlat)
            + ", stacked=" + SchemaSections.stacked().summary(stacked)
            + ", foreverNight=" + SchemaSections.foreverNight().summary(foreverNight)
            + ", risingLava=" + SchemaSections.risingLava().summary(risingLava)
            + ", structureDistance=" + SchemaSections.structureDistance().summary(structureDistance)
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
        values.put("overworldBorder", SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal").toMap(overworldBorder));
        values.put("netherBorder", SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess").toMap(netherBorder));
        values.put("endBorder", SchemaSections.endBorder().toMap(endBorder));
        values.put("overworldExterior", SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true).toMap(overworldExterior));
        values.put("netherExterior", SchemaSections.exterior("netherExterior", c -> c.netherBorder, false).toMap(netherExterior));
        values.put("strip", SchemaSections.strip().toMap(strip));
        values.put("layout", SchemaSections.layout().toMap(layout));
        values.put("spawn", SchemaSections.spawn("spawn").toMap(spawn));
        values.put("singleBiome", SchemaSections.singleBiome().toMap(singleBiome));
        values.put("chaosBiomes", SchemaSections.chaosBiomes().toMap(chaosBiomes));
        values.put("stripWorld", SchemaSections.stripWorld().toMap(stripWorld));
        values.put("oceanIsland", SchemaSections.oceanIsland().toMap(oceanIsland));
        values.put("skyIsland", SchemaSections.skyIsland().toMap(skyIsland));
        values.put("chunkIsland", SchemaSections.chunkIsland().toMap(chunkIsland));
        values.put("cave", SchemaSections.cave().toMap(cave));
        values.put("netherStart", SchemaSections.netherStart().toMap(netherStart));
        values.put("endStart", SchemaSections.endStart().toMap(endStart));
        values.put("flat", SchemaSections.flat().toMap(flat));
        values.put("deepFlat", SchemaSections.deepFlat().toMap(deepFlat));
        values.put("stacked", SchemaSections.stacked().toMap(stacked));
        values.put("foreverNight", SchemaSections.foreverNight().toMap(foreverNight));
        values.put("risingLava", SchemaSections.risingLava().toMap(risingLava));
        values.put("structureDistance", SchemaSections.structureDistance().toMap(structureDistance));
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

    static List<String> readStringList(Object value, String name, Logger logger) {
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

    static String readString(Object value, String name) {
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return string;
    }

    static boolean readBoolean(Object value, String name) {
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException(name + " must be a boolean");
        }
        return booleanValue;
    }

    static BorderConfig readBorderConfig(Object value, String name, String objectiveKey) {
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

    static EndBorderConfig readEndBorderConfig(Object value, String name) {
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

    static ExteriorConfig readExteriorConfig(Object value, String name) {
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

    static StripConfig readStripConfig(Object value, String name) {
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

    static ChunkIslandConfig readChunkIslandConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        ChunkIslandConfig config = new ChunkIslandConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("spawnChance")) {
            config.spawnChance = readDouble(map.get("spawnChance"), name + ".spawnChance");
        }
        if (map.containsKey("cellSizeChunks")) {
            config.cellSizeChunks = readInt(map.get("cellSizeChunks"), name + ".cellSizeChunks");
        }
        if (map.containsKey("topOnly")) {
            config.topOnly = readBoolean(map.get("topOnly"), name + ".topOnly");
        }
        if (map.containsKey("topOnlyDepthBlocks")) {
            config.topOnlyDepthBlocks = readInt(map.get("topOnlyDepthBlocks"), name + ".topOnlyDepthBlocks");
        }
        if (map.containsKey("exclusionZoneEnabled")) {
            config.exclusionZoneEnabled = readBoolean(map.get("exclusionZoneEnabled"), name + ".exclusionZoneEnabled");
        }
        if (map.containsKey("exclusionZoneRadiusBlocks")) {
            config.exclusionZoneRadiusBlocks = readInt(map.get("exclusionZoneRadiusBlocks"), name + ".exclusionZoneRadiusBlocks");
        }
        if (map.containsKey("scatteredTopOnlyChance")) {
            config.scatteredTopOnlyChance = readDouble(map.get("scatteredTopOnlyChance"), name + ".scatteredTopOnlyChance");
        }
        if (map.containsKey("applyToNether")) {
            config.applyToNether = readBoolean(map.get("applyToNether"), name + ".applyToNether");
        }
        if (map.containsKey("applyToEnd")) {
            config.applyToEnd = readBoolean(map.get("applyToEnd"), name + ".applyToEnd");
        }
        if (map.containsKey("geodeFeatureIds")) {
            config.geodeFeatureIds = readStringList(map.get("geodeFeatureIds"), name + ".geodeFeatureIds", logger);
        }
        return config;
    }

    static LayoutConfig readLayoutConfig(Object value, String name, Logger logger) {
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

    static SpawnConfig readSpawnConfig(Object value, String name) {
        return SchemaSections.spawn(name).read(value, new ParseContext(NOPLogger.NOP_LOGGER));
    }

    static SingleBiomeConfig readSingleBiomeConfig(Object value, String name) {
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

    static ChaosBiomesConfig readChaosBiomesConfig(Object value, String name, Logger logger) {
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

    static StripWorldConfig readStripWorldConfig(Object value, String name, Logger logger) {
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

    static OceanIslandConfig readOceanIslandConfig(Object value, String name, Logger logger) {
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

    static SkyIslandConfig readSkyIslandConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        SkyIslandConfig config = new SkyIslandConfig();
        if (map.containsKey("islandBiome")) {
            config.islandBiome = readString(map.get("islandBiome"), name + ".islandBiome");
        }
        if (map.containsKey("radiusBlocks")) {
            config.radiusBlocks = readInt(map.get("radiusBlocks"), name + ".radiusBlocks");
        }
        if (map.containsKey("shapeAmplitude")) {
            config.shapeAmplitude = readDouble(map.get("shapeAmplitude"), name + ".shapeAmplitude");
        }
        if (map.containsKey("surfaceY")) {
            config.surfaceY = readInt(map.get("surfaceY"), name + ".surfaceY");
        }
        if (map.containsKey("thicknessBlocks")) {
            config.thicknessBlocks = readInt(map.get("thicknessBlocks"), name + ".thicknessBlocks");
        }
        if (map.containsKey("chestTier")) {
            config.chestTier = StarterKitTier.parse(readString(map.get("chestTier"), name + ".chestTier"));
        }
        if (map.containsKey("easyKit")) {
            config.easyKit = readStarterKitConfig(map.get("easyKit"), name + ".easyKit", logger);
        }
        if (map.containsKey("mediumKit")) {
            config.mediumKit = readStarterKitConfig(map.get("mediumKit"), name + ".mediumKit", logger);
        }
        if (map.containsKey("hardKit")) {
            config.hardKit = readStarterKitConfig(map.get("hardKit"), name + ".hardKit", logger);
        }
        if (map.containsKey("applyToNether")) {
            config.applyToNether = readBoolean(map.get("applyToNether"), name + ".applyToNether");
        }
        if (map.containsKey("floatingIslands")) {
            config.floatingIslands = readFloatingIslandsConfig(map.get("floatingIslands"), name + ".floatingIslands", logger);
        }
        return config;
    }

    static CaveConfig readCaveConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        CaveConfig config = new CaveConfig();
        if (map.containsKey("spawnDepthY")) {
            config.spawnDepthY = readInt(map.get("spawnDepthY"), name + ".spawnDepthY");
        }
        if (map.containsKey("sealedSurface")) {
            config.sealedSurface = readBoolean(map.get("sealedSurface"), name + ".sealedSurface");
        }
        if (map.containsKey("sealedSurfaceY")) {
            config.sealedSurfaceY = readInt(map.get("sealedSurfaceY"), name + ".sealedSurfaceY");
        }
        if (map.containsKey("sealedSurfaceBlock")) {
            config.sealedSurfaceBlock =
                SealedSurfaceBlock.parse(readString(map.get("sealedSurfaceBlock"), name + ".sealedSurfaceBlock"));
        }
        if (map.containsKey("sealedSurfaceThicknessBlocks")) {
            config.sealedSurfaceThicknessBlocks =
                readInt(map.get("sealedSurfaceThicknessBlocks"), name + ".sealedSurfaceThicknessBlocks");
        }
        if (map.containsKey("cavernEnabled")) {
            config.cavernEnabled = readBoolean(map.get("cavernEnabled"), name + ".cavernEnabled");
        }
        if (map.containsKey("cavernRadiusBlocks")) {
            config.cavernRadiusBlocks = readInt(map.get("cavernRadiusBlocks"), name + ".cavernRadiusBlocks");
        }
        if (map.containsKey("cavernHeightBlocks")) {
            config.cavernHeightBlocks = readInt(map.get("cavernHeightBlocks"), name + ".cavernHeightBlocks");
        }
        if (map.containsKey("chestEnabled")) {
            config.chestEnabled = readBoolean(map.get("chestEnabled"), name + ".chestEnabled");
        }
        if (map.containsKey("chestTier")) {
            config.chestTier = StarterKitTier.parse(readString(map.get("chestTier"), name + ".chestTier"));
        }
        if (map.containsKey("easyKit")) {
            config.easyKit = readStarterKitConfig(map.get("easyKit"), name + ".easyKit", logger);
        }
        if (map.containsKey("mediumKit")) {
            config.mediumKit = readStarterKitConfig(map.get("mediumKit"), name + ".mediumKit", logger);
        }
        if (map.containsKey("hardKit")) {
            config.hardKit = readStarterKitConfig(map.get("hardKit"), name + ".hardKit", logger);
        }
        return config;
    }

    static NetherStartConfig readNetherStartConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        NetherStartConfig config = new NetherStartConfig();
        if (map.containsKey("spawnY")) {
            config.spawnY = readInt(map.get("spawnY"), name + ".spawnY");
        }
        if (map.containsKey("chestTier")) {
            config.chestTier = StarterKitTier.parse(readString(map.get("chestTier"), name + ".chestTier"));
        }
        if (map.containsKey("easyKit")) {
            config.easyKit = readStarterKitConfig(map.get("easyKit"), name + ".easyKit", logger);
        }
        if (map.containsKey("mediumKit")) {
            config.mediumKit = readStarterKitConfig(map.get("mediumKit"), name + ".mediumKit", logger);
        }
        if (map.containsKey("hardKit")) {
            config.hardKit = readStarterKitConfig(map.get("hardKit"), name + ".hardKit", logger);
        }
        if (map.containsKey("forceCapsule")) {
            config.forceCapsule = readBoolean(map.get("forceCapsule"), name + ".forceCapsule");
        }
        if (map.containsKey("capsule")) {
            config.capsule = readStarterCapsuleConfig(map.get("capsule"), name + ".capsule");
        }
        return config;
    }

    static StarterCapsuleConfig readStarterCapsuleConfig(Object value, String name) {
        // Bounds are irrelevant to read (only sanitize consults them); a schema instance is
        // constructed with placeholder bounds purely to reuse the shared read logic.
        StarterCapsuleSchema schema = new StarterCapsuleSchema(
            name, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE
        );
        return schema.read(value, new ParseContext(NOPLogger.NOP_LOGGER));
    }

    static EndStartConfig readEndStartConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        EndStartConfig config = new EndStartConfig();
        if (map.containsKey("chestTier")) {
            config.chestTier = StarterKitTier.parse(readString(map.get("chestTier"), name + ".chestTier"));
        }
        if (map.containsKey("easyKit")) {
            config.easyKit = readStarterKitConfig(map.get("easyKit"), name + ".easyKit", logger);
        }
        if (map.containsKey("mediumKit")) {
            config.mediumKit = readStarterKitConfig(map.get("mediumKit"), name + ".mediumKit", logger);
        }
        if (map.containsKey("hardKit")) {
            config.hardKit = readStarterKitConfig(map.get("hardKit"), name + ".hardKit", logger);
        }
        if (map.containsKey("capsule")) {
            config.capsule = readStarterCapsuleConfig(map.get("capsule"), name + ".capsule");
        }
        return config;
    }

    static FlatConfig readFlatConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        FlatConfig config = new FlatConfig();
        if (map.containsKey("layers")) {
            config.layers = readStringList(map.get("layers"), name + ".layers", logger);
        }
        if (map.containsKey("biome")) {
            config.biome = readString(map.get("biome"), name + ".biome");
        }
        if (map.containsKey("decoration")) {
            config.decoration = readBoolean(map.get("decoration"), name + ".decoration");
        }
        if (map.containsKey("structureOverrides")) {
            config.structureOverrides = readStringList(map.get("structureOverrides"), name + ".structureOverrides", logger);
        }
        return config;
    }

    static DeepFlatConfig readDeepFlatConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        DeepFlatConfig config = new DeepFlatConfig();
        if (map.containsKey("surfaceY")) {
            config.surfaceY = readInt(map.get("surfaceY"), name + ".surfaceY");
        }
        if (map.containsKey("capLayers")) {
            config.capLayers = readStringList(map.get("capLayers"), name + ".capLayers", logger);
        }
        if (map.containsKey("riversEnabled")) {
            config.riversEnabled = readBoolean(map.get("riversEnabled"), name + ".riversEnabled");
        }
        if (map.containsKey("riverExclusionRadiusBlocks")) {
            config.riverExclusionRadiusBlocks = readInt(map.get("riverExclusionRadiusBlocks"), name + ".riverExclusionRadiusBlocks");
        }
        return config;
    }

    static StackedConfig readStackedConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StackedConfig config = new StackedConfig();
        if (map.containsKey("layers")) {
            config.layers = readStringList(map.get("layers"), name + ".layers", logger);
        }
        if (map.containsKey("seedRandomizedOrder")) {
            config.seedRandomizedOrder = readBoolean(map.get("seedRandomizedOrder"), name + ".seedRandomizedOrder");
        }
        if (map.containsKey("worldSizeChunks")) {
            config.worldSizeChunks = readInt(map.get("worldSizeChunks"), name + ".worldSizeChunks");
        }
        if (map.containsKey("reliefBlocks")) {
            config.reliefBlocks = readInt(map.get("reliefBlocks"), name + ".reliefBlocks");
        }
        if (map.containsKey("forceTopVillage")) {
            config.forceTopVillage = readBoolean(map.get("forceTopVillage"), name + ".forceTopVillage");
        }
        return config;
    }

    static ForeverNightConfig readForeverNightConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        ForeverNightConfig config = new ForeverNightConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("lockAfterDays")) {
            config.lockAfterDays = readInt(map.get("lockAfterDays"), name + ".lockAfterDays");
        }
        if (map.containsKey("relaxInsomnia")) {
            config.relaxInsomnia = readBoolean(map.get("relaxInsomnia"), name + ".relaxInsomnia");
        }
        return config;
    }

    static RisingLavaConfig readRisingLavaConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        RisingLavaConfig config = new RisingLavaConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("delayDays")) {
            config.delayDays = readInt(map.get("delayDays"), name + ".delayDays");
        }
        if (map.containsKey("startY")) {
            config.startY = readInt(map.get("startY"), name + ".startY");
        }
        if (map.containsKey("maxY")) {
            config.maxY = readInt(map.get("maxY"), name + ".maxY");
        }
        if (map.containsKey("rateBlocks")) {
            config.rateBlocks = readInt(map.get("rateBlocks"), name + ".rateBlocks");
        }
        if (map.containsKey("rateDays")) {
            config.rateDays = readInt(map.get("rateDays"), name + ".rateDays");
        }
        return config;
    }

    static StructureDistanceConfig readStructureDistanceConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        StructureDistanceConfig config = new StructureDistanceConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("minDistanceBlocks")) {
            config.minDistanceBlocks = readInt(map.get("minDistanceBlocks"), name + ".minDistanceBlocks");
        }
        if (map.containsKey("exemptStructureSets")) {
            config.exemptStructureSets = readStringList(map.get("exemptStructureSets"), name + ".exemptStructureSets", logger);
        }
        return config;
    }

    static FloatingIslandsConfig readFloatingIslandsConfig(Object value, String name, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(name + " must be a mapping");
        }
        FloatingIslandsConfig config = new FloatingIslandsConfig();
        if (map.containsKey("enabled")) {
            config.enabled = readBoolean(map.get("enabled"), name + ".enabled");
        }
        if (map.containsKey("minRadiusBlocks")) {
            config.minRadiusBlocks = readInt(map.get("minRadiusBlocks"), name + ".minRadiusBlocks");
        }
        if (map.containsKey("maxRadiusBlocks")) {
            config.maxRadiusBlocks = readInt(map.get("maxRadiusBlocks"), name + ".maxRadiusBlocks");
        }
        if (map.containsKey("shapeAmplitude")) {
            config.shapeAmplitude = readDouble(map.get("shapeAmplitude"), name + ".shapeAmplitude");
        }
        if (map.containsKey("cellSizeBlocks")) {
            config.cellSizeBlocks = readInt(map.get("cellSizeBlocks"), name + ".cellSizeBlocks");
        }
        if (map.containsKey("spawnChance")) {
            config.spawnChance = readDouble(map.get("spawnChance"), name + ".spawnChance");
        }
        if (map.containsKey("biomeVariety")) {
            config.biomeVariety = readBoolean(map.get("biomeVariety"), name + ".biomeVariety");
        }
        if (map.containsKey("islandBiomes")) {
            config.islandBiomes = readStringList(map.get("islandBiomes"), name + ".islandBiomes", logger);
        }
        if (map.containsKey("exclusionZoneEnabled")) {
            config.exclusionZoneEnabled = readBoolean(map.get("exclusionZoneEnabled"), name + ".exclusionZoneEnabled");
        }
        if (map.containsKey("exclusionZoneRadiusBlocks")) {
            config.exclusionZoneRadiusBlocks = readInt(map.get("exclusionZoneRadiusBlocks"), name + ".exclusionZoneRadiusBlocks");
        }
        if (map.containsKey("oreDepositsEnabled")) {
            config.oreDepositsEnabled = readBoolean(map.get("oreDepositsEnabled"), name + ".oreDepositsEnabled");
        }
        if (map.containsKey("oreFeatureIds")) {
            config.oreFeatureIds = readStringList(map.get("oreFeatureIds"), name + ".oreFeatureIds", logger);
        }
        if (map.containsKey("lootChestEnabled")) {
            config.lootChestEnabled = readBoolean(map.get("lootChestEnabled"), name + ".lootChestEnabled");
        }
        if (map.containsKey("lootKit")) {
            config.lootKit = readStarterKitConfig(map.get("lootKit"), name + ".lootKit", logger);
        }
        if (map.containsKey("naturalBiome")) {
            config.naturalBiome = readBoolean(map.get("naturalBiome"), name + ".naturalBiome");
        }
        return config;
    }

    static StarterKitConfig readStarterKitConfig(Object value, String name, Logger logger) {
        return new StarterKitSchema(name).read(value, new ParseContext(logger));
    }

    static StripBandsConfig readStripBandsConfig(Object value, String name, Logger logger) {
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

    static Map<String, String> readStringMap(Object value, String name) {
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

    static int readInt(Object value, String name) {
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

    static double readDouble(Object value, String name) {
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

    static BorderConfig sanitizeBorder(BorderConfig config, String name, Logger logger) {
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

    static EndBorderConfig sanitizeEndBorder(EndBorderConfig config, Logger logger) {
        EndBorderConfig sanitized = config == null ? new EndBorderConfig() : config;
        sanitized.minimumRadiusBlocks = clampWithWarning(
            sanitized.minimumRadiusBlocks, MIN_BORDER_RADIUS_BLOCKS, MAX_BORDER_RADIUS_BLOCKS,
            "endBorder.minimumRadiusBlocks", logger
        );
        return sanitized;
    }

    static StripConfig sanitizeStrip(StripConfig config, Logger logger) {
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

    static ChunkIslandConfig sanitizeChunkIsland(ChunkIslandConfig config, Logger logger) {
        ChunkIslandConfig sanitized = config == null ? new ChunkIslandConfig() : config;
        double originalChance = sanitized.spawnChance;
        sanitized.spawnChance = Math.clamp(sanitized.spawnChance, 0.0, 1.0);
        if (sanitized.spawnChance != originalChance) {
            logger.warn("Clamped chunkIsland.spawnChance from {} to {}.", originalChance, sanitized.spawnChance);
        }
        sanitized.cellSizeChunks = clampWithWarning(
            sanitized.cellSizeChunks, 1, MAX_LAYOUT_REGION_SCALE_BLOCKS / 16, "chunkIsland.cellSizeChunks", logger
        );
        sanitized.topOnlyDepthBlocks = clampWithWarning(
            sanitized.topOnlyDepthBlocks, 1, MAX_BORDER_RADIUS_BLOCKS, "chunkIsland.topOnlyDepthBlocks", logger
        );
        sanitized.exclusionZoneRadiusBlocks = clampWithWarning(
            sanitized.exclusionZoneRadiusBlocks, 0, MAX_BORDER_RADIUS_BLOCKS, "chunkIsland.exclusionZoneRadiusBlocks", logger
        );
        double originalScatteredChance = sanitized.scatteredTopOnlyChance;
        sanitized.scatteredTopOnlyChance = Math.clamp(sanitized.scatteredTopOnlyChance, 0.0, 1.0);
        if (sanitized.scatteredTopOnlyChance != originalScatteredChance) {
            logger.warn(
                "Clamped chunkIsland.scatteredTopOnlyChance from {} to {}.", originalScatteredChance, sanitized.scatteredTopOnlyChance
            );
        }
        sanitized.geodeFeatureIds = new ArrayList<>(
            sanitized.geodeFeatureIds.stream().map(String::trim).filter(id -> !id.isEmpty()).toList()
        );
        return sanitized;
    }

    static ExteriorConfig sanitizeExterior(
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

    static LayoutConfig sanitizeLayout(LayoutConfig config, Logger logger) {
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

    static String stripWeight(String configValue) {
        int at = configValue.lastIndexOf('@');
        return at < 0 ? configValue : configValue.substring(0, at);
    }

    static int clampWithWarning(int value, int minimum, int maximum, String name, Logger logger) {
        int clamped = Math.clamp(value, minimum, maximum);
        if (clamped != value) {
            logger.warn("Clamped {} from {} to {}.", name, value, clamped);
        }
        return clamped;
    }

    static Map<String, Object> borderMap(BorderConfig config, String objectiveKey) {
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

    static Map<String, Object> endBorderMap(EndBorderConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("carryFromOverworld", config.carryFromOverworld);
        values.put("minimumRadiusBlocks", config.minimumRadiusBlocks);
        return values;
    }

    static Map<String, Object> exteriorMap(ExteriorConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", config.mode.serializedName());
        values.put("boundaryRadiusBlocks", config.boundaryRadiusBlocks);
        values.put("oceanTransitionWidthBlocks", config.oceanTransitionWidthBlocks);
        return values;
    }

    static Map<String, Object> stripMap(StripConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("widthRadiusBlocks", config.widthRadiusBlocks);
        values.put("widthMode", config.widthMode.serializedName());
        values.put("applyToNether", config.applyToNether);
        return values;
    }

    static Map<String, Object> chunkIslandMap(ChunkIslandConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("spawnChance", config.spawnChance);
        values.put("cellSizeChunks", config.cellSizeChunks);
        values.put("topOnly", config.topOnly);
        values.put("topOnlyDepthBlocks", config.topOnlyDepthBlocks);
        values.put("exclusionZoneEnabled", config.exclusionZoneEnabled);
        values.put("exclusionZoneRadiusBlocks", config.exclusionZoneRadiusBlocks);
        values.put("scatteredTopOnlyChance", config.scatteredTopOnlyChance);
        values.put("applyToNether", config.applyToNether);
        values.put("applyToEnd", config.applyToEnd);
        values.put("geodeFeatureIds", config.geodeFeatureIds);
        return values;
    }

    static Map<String, Object> layoutMap(LayoutConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", config.mode.serializedName());
        values.put("biomes", config.biomes);
        values.put("regionScaleBlocks", config.regionScaleBlocks);
        values.put("singleBiome", config.singleBiome);
        values.put("roleOverrides", config.roleOverrides);
        return values;
    }

    static Map<String, Object> spawnMap(SpawnConfig config) {
        return SchemaSections.spawn("spawn").toMap(config);
    }

    static Map<String, Object> singleBiomeMap(SingleBiomeConfig config) {
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

    static Map<String, Object> chaosBiomesMap(ChaosBiomesConfig config) {
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

    static Map<String, Object> stripWorldMap(StripWorldConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spawn", spawnMap(config.spawn));
        values.put("bands", stripBandsMap(config.bands));
        return values;
    }

    static Map<String, Object> oceanIslandMap(OceanIslandConfig config) {
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

    static Map<String, Object> skyIslandMap(SkyIslandConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("islandBiome", config.islandBiome);
        values.put("radiusBlocks", config.radiusBlocks);
        values.put("shapeAmplitude", config.shapeAmplitude);
        values.put("surfaceY", config.surfaceY);
        values.put("thicknessBlocks", config.thicknessBlocks);
        values.put("chestTier", config.chestTier.serializedName());
        values.put("easyKit", starterKitMap(config.easyKit));
        values.put("mediumKit", starterKitMap(config.mediumKit));
        values.put("hardKit", starterKitMap(config.hardKit));
        values.put("applyToNether", config.applyToNether);
        values.put("floatingIslands", floatingIslandsMap(config.floatingIslands));
        return values;
    }

    static Map<String, Object> caveMap(CaveConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spawnDepthY", config.spawnDepthY);
        values.put("sealedSurface", config.sealedSurface);
        values.put("sealedSurfaceY", config.sealedSurfaceY);
        values.put("sealedSurfaceBlock", config.sealedSurfaceBlock.serializedName());
        values.put("sealedSurfaceThicknessBlocks", config.sealedSurfaceThicknessBlocks);
        values.put("cavernEnabled", config.cavernEnabled);
        values.put("cavernRadiusBlocks", config.cavernRadiusBlocks);
        values.put("cavernHeightBlocks", config.cavernHeightBlocks);
        values.put("chestEnabled", config.chestEnabled);
        values.put("chestTier", config.chestTier.serializedName());
        values.put("easyKit", starterKitMap(config.easyKit));
        values.put("mediumKit", starterKitMap(config.mediumKit));
        values.put("hardKit", starterKitMap(config.hardKit));
        return values;
    }

    static Map<String, Object> netherStartMap(NetherStartConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spawnY", config.spawnY);
        values.put("chestTier", config.chestTier.serializedName());
        values.put("easyKit", starterKitMap(config.easyKit));
        values.put("mediumKit", starterKitMap(config.mediumKit));
        values.put("hardKit", starterKitMap(config.hardKit));
        values.put("forceCapsule", config.forceCapsule);
        values.put("capsule", starterCapsuleMap(config.capsule));
        return values;
    }

    static Map<String, Object> starterCapsuleMap(StarterCapsuleConfig config) {
        return new StarterCapsuleSchema("capsule", 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE).toMap(config);
    }

    static Map<String, Object> endStartMap(EndStartConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("chestTier", config.chestTier.serializedName());
        values.put("easyKit", starterKitMap(config.easyKit));
        values.put("mediumKit", starterKitMap(config.mediumKit));
        values.put("hardKit", starterKitMap(config.hardKit));
        values.put("capsule", starterCapsuleMap(config.capsule));
        return values;
    }

    static Map<String, Object> flatMap(FlatConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("layers", config.layers);
        values.put("biome", config.biome);
        values.put("decoration", config.decoration);
        values.put("structureOverrides", config.structureOverrides);
        return values;
    }

    static Map<String, Object> deepFlatMap(DeepFlatConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceY", config.surfaceY);
        values.put("capLayers", config.capLayers);
        values.put("riversEnabled", config.riversEnabled);
        values.put("riverExclusionRadiusBlocks", config.riverExclusionRadiusBlocks);
        return values;
    }

    static Map<String, Object> stackedMap(StackedConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("layers", config.layers);
        values.put("seedRandomizedOrder", config.seedRandomizedOrder);
        values.put("worldSizeChunks", config.worldSizeChunks);
        values.put("reliefBlocks", config.reliefBlocks);
        values.put("forceTopVillage", config.forceTopVillage);
        return values;
    }

    static Map<String, Object> foreverNightMap(ForeverNightConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("lockAfterDays", config.lockAfterDays);
        values.put("relaxInsomnia", config.relaxInsomnia);
        return values;
    }

    static Map<String, Object> risingLavaMap(RisingLavaConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("delayDays", config.delayDays);
        values.put("startY", config.startY);
        values.put("maxY", config.maxY);
        values.put("rateBlocks", config.rateBlocks);
        values.put("rateDays", config.rateDays);
        return values;
    }

    static Map<String, Object> structureDistanceMap(StructureDistanceConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("minDistanceBlocks", config.minDistanceBlocks);
        values.put("exemptStructureSets", config.exemptStructureSets);
        return values;
    }

    static Map<String, Object> floatingIslandsMap(FloatingIslandsConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.enabled);
        values.put("minRadiusBlocks", config.minRadiusBlocks);
        values.put("maxRadiusBlocks", config.maxRadiusBlocks);
        values.put("shapeAmplitude", config.shapeAmplitude);
        values.put("cellSizeBlocks", config.cellSizeBlocks);
        values.put("spawnChance", config.spawnChance);
        values.put("biomeVariety", config.biomeVariety);
        values.put("islandBiomes", config.islandBiomes);
        values.put("exclusionZoneEnabled", config.exclusionZoneEnabled);
        values.put("exclusionZoneRadiusBlocks", config.exclusionZoneRadiusBlocks);
        values.put("oreDepositsEnabled", config.oreDepositsEnabled);
        values.put("oreFeatureIds", config.oreFeatureIds);
        values.put("lootChestEnabled", config.lootChestEnabled);
        values.put("lootKit", starterKitMap(config.lootKit));
        values.put("naturalBiome", config.naturalBiome);
        return values;
    }

    static Map<String, Object> starterKitMap(StarterKitConfig config) {
        return new StarterKitSchema("starterKit").toMap(config);
    }

    static Map<String, Object> stripBandsMap(StripBandsConfig config) {
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

    static String borderSummary(BorderConfig config, String objectiveName) {
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

    static String endBorderSummary(EndBorderConfig config) {
        return config.carryFromOverworld
            ? "carried, minimum=" + config.minimumRadiusBlocks
            : "<disabled>";
    }

    static String exteriorSummary(ExteriorConfig config) {
        if (config.mode == ExteriorMode.NORMAL) {
            return "<normal>";
        }
        return config.mode.serializedName() + ", boundary="
            + (config.boundaryRadiusBlocks == 0 ? "auto" : config.boundaryRadiusBlocks)
            + (config.mode == ExteriorMode.OCEAN ? ", transition=" + config.oceanTransitionWidthBlocks : "");
    }

    static String stripSummary(StripConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "widthRadius=" + config.widthRadiusBlocks
            + ", widthMode=" + config.widthMode.serializedName()
            + ", applyToNether=" + config.applyToNether;
    }

    static String chunkIslandSummary(ChunkIslandConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "spawnChance=" + config.spawnChance
            + ", cellSizeChunks=" + config.cellSizeChunks
            + ", topOnly=" + config.topOnly
            + (config.topOnly ? ", topOnlyDepthBlocks=" + config.topOnlyDepthBlocks : "")
            + ", scatteredTopOnlyChance=" + config.scatteredTopOnlyChance
            + ", applyToNether=" + config.applyToNether
            + ", applyToEnd=" + config.applyToEnd
            + ", geodeFeatureIds=" + config.geodeFeatureIds;
    }

    static String singleBiomeSummary(SingleBiomeConfig config) {
        return "landBiome=" + config.landBiome
            + ", starterBiome=" + (config.starterBiome.isEmpty() ? "<none>" : config.starterBiome)
            + ", starterRadiusBlocks=" + config.starterRadiusBlocks
            + ", spawn=" + config.spawn.strategy.serializedName()
            + ", allowRivers=" + config.allowRivers
            + ", allowOceans=" + config.allowOceans
            + ", allowBeaches=" + config.allowBeaches;
    }

    static String chaosBiomesSummary(ChaosBiomesConfig config) {
        return "biomes=" + config.biomes
            + ", regionScaleBlocks=" + config.regionScaleBlocks
            + ", starterBiome=" + (config.starterBiome.isEmpty() ? "<none>" : config.starterBiome)
            + ", starterRadiusBlocks=" + config.starterRadiusBlocks
            + ", spawn=" + config.spawn.strategy.serializedName()
            + ", allowRivers=" + config.allowRivers
            + ", allowOceans=" + config.allowOceans
            + ", allowBeaches=" + config.allowBeaches;
    }

    static String stripWorldSummary(StripWorldConfig config) {
        return "spawn=" + config.spawn.strategy.serializedName()
            + ", bands=" + stripBandsSummary(config.bands);
    }

    static String oceanIslandSummary(OceanIslandConfig config) {
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

    static String skyIslandSummary(SkyIslandConfig config) {
        return "islandBiome=" + config.islandBiome
            + ", radiusBlocks=" + config.radiusBlocks
            + ", shapeAmplitude=" + config.shapeAmplitude
            + ", surfaceY=" + config.surfaceY
            + ", thicknessBlocks=" + config.thicknessBlocks
            + ", chestTier=" + config.chestTier.serializedName()
            + ", easyKit=" + starterKitSummary(config.easyKit)
            + ", mediumKit=" + starterKitSummary(config.mediumKit)
            + ", hardKit=" + starterKitSummary(config.hardKit)
            + ", applyToNether=" + config.applyToNether
            + ", exclusionZone=" + (config.exclusionZoneEnabled ? "radius=" + config.exclusionZoneRadiusBlocks : "<disabled>")
            + ", floatingIslands=" + floatingIslandsSummary(config.floatingIslands);
    }

    static String caveSummary(CaveConfig config) {
        return "spawnDepthY=" + config.spawnDepthY
            + ", sealedSurface=" + config.sealedSurface
            + ", sealedSurfaceY=" + config.sealedSurfaceY
            + ", sealedSurfaceBlock=" + config.sealedSurfaceBlock.serializedName()
            + ", sealedSurfaceThicknessBlocks=" + config.sealedSurfaceThicknessBlocks
            + ", cavernEnabled=" + config.cavernEnabled
            + ", cavernRadiusBlocks=" + config.cavernRadiusBlocks
            + ", cavernHeightBlocks=" + config.cavernHeightBlocks
            + ", chestEnabled=" + config.chestEnabled
            + ", chestTier=" + config.chestTier.serializedName()
            + ", easyKit=" + starterKitSummary(config.easyKit)
            + ", mediumKit=" + starterKitSummary(config.mediumKit)
            + ", hardKit=" + starterKitSummary(config.hardKit);
    }

    static String netherStartSummary(NetherStartConfig config) {
        return "spawnY=" + config.spawnY + ", chestTier=" + config.chestTier.serializedName()
            + ", easyKit=" + starterKitSummary(config.easyKit)
            + ", mediumKit=" + starterKitSummary(config.mediumKit)
            + ", hardKit=" + starterKitSummary(config.hardKit)
            + ", forceCapsule=" + config.forceCapsule
            + ", capsule=" + starterCapsuleSummary(config.capsule);
    }

    static String starterCapsuleSummary(StarterCapsuleConfig config) {
        return new StarterCapsuleSchema("capsule", 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE).summary(config);
    }

    static String endStartSummary(EndStartConfig config) {
        return "chestTier=" + config.chestTier.serializedName()
            + ", easyKit=" + starterKitSummary(config.easyKit)
            + ", mediumKit=" + starterKitSummary(config.mediumKit)
            + ", hardKit=" + starterKitSummary(config.hardKit)
            + ", capsule=" + starterCapsuleSummary(config.capsule);
    }

    static String flatSummary(FlatConfig config) {
        return "layers=" + config.layers
            + ", biome=" + config.biome
            + ", decoration=" + config.decoration
            + ", structureOverrides=" + config.structureOverrides;
    }

    static String deepFlatSummary(DeepFlatConfig config) {
        return "surfaceY=" + config.surfaceY
            + ", capLayers=" + config.capLayers
            + ", riversEnabled=" + config.riversEnabled
            + ", riverExclusionRadiusBlocks=" + config.riverExclusionRadiusBlocks;
    }

    static String stackedSummary(StackedConfig config) {
        return "layers=" + config.layers + ", seedRandomizedOrder=" + config.seedRandomizedOrder
            + ", worldSizeChunks=" + config.worldSizeChunks + ", reliefBlocks=" + config.reliefBlocks
            + ", forceTopVillage=" + config.forceTopVillage;
    }

    static String foreverNightSummary(ForeverNightConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "lockAfterDays=" + config.lockAfterDays + ", relaxInsomnia=" + config.relaxInsomnia;
    }

    static String risingLavaSummary(RisingLavaConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "delayDays=" + config.delayDays + ", startY=" + config.startY + ", maxY=" + config.maxY
            + ", rateBlocks=" + config.rateBlocks + ", rateDays=" + config.rateDays;
    }

    static String structureDistanceSummary(StructureDistanceConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "minDistanceBlocks=" + config.minDistanceBlocks + ", exemptStructureSets=" + config.exemptStructureSets;
    }

    static String floatingIslandsSummary(FloatingIslandsConfig config) {
        if (!config.enabled) {
            return "<disabled>";
        }
        return "radius=" + config.minRadiusBlocks + ".." + config.maxRadiusBlocks
            + ", shapeAmplitude=" + config.shapeAmplitude
            + ", cellSizeBlocks=" + config.cellSizeBlocks
            + ", spawnChance=" + config.spawnChance
            + ", biomeVariety=" + config.biomeVariety
            + ", naturalBiome=" + config.naturalBiome
            + ", islandBiomes=" + config.islandBiomes
            + ", exclusionZone=" + (config.exclusionZoneEnabled ? "radius=" + config.exclusionZoneRadiusBlocks : "<disabled>")
            + ", oreDeposits=" + (config.oreDepositsEnabled ? "features=" + config.oreFeatureIds : "<disabled>")
            + ", lootChest=" + (config.lootChestEnabled ? starterKitSummary(config.lootKit) : "<disabled>");
    }

    static String starterKitSummary(StarterKitConfig config) {
        return new StarterKitSchema("starterKit").summary(config);
    }

    static String stripBandsSummary(StripBandsConfig config) {
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

    static String layoutSummary(LayoutConfig config) {
        if (config.mode == LayoutMode.LEGACY) {
            return "<legacy>";
        }
        return config.mode.serializedName()
            + ", biomes=" + config.biomes
            + ", regionScaleBlocks=" + config.regionScaleBlocks
            + (config.singleBiome.isEmpty() ? "" : ", singleBiome=" + config.singleBiome);
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
