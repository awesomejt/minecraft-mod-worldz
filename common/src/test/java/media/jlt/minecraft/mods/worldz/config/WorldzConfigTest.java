package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldzConfigTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingConfigCreatesDocumentedDefaults() throws IOException {
        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals("", config.starterBiome);
        assertEquals(512, config.starterRadiusBlocks);
        assertFalse(config.overworldBorder.enabled);
        assertTrue(config.overworldBorder.ensureObjective);
        assertFalse(config.netherBorder.enabled);
        assertEquals(ExteriorMode.NORMAL, config.overworldExterior.mode);
        assertEquals(ExteriorMode.NORMAL, config.netherExterior.mode);
        Path generated = temporaryDirectory.resolve("jlt_worldz.yaml");
        assertTrue(Files.isRegularFile(generated));
        Object example = YAML.load(Files.readString(Path.of("../config/jlt_worldz.example.yaml")));
        Object actual = YAML.load(Files.readString(generated));
        assertEquals(example, actual);
    }

    @Test
    void malformedConfigUsesDefaultsWithoutOverwritingInput() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String malformed = "allowedBiomes: [tru";
        Files.writeString(configFile, malformed);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void unknownKeysAreTolerated() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        Files.writeString(configFile, """
            allowedBiomes:
              - desert
            futureOption:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertEquals(512, config.starterRadiusBlocks);
        assertFalse(Files.readString(configFile).contains("futureOption"));
    }

    @Test
    void nonStringAndSyntacticallyInvalidBiomeEntriesAreDropped() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        Files.writeString(configFile, """
            allowedBiomes:
              - plains
              - 42
              - null
              - 'Bad Namespace:plains'
              - '#is_overworld'
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains", "#minecraft:is_overworld"), config.allowedBiomes);
        String rewritten = Files.readString(configFile);
        assertFalse(rewritten.contains("42"));
        assertFalse(rewritten.contains("Bad Namespace"));
    }

    @Test
    void radiusIsClampedAtBothBounds() {
        WorldzConfig below = WorldzConfig.parse("starterRadiusBlocks: -1", LOGGER).sanitize(LOGGER);
        WorldzConfig above = WorldzConfig.parse("starterRadiusBlocks: 999999", LOGGER).sanitize(LOGGER);

        assertEquals(64, below.starterRadiusBlocks);
        assertEquals(4096, above.starterRadiusBlocks);
    }

    @Test
    void borderSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              initialRadiusBlocks: 32
              finalRadiusBlocks: 2000
              resizeDays: 100
              resizeDelayDays: 12
              resizeRateBlocks: 128
              resizeRateDays: 5
              ensureEndPortal: false
            netherBorder:
              enabled: true
              initialRadiusBlocks: 256
              finalRadiusBlocks: 128
              resizeDays: 25
              ensureBlazeAccess: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.overworldBorder.enabled);
        assertEquals(64, config.overworldBorder.initialRadiusBlocks);
        assertEquals(2000, config.overworldBorder.finalRadiusBlocks);
        assertEquals(100, config.overworldBorder.resizeDays);
        assertEquals(12, config.overworldBorder.resizeDelayDays);
        assertEquals(128, config.overworldBorder.resizeRateBlocks);
        assertEquals(5, config.overworldBorder.resizeRateDays);
        assertFalse(config.overworldBorder.ensureObjective);
        assertTrue(config.netherBorder.enabled);
        assertEquals(256, config.netherBorder.initialRadiusBlocks);
        assertEquals(128, config.netherBorder.finalRadiusBlocks);
        assertEquals(25, config.netherBorder.resizeDays);
        assertTrue(config.netherBorder.ensureObjective);
    }

    @Test
    void incompleteBorderRateFallsBackToTotalDuration() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              resizeDays: 25
              resizeRateBlocks: 128
            """, LOGGER).sanitize(LOGGER);

        assertEquals(25, config.overworldBorder.resizeDays);
        assertEquals(0, config.overworldBorder.resizeRateBlocks);
        assertEquals(0, config.overworldBorder.resizeRateDays);
    }

    @Test
    void negativeResizeDelayIsClampedToZero() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              resizeDelayDays: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.overworldBorder.resizeDelayDays);
    }

    @Test
    void exteriorModesResolveAutoBoundariesAndSanitizeUnsupportedCombinations() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              initialRadiusBlocks: 512
              finalRadiusBlocks: 2048
            overworldExterior:
              mode: ocean
              boundaryRadiusBlocks: 0
              oceanTransitionWidthBlocks: 256
            netherExterior:
              mode: ocean
              boundaryRadiusBlocks: 512
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ExteriorMode.OCEAN, config.overworldExterior.mode);
        assertEquals(0, config.overworldExterior.boundaryRadiusBlocks);
        assertEquals(256, config.overworldExterior.oceanTransitionWidthBlocks);
        assertEquals(ExteriorMode.NORMAL, config.netherExterior.mode);
    }

    @Test
    void automaticExteriorWithoutBorderSafelyFallsBackToNormal() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldExterior:
              mode: void
              boundaryRadiusBlocks: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ExteriorMode.NORMAL, config.overworldExterior.mode);
    }

    @Test
    void invalidNestedBorderTypeMakesFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = "overworldBorder: true";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertFalse(config.overworldBorder.enabled);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void invalidBorderScalarMakesFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = """
            overworldBorder:
              enabled: 'yes'
            """;
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertFalse(config.overworldBorder.enabled);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void fractionalRadiusMakesTheFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = "starterRadiusBlocks: 64.5";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(512, config.starterRadiusBlocks);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void starterBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig id = WorldzConfig.parse("starterBiome: ' plains '", LOGGER).sanitize(LOGGER);
        WorldzConfig tag = WorldzConfig.parse("starterBiome: '#minecraft:is_overworld'", LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", id.starterBiome);
        assertEquals("", tag.starterBiome);
    }

    @Test
    void wrongFieldTypeUsesDefaultsWithoutOverwritingInput() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = "allowedBiomes: {biome: plains}";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void emptyAllowedListIsPreservedForWorldCreationFailsafe() {
        WorldzConfig config = WorldzConfig.parse("allowedBiomes: []", LOGGER).sanitize(LOGGER);

        assertTrue(config.allowedBiomes.isEmpty());
    }

    @Test
    void docsKeepCustomTextAndRestoreRequiredEntries() {
        WorldzConfig config = WorldzConfig.parse("""
            _docs:
              allowedBiomes: Custom allowed-biome help
              customNote: Keep me
              invalid: 42
            """, LOGGER).sanitize(LOGGER);

        assertEquals("Custom allowed-biome help", config._docs.get("allowedBiomes"));
        assertEquals("Keep me", config._docs.get("customNote"));
        assertFalse(config._docs.containsKey("invalid"));
        assertTrue(config._docs.containsKey("starterBiome"));
        assertTrue(config._docs.containsKey("starterRadiusBlocks"));
    }

    @Test
    void summaryUsesCanonicalValuesAndReadableDisabledStarter() {
        WorldzConfig config = WorldzConfig.parse("""
            allowedBiomes:
              - plains
              - '#is_overworld'
            starterBiome: ''
            starterRadiusBlocks: 256
            """, LOGGER).sanitize(LOGGER);

        assertEquals(
            "allowedBiomes=[minecraft:plains, #minecraft:is_overworld], starterBiome=<none>, starterRadiusBlocks=256"
                + ", overworldBorder=<disabled>, netherBorder=<disabled>"
                + ", overworldExterior=<normal>, netherExterior=<normal>",
            config.summary()
        );
    }

    @Test
    void legacyJsonIsMigratedToYamlAndBackedUp() throws IOException {
        Path legacy = temporaryDirectory.resolve("jlt_worldz.json");
        String original = """
            {"allowedBiomes":["desert"],"starterBiome":"plains","starterRadiusBlocks":128}
            """;
        Files.writeString(legacy, original);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertEquals("minecraft:plains", config.starterBiome);
        assertEquals(128, config.starterRadiusBlocks);
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("jlt_worldz.yaml")));
        assertFalse(Files.exists(legacy));
        assertEquals(original, Files.readString(temporaryDirectory.resolve("jlt_worldz.json.bak")));
    }

    @Test
    void yamlTakesPrecedenceOverLegacyJson() throws IOException {
        Files.writeString(temporaryDirectory.resolve("jlt_worldz.yaml"), "allowedBiomes: [desert]");
        Path legacy = temporaryDirectory.resolve("jlt_worldz.json");
        Files.writeString(legacy, "{\"allowedBiomes\":[\"plains\"]}");

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertTrue(Files.isRegularFile(legacy));
        assertFalse(Files.exists(temporaryDirectory.resolve("jlt_worldz.json.bak")));
    }

    @Test
    void invalidLegacyJsonIsLeftUntouched() throws IOException {
        Path legacy = temporaryDirectory.resolve("jlt_worldz.json");
        String malformed = "{\"allowedBiomes\":[";
        Files.writeString(legacy, malformed);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains"), config.allowedBiomes);
        assertEquals(malformed, Files.readString(legacy));
        assertFalse(Files.exists(temporaryDirectory.resolve("jlt_worldz.yaml")));
    }
}
