package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
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
                + ", allowRivers=false, allowOceans=false, allowBeaches=false"
                + ", chaosBiomes=biomes=[minecraft:desert, minecraft:jungle, minecraft:ice_spikes,"
                + " minecraft:badlands, minecraft:taiga], regionScaleBlocks=512, starterBiome=<none>"
                + ", starterRadiusBlocks=256, spawn=starter_at_origin, allowRivers=false, allowOceans=false"
                + ", allowBeaches=false"
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
                + ", easyKit=essentials=[minecraft:oak_sapling:4, minecraft:bread:8, minecraft:crafting_table:1],"
                + " extras=[minecraft:wooden_pickaxe:1, minecraft:wooden_axe:1, minecraft:torch:16,"
                + " minecraft:cobblestone:32], extrasCount=3"
                + ", mediumKit=essentials=[minecraft:oak_sapling:3, minecraft:bread:4],"
                + " extras=[minecraft:wooden_pickaxe:1, minecraft:torch:8, minecraft:cobblestone:16], extrasCount=2"
                + ", hardKit=essentials=[minecraft:oak_sapling:2], extras=[minecraft:bread:2, minecraft:torch:4],"
                + " extrasCount=1"
                + ", applyToNether=false"
                + ", floatingIslands=<disabled>"
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
              allowBeaches: true
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
              allowBeaches: true
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
    void stripWorldBandsSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            stripWorld:
              bands:
                enabled: true
                biomes:
                  - minecraft:desert
                  - minecraft:jungle
                widthBlocks: 256
                seedRandomOrder: true
                allowRivers: false
                allowOceans: false
                allowBeaches: false
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
                widthBlocks: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            stripWorld:
              bands:
                widthBlocks: 999999
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
