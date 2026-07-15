package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
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

    private static final String YAML_EXTENSION = ".yaml";
    private static final String LEGACY_JSON_EXTENSION = ".json";

    /** Biome ids and biome-tag ids allowed in new Worldz worlds. */
    public List<String> allowedBiomes = new ArrayList<>(List.of("minecraft:plains"));
    /** Optional biome id forced around the origin. */
    public String starterBiome = "";
    /** Starter-zone radius measured in blocks. */
    public int starterRadiusBlocks = 512;
    /** Human-readable inline documentation persisted with the config. */
    public Map<String, String> _docs = defaultDocs();

    /** Creates a config populated with documented defaults. */
    public WorldzConfig() {
    }

    /**
     * Loads {@code <configDir>/<modId>.yaml}, creating a default file when absent.
     * A legacy JSON config is migrated because JSON is valid YAML input.
     *
     * @param configDir loader-provided configuration directory
     * @param modId stable mod id used as the filename
     * @param logger destination for validation diagnostics
     * @return sanitized configuration, or defaults when input cannot be read
     */
    public static WorldzConfig load(Path configDir, String modId, Logger logger) {
        Path configFile = configDir.resolve(modId + YAML_EXTENSION);
        if (Files.exists(configFile)) {
            return loadExisting(configFile, logger);
        }

        Path legacyFile = configDir.resolve(modId + LEGACY_JSON_EXTENSION);
        if (Files.exists(legacyFile)) {
            WorldzConfig migrated = loadLegacy(legacyFile, configFile, logger);
            if (migrated != null) {
                return migrated;
            }
            return new WorldzConfig().sanitize(logger);
        }

        WorldzConfig defaults = new WorldzConfig().sanitize(logger);
        try {
            defaults.save(configFile);
        } catch (IOException exception) {
            logger.warn("Could not create config {}: {}", configFile, exception.getMessage());
        }
        return defaults;
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

    private static WorldzConfig loadLegacy(Path legacyFile, Path configFile, Logger logger) {
        try {
            WorldzConfig config = parse(Files.readString(legacyFile), logger).sanitize(logger);
            config.save(configFile);
            Path backup = legacyFile.resolveSibling(legacyFile.getFileName() + ".bak");
            try {
                Files.move(legacyFile, backup, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Migrated legacy config {} to {}; old file kept as {}.", legacyFile, configFile, backup);
            } catch (IOException exception) {
                logger.warn("Migrated legacy config to {}, but could not move {} to a backup: {}",
                    configFile, legacyFile, exception.getMessage());
            }
            return config;
        } catch (Exception exception) {
            logger.warn("Could not migrate legacy config {}: {}. Using defaults without changing the file.",
                legacyFile, exception.getMessage());
            return null;
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
        if (object.containsKey("_docs")) {
            config._docs = readDocs(object.get("_docs"), logger);
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

        _docs = _docs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(_docs);
        defaultDocs().forEach(_docs::putIfAbsent);
        return this;
    }

    /**
     * Returns a compact representation suitable for startup logging.
     *
     * @return config summary
     */
    public String summary() {
        return "allowedBiomes=" + allowedBiomes
            + ", starterBiome=" + (starterBiome.isEmpty() ? "<none>" : starterBiome)
            + ", starterRadiusBlocks=" + starterRadiusBlocks;
    }

    String toYaml() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedBiomes", allowedBiomes);
        values.put("starterBiome", starterBiome);
        values.put("starterRadiusBlocks", starterRadiusBlocks);
        values.put("_docs", _docs);
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

    private static Map<String, String> readDocs(Object value, Logger logger) {
        if (!(value instanceof Map<?, ?> map)) {
            logger.warn("Ignoring non-mapping _docs value.");
            return new LinkedHashMap<>();
        }
        Map<String, String> docs = new LinkedHashMap<>();
        map.forEach((key, entry) -> {
            if (!(key instanceof String stringKey)) {
                logger.warn("Ignoring non-string _docs key '{}'.", key);
            } else if (entry instanceof String stringValue) {
                docs.put(stringKey, stringValue);
            } else {
                logger.warn("Ignoring non-string _docs entry '{}'.", stringKey);
            }
        });
        return docs;
    }

    private static Map<String, String> defaultDocs() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("allowedBiomes", "Biome ids and/or #tag ids allowed in newly created Worldz overworlds. Default: [minecraft:plains].");
        docs.put("starterBiome", "Biome id forced around origin in newly created worlds. Default: empty (disabled). Tags are not accepted.");
        docs.put("starterRadiusBlocks", "Circular starter-zone radius in blocks. Default: 512. Range: 64..4096.");
        return docs;
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
