package media.jlt.minecraft.mods.worldz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldzCommon {
    public static final String MOD_ID = "jlt_worldz";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private WorldzCommon() {
    }

    public static void init() {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
