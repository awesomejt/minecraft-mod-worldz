package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/** Loader-neutral Worldz bootstrap and shared state. */
public final class WorldzCommon {
    /** Stable mod namespace. */
    public static final String MOD_ID = "jlt_worldz";
    /** Shared mod logger. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /** Built mod version, expanded at build time from {@code gradle.properties}; "unknown" if unresolved. */
    public static final String MOD_VERSION = readModVersion();
    private static WorldzConfig config = new WorldzConfig();

    private WorldzCommon() {
    }

    /**
     * Loads configuration from the loader's configuration directory.
     *
     * @param configDirectory loader-provided configuration directory
     */
    public static void init(Path configDirectory) {
        config = WorldzConfig.load(configDirectory, MOD_ID, LOGGER);
        LOGGER.info("{} initialized with config {}", MOD_ID, config.summary());
    }

    /**
     * Returns the sanitized startup configuration used for new-world decoding.
     *
     * @return active configuration
     */
    public static WorldzConfig config() {
        return config;
    }

    private static String readModVersion() {
        try (InputStream stream = WorldzCommon.class.getResourceAsStream("/" + MOD_ID + "-version.properties")) {
            if (stream == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version", "unknown");
        } catch (IOException exception) {
            return "unknown";
        }
    }
}
