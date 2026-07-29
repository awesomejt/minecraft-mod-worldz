package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;
import media.jlt.minecraft.mods.worldz.logic.FlatPlan;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
        String malformed = "allowedBiomes: [tru";
        Files.writeString(configFile, malformed);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(DEFAULT_ALLOWED_BIOMES, config.allowedBiomes);
        assertEquals(malformed, Files.readString(configFile));
    }

    @Test
    void unknownKeysAreTolerated() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
    void annotatedReferenceMatchesTheCapturedSchemaDefaults() throws IOException {
        String defaults = Files.readString(Path.of("src/test/resources/config/reference-defaults.yaml"));
        String reference = WorldzConfig.referenceYaml();

        Object expected = YAML.load(defaults);
        Object actual = YAML.load(reference);
        assertEquals(expected, actual);
        assertTrue(reference.startsWith("# jlt_worldz reference config -- GENERATED, do not edit."));
        assertTrue(reference.contains("# Path: allowedBiomes"));
        assertTrue(reference.contains("# Split file: config/jlt_worldz/world-types/worldz.yaml"));
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

    /**
     * DESIGN §44.4.2's own caution (the 25.6a {@code Setting.group} bug precedent): at sanitize
     * time {@code kits}' getter and its {@code Rule.NestedMap} both hand back the exact same map
     * instance, so the setter's {@code putAll(map, itself)} must be a genuine no-op -- asserted
     * explicitly here rather than only reasoned about.
     */
    @Test
    void kitsSanitizeSelfPutAllIsAGenuineNoOp() {
        WorldzConfig config = new WorldzConfig();
        Map<String, StarterKitConfig> beforeSanitize = config.kits;
        int sizeBefore = beforeSanitize.size();

        config.sanitize(LOGGER);

        assertEquals(14, sizeBefore, "sanity check: the shipped library has 14 entries before sanitize");
        assertSame(beforeSanitize, config.kits, "sanitize must not replace the kits map instance");
        assertEquals(sizeBefore, config.kits.size(), "self-putAll(map, itself) must not duplicate or drop entries");
    }

    /**
     * A user {@code kits} entry overrides a shipped name: the setter merges (DESIGN §44.4.2), so
     * the override's contents win for that one name while every other shipped kit is untouched.
     */
    @Test
    void kitsSectionUserEntryOverridesAShippedName() {
        WorldzConfig config = WorldzConfig.parse("""
            kits:
              cave-easy:
                essentials: [minecraft:diamond:1]
                extras: []
                extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(14, config.kits.size(), "overriding an existing name must not change the entry count");
        assertEquals(List.of("minecraft:diamond:1"), config.kits.get("cave-easy").essentials);
        assertEquals(
            List.of("minecraft:torch:8", "minecraft:oak_log:2", "minecraft:dirt:4"), config.kits.get("cave-medium").essentials,
            "every other shipped kit must be untouched"
        );
    }

    /**
     * A user {@code kits} entry adds a brand-new name: the setter merges rather than replaces
     * (DESIGN §44.4.2), so the 14 shipped names survive alongside it.
     */
    @Test
    void kitsSectionUserEntryAddsANewNameWithoutDeletingTheFourteenShipped() {
        WorldzConfig config = WorldzConfig.parse("""
            kits:
              my-brutal-kit:
                essentials: [minecraft:stick:1]
                extras: []
                extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(15, config.kits.size());
        assertTrue(config.kits.containsKey("my-brutal-kit"));
        assertEquals(List.of("minecraft:stick:1"), config.kits.get("my-brutal-kit").essentials);
        assertTrue(config.kits.containsKey("cave-easy"), "the shipped 14 must survive a user addition");
        assertTrue(config.kits.containsKey("floating-islands-loot"), "the shipped 14 must survive a user addition");
    }

    /**
     * The core regression gate for TODO 25.8c (DESIGN §44.8 row c): a config with zero kit-related
     * keys at any of the 12 tiered sites must resolve to byte-identical {@code essentials}/{@code
     * extras}/{@code extrasCount} to the pre-25.8 values -- not just "it parses". The comparison
     * target is {@link KitLibrary#shipped()}'s own entry, which {@code KitLibraryTest} (TODO 25.8b)
     * already proved matches the original {@code *Defaults()} factories verbatim, so this test is
     * what actually proves the 12 now-deleted factories' values still reach each site.
     */
    @Test
    void zeroKitKeyConfigsResolveToByteIdenticalContentsForEveryTieredSite() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        Map<String, StarterKitConfig> shipped = KitLibrary.shipped();

        assertKitMatchesLibraryEntry(config.cave.easyKit, shipped.get("cave-easy"));
        assertKitMatchesLibraryEntry(config.cave.mediumKit, shipped.get("cave-medium"));
        assertKitMatchesLibraryEntry(config.cave.hardKit, shipped.get("cave-hard"));
        assertKitMatchesLibraryEntry(config.skyIsland.easyKit, shipped.get("sky-island-easy"));
        assertKitMatchesLibraryEntry(config.skyIsland.mediumKit, shipped.get("sky-island-medium"));
        assertKitMatchesLibraryEntry(config.skyIsland.hardKit, shipped.get("sky-island-hard"));
        assertKitMatchesLibraryEntry(config.netherStart.easyKit, shipped.get("nether-start-easy"));
        assertKitMatchesLibraryEntry(config.netherStart.mediumKit, shipped.get("nether-start-medium"));
        assertKitMatchesLibraryEntry(config.netherStart.hardKit, shipped.get("nether-start-hard"));
        assertKitMatchesLibraryEntry(config.endStart.easyKit, shipped.get("end-start-easy"));
        assertKitMatchesLibraryEntry(config.endStart.mediumKit, shipped.get("end-start-medium"));
        assertKitMatchesLibraryEntry(config.endStart.hardKit, shipped.get("end-start-hard"));
    }

    private static void assertKitMatchesLibraryEntry(StarterKitConfig resolved, StarterKitConfig libraryEntry) {
        assertEquals(libraryEntry.essentials, resolved.essentials);
        assertEquals(libraryEntry.extras, resolved.extras);
        assertEquals(libraryEntry.extrasCount, resolved.extrasCount);
    }

    /**
     * An unknown kit name at one of the 12 tiered sites (DESIGN §44.3.4/§44.6) warns and falls back
     * to that site's own shipped default -- not a crash, not an empty kit.
     */
    @Test
    void unknownKitNameAtATieredSiteWarnsAndFallsBackToItsOwnShippedDefault() {
        WorldzConfig config = WorldzConfig.parse("""
            cave:
              chest:
                kits:
                  easy: nonexistent-kit
            """, LOGGER).sanitize(LOGGER);

        StarterKitConfig fallenBack = config.cave.easyKit;
        StarterKitConfig caveEasy = KitLibrary.shipped().get("cave-easy");

        assertEquals("cave-easy", fallenBack.ref, "the unknown name is rewritten to the site's own default name");
        assertEquals(caveEasy.essentials, fallenBack.essentials);
        assertEquals(caveEasy.extras, fallenBack.extras);
        assertEquals(caveEasy.extrasCount, fallenBack.extrasCount);
    }

    /**
     * The last 2 sites' own regression gate for TODO 25.8d (DESIGN §44.8 row d), the same proof
     * {@link #zeroKitKeyConfigsResolveToByteIdenticalContentsForEveryTieredSite} already gives the
     * 12 tiered sites: a config with zero kit-related keys at {@code oceanIsland.starterKit} or
     * {@code skyIsland.floatingIslands.lootChest.kit} must resolve to byte-identical {@code
     * essentials}/{@code extras}/{@code extrasCount} to the pre-25.8 values. {@code
     * ocean-island-default}'s comparison target is {@code new StarterKitConfig()} itself (DESIGN
     * §44.3.2 -- it is not a moved factory), already proved equal to {@code KitLibrary.shipped()
     * .get("ocean-island-default")} by {@code KitLibraryTest
     * .oceanIslandDefaultIsTheBareConstructorDefaultNotAMovedFactory}.
     */
    @Test
    void zeroKitKeyConfigsResolveToByteIdenticalContentsForOceanIslandAndFloatingIslands() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);
        Map<String, StarterKitConfig> shipped = KitLibrary.shipped();

        assertKitMatchesLibraryEntry(config.oceanIsland.starterKit, shipped.get("ocean-island-default"));
        assertKitMatchesLibraryEntry(config.skyIsland.floatingIslands.lootKit, shipped.get("floating-islands-loot"));
    }

    /**
     * An unknown kit name at either of the last 2 sites (DESIGN §44.3.4/§44.6, TODO 25.8d) warns and
     * falls back to that site's own shipped default -- the same posture {@link
     * #unknownKitNameAtATieredSiteWarnsAndFallsBackToItsOwnShippedDefault} already proves for the 12
     * tiered sites.
     */
    @Test
    void unknownKitNameAtOceanIslandOrFloatingIslandsWarnsAndFallsBackToItsOwnShippedDefault() {
        WorldzConfig oceanConfig = WorldzConfig.parse("""
            oceanIsland:
              starterKit: nonexistent-kit
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig floatingConfig = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                lootChest:
                  kit: nonexistent-kit
            """, LOGGER).sanitize(LOGGER);

        StarterKitConfig oceanFallenBack = oceanConfig.oceanIsland.starterKit;
        StarterKitConfig oceanDefault = KitLibrary.shipped().get("ocean-island-default");
        assertEquals("ocean-island-default", oceanFallenBack.ref, "the unknown name is rewritten to the site's own default name");
        assertKitMatchesLibraryEntry(oceanFallenBack, oceanDefault);

        StarterKitConfig floatingFallenBack = floatingConfig.skyIsland.floatingIslands.lootKit;
        StarterKitConfig floatingDefault = KitLibrary.shipped().get("floating-islands-loot");
        assertEquals("floating-islands-loot", floatingFallenBack.ref, "the unknown name is rewritten to the site's own default name");
        assertKitMatchesLibraryEntry(floatingFallenBack, floatingDefault);
    }

    /**
     * The completeness half of DESIGN §44.5's own test recommendation ("every one of the 14 sites'
     * default ref resolves in that map") -- {@code KitLibraryTest
     * .shippedHasExactlyTheFourteenNamesInOrder} already proves the library holds exactly these 14
     * names; this proves every site's own unsanitized default actually names one of them. Provable
     * only now that TODO 25.8d converts the last 2 sites, completing all 14 (DESIGN §44.8 row d).
     */
    @Test
    void everySitesDefaultRefNamesARealShippedKitLibraryEntry() {
        WorldzConfig config = new WorldzConfig();
        Set<String> siteDefaultRefs = Set.of(
            config.cave.easyKit.ref, config.cave.mediumKit.ref, config.cave.hardKit.ref,
            config.skyIsland.easyKit.ref, config.skyIsland.mediumKit.ref, config.skyIsland.hardKit.ref,
            config.netherStart.easyKit.ref, config.netherStart.mediumKit.ref, config.netherStart.hardKit.ref,
            config.endStart.easyKit.ref, config.endStart.mediumKit.ref, config.endStart.hardKit.ref,
            config.oceanIsland.starterKit.ref, config.skyIsland.floatingIslands.lootKit.ref
        );

        assertEquals(KitLibrary.shipped().keySet(), siteDefaultRefs, "every site's own default ref must name a real shipped kit");
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
            stripWorld:
              enabled: true
              width: 0
              widthMode: ocean
              applyToNether: true
            """, LOGGER).sanitize(LOGGER);

        assertTrue(config.stripWorld.enabled);
        assertEquals(1, config.stripWorld.width);
        assertEquals(ExteriorMode.OCEAN, config.stripWorld.widthMode);
        assertTrue(config.stripWorld.applyToNether);
    }

    @Test
    void stripDefaultsToDisabledWithAVoidWidthMode() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertFalse(config.stripWorld.enabled);
        assertEquals(65, config.stripWorld.width);
        assertEquals(ExteriorMode.VOID, config.stripWorld.widthMode);
        assertFalse(config.stripWorld.applyToNether);
    }

    @Test
    void stripWidthModeCannotBeNormal() {
        WorldzConfig config = WorldzConfig.parse("""
            stripWorld:
              enabled: true
              widthMode: normal
            """, LOGGER).sanitize(LOGGER);

        assertEquals(ExteriorMode.VOID, config.stripWorld.widthMode);
    }

    @Test
    void oldStripSectionAndRadiusKeyAreNotCompatibilityAliases() {
        WorldzConfig config = WorldzConfig.parse("""
            strip:
              enabled: true
              widthRadiusBlocks: 7
            stripWorld:
              widthRadiusBlocks: 9
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.stripWorld.enabled);
        assertEquals(65, config.stripWorld.width);
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
        String invalid = "spawn: true";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, config.spawn.strategy);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void invalidNestedBorderTypeMakesFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
        String invalid = "overworldBorder: true";
        Files.writeString(configFile, invalid);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertFalse(config.overworldBorder.enabled);
        assertEquals(invalid, Files.readString(configFile));
    }

    @Test
    void invalidBorderScalarMakesFileInvalidWithoutOverwritingIt() throws IOException {
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
        Path configFile = temporaryDirectory.resolve("jlt_worldz/all.yaml");
        Files.createDirectories(configFile.getParent());
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
            "kits=[cave-easy, cave-medium, cave-hard, sky-island-easy, sky-island-medium, sky-island-hard,"
                + " nether-start-easy, nether-start-medium, nether-start-hard, end-start-easy, end-start-medium,"
                + " end-start-hard, ocean-island-default, floating-islands-loot]"
                + ", allowedBiomes=[minecraft:plains, #minecraft:is_overworld]"
                + ", starter=biome=<none>, radius=256, land=transition=128, foundation=48"
                + ", naturalBiomes=rivers=false, oceans=false"
                + ", overworldBorder=<disabled>, netherBorder=<disabled>, endBorder=<disabled>"
                + ", overworldExterior=<normal>, netherExterior=<normal>"
                + ", layout=<legacy>"
                + ", spawn=starter_at_origin"
                + ", singleBiome=biome=minecraft:plains, starter=biome=<none>, radius=256"
                + ", spawn=starter_at_origin"
                + ", naturalBiomes=rivers=false, oceans=false, beaches=false"
                + ", chaosBiomes=biomes=[minecraft:desert, minecraft:jungle, minecraft:ice_spikes,"
                + " minecraft:badlands, minecraft:taiga], regionScale=512, starter=biome=<none>, radius=256"
                + ", spawn=starter_at_origin, naturalBiomes=rivers=false, oceans=false, beaches=false"
                + ", stripWorld=enabled=false, width=65, widthMode=void, applyToNether=false"
                + ", spawn=starter_at_origin, bands=<disabled>"
                + ", oceanIsland=island=source=artificial, biome=minecraft:plains, radius=128, shapeAmplitude=0.3"
                + ", fluid=water, shoreWidth=12"
                + ", ocean=shallowWidth=64, deepenWidth=128, shallowDepth=8, deepDepth=32, regionScale=128"
                + ", exclusionZone=<disabled>"
                + ", starterKit=ocean-island-default"
                + ", skyIsland=biome=minecraft:plains, radius=16, shapeAmplitude=0.3"
                + ", surfaceY=64, thickness=6, chest=tier=medium"
                + ", kits=easy=sky-island-easy, medium=sky-island-medium, hard=sky-island-hard"
                + ", applyToNether=false"
                + ", exclusionZone=radius=128"
                + ", underground=biome=<none>, belowSurface=10"
                + ", floatingIslands=<disabled>"
                + ", chunkIsland=<disabled>"
                + ", cave=spawnY=-32, sealedSurface=enabled=false, y=128, block=stone, thickness=5"
                + ", cavern=enabled=false, radius=48, height=24"
                + ", chest=enabled=false, tier=medium"
                + ", kits=easy=cave-easy, medium=cave-medium, hard=cave-hard"
                + ", netherStart=spawnY=32, chest=tier=medium"
                + ", kits=easy=nether-start-easy, medium=nether-start-medium, hard=nether-start-hard"
                + ", forceCapsule=false"
                + ", capsule=size=7, height=3, light=source=glowstone, spacing=5"
                + ", endStart=chest=tier=medium"
                + ", kits=easy=end-start-easy, medium=end-start-medium, hard=end-start-hard"
                + ", capsule=size=7, height=3, light=source=glowstone, spacing=5"
                + ", flat=layers=[minecraft:bedrock:1, minecraft:stone:123, minecraft:dirt:3, minecraft:grass_block:1],"
                + " biome=minecraft:plains, decoration=false,"
                + " structureOverrides=[minecraft:villages, minecraft:strongholds]"
                + ", underground=biome=<none>, belowSurface=10"
                + ", deepFlat=surfaceY=64, capLayers=[minecraft:dirt:3, minecraft:grass_block:1],"
                + " rivers=enabled=true, exclusionRadius=512"
                + ", stacked=layers=[minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30,"
                + " minecraft:desert, minecraft:badlands, minecraft:swamp, minecraft:jungle,"
                + " minecraft:savanna, minecraft:snowy_taiga,"
                + " minecraft:plains;minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1;0],"
                + " seedRandomizedOrder=false, worldSizeChunks=4, relief=4, forceTopVillage=false"
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
              island:
                biome: desert
                radius: 256
                shapeAmplitude: 0.4
              shoreWidth: 16
              ocean:
                shallowWidth: 32
                deepenWidth: 64
                shallowDepth: 4
                deepDepth: 40
                regionScale: 96
              exclusionZone:
                enabled: true
                radius: 1500
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
              island:
                radius: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            oceanIsland:
              island:
                radius: 9999999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, tooSmall.oceanIsland.radiusBlocks);
        assertEquals(WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS, tooLarge.oceanIsland.radiusBlocks);
    }

    @Test
    void oceanIslandShapeAmplitudeIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            oceanIsland:
              island:
                shapeAmplitude: -0.5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            oceanIsland:
              island:
                shapeAmplitude: 5.0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0.0, tooSmall.oceanIsland.shapeAmplitude);
        assertEquals(IslandShapeProfile.MAX_AMPLITUDE, tooLarge.oceanIsland.shapeAmplitude);
    }

    @Test
    void oceanIslandInvalidIslandBiomeFallsBackToDefault() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              island:
                biome: '#minecraft:is_overworld'
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
              island:
                source: chest_boat
            """, LOGGER).sanitize(LOGGER);

        assertEquals(IslandSource.CHEST_BOAT, config.oceanIsland.islandSource);
    }

    @Test
    void oceanIslandSourceLoadsNatural() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              island:
                source: natural
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
              biome: desert
              radius: 32
              shapeAmplitude: 0.4
              surfaceY: 80
              thickness: 10
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
              radius: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              radius: 9999999
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
              thickness: 0
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            skyIsland:
              thickness: 999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(SkyIslandPlan.MIN_THICKNESS_BLOCKS, tooSmall.skyIsland.thicknessBlocks);
        assertEquals(SkyIslandPlan.MAX_THICKNESS_BLOCKS, tooLarge.skyIsland.thicknessBlocks);
    }

    @Test
    void skyIslandInvalidIslandBiomeFallsBackToDefault() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              biome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:plains", config.skyIsland.islandBiome);
    }

    /**
     * TODO 25.6d: {@code skyIsland}'s own {@code exclusionZone} was a real {@link SkyIslandConfig}
     * field pair, consumed by world-gen logic, but never wired into read/sanitize at all before this
     * task -- {@code config/tests/57-sky-island-biome-exclusion-zone.yaml}'s values were silently
     * ignored. This is the regression test that would have caught that gap, and confirms the wire-up
     * actually threads a non-default value through, not merely that the key is recognized.
     */
    @Test
    void skyIslandExclusionZoneLoadsAndSanitizesIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              exclusionZone:
                enabled: false
                radius: 64
            """, LOGGER).sanitize(LOGGER);

        assertFalse(config.skyIsland.exclusionZoneEnabled);
        assertEquals(64, config.skyIsland.exclusionZoneRadiusBlocks);
    }

    @Test
    void skyIslandExclusionZoneDefaultsToEnabledWithA128BlockRadius() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertTrue(config.skyIsland.exclusionZoneEnabled);
        assertEquals(128, config.skyIsland.exclusionZoneRadiusBlocks);
    }

    @Test
    void skyIslandExclusionZoneRadiusIsClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            skyIsland:
              exclusionZone:
                radius: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(1, tooSmall.skyIsland.exclusionZoneRadiusBlocks);
    }

    /**
     * TODO 25.6g: {@code skyIsland.undergroundBiome}/{@code undergroundBelowSurfaceBlocks} were
     * real {@link SkyIslandConfig} fields, read directly by {@link SkyIslandPlan#fromConfig}, but
     * never wired into read/sanitize at all before this task -- {@code
     * config/tests/98-sky-island-underground-biome-band.yaml}'s values were silently ignored. This
     * is the regression test that would have caught that gap, confirming the wire-up actually
     * threads a non-default value through, not merely that the key is recognized.
     */
    @Test
    void skyIslandUndergroundLoadsAndSanitizesIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              underground:
                biome: minecraft:dripstone_caves
                belowSurface: 3
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:dripstone_caves", config.skyIsland.undergroundBiome);
        assertEquals(3, config.skyIsland.undergroundBelowSurfaceBlocks);
    }

    @Test
    void skyIslandUndergroundDefaultsToDisabled() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals("", config.skyIsland.undergroundBiome);
        assertEquals(10, config.skyIsland.undergroundBelowSurfaceBlocks);
    }

    @Test
    void skyIslandUndergroundBelowSurfaceIsFlooredAtZero() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              underground:
                belowSurface: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.skyIsland.undergroundBelowSurfaceBlocks);
    }

    @Test
    void caveSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            cave:
              spawnY: -40
              sealedSurface:
                enabled: true
                y: 100
              cavern:
                enabled: true
                radius: 64
                height: 32
              chest:
                enabled: true
                tier: hard
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
              sealedSurface:
                enabled: true
                y: -999
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig ignoredWhenDisabled = WorldzConfig.parse("""
            cave:
              sealedSurface:
                enabled: false
                y: -999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(CaveConfig.MIN_SEALED_SURFACE_Y, tooLow.cave.sealedSurfaceY);
        assertEquals(-999, ignoredWhenDisabled.cave.sealedSurfaceY);
    }

    @Test
    void caveCavernRadiusAndHeightAreClamped() {
        WorldzConfig tooSmall = WorldzConfig.parse("""
            cave:
              cavern:
                radius: 1
                height: 1
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            cave:
              cavern:
                radius: 9999999
                height: 9999999
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
              chest:
                tier: hard
              forceCapsule: true
              capsule:
                size: 7
                height: 4
                light:
                  source: lantern
                  spacing: 3
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
              chest:
                kits:
                  easy:
                    essentials:
                      - minecraft:bread:10
                    extrasCount: 0
                  hard:
                    essentials:
                      - minecraft:oak_sapling:1
                    extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:bread:10"), config.netherStart.easyKit.essentials);
        assertEquals(List.of("minecraft:oak_sapling:1"), config.netherStart.hardKit.essentials);
        // Untouched kit keeps resolving its own shipped kits-library default (TODO 25.8c: the raw
        // field is now a bare reference stub, not an inline copy, so the pre-25.8 comparison target
        // is KitLibrary's own entry, not `new NetherStartConfig().mediumKit`).
        assertEquals(KitLibrary.shipped().get("nether-start-medium").essentials, config.netherStart.mediumKit.essentials);
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
                size: 6
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooSmall = WorldzConfig.parse("""
            netherStart:
              capsule:
                size: -5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            netherStart:
              capsule:
                size: 9999
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
              chest:
                tier: hard
                kits:
                  easy:
                    essentials: ["minecraft:firework_rocket:99"]
                    extras: []
                    extrasCount: 0
              capsule:
                size: 7
                height: 4
                light:
                  source: lantern
                  spacing: 3
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
                size: 6
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooSmall = WorldzConfig.parse("""
            endStart:
              capsule:
                size: -5
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig tooLarge = WorldzConfig.parse("""
            endStart:
              capsule:
                size: 9999
            """, LOGGER).sanitize(LOGGER);

        assertEquals(7, even.endStart.capsule.sizeBlocks);
        assertEquals(EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS, tooSmall.endStart.capsule.sizeBlocks);
        assertEquals(EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS, tooLarge.endStart.capsule.sizeBlocks);
    }

    @Test
    void endStartKitExtrasCountIsClampedWhenPoolIsEmpty() {
        WorldzConfig config = WorldzConfig.parse("""
            endStart:
              chest:
                kits:
                  hard:
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

    /**
     * TODO 25.6g: {@code flat.undergroundBiome}/{@code undergroundBelowSurfaceBlocks} were real
     * {@link FlatConfig} fields, read directly by {@link FlatPlan#fromConfig}, but never wired into
     * read/sanitize at all before this task -- {@code
     * config/tests/97-flat-underground-biome-band.yaml}'s values were silently ignored. This is the
     * regression test that would have caught that gap, confirming the wire-up actually threads a
     * non-default value through, not merely that the key is recognized.
     */
    @Test
    void flatUndergroundLoadsAndSanitizesIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            flat:
              underground:
                biome: minecraft:dripstone_caves
                belowSurface: 15
            """, LOGGER).sanitize(LOGGER);

        assertEquals("minecraft:dripstone_caves", config.flat.undergroundBiome);
        assertEquals(15, config.flat.undergroundBelowSurfaceBlocks);
    }

    @Test
    void flatUndergroundDefaultsToDisabled() {
        WorldzConfig config = new WorldzConfig().sanitize(LOGGER);

        assertEquals("", config.flat.undergroundBiome);
        assertEquals(10, config.flat.undergroundBelowSurfaceBlocks);
    }

    @Test
    void flatUndergroundBelowSurfaceIsFlooredAtZero() {
        WorldzConfig config = WorldzConfig.parse("""
            flat:
              underground:
                belowSurface: -5
            """, LOGGER).sanitize(LOGGER);

        assertEquals(0, config.flat.undergroundBelowSurfaceBlocks);
    }

    @Test
    void flatUndergroundInvalidBiomeIsIgnoredRatherThanFailing() {
        WorldzConfig config = WorldzConfig.parse("""
            flat:
              underground:
                biome: '#minecraft:is_overworld'
            """, LOGGER).sanitize(LOGGER);

        assertEquals("", config.flat.undergroundBiome);
    }

    @Test
    void deepFlatSettingsLoadAndSanitizeIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            deepFlat:
              surfaceY: 80
              capLayers: ["minecraft:dirt:2", "minecraft:grass_block:1"]
              rivers:
                enabled: false
                exclusionRadius: 256
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
              relief: 2
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
              relief: 9999
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
                radius:
                  min: 20
                  max: 50
                shapeAmplitude: 0.4
                cellSize: 300
                spawnChance: 0.8
                biomeVariety: false
                biomes:
                  - desert
                  - taiga
                exclusionZone:
                  enabled: true
                  radius: 400
                oreDeposits:
                  enabled: true
                  featureIds:
                    - 'minecraft:ore_coal'
                    - 'minecraft:ore_diamond_small'
                lootChest:
                  enabled: true
                  kit:
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
                oreDeposits:
                  enabled: true
                  featureIds:
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

    /**
     * DESIGN §44.3.2's load-bearing partial-inline case, proved directly at {@code
     * skyIsland.floatingIslands.lootChest.kit} (TODO 25.8d): writing only {@code extrasCount:}
     * inline (no {@code essentials}/{@code extras}) must still inherit {@link StarterKitConfig}'s
     * own no-arg constructor defaults for the two untouched fields -- the same guarantee {@link
     * #starterKitPartialInlineExtrasCountOnlyInheritsConstructorEssentialsAndExtras} proves at {@code
     * oceanIsland.starterKit}.
     */
    @Test
    void floatingIslandsLootKitPartialInlineExtrasCountOnlyInheritsConstructorEssentialsAndExtras() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                lootChest:
                  kit:
                    extrasCount: 1
            """, LOGGER).sanitize(LOGGER);
        StarterKitConfig constructorDefaults = new StarterKitConfig();

        assertEquals(constructorDefaults.essentials, config.skyIsland.floatingIslands.lootKit.essentials);
        assertEquals(constructorDefaults.extras, config.skyIsland.floatingIslands.lootKit.extras);
        assertEquals(1, config.skyIsland.floatingIslands.lootKit.extrasCount);
    }

    @Test
    void floatingIslandsMaxRadiusIsClampedToAtLeastMinRadius() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              floatingIslands:
                radius:
                  min: 100
                  max: 50
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
                biomes:
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
              chest:
                tier: easy
            """, LOGGER).sanitize(LOGGER);
        WorldzConfig hard = WorldzConfig.parse("""
            skyIsland:
              chest:
                tier: hard
            """, LOGGER).sanitize(LOGGER);

        assertEquals(StarterKitTier.EASY, easy.skyIsland.chestTier);
        assertEquals(StarterKitTier.HARD, hard.skyIsland.chestTier);
    }

    @Test
    void skyIslandKitsLoadIndependently() {
        WorldzConfig config = WorldzConfig.parse("""
            skyIsland:
              chest:
                kits:
                  easy:
                    essentials:
                      - minecraft:bread:10
                    extrasCount: 0
                  hard:
                    essentials:
                      - minecraft:oak_sapling:1
                    extrasCount: 0
            """, LOGGER).sanitize(LOGGER);

        assertEquals(List.of("minecraft:bread:10"), config.skyIsland.easyKit.essentials);
        assertEquals(List.of("minecraft:oak_sapling:1"), config.skyIsland.hardKit.essentials);
        // Untouched kit keeps resolving its own shipped kits-library default (TODO 25.8c: the raw
        // field is now a bare reference stub, not an inline copy, so the pre-25.8 comparison target
        // is KitLibrary's own entry, not `new SkyIslandConfig().mediumKit`).
        assertEquals(KitLibrary.shipped().get("sky-island-medium").essentials, config.skyIsland.mediumKit.essentials);
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

    /**
     * DESIGN §44.3.2's load-bearing partial-inline case, proved directly at {@code
     * oceanIsland.starterKit}: writing only {@code extrasCount:} inline (no {@code essentials}/
     * {@code extras}) must still inherit {@link StarterKitConfig}'s own no-arg constructor defaults
     * for the two untouched fields -- {@code SchemaSection.read} builds a fresh instance from {@code
     * factory.get()} and sets only the keys present, unaffected by TODO 25.8d's reference conversion
     * since an inline mapping still parses through the inline path exactly as before.
     */
    @Test
    void starterKitPartialInlineExtrasCountOnlyInheritsConstructorEssentialsAndExtras() {
        WorldzConfig config = WorldzConfig.parse("""
            oceanIsland:
              starterKit:
                extrasCount: 1
            """, LOGGER).sanitize(LOGGER);
        StarterKitConfig constructorDefaults = new StarterKitConfig();

        assertEquals(constructorDefaults.essentials, config.oceanIsland.starterKit.essentials);
        assertEquals(constructorDefaults.extras, config.oceanIsland.starterKit.extras);
        assertEquals(1, config.oceanIsland.starterKit.extrasCount);
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
