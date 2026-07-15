package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** Loader-neutral Worldz bootstrap and shared state. */
public final class WorldzCommon {
    /** Stable mod namespace. */
    public static final String MOD_ID = "jlt_worldz";
    /** Shared mod logger. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
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
}
