package media.jlt.minecraft.mods.worldz.config;

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
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldzConfigTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));
    private static final List<String> DEFAULT_ALLOWED_BIOMES = List.of(
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
    );

    @TempDir
    Path temporaryDirectory;

    private Path referenceFile() {
        return temporaryDirectory.resolve("jlt_worldz.reference.yaml");
    }

    @Test
    void missingConfigUsesDefaultsWithoutCreatingAUserConfigFile() throws IOException {
        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(DEFAULT_ALLOWED_BIOMES, config.allowedBiomes);
        assertEquals("minecraft:plains", config.starterBiome);
        assertEquals(256, config.starterRadiusBlocks);
        assertTrue(config.ensureStarterLand);
        assertEquals(128, config.starterLandTransitionBlocks);
        assertEquals(48, config.starterLandFoundationDepthBlocks);
        assertFalse(config.overworldBorder.enabled);
        assertTrue(config.overworldBorder.ensureObjective);
        assertFalse(config.netherBorder.enabled);
        assertEquals(ExteriorMode.NORMAL, config.overworldExterior.mode);
        assertEquals(ExteriorMode.NORMAL, config.netherExterior.mode);
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.spawn.strategy);
        assertEquals("minecraft:plains", config.singleBiome.landBiome);
        assertEquals("", config.singleBiome.starterBiome);
        assertEquals(256, config.singleBiome.starterRadiusBlocks);
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.singleBiome.spawn.strategy);
        assertEquals(
            List.of("minecraft:desert", "minecraft:jungle", "minecraft:ice_spikes", "minecraft:badlands", "minecraft:taiga"),
            config.chaosBiomes.biomes
        );
        assertEquals(512, config.chaosBiomes.regionScaleBlocks);
        assertEquals("", config.chaosBiomes.starterBiome);
        assertEquals(256, config.chaosBiomes.starterRadiusBlocks);
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.chaosBiomes.spawn.strategy);
        assertFalse(config.allowRivers);
        assertFalse(config.allowOceans);
        assertFalse(Files.exists(temporaryDirectory.resolve("jlt_worldz.yaml")));
        assertTrue(Files.exists(referenceFile()));
        assertEquals(WorldzConfig.referenceYaml(), Files.readString(referenceFile()));
    }

    @Test
    void documentedExampleParsesToTheSameDefaultsAsCode() throws IOException {
        WorldzConfig fromExample = WorldzConfig.parse(
            Files.readString(Path.of("../config/jlt_worldz.example.yaml")), LOGGER
        ).sanitize(LOGGER);
        WorldzConfig codeDefaults = new WorldzConfig().sanitize(LOGGER);

        Object expected = YAML.load(codeDefaults.toYaml());
        Object actual = YAML.load(fromExample.toYaml());
        assertEquals(expected, actual);
    }

    /**
     * Permanent byte-identity anchor for TODO 25.2 (DESIGN §41.8): {@code
     * reference-defaults.yaml} is the exact {@code toYaml()} output of the pre-refactor code,
     * captured once at the start of the phase and committed unchanged. This must keep passing,
     * string-identical, through every sub-step (25.2a-h) -- it is what 25.6/25.7 will
     * deliberately regenerate when the keys really do move.
     */
    @Test
    void defaultConfigMatchesTheCapturedReferenceDefaults() throws IOException {
        String reference = Files.readString(Path.of("src/test/resources/config/reference-defaults.yaml"));
        String actual = new WorldzConfig().sanitize(LOGGER).toYaml();
        assertEquals(reference, actual);
    }

    @Test
    void malformedConfigUsesDefaultsWithoutOverwritingInput() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String malformed = "allowedBiomes: [tru";
        Files.writeString(configFile, malformed);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(DEFAULT_ALLOWED_BIOMES, config.allowedBiomes);
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void unknownKeysAreTolerated() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String written = """
            allowedBiomes:
              - desert
            futureOption:
              enabled: true
            """;
        Files.writeString(configFile, written);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertEquals(256, config.starterRadiusBlocks);
        // The file on disk is untouched byte-for-byte, futureOption included.
        assertEquals(written, Files.readString(configFile));
        // The parsed model still drops what the schema doesn't declare.
        assertFalse(config.toYaml().contains("futureOption"));
    }

    @Test
    void nonStringAndSyntacticallyInvalidBiomeEntriesAreDropped() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String written = """
            allowedBiomes:
              - plains
              - 42
              - null
              - 'Bad Namespace:plains'
              - '#is_overworld'
            """;
        Files.writeString(configFile, written);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:plains", "#minecraft:is_overworld"), config.allowedBiomes);
        // The file on disk is untouched byte-for-byte.
        assertEquals(written, Files.readString(configFile));
        // The parsed model still drops the invalid entries.
        assertFalse(config.toYaml().contains("42"));
        assertFalse(config.toYaml().contains("Bad Namespace"));
    }

    /**
     * The headline regression test for D4/TODO 25.4 (see MEMORY.md, F5): a small, hand-commented
     * config must survive a launch byte-identical, not get expanded into the full multi-hundred-
     * line dump the old unconditional rewrite produced.
     */
    @Test
    void validConfigWithCommentsSurvivesLoadUnchanged() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String written = """
            # My personal Worldz config.
            # Only overriding the two settings I care about.
            starter:
              radius: 512  # bigger starter zone
            allowedBiomes:
              - minecraft:desert
              - minecraft:badlands
            """;
        Files.writeString(configFile, written);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(512, config.starterRadiusBlocks);
        assertEquals(List.of("minecraft:desert", "minecraft:badlands"), config.allowedBiomes);
        assertEquals(written, Files.readString(configFile));
        assertFalse(Files.readString(configFile).contains("oceanIsland"));
    }

    @Test
    void referenceFileIsRegeneratedOverStaleContent() throws IOException {
        Files.writeString(referenceFile(), "stale: true\n");

        WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(WorldzConfig.referenceYaml(), Files.readString(referenceFile()));
    }

    @Test
    void referenceFileBodyMatchesTheCapturedSchemaDefaults() throws IOException {
        String defaults = Files.readString(Path.of("src/test/resources/config/reference-defaults.yaml"));
        String reference = WorldzConfig.referenceYaml();

        assertTrue(reference.endsWith(defaults));
        String header = reference.substring(0, reference.length() - defaults.length());
        for (String line : header.lines().toList()) {
            assertTrue(line.startsWith("#"));
        }
    }

    @Test
    void referenceFileRoundTripsBackToDefaults() {
        String roundTripped = WorldzConfig.parse(WorldzConfig.referenceYaml(), LOGGER).sanitize(LOGGER).toYaml();
        String defaults = new WorldzConfig().sanitize(LOGGER).toYaml();

        assertEquals(defaults, roundTripped);
    }

    // Assumes a POSIX filesystem (AGENTS.md: Temurin 25 on Linux): Files.createDirectories on a
    // path that already exists as a regular file throws, which is exactly the failure this
    // test forces onto writeReference's config directory argument.
    @Test
    void referenceWriteFailureDoesNotBlockConfigLoading() throws IOException {
        Path notADirectory = temporaryDirectory.resolve("notADirectory");
        Files.writeString(notADirectory, "not a directory");

        WorldzConfig config = WorldzConfig.load(notADirectory, "jlt_worldz", LOGGER);

        assertEquals(DEFAULT_ALLOWED_BIOMES, config.allowedBiomes);
    }

    @Test
    void radiusIsClampedAtBothBounds() {
        WorldzConfig below = WorldzConfig.parse("starter:\n  radius: -1", LOGGER).sanitize(LOGGER);
        WorldzConfig above = WorldzConfig.parse("starter:\n  radius: 999999", LOGGER).sanitize(LOGGER);

        assertEquals(64, below.starterRadiusBlocks);
        assertEquals(4096, above.starterRadiusBlocks);
    }

    @Test
    void starterLandSettingsLoadAndClampIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            starter:
              land:
                enabled: false
                transition: 5000
                foundationDepth: -4
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.ensureStarterLand);
        assertEquals(4096, config.starterLandTransitionBlocks);
        assertEquals(0, config.starterLandFoundationDepthBlocks);
    }

    @Test
    void borderSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              initialRadius: 0
              finalRadius: 2000
              resize:
                days: 100
                delayDays: 12
                style: stepped
                rate:
                  blocks: 128
                  days: 5
              ensureEndPortal: false
            netherBorder:
              enabled: true
              initialRadius: 256
              finalRadius: 128
              resize:
                days: 25
              ensureBlazeAccess: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.overworldBorder.enabled);
        assertEquals(1, config.overworldBorder.initialRadiusBlocks);
        assertEquals(2000, config.overworldBorder.finalRadiusBlocks);
        assertEquals(100, config.overworldBorder.resizeDays);
        assertEquals(12, config.overworldBorder.resizeDelayDays);
        assertEquals(128, config.overworldBorder.resizeRateBlocks);
        assertEquals(5, config.overworldBorder.resizeRateDays);
        assertFalse(config.overworldBorder.ensureObjective);
        assertEquals(ResizeStyle.STEPPED, config.overworldBorder.resizeStyle);
        assertTrue(config.netherBorder.enabled);
        assertEquals(256, config.netherBorder.initialRadiusBlocks);
        assertEquals(128, config.netherBorder.finalRadiusBlocks);
        assertEquals(25, config.netherBorder.resizeDays);
        assertTrue(config.netherBorder.ensureObjective);
        assertEquals(ResizeStyle.CONTINUOUS, config.netherBorder.resizeStyle);
    }

    @Test
    void steppedResizeStyleWithoutARateFallsBackToContinuous() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              initialRadius: 8
              finalRadius: 1024
              resize:
                style: stepped
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ResizeStyle.CONTINUOUS, config.overworldBorder.resizeStyle);
    }

    @Test
    void endBorderLoadsAndClampsItsMinimumRadius() {
        WorldzConfig config = WorldzConfig.parse("""
            endBorder:
              carryFromOverworld: true
              minimumRadius: 0
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.endBorder.carryFromOverworld);
        assertEquals(1, config.endBorder.minimumRadiusBlocks);
    }

    @Test
    void endBorderDefaultsToDisabledWithAReasonableFloor() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.endBorder.carryFromOverworld);
        assertEquals(256, config.endBorder.minimumRadiusBlocks);
    }

    @Test
    void incompleteBorderRateFallsBackToTotalDuration() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              resize:
                days: 25
                rate:
                  blocks: 128
            """, LOGGER).sanitize(LOGGER);

        assertEquals(25, config.overworldBorder.resizeDays);
        assertEquals(0, config.overworldBorder.resizeRateBlocks);
        assertEquals(0, config.overworldBorder.resizeRateDays);
    }

    @Test
    void negativeResizeDelayIsClampedToZero() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              resize:
                delayDays: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.overworldBorder.resizeDelayDays);
    }

    @Test
    void exteriorModesResolveAutoBoundariesAndSanitizeUnsupportedCombinations() {
        WorldzConfig config = WorldzConfig.parse("""
            overworldBorder:
              enabled: true
              initialRadius: 512
              finalRadius: 2048
            overworldExterior:
              mode: ocean
              boundaryRadius: 0
              oceanTransitionWidth: 256
            netherExterior:
              mode: ocean
              boundaryRadius: 512
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
              boundaryRadius: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ExteriorMode.NORMAL, config.overworldExterior.mode);
    }

    @Test
    void stripSettingsLoadAndSanitize() {
        WorldzConfig config = WorldzConfig.parse("""
            strip:
              enabled: true
              widthRadiusBlocks: 0
              widthMode: ocean
              applyToNether: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.strip.enabled);
        assertEquals(1, config.strip.widthRadiusBlocks);
        assertEquals(ExteriorMode.OCEAN, config.strip.widthMode);
        assertTrue(config.strip.applyToNether);
    }

    @Test
    void stripDefaultsToDisabledWithAVoidWidthMode() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.strip.enabled);
        assertEquals(ExteriorMode.VOID, config.strip.widthMode);
        assertFalse(config.strip.applyToNether);
    }

    @Test
    void stripWidthModeCannotBeNormal() {
        WorldzConfig config = WorldzConfig.parse("""
            strip:
              enabled: true
              widthMode: normal
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ExteriorMode.VOID, config.strip.widthMode);
    }

    @Test
    void layoutSettingsLoadWeightedBiomesAndRoleOverrides() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              mode: ocean
              biomes:
                - "minecraft:plains@3"
                - "minecraft:desert"
                - "minecraft:ocean"
                - "minecraft:swamp"
              roleOverrides:
                "minecraft:swamp": "ocean"
              regionScale: 300
            """, LOGGER).sanitize(LOGGER);

        assertEquals(LayoutMode.OCEAN, config.layout.mode);
        assertEquals(
            List.of("minecraft:plains@3.0", "minecraft:desert", "minecraft:ocean", "minecraft:swamp"),
            config.layout.biomes
        );
        assertEquals("ocean", config.layout.roleOverrides.get("minecraft:swamp"));
        assertEquals(300, config.layout.regionScaleBlocks);
    }

    @Test
    void layoutInvalidBiomeEntriesAreDroppedNotRejected() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              biomes:
                - "minecraft:plains"
                - "#minecraft:is_overworld"
                - "minecraft:desert@-1"
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:plains"), config.layout.biomes);
    }

    @Test
    void layoutFallsBackToLegacyWhenTheModeHasNoUsableBiomes() {
        WorldzConfig ocean = WorldzConfig.parse("""
            layout:
              mode: ocean
              biomes:
                - "minecraft:plains"
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig singleBiome = WorldzConfig.parse("""
            layout:
              mode: single_biome
            """, LOGGER).sanitize(LOGGER);

        assertEquals(LayoutMode.LEGACY, ocean.layout.mode);
        assertEquals(LayoutMode.LEGACY, singleBiome.layout.mode);
    }

    @Test
    void layoutRegionScaleIsClamped() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              regionScale: 1
            """, LOGGER).sanitize(LOGGER);

        assertEquals(16, config.layout.regionScaleBlocks);
    }

    @Test
    void layoutSingleBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              singleBiome: "#minecraft:is_overworld"
            """, LOGGER).sanitize(LOGGER);

        assertEquals("", config.layout.singleBiome);
    }

    @Test
    void layoutRoleOverridesDropInvalidIdsAndRoles() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              roleOverrides:
                "Uppercase:plains": "ocean"
                "minecraft:desert": "not-a-role"
                "minecraft:swamp": "beach"
            """, LOGGER).sanitize(LOGGER);

        assertEquals(Map.of("minecraft:swamp", "beach"), config.layout.roleOverrides);
    }

    @Test
    void spawnStrategyDefaultsToStarterAtOrigin() {
        WorldzConfig config = WorldzConfig.parse("starter:\n  biome: minecraft:plains", LOGGER).sanitize(LOGGER);

        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.spawn.strategy);
    }

    @Test
    void spawnStrategyLoadsRecognizedValue() {
        WorldzConfig config = WorldzConfig.parse("""
            spawn:
              strategy: preferred_natural_biome
            """, LOGGER).sanitize(LOGGER);

        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, config.spawn.strategy);
    }

    @Test
    void invalidSpawnStrategyMakesTheFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = """
            spawn:
              strategy: not-a-strategy
            """;
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.spawn.strategy);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void invalidNestedSpawnTypeMakesFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = "spawn: true";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.spawn.strategy);
        assertEquals(invalid, Files.readString(configFile));
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
        String invalid = "starter:\n  radius: 64.5";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(256, config.starterRadiusBlocks);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void starterBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig id = WorldzConfig.parse("starter:\n  biome: ' plains '", LOGGER).sanitize(LOGGER);
        WorldzConfig tag = WorldzConfig.parse("starter:\n  biome: '#minecraft:is_overworld'", LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", id.starterBiome);
        assertEquals("", tag.starterBiome);
    }

    @Test
    void wrongFieldTypeUsesDefaultsWithoutOverwritingInput() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz.yaml");
        String invalid = "allowedBiomes: {biome: plains}";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(DEFAULT_ALLOWED_BIOMES, config.allowedBiomes);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void emptyAllowedListIsPreservedForWorldCreationFailsafe() {
        WorldzConfig config = WorldzConfig.parse("allowedBiomes: []", LOGGER).sanitize(LOGGER);

        assertTrue(config.allowedBiomes.isEmpty());
    }

    @Test
    void summaryUsesCanonicalValuesAndReadableDisabledStarter() {
        WorldzConfig config = WorldzConfig.parse("""
            allowedBiomes:
              - plains
              - '#is_overworld'
            starter:
              biome: ''
              radius: 256
            """, LOGGER).sanitize(LOGGER);

        assertEquals(
            "allowedBiomes=[minecraft:plains, #minecraft:is_overworld]"
                + ", starter=biome=<none>, radius=256, land=transition=128, foundation=48"
                + ", naturalBiomes=rivers=false, oceans=false"
                + ", overworldBorder=<disabled>, netherBorder=<disabled>, endBorder=<disabled>"
                + ", overworldExterior=<normal>, netherExterior=<normal>"
                + ", strip=<disabled>"
                + ", layout=<legacy>"
                + ", spawn=starter_at_origin"
                + ", singleBiome=biome=minecraft:plains, starter=biome=<none>, radius=256"
                + ", spawn=starter_at_origin"
                + ", naturalBiomes=rivers=false, oceans=false, beaches=false"
                + ", chaosBiomes=biomes=[minecraft:desert, minecraft:jungle, minecraft:ice_spikes,"
                + " minecraft:badlands, minecraft:taiga], regionScale=512, starter=biome=<none>, radius=256"
                + ", spawn=starter_at_origin, naturalBiomes=rivers=false, oceans=false, beaches=false"
                + ", stripWorld=spawn=starter_at_origin, bands=<disabled>"
                + ", oceanIsland=islandSource=artificial, fluid=water, islandBiome=minecraft:plains, radiusBlocks=128, shapeAmplitude=0.3"
                + ", shoreWidthBlocks=12, oceanShallowWidthBlocks=64, oceanDeepenWidthBlocks=128"
                + ", oceanShallowDepthBlocks=8, oceanDeepDepthBlocks=32, oceanRegionScaleBlocks=128"
                + ", exclusionZone=<disabled>"
                + ", starterKit=essentials=[minecraft:lily_pad:1, minecraft:dirt:4, minecraft:grass_block:2,"
                + " minecraft:oak_sapling:3], extras=[minecraft:bread:3, minecraft:wooden_axe:1,"
                + " minecraft:wooden_pickaxe:1, minecraft:torch:8, minecraft:water_bucket:1], extrasCount=2"
                + ", skyIsland=islandBiome=minecraft:plains, radiusBlocks=16, shapeAmplitude=0.3"
                + ", surfaceY=64, thicknessBlocks=6, chestTier=medium"
                + ", easyKit=essentials=[minecraft:oak_sapling:4, minecraft:bread:8, minecraft:crafting_table:1,"
                + " minecraft:lava_bucket:1],"
                + " extras=[minecraft:wooden_pickaxe:1, minecraft:wooden_axe:1, minecraft:torch:16,"
                + " minecraft:cobblestone:32], extrasCount=3"
                + ", mediumKit=essentials=[minecraft:oak_sapling:3, minecraft:bread:4, minecraft:lava_bucket:1],"
                + " extras=[minecraft:wooden_pickaxe:1, minecraft:torch:8, minecraft:cobblestone:16], extrasCount=2"
                + ", hardKit=essentials=[minecraft:oak_sapling:2, minecraft:lava_bucket:1],"
                + " extras=[minecraft:bread:2, minecraft:torch:4],"
                + " extrasCount=1"
                + ", applyToNether=false"
                + ", exclusionZone=radius=128"
                + ", floatingIslands=<disabled>"
                + ", chunkIsland=<disabled>"
                + ", cave=spawnDepthY=-32, sealedSurface=false, sealedSurfaceY=128, sealedSurfaceBlock=stone,"
                + " sealedSurfaceThicknessBlocks=5, cavernEnabled=false"
                + ", cavernRadiusBlocks=48, cavernHeightBlocks=24, chestEnabled=false, chestTier=medium"
                + ", easyKit=essentials=[minecraft:oak_log:4, minecraft:bread:6, minecraft:wheat_seeds:4,"
                + " minecraft:oak_sapling:3, minecraft:torch:16, minecraft:dirt:8],"
                + " extras=[minecraft:cobblestone:16, minecraft:coal:8], extrasCount=2"
                + ", mediumKit=essentials=[minecraft:torch:8, minecraft:oak_log:2, minecraft:dirt:4],"
                + " extras=[minecraft:coal:4], extrasCount=1"
                + ", hardKit=essentials=[minecraft:torch:1, minecraft:wooden_pickaxe:1], extras=[], extrasCount=0"
                + ", netherStart=spawnY=32, chestTier=medium"
                + ", easyKit=essentials=[minecraft:obsidian:10, minecraft:flint_and_steel:1, minecraft:bread:8,"
                + " minecraft:wooden_pickaxe:1],"
                + " extras=[minecraft:golden_pickaxe:1, minecraft:golden_sword:1, minecraft:gold_ingot:8,"
                + " minecraft:torch:16], extrasCount=3"
                + ", mediumKit=essentials=[minecraft:obsidian:10, minecraft:bread:4, minecraft:wooden_pickaxe:1],"
                + " extras=[minecraft:gold_ingot:4, minecraft:torch:8, minecraft:iron_sword:1], extrasCount=2"
                + ", hardKit=essentials=[minecraft:bread:2, minecraft:wooden_pickaxe:1],"
                + " extras=[minecraft:gold_ingot:2, minecraft:torch:4], extrasCount=1"
                + ", forceCapsule=false"
                + ", capsule=sizeBlocks=7, heightBlocks=3, lightSource=glowstone, lightSpacingBlocks=5"
                + ", endStart=chestTier=medium"
                + ", easyKit=essentials=[minecraft:firework_rocket:16, minecraft:cobblestone:64, minecraft:bread:8,"
                + " minecraft:bow:1, minecraft:arrow:32, minecraft:iron_sword:1, minecraft:copper_pickaxe:1],"
                + " extras=[minecraft:iron_chestplate:1, minecraft:iron_helmet:1, minecraft:golden_apple:2,"
                + " minecraft:ender_pearl:4], extrasCount=3"
                + ", mediumKit=essentials=[minecraft:firework_rocket:8, minecraft:cobblestone:32, minecraft:bread:4,"
                + " minecraft:iron_sword:1, minecraft:stone_pickaxe:1], extras=[minecraft:arrow:16, minecraft:bow:1,"
                + " minecraft:ender_pearl:2], extrasCount=2"
                + ", hardKit=essentials=[minecraft:bread:2, minecraft:wooden_pickaxe:1],"
                + " extras=[minecraft:arrow:8, minecraft:ender_pearl:1], extrasCount=1"
                + ", capsule=sizeBlocks=7, heightBlocks=3, lightSource=glowstone, lightSpacingBlocks=5"
                + ", flat=layers=[minecraft:bedrock:1, minecraft:stone:123, minecraft:dirt:3, minecraft:grass_block:1],"
                + " biome=minecraft:plains, decoration=false,"
                + " structureOverrides=[minecraft:villages, minecraft:strongholds]"
                + ", deepFlat=surfaceY=64, capLayers=[minecraft:dirt:3, minecraft:grass_block:1],"
                + " riversEnabled=true, riverExclusionRadiusBlocks=512"
                + ", stacked=layers=[minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30,"
                + " minecraft:desert, minecraft:badlands, minecraft:swamp, minecraft:jungle,"
                + " minecraft:savanna, minecraft:snowy_taiga,"
                + " minecraft:plains;minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1;0],"
                + " seedRandomizedOrder=false, worldSizeChunks=4, reliefBlocks=4, forceTopVillage=false"
                + ", foreverNight=<disabled>"
                + ", risingLava=<disabled>"
                + ", structureDistance=<disabled>",
            config.summary()
        );
    }

    @Test
    void singleBiomeSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            singleBiome:
              biome: desert
              starter:
                biome: plains
                radius: 512
              spawn:
                strategy: preferred_natural_biome
              naturalBiomes:
                rivers: true
                oceans: true
                beaches: true
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", config.singleBiome.landBiome);
        assertEquals("minecraft:plains", config.singleBiome.starterBiome);
        assertEquals(512, config.singleBiome.starterRadiusBlocks);
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, config.singleBiome.spawn.strategy);
        assertTrue(config.singleBiome.allowRivers);
        assertTrue(config.singleBiome.allowOceans);
        assertTrue(config.singleBiome.allowBeaches);
    }

    @Test
    void singleBiomeAllowRiversAndOceansDefaultFalse() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.singleBiome.allowRivers);
        assertFalse(config.singleBiome.allowOceans);
        assertFalse(config.singleBiome.allowBeaches);
    }

    @Test
    void singleBiomeInvalidLandBiomeFallsBackToPlains() {
        WorldzConfig config = WorldzConfig.parse("""
            singleBiome:
              biome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", config.singleBiome.landBiome);
    }

    @Test
    void singleBiomeStarterBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig id = WorldzConfig.parse("""
            singleBiome:
              starter:
                biome: ' desert '
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tag = WorldzConfig.parse("""
            singleBiome:
              starter:
                biome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", id.singleBiome.starterBiome);
        assertEquals("", tag.singleBiome.starterBiome);
    }

    @Test
    void singleBiomeRadiusIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            singleBiome:
              starter:
                radius: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            singleBiome:
              starter:
                radius: 999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_STARTER_RADIUS_BLOCKS, tooSmall.singleBiome.starterRadiusBlocks);
        assertEquals(WorldzConfig.MAX_STARTER_RADIUS_BLOCKS, tooLarge.singleBiome.starterRadiusBlocks);
    }

    @Test
    void chaosBiomesSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            chaosBiomes:
              biomes:
                - minecraft:plains@3
                - minecraft:desert
              regionScale: 256
              starter:
                biome: plains
                radius: 512
              spawn:
                strategy: preferred_natural_biome
              naturalBiomes:
                rivers: true
                oceans: true
                beaches: true
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:plains@3.0", "minecraft:desert"), config.chaosBiomes.biomes);
        assertEquals(256, config.chaosBiomes.regionScaleBlocks);
        assertEquals("minecraft:plains", config.chaosBiomes.starterBiome);
        assertEquals(512, config.chaosBiomes.starterRadiusBlocks);
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, config.chaosBiomes.spawn.strategy);
        assertTrue(config.chaosBiomes.allowRivers);
        assertTrue(config.chaosBiomes.allowOceans);
        assertTrue(config.chaosBiomes.allowBeaches);
    }

    @Test
    void chaosBiomesEmptyListFallsBackToDefaultBiomes() {
        WorldzConfig config = WorldzConfig.parse("""
            chaosBiomes:
              biomes: []
            """, LOGGER).sanitize(LOGGER);

        assertEquals(new ChaosBiomesConfig().biomes, config.chaosBiomes.biomes);
    }

    @Test
    void chaosBiomesInvalidEntriesAreDroppedIndividually() {
        WorldzConfig config = WorldzConfig.parse("""
            chaosBiomes:
              biomes:
                - minecraft:desert
                - '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:desert"), config.chaosBiomes.biomes);
    }

    @Test
    void chaosBiomesRegionScaleIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            chaosBiomes:
              regionScale: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            chaosBiomes:
              regionScale: 999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, tooSmall.chaosBiomes.regionScaleBlocks);
        assertEquals(WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS, tooLarge.chaosBiomes.regionScaleBlocks);
    }

    @Test
    void stripWorldBandsSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            stripWorld:
              bands:
                enabled: true
                biomes:
                  - minecraft:desert
                  - minecraft:jungle
                width: 256
                seedRandomOrder: true
                naturalBiomes:
                  rivers: false
                  oceans: false
                  beaches: false
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.stripWorld.bands.enabled);
        assertEquals(List.of("minecraft:desert", "minecraft:jungle"), config.stripWorld.bands.biomes);
        assertEquals(256, config.stripWorld.bands.widthBlocks);
        assertTrue(config.stripWorld.bands.seedRandomOrder);
        assertFalse(config.stripWorld.bands.allowRivers);
        assertFalse(config.stripWorld.bands.allowOceans);
        assertFalse(config.stripWorld.bands.allowBeaches);
    }

    @Test
    void stripWorldBandsPassThroughDefaultsTrue() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertTrue(config.stripWorld.bands.allowRivers);
        assertTrue(config.stripWorld.bands.allowOceans);
        assertTrue(config.stripWorld.bands.allowBeaches);
    }

    @Test
    void stripWorldBandsTagsAreDroppedIndividually() {
        WorldzConfig config = WorldzConfig.parse("""
            stripWorld:
              bands:
                enabled: true
                biomes:
                  - minecraft:desert
                  - '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:desert"), config.stripWorld.bands.biomes);
    }

    @Test
    void stripWorldBandsEnabledWithNoUsableBiomesDisablesItself() {
        WorldzConfig config = WorldzConfig.parse("""
            stripWorld:
              bands:
                enabled: true
                biomes:
                  - '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.stripWorld.bands.enabled);
    }

    @Test
    void stripWorldBandsWidthIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            stripWorld:
              bands:
                width: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            stripWorld:
              bands:
                width: 999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, tooSmall.stripWorld.bands.widthBlocks);
        assertEquals(WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS, tooLarge.stripWorld.bands.widthBlocks);
    }

    @Test
    void oceanIslandSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              islandBiome: desert
              radiusBlocks: 256
              shapeAmplitude: 0.4
              shoreWidthBlocks: 16
              oceanShallowWidthBlocks: 32
              oceanDeepenWidthBlocks: 64
              oceanShallowDepthBlocks: 4
              oceanDeepDepthBlocks: 40
              oceanRegionScaleBlocks: 96
              exclusionZoneEnabled: true
              exclusionZoneRadiusBlocks: 1500
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", config.oceanIsland.islandBiome);
        assertEquals(256, config.oceanIsland.radiusBlocks);
        assertEquals(0.4, config.oceanIsland.shapeAmplitude);
        assertEquals(16, config.oceanIsland.shoreWidthBlocks);
        assertEquals(32, config.oceanIsland.oceanShallowWidthBlocks);
        assertEquals(64, config.oceanIsland.oceanDeepenWidthBlocks);
        assertEquals(4, config.oceanIsland.oceanShallowDepthBlocks);
        assertEquals(40, config.oceanIsland.oceanDeepDepthBlocks);
        assertEquals(96, config.oceanIsland.oceanRegionScaleBlocks);
        assertTrue(config.oceanIsland.exclusionZoneEnabled);
        assertEquals(1500, config.oceanIsland.exclusionZoneRadiusBlocks);
    }

    @Test
    void oceanIslandDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals("minecraft:plains", config.oceanIsland.islandBiome);
        assertFalse(config.oceanIsland.exclusionZoneEnabled);
    }

    @Test
    void oceanIslandRadiusIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            oceanIsland:
              radiusBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            oceanIsland:
              radiusBlocks: 9999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, tooSmall.oceanIsland.radiusBlocks);
        assertEquals(WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS, tooLarge.oceanIsland.radiusBlocks);
    }

    @Test
    void oceanIslandShapeAmplitudeIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            oceanIsland:
              shapeAmplitude: -0.5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            oceanIsland:
              shapeAmplitude: 5.0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0.0, tooSmall.oceanIsland.shapeAmplitude);
        assertEquals(IslandShapeProfile.MAX_AMPLITUDE, tooLarge.oceanIsland.shapeAmplitude);
    }

    @Test
    void oceanIslandInvalidIslandBiomeFallsBackToDefault() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              islandBiome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", config.oceanIsland.islandBiome);
    }

    @Test
    void oceanIslandSourceDefaultsToArtificial() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        assertEquals(IslandSource.ARTIFICIAL, config.oceanIsland.islandSource);
    }

    @Test
    void oceanIslandSourceLoadsChestBoat() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              islandSource: chest_boat
            """, LOGGER).sanitize(LOGGER);

        assertEquals(IslandSource.CHEST_BOAT, config.oceanIsland.islandSource);
    }

    @Test
    void oceanIslandSourceLoadsNatural() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              islandSource: natural
            """, LOGGER).sanitize(LOGGER);

        assertEquals(IslandSource.NATURAL, config.oceanIsland.islandSource);
    }

    @Test
    void oceanIslandFluidDefaultsToWater() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        assertEquals(IslandFluid.WATER, config.oceanIsland.fluid);
    }

    @Test
    void oceanIslandFluidLoadsLava() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              fluid: lava
            """, LOGGER).sanitize(LOGGER);

        assertEquals(IslandFluid.LAVA, config.oceanIsland.fluid);
    }

    @Test
    void oceanIslandFluidLoadsNone() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              fluid: none
            """, LOGGER).sanitize(LOGGER);

        assertEquals(IslandFluid.NONE, config.oceanIsland.fluid);
    }

    @Test
    void skyIslandSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              islandBiome: desert
              radiusBlocks: 32
              shapeAmplitude: 0.4
              surfaceY: 80
              thicknessBlocks: 10
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", config.skyIsland.islandBiome);
        assertEquals(32, config.skyIsland.radiusBlocks);
        assertEquals(0.4, config.skyIsland.shapeAmplitude);
        assertEquals(80, config.skyIsland.surfaceY);
        assertEquals(10, config.skyIsland.thicknessBlocks);
    }

    @Test
    void skyIslandDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals("minecraft:plains", config.skyIsland.islandBiome);
        assertEquals(SkyIslandPlan.DEFAULT_SURFACE_Y, config.skyIsland.surfaceY);
        assertEquals(SkyIslandPlan.DEFAULT_THICKNESS_BLOCKS, config.skyIsland.thicknessBlocks);
    }

    @Test
    void skyIslandRadiusIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            skyIsland:
              radiusBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              radiusBlocks: 9999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, tooSmall.skyIsland.radiusBlocks);
        assertEquals(WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS, tooLarge.skyIsland.radiusBlocks);
    }

    @Test
    void skyIslandShapeAmplitudeIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            skyIsland:
              shapeAmplitude: -0.5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              shapeAmplitude: 5.0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0.0, tooSmall.skyIsland.shapeAmplitude);
        assertEquals(IslandShapeProfile.MAX_AMPLITUDE, tooLarge.skyIsland.shapeAmplitude);
    }

    @Test
    void skyIslandThicknessIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            skyIsland:
              thicknessBlocks: 0
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              thicknessBlocks: 999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(SkyIslandPlan.MIN_THICKNESS_BLOCKS, tooSmall.skyIsland.thicknessBlocks);
        assertEquals(SkyIslandPlan.MAX_THICKNESS_BLOCKS, tooLarge.skyIsland.thicknessBlocks);
    }

    @Test
    void skyIslandInvalidIslandBiomeFallsBackToDefault() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              islandBiome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", config.skyIsland.islandBiome);
    }

    @Test
    void caveSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            cave:
              spawnDepthY: -40
              sealedSurface: true
              sealedSurfaceY: 100
              cavernEnabled: true
              cavernRadiusBlocks: 64
              cavernHeightBlocks: 32
              chestEnabled: true
              chestTier: hard
            """, LOGGER).sanitize(LOGGER);

        assertEquals(-40, config.cave.spawnDepthY);
        assertTrue(config.cave.sealedSurface);
        assertEquals(100, config.cave.sealedSurfaceY);
        assertTrue(config.cave.cavernEnabled);
        assertEquals(64, config.cave.cavernRadiusBlocks);
        assertEquals(32, config.cave.cavernHeightBlocks);
        assertTrue(config.cave.chestEnabled);
        assertEquals(StarterKitTier.HARD, config.cave.chestTier);
    }

    @Test
    void caveDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals(CavePlan.DEFAULT_SPAWN_DEPTH_Y, config.cave.spawnDepthY);
        assertFalse(config.cave.sealedSurface);
        assertEquals(CavePlan.DEFAULT_SEALED_SURFACE_Y, config.cave.sealedSurfaceY);
        assertFalse(config.cave.cavernEnabled);
        assertFalse(config.cave.chestEnabled);
    }

    @Test
    void caveSealedSurfaceYIsClampedOnlyWhenEnabled() {
        WorldzConfig tooLow = WorldzConfig.parse("""
            cave:
              sealedSurface: true
              sealedSurfaceY: -999
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig ignoredWhenDisabled = WorldzConfig.parse("""
            cave:
              sealedSurface: false
              sealedSurfaceY: -999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(CaveConfig.MIN_SEALED_SURFACE_Y, tooLow.cave.sealedSurfaceY);
        assertEquals(-999, ignoredWhenDisabled.cave.sealedSurfaceY);
    }

    @Test
    void caveCavernRadiusAndHeightAreClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            cave:
              cavernRadiusBlocks: 1
              cavernHeightBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            cave:
              cavernRadiusBlocks: 9999999
              cavernHeightBlocks: 9999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(CavePlan.MIN_CAVERN_BLOCKS, tooSmall.cave.cavernRadiusBlocks);
        assertEquals(CavePlan.MIN_CAVERN_BLOCKS, tooSmall.cave.cavernHeightBlocks);
        assertEquals(CavePlan.MAX_CAVERN_BLOCKS, tooLarge.cave.cavernRadiusBlocks);
        assertEquals(CavePlan.MAX_CAVERN_BLOCKS, tooLarge.cave.cavernHeightBlocks);
    }

    @Test
    void netherStartSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            netherStart:
              spawnY: 64
              chestTier: hard
              forceCapsule: true
              capsule:
                sizeBlocks: 7
                heightBlocks: 4
                lightSource: lantern
                lightSpacingBlocks: 3
            """, LOGGER).sanitize(LOGGER);

        assertEquals(64, config.netherStart.spawnY);
        assertEquals(StarterKitTier.HARD, config.netherStart.chestTier);
        assertTrue(config.netherStart.forceCapsule);
        assertEquals(7, config.netherStart.capsule.sizeBlocks);
        assertEquals(4, config.netherStart.capsule.heightBlocks);
        assertEquals(LightSource.LANTERN, config.netherStart.capsule.lightSource);
        assertEquals(3, config.netherStart.capsule.lightSpacingBlocks);
    }

    @Test
    void netherStartKitsLoadIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            netherStart:
              easyKit:
                essentials:
                  - minecraft:bread:10
                extrasCount: 0
              hardKit:
                essentials:
                  - minecraft:oak_sapling:1
                extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:bread:10"), config.netherStart.easyKit.essentials);
        assertEquals(List.of("minecraft:oak_sapling:1"), config.netherStart.hardKit.essentials);
        // Untouched kit keeps its own defaults.
        assertEquals(new NetherStartConfig().mediumKit.essentials, config.netherStart.mediumKit.essentials);
    }

    @Test
    void netherStartDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals(NetherStartPlan.DEFAULT_SPAWN_Y, config.netherStart.spawnY);
        assertEquals(StarterKitTier.MEDIUM, config.netherStart.chestTier);
        assertFalse(config.netherStart.forceCapsule);
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_SIZE_BLOCKS, config.netherStart.capsule.sizeBlocks);
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_HEIGHT_BLOCKS, config.netherStart.capsule.heightBlocks);
        assertEquals(LightSource.GLOWSTONE, config.netherStart.capsule.lightSource);
        assertEquals(NetherStartPlan.DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS, config.netherStart.capsule.lightSpacingBlocks);
    }

    @Test
    void netherStartCapsuleSizeIsOddenedAndClamped() {
        WorldzConfig even = WorldzConfig.parse("""
            netherStart:
              capsule:
                sizeBlocks: 6
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooSmall = WorldzConfig.parse("""
            netherStart:
              capsule:
                sizeBlocks: -5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            netherStart:
              capsule:
                sizeBlocks: 9999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(7, even.netherStart.capsule.sizeBlocks);
        assertEquals(NetherStartPlan.MIN_CAPSULE_SIZE_BLOCKS, tooSmall.netherStart.capsule.sizeBlocks);
        assertEquals(NetherStartPlan.MAX_CAPSULE_SIZE_BLOCKS, tooLarge.netherStart.capsule.sizeBlocks);
    }

    @Test
    void netherStartSpawnYIsClamped() {
        WorldzConfig tooLow = WorldzConfig.parse("""
            netherStart:
              spawnY: -999
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooHigh = WorldzConfig.parse("""
            netherStart:
              spawnY: 9999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(NetherStartPlan.MIN_SPAWN_Y, tooLow.netherStart.spawnY);
        assertEquals(NetherStartPlan.MAX_SPAWN_Y, tooHigh.netherStart.spawnY);
    }

    @Test
    void endStartSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            endStart:
              chestTier: hard
              easyKit:
                essentials: ["minecraft:firework_rocket:99"]
                extras: []
                extrasCount: 0
              capsule:
                sizeBlocks: 7
                heightBlocks: 4
                lightSource: lantern
                lightSpacingBlocks: 3
            """, LOGGER).sanitize(LOGGER);

        assertEquals(StarterKitTier.HARD, config.endStart.chestTier);
        assertEquals(List.of("minecraft:firework_rocket:99"), config.endStart.easyKit.essentials);
        assertEquals(0, config.endStart.easyKit.extrasCount);
        assertEquals(7, config.endStart.capsule.sizeBlocks);
        assertEquals(4, config.endStart.capsule.heightBlocks);
        assertEquals(LightSource.LANTERN, config.endStart.capsule.lightSource);
        assertEquals(3, config.endStart.capsule.lightSpacingBlocks);
    }

    @Test
    void endStartDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals(StarterKitTier.MEDIUM, config.endStart.chestTier);
        assertFalse(config.endStart.easyKit.essentials.isEmpty());
        assertFalse(config.endStart.mediumKit.essentials.isEmpty());
        assertFalse(config.endStart.hardKit.essentials.isEmpty());
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_SIZE_BLOCKS, config.endStart.capsule.sizeBlocks);
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_HEIGHT_BLOCKS, config.endStart.capsule.heightBlocks);
        assertEquals(LightSource.GLOWSTONE, config.endStart.capsule.lightSource);
        assertEquals(EndStartPlan.DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS, config.endStart.capsule.lightSpacingBlocks);
    }

    @Test
    void endStartCapsuleSizeIsOddenedAndClamped() {
        WorldzConfig even = WorldzConfig.parse("""
            endStart:
              capsule:
                sizeBlocks: 6
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooSmall = WorldzConfig.parse("""
            endStart:
              capsule:
                sizeBlocks: -5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            endStart:
              capsule:
                sizeBlocks: 9999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(7, even.endStart.capsule.sizeBlocks);
        assertEquals(EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS, tooSmall.endStart.capsule.sizeBlocks);
        assertEquals(EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS, tooLarge.endStart.capsule.sizeBlocks);
    }

    @Test
    void endStartKitExtrasCountIsClampedWhenPoolIsEmpty() {
        WorldzConfig config = WorldzConfig.parse("""
            endStart:
              hardKit:
                essentials: []
                extras: []
                extrasCount: 5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.endStart.hardKit.extrasCount);
    }

    @Test
    void flatSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            flat:
              layers: ["minecraft:bedrock:1", "minecraft:stone:10", "minecraft:grass_block:1"]
              biome: minecraft:desert
              decoration: true
              structureOverrides: ["minecraft:villages", "minecraft:desert_pyramids"]
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:bedrock:1", "minecraft:stone:10", "minecraft:grass_block:1"), config.flat.layers);
        assertEquals("minecraft:desert", config.flat.biome);
        assertTrue(config.flat.decoration);
        assertEquals(List.of("minecraft:villages", "minecraft:desert_pyramids"), config.flat.structureOverrides);
    }

    @Test
    void flatDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.flat.layers.isEmpty());
        assertEquals("minecraft:plains", config.flat.biome);
        assertFalse(config.flat.decoration);
        assertFalse(config.flat.structureOverrides.isEmpty());
    }

    @Test
    void flatEmptyLayersFallBackToDefaults() {
        WorldzConfig config = WorldzConfig.parse("""
            flat:
              layers: []
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.flat.layers.isEmpty());
    }

    @Test
    void deepFlatSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            deepFlat:
              surfaceY: 80
              capLayers: ["minecraft:dirt:2", "minecraft:grass_block:1"]
              riversEnabled: false
              riverExclusionRadiusBlocks: 256
            """, LOGGER).sanitize(LOGGER);

        assertEquals(80, config.deepFlat.surfaceY);
        assertEquals(List.of("minecraft:dirt:2", "minecraft:grass_block:1"), config.deepFlat.capLayers);
        assertFalse(config.deepFlat.riversEnabled);
        assertEquals(256, config.deepFlat.riverExclusionRadiusBlocks);
    }

    @Test
    void deepFlatDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals(DeepFlatPlan.DEFAULT_SURFACE_Y, config.deepFlat.surfaceY);
        assertFalse(config.deepFlat.capLayers.isEmpty());
        assertTrue(config.deepFlat.riversEnabled);
        assertEquals(DeepFlatPlan.DEFAULT_RIVER_EXCLUSION_RADIUS_BLOCKS, config.deepFlat.riverExclusionRadiusBlocks);
    }

    @Test
    void deepFlatSurfaceYIsClamped() {
        WorldzConfig tooLow = WorldzConfig.parse("""
            deepFlat:
              surfaceY: -999
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooHigh = WorldzConfig.parse("""
            deepFlat:
              surfaceY: 9999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(DeepFlatPlan.MIN_SURFACE_Y, tooLow.deepFlat.surfaceY);
        assertEquals(DeepFlatPlan.MAX_SURFACE_Y, tooHigh.deepFlat.surfaceY);
    }

    @Test
    void deepFlatEmptyCapLayersFallBackToDefaults() {
        WorldzConfig config = WorldzConfig.parse("""
            deepFlat:
              capLayers: []
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.deepFlat.capLayers.isEmpty());
    }

    @Test
    void stackedSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              layers: ["minecraft:taiga;minecraft:stone:40;6", "minecraft:plains;minecraft:stone:20,minecraft:grass_block:1;0"]
              seedRandomizedOrder: true
              worldSizeChunks: 8
              reliefBlocks: 2
              forceTopVillage: true
            """, LOGGER).sanitize(LOGGER);

        assertEquals(
            List.of("minecraft:taiga;minecraft:stone:40;6", "minecraft:plains;minecraft:stone:20,minecraft:grass_block:1;0"),
            config.stacked.layers
        );
        assertTrue(config.stacked.seedRandomizedOrder);
        assertEquals(8, config.stacked.worldSizeChunks);
        assertEquals(2, config.stacked.reliefBlocks);
        assertTrue(config.stacked.forceTopVillage);
    }

    @Test
    void stackedDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.stacked.layers.isEmpty());
        assertTrue(config.stacked.layers.size() >= 8);
        assertFalse(config.stacked.seedRandomizedOrder);
        assertEquals(4, config.stacked.worldSizeChunks);
        assertEquals(4, config.stacked.reliefBlocks);
        assertFalse(config.stacked.forceTopVillage);
    }

    @Test
    void stackedEmptyLayersFallBackToDefaults() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              layers: []
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.stacked.layers.isEmpty());
    }

    @Test
    void stackedWorldSizeChunksClampsToNonNegative() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              worldSizeChunks: -1
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.stacked.worldSizeChunks);
    }

    @Test
    void stackedReliefBlocksClampsToConfiguredMaximum() {
        WorldzConfig config = WorldzConfig.parse("""
            stacked:
              reliefBlocks: 9999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MAX_STACKED_RELIEF_BLOCKS, config.stacked.reliefBlocks);
    }

    @Test
    void foreverNightSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            foreverNight:
              enabled: true
              lockAfterDays: 5
              relaxInsomnia: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.foreverNight.enabled);
        assertEquals(5, config.foreverNight.lockAfterDays);
        assertTrue(config.foreverNight.relaxInsomnia);
    }

    @Test
    void foreverNightDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.foreverNight.enabled);
        assertEquals(0, config.foreverNight.lockAfterDays);
        assertFalse(config.foreverNight.relaxInsomnia);
    }

    @Test
    void foreverNightLockAfterDaysClampsToNonNegative() {
        WorldzConfig config = WorldzConfig.parse("""
            foreverNight:
              lockAfterDays: -1
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.foreverNight.lockAfterDays);
    }

    @Test
    void risingLavaSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            risingLava:
              enabled: true
              delayDays: 5
              startY: -32
              maxY: 32
              rate:
                blocks: 2
                days: 3
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.risingLava.enabled);
        assertEquals(5, config.risingLava.delayDays);
        assertEquals(-32, config.risingLava.startY);
        assertEquals(32, config.risingLava.maxY);
        assertEquals(2, config.risingLava.rateBlocks);
        assertEquals(3, config.risingLava.rateDays);
    }

    @Test
    void risingLavaDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.risingLava.enabled);
        assertEquals(3, config.risingLava.delayDays);
        assertEquals(-64, config.risingLava.startY);
        assertEquals(64, config.risingLava.maxY);
        assertEquals(1, config.risingLava.rateBlocks);
        assertEquals(1, config.risingLava.rateDays);
    }

    @Test
    void risingLavaMaxYIsRaisedToMatchAnAboveStartY() {
        WorldzConfig config = WorldzConfig.parse("""
            risingLava:
              startY: 32
              maxY: -32
            """, LOGGER).sanitize(LOGGER);

        assertEquals(32, config.risingLava.startY);
        assertEquals(32, config.risingLava.maxY);
    }

    @Test
    void risingLavaRateValuesClampToAtLeastOne() {
        WorldzConfig config = WorldzConfig.parse("""
            risingLava:
              rate:
                blocks: 0
                days: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(1, config.risingLava.rateBlocks);
        assertEquals(1, config.risingLava.rateDays);
    }

    @Test
    void structureDistanceSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            structureDistance:
              enabled: true
              minDistance: 3000
              exemptStructureSets:
                - minecraft:strongholds
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.structureDistance.enabled);
        assertEquals(3000, config.structureDistance.minDistanceBlocks);
        assertEquals(List.of("minecraft:strongholds"), config.structureDistance.exemptStructureSets);
    }

    @Test
    void structureDistanceDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.structureDistance.enabled);
        assertEquals(2000, config.structureDistance.minDistanceBlocks);
        assertTrue(config.structureDistance.exemptStructureSets.isEmpty());
    }

    @Test
    void structureDistanceMinDistanceClampsToNonNegative() {
        WorldzConfig config = WorldzConfig.parse("""
            structureDistance:
              minDistance: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.structureDistance.minDistanceBlocks);
    }

    @Test
    void floatingIslandsSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                enabled: true
                minRadiusBlocks: 20
                maxRadiusBlocks: 50
                shapeAmplitude: 0.4
                cellSizeBlocks: 300
                spawnChance: 0.8
                biomeVariety: false
                islandBiomes:
                  - desert
                  - taiga
                exclusionZoneEnabled: true
                exclusionZoneRadiusBlocks: 400
                oreDepositsEnabled: true
                oreFeatureIds:
                  - 'minecraft:ore_coal'
                  - 'minecraft:ore_diamond_small'
                lootChestEnabled: true
                lootKit:
                  essentials:
                    - 'minecraft:bread:1'
                  extras:
                    - 'minecraft:emerald:1'
                  extrasCount: 1
                naturalBiome: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.skyIsland.floatingIslands.enabled);
        assertEquals(20, config.skyIsland.floatingIslands.minRadiusBlocks);
        assertEquals(50, config.skyIsland.floatingIslands.maxRadiusBlocks);
        assertEquals(0.4, config.skyIsland.floatingIslands.shapeAmplitude);
        assertEquals(300, config.skyIsland.floatingIslands.cellSizeBlocks);
        assertEquals(0.8, config.skyIsland.floatingIslands.spawnChance);
        assertFalse(config.skyIsland.floatingIslands.biomeVariety);
        assertEquals(List.of("minecraft:desert", "minecraft:taiga"), config.skyIsland.floatingIslands.islandBiomes);
        assertTrue(config.skyIsland.floatingIslands.exclusionZoneEnabled);
        assertEquals(400, config.skyIsland.floatingIslands.exclusionZoneRadiusBlocks);
        assertTrue(config.skyIsland.floatingIslands.oreDepositsEnabled);
        assertEquals(List.of("minecraft:ore_coal", "minecraft:ore_diamond_small"), config.skyIsland.floatingIslands.oreFeatureIds);
        assertTrue(config.skyIsland.floatingIslands.lootChestEnabled);
        assertEquals(List.of("minecraft:bread:1"), config.skyIsland.floatingIslands.lootKit.essentials);
        assertEquals(List.of("minecraft:emerald:1"), config.skyIsland.floatingIslands.lootKit.extras);
        assertEquals(1, config.skyIsland.floatingIslands.lootKit.extrasCount);
        // Regression coverage (2026-07-25): naturalBiome was defined on FloatingIslandsConfig,
        // the codec, and the config-dump summary, but readFloatingIslandsConfig never actually
        // read it from YAML -- config 58's `naturalBiome: true` silently stayed false in every
        // created world (confirmed directly from a real world's persisted world_gen_settings.dat).
        assertTrue(config.skyIsland.floatingIslands.naturalBiome);
    }

    @Test
    void floatingIslandsOreDepositsWithNoUsableFeatureIdsIsDisabled() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                oreDepositsEnabled: true
                oreFeatureIds:
                  - ''
                  - '  '
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.skyIsland.floatingIslands.oreDepositsEnabled);
    }

    @Test
    void floatingIslandsDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.skyIsland.floatingIslands.enabled);
        assertTrue(config.skyIsland.floatingIslands.biomeVariety);
        assertFalse(config.skyIsland.floatingIslands.islandBiomes.isEmpty());
        assertFalse(config.skyIsland.floatingIslands.oreDepositsEnabled);
        assertFalse(config.skyIsland.floatingIslands.oreFeatureIds.isEmpty());
        assertFalse(config.skyIsland.floatingIslands.lootChestEnabled);
        assertFalse(config.skyIsland.floatingIslands.lootKit.essentials.isEmpty());
    }

    @Test
    void floatingIslandsMaxRadiusIsClampedToAtLeastMinRadius() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                minRadiusBlocks: 100
                maxRadiusBlocks: 50
            """, LOGGER).sanitize(LOGGER);

        assertEquals(100, config.skyIsland.floatingIslands.minRadiusBlocks);
        assertEquals(100, config.skyIsland.floatingIslands.maxRadiusBlocks);
    }

    @Test
    void floatingIslandsSpawnChanceIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                spawnChance: -0.5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                spawnChance: 5.0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0.0, tooSmall.skyIsland.floatingIslands.spawnChance);
        assertEquals(1.0, tooLarge.skyIsland.floatingIslands.spawnChance);
    }

    @Test
    void floatingIslandsBiomeVarietyWithNoUsableBiomesIsDisabled() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                biomeVariety: true
                islandBiomes:
                  - '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.skyIsland.floatingIslands.biomeVariety);
    }

    @Test
    void skyIslandChestTierDefaultsToMedium() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        assertEquals(StarterKitTier.MEDIUM, config.skyIsland.chestTier);
    }

    @Test
    void skyIslandChestTierLoadsEasyAndHard() {
        WorldzConfig easy = WorldzConfig.parse("""
            skyIsland:
              chestTier: easy
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig hard = WorldzConfig.parse("""
            skyIsland:
              chestTier: hard
            """, LOGGER).sanitize(LOGGER);

        assertEquals(StarterKitTier.EASY, easy.skyIsland.chestTier);
        assertEquals(StarterKitTier.HARD, hard.skyIsland.chestTier);
    }

    @Test
    void skyIslandKitsLoadIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              easyKit:
                essentials:
                  - minecraft:bread:10
                extrasCount: 0
              hardKit:
                essentials:
                  - minecraft:oak_sapling:1
                extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:bread:10"), config.skyIsland.easyKit.essentials);
        assertEquals(List.of("minecraft:oak_sapling:1"), config.skyIsland.hardKit.essentials);
        // Untouched kit keeps its own defaults.
        assertEquals(new SkyIslandConfig().mediumKit.essentials, config.skyIsland.mediumKit.essentials);
    }

    @Test
    void skyIslandApplyToNetherDefaultsToFalseAndLoads() {
        WorldzConfig defaults = new WorldzConfig().sanitize(LOGGER);
        WorldzConfig enabled = WorldzConfig.parse("""
            skyIsland:
              applyToNether: true
            """, LOGGER).sanitize(LOGGER);

        assertFalse(defaults.skyIsland.applyToNether);
        assertTrue(enabled.skyIsland.applyToNether);
    }

    @Test
    void starterKitSettingsLoadIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              starterKit:
                essentials: ["minecraft:lily_pad:1", "minecraft:dirt:2"]
                extras: ["minecraft:bread:5"]
                extrasCount: 1
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:lily_pad:1", "minecraft:dirt:2"), config.oceanIsland.starterKit.essentials);
        assertEquals(List.of("minecraft:bread:5"), config.oceanIsland.starterKit.extras);
        assertEquals(1, config.oceanIsland.starterKit.extrasCount);
    }

    @Test
    void starterKitDefaultsAreSaneOutOfTheBox() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        assertFalse(config.oceanIsland.starterKit.essentials.isEmpty());
        assertFalse(config.oceanIsland.starterKit.extras.isEmpty());
        assertEquals(2, config.oceanIsland.starterKit.extrasCount);
    }

    @Test
    void starterKitNegativeExtrasCountIsClampedToZero() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              starterKit:
                extrasCount: -3
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.oceanIsland.starterKit.extrasCount);
    }

    @Test
    void starterKitExtrasCountIsClampedToZeroWhenThePoolIsEmpty() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              starterKit:
                extras: []
                extrasCount: 3
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.oceanIsland.starterKit.extrasCount);
    }

    @Test
    void genericPresetAllowRiversAndOceansLoadIndependentlyOfSingleBiomeAndChaosBiomes() {
        WorldzConfig config = WorldzConfig.parse("""
            naturalBiomes:
              rivers: true
              oceans: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.allowRivers);
        assertTrue(config.allowOceans);
        assertFalse(config.singleBiome.allowRivers);
        assertFalse(config.chaosBiomes.allowRivers);
    }

    @Test
    void chaosLayoutModeWithNoLandBiomeFallsBackToLegacy() {
        WorldzConfig config = WorldzConfig.parse("""
            layout:
              mode: chaos
              biomes:
                - minecraft:ocean
            """, LOGGER).sanitize(LOGGER);

        assertEquals(LayoutMode.LEGACY, config.layout.mode);
    }

}
