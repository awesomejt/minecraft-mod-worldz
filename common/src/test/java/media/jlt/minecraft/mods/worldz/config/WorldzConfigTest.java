package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
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
        "minecraft:deep_dark"
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingConfigUsesDefaultsWithoutCreatingAFile() throws IOException {
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
        Files.writeString(configFile, """
            allowedBiomes:
              - desert
            futureOption:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(List.of("minecraft:desert"), config.allowedBiomes);
        assertEquals(256, config.starterRadiusBlocks);
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
    void starterLandSettingsLoadAndClampIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            ensureStarterLand: false
            starterLandTransitionBlocks: 5000
            starterLandFoundationDepthBlocks: -4
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
              initialRadiusBlocks: 0
              finalRadiusBlocks: 2000
              resizeDays: 100
              resizeDelayDays: 12
              resizeRateBlocks: 128
              resizeRateDays: 5
              resizeStyle: stepped
              ensureEndPortal: false
            netherBorder:
              enabled: true
              initialRadiusBlocks: 256
              finalRadiusBlocks: 128
              resizeDays: 25
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
              initialRadiusBlocks: 8
              finalRadiusBlocks: 1024
              resizeStyle: stepped
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ResizeStyle.CONTINUOUS, config.overworldBorder.resizeStyle);
    }

    @Test
    void endBorderLoadsAndClampsItsMinimumRadius() {
        WorldzConfig config = WorldzConfig.parse("""
            endBorder:
              carryFromOverworld: true
              minimumRadiusBlocks: 0
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
              regionScaleBlocks: 300
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
              regionScaleBlocks: 1
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
        WorldzConfig config = WorldzConfig.parse("starterBiome: minecraft:plains", LOGGER).sanitize(LOGGER);

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
        String invalid = "starterRadiusBlocks: 64.5";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(256, config.starterRadiusBlocks);
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
            starterBiome: ''
            starterRadiusBlocks: 256
            """, LOGGER).sanitize(LOGGER);

        assertEquals(
            "allowedBiomes=[minecraft:plains, #minecraft:is_overworld], starterBiome=<none>, starterRadiusBlocks=256"
                + ", starterLand=transition=128, foundation=48"
                + ", overworldBorder=<disabled>, netherBorder=<disabled>, endBorder=<disabled>"
                + ", overworldExterior=<normal>, netherExterior=<normal>"
                + ", strip=<disabled>"
                + ", layout=<legacy>"
                + ", spawn=starter_at_origin"
                + ", singleBiome=landBiome=minecraft:plains, starterBiome=<none>"
                + ", starterRadiusBlocks=256, spawn=starter_at_origin"
                + ", allowRivers=false, allowOceans=false"
                + ", chaosBiomes=biomes=[minecraft:desert, minecraft:jungle, minecraft:ice_spikes,"
                + " minecraft:badlands, minecraft:taiga], regionScaleBlocks=512, starterBiome=<none>"
                + ", starterRadiusBlocks=256, spawn=starter_at_origin, allowRivers=false, allowOceans=false"
                + ", allowRivers=false, allowOceans=false",
            config.summary()
        );
    }

    @Test
    void singleBiomeSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            singleBiome:
              landBiome: desert
              starterBiome: plains
              starterRadiusBlocks: 512
              spawn:
                strategy: preferred_natural_biome
              allowRivers: true
              allowOceans: true
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", config.singleBiome.landBiome);
        assertEquals("minecraft:plains", config.singleBiome.starterBiome);
        assertEquals(512, config.singleBiome.starterRadiusBlocks);
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, config.singleBiome.spawn.strategy);
        assertTrue(config.singleBiome.allowRivers);
        assertTrue(config.singleBiome.allowOceans);
    }

    @Test
    void singleBiomeAllowRiversAndOceansDefaultFalse() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.singleBiome.allowRivers);
        assertFalse(config.singleBiome.allowOceans);
    }

    @Test
    void singleBiomeInvalidLandBiomeFallsBackToPlains() {
        WorldzConfig config = WorldzConfig.parse("""
            singleBiome:
              landBiome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", config.singleBiome.landBiome);
    }

    @Test
    void singleBiomeStarterBiomeAcceptsIdsButRejectsTags() {
        WorldzConfig id = WorldzConfig.parse("""
            singleBiome:
              starterBiome: ' desert '
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tag = WorldzConfig.parse("""
            singleBiome:
              starterBiome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:desert", id.singleBiome.starterBiome);
        assertEquals("", tag.singleBiome.starterBiome);
    }

    @Test
    void singleBiomeRadiusIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            singleBiome:
              starterRadiusBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            singleBiome:
              starterRadiusBlocks: 999999
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
              regionScaleBlocks: 256
              starterBiome: plains
              starterRadiusBlocks: 512
              spawn:
                strategy: preferred_natural_biome
              allowRivers: true
              allowOceans: true
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:plains@3.0", "minecraft:desert"), config.chaosBiomes.biomes);
        assertEquals(256, config.chaosBiomes.regionScaleBlocks);
        assertEquals("minecraft:plains", config.chaosBiomes.starterBiome);
        assertEquals(512, config.chaosBiomes.starterRadiusBlocks);
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, config.chaosBiomes.spawn.strategy);
        assertTrue(config.chaosBiomes.allowRivers);
        assertTrue(config.chaosBiomes.allowOceans);
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
              regionScaleBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            chaosBiomes:
              regionScaleBlocks: 999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, tooSmall.chaosBiomes.regionScaleBlocks);
        assertEquals(WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS, tooLarge.chaosBiomes.regionScaleBlocks);
    }

    @Test
    void genericPresetAllowRiversAndOceansLoadIndependentlyOfSingleBiomeAndChaosBiomes() {
        WorldzConfig config = WorldzConfig.parse("""
            allowRivers: true
            allowOceans: true
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
