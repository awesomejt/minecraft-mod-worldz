package media.jlt.minecraft.mods.worldz.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import org.slf4j.Logger;

import java.io.IOException;
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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
     * Loads {@code <configDir>/<modId>.json}, creating a default file when absent.
     *
     * @param configDir loader-provided configuration directory
     * @param modId stable mod id used as the filename
     * @param logger destination for validation diagnostics
     * @return sanitized configuration, or defaults when input cannot be read
     */
    public static WorldzConfig load(Path configDir, String modId, Logger logger) {
        Path configFile = configDir.resolve(modId + ".json");
        if (!Files.exists(configFile)) {
            WorldzConfig defaults = new WorldzConfig().sanitize(logger);
            try {
                defaults.save(configFile);
            } catch (IOException exception) {
                logger.warn("Could not create config {}: {}", configFile, exception.getMessage());
            }
            return defaults;
        }

        String original;
        try {
            original = Files.readString(configFile);
            WorldzConfig config = parse(original, logger).sanitize(logger);
            config.save(configFile);
            return config;
        } catch (Exception exception) {
            logger.warn("Could not load config {}: {}. Using defaults without changing the file.",
                configFile, exception.getMessage());
            return new WorldzConfig().sanitize(logger);
        }
    }

    static WorldzConfig parse(String json, Logger logger) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("root value must be a JSON object");
        }

        JsonObject object = root.getAsJsonObject();
        WorldzConfig config = new WorldzConfig();
        if (object.has("allowedBiomes")) {
            config.allowedBiomes = readStringList(object.get("allowedBiomes"), "allowedBiomes", logger);
        }
        if (object.has("starterBiome")) {
            config.starterBiome = readString(object.get("starterBiome"), "starterBiome");
        }
        if (object.has("starterRadiusBlocks")) {
            config.starterRadiusBlocks = readInt(object.get("starterRadiusBlocks"), "starterRadiusBlocks");
        }
        if (object.has("_docs")) {
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

    String toJson() {
        return GSON.toJson(this);
    }

    private void save(Path configFile) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        Files.writeString(temporary, toJson() + System.lineSeparator());
        try {
            Files.move(temporary, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> readStringList(JsonElement element, String name, Logger logger) {
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException(name + " must be an array");
        }
        List<String> values = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            JsonElement value = array.get(index);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                values.add(value.getAsString());
            } else {
                logger.warn("Ignoring non-string {} entry at index {}.", name, index);
            }
        }
        return values;
    }

    private static String readString(JsonElement element, String name) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return element.getAsString();
    }

    private static int readInt(JsonElement element, String name) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static Map<String, String> readDocs(JsonElement element, Logger logger) {
        if (!element.isJsonObject()) {
            logger.warn("Ignoring non-object _docs value.");
            return new LinkedHashMap<>();
        }
        Map<String, String> docs = new LinkedHashMap<>();
        element.getAsJsonObject().entrySet().forEach(entry -> {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                docs.put(entry.getKey(), value.getAsString());
            } else {
                logger.warn("Ignoring non-string _docs entry '{}'.", entry.getKey());
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
}
