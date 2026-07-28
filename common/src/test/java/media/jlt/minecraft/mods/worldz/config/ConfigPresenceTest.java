package media.jlt.minecraft.mods.worldz.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for {@link WorldzConfig#present} (TODO 25.3, DESIGN §41.9): proves the recording
 * presence sink wired through {@code ParseContext} answers "was this key explicitly written",
 * independent of whether the written value happens to match the default.
 */
class ConfigPresenceTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @Test
    void neverParsedConfigReportsNothingPresent() {
        WorldzConfig config = new WorldzConfig();

        assertFalse(config.present("starter.radius"));
        assertFalse(config.present("stacked.worldSizeChunks"));
    }

    @Test
    void emptyMappingReportsNothingPresent() {
        WorldzConfig config = WorldzConfig.parse("{}", LOGGER);

        assertFalse(config.present("starter.radius"));
        assertFalse(config.present("stacked"));
        assertFalse(config.present("cave.easyKit.essentials"));
    }

    @Test
    void nestedGroupLeafSetToItsOwnDefaultValueIsStillReportedPresent() {
        WorldzConfig config = WorldzConfig.parse("""
            starter:
              radius: 256
            """, LOGGER);

        assertTrue(config.present("starter.radius"));
        assertTrue(config.present("starter"));
        assertFalse(config.present("starter.biome"));
    }

    @Test
    void nestedSectionLeafSetToItsOwnDefaultValueMarksLeafAndContainerPresent() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              worldSizeChunks: 4
            """, LOGGER);

        assertTrue(config.present("stacked.worldSizeChunks"));
        assertTrue(config.present("stacked"));
        assertFalse(config.present("stacked.seedRandomizedOrder"));
    }

    @Test
    void doublyNestedLeafMarksEveryAncestorPresentButNotSiblings() {
        WorldzConfig config = WorldzConfig.parse("""
            cave:
              easyKit:
                essentials:
                  - "minecraft:torch"
            """, LOGGER);

        assertTrue(config.present("cave.easyKit.essentials"));
        assertTrue(config.present("cave.easyKit"));
        assertFalse(config.present("cave.mediumKit"));
        assertFalse(config.present("cave.spawnDepthY"));
    }

    @Test
    void presenceSurvivesSanitize() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              worldSizeChunks: 4
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.present("stacked.worldSizeChunks"));
        assertTrue(config.present("stacked"));
        assertFalse(config.present("stacked.seedRandomizedOrder"));
    }
}
