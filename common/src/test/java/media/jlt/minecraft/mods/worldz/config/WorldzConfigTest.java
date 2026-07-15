package media.jlt.minecraft.mods.worldz.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldzConfigTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingConfigCreatesDocumentedDefaults() throws IOException {
        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals("", config.starterBiome);
        assertEquals(512, config.starterRadiusBlocks);
        Path generated = temporaryDirectory.resolve("jlt_worldz.json");
        assertTrue(Files.isRegularFile(generated));
        assertEquals(
            JsonParser.parseString(Files.readString(Path.of("../config/jlt_worldz.example.json"))),
            JsonParser.parseString(Files.readString(generated))
        );
    }

    @Test
    void malformedConfigUsesDefaultsWithoutOverwritingInput() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.json");
        String malformed = "{ \"allowedBiomes\": [tru";
        Files.writeString(configFile, malformed);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void unknownKeysAreTolerated() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.json");
        Files.writeString(configFile, """
            {
              "allowedBiomes": ["desert"],
              "futureOption": {"enabled": true}
            }
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertEquals(512, config.starterRadiusBlocks);
    }

    @Test
    void nonStringAndSyntacticallyInvalidBiomeEntriesAreDropped() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.json");
        Files.writeString(configFile, """
            {
              "allowedBiomes": ["plains", 42, null, "Bad Namespace:plains", "#is_overworld"]
            }
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains", "#minecraft:is_overworld"), config.allowedBiomes);
        String rewritten = Files.readString(configFile);
        assertFalse(rewritten.contains("42"));
        assertFalse(rewritten.contains("Bad Namespace"));
    }

    @Test
    void radiusIsClampedAtBothBounds() {
        WorldzConfig below = WorldzConfig.parse("{\"starterRadiusBlocks\": -1}", LOGGER).sanitize(LOGGER);
        WorldzConfig above = WorldzConfig.parse("{\"starterRadiusBlocks\": 999999}", LOGGER).sanitize(LOGGER);

        assertEquals(64, below.starterRadiusBlocks);
        assertEquals(4096, above.starterRadiusBlocks);
    }

    @Test
    void fractionalRadiusMakesTheFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.json");
        String invalid = "{\"starterRadiusBlocks\": 64.5}";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(512, config.starterRadiusBlocks);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void starterBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig id = WorldzConfig.parse("{\"starterBiome\": \" plains \"}", LOGGER).sanitize(LOGGER);
        WorldzConfig tag = WorldzConfig.parse("{\"starterBiome\": \"#minecraft:is_overworld\"}", LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", id.starterBiome);
        assertEquals("", tag.starterBiome);
    }
}
