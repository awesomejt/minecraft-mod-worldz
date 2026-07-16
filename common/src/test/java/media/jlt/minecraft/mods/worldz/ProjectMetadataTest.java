package media.jlt.minecraft.mods.worldz;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataTest {
    private static final Path ROOT = Path.of("..");

    @Test
    void declaredAndRepositoryLicenseIdentityIsConsistentlyMit() throws IOException {
        Properties properties = projectProperties();
        JsonObject fabricMetadata = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/fabric.mod.json")
        )).getAsJsonObject();
        String neoForgeMetadata = Files.readString(
            ROOT.resolve("neoforge/src/main/resources/META-INF/neoforge.mods.toml")
        );
        String license = Files.readString(ROOT.resolve("LICENSE"));

        assertEquals("MIT", properties.getProperty("license"));
        assertEquals("MIT", fabricMetadata.get("license").getAsString());
        assertTrue(neoForgeMetadata.contains("license = \"${license}\""));
        assertTrue(license.startsWith("MIT License\n\nCopyright (c) 2026 Jason Taylor"));
        assertFalse(license.contains("CC0 1.0"));
        assertTrue(Files.readString(
            ROOT.resolve("common/src/main/resources/META-INF/LICENSES/Apache-2.0.txt")
        ).contains("Apache License\n                           Version 2.0"));
        assertTrue(Files.readString(
            ROOT.resolve("common/src/main/resources/META-INF/NOTICE")
        ).contains("SnakeYAML 2.6"));
    }

    @Test
    void projectIdentityMatchesDesignContract() throws IOException {
        Properties properties = projectProperties();
        String settings = Files.readString(ROOT.resolve("settings.gradle"));

        assertTrue(settings.contains("rootProject.name = 'mod-worldz'"));
        assertEquals("media.jlt.minecraft.mods", properties.getProperty("group"));
        assertEquals("0.1.13", properties.getProperty("version"));
        assertEquals("jlt_worldz", properties.getProperty("mod_id"));
        assertEquals("JLT Worldz", properties.getProperty("mod_name"));
        assertEquals("25", properties.getProperty("java_version"));
        assertEquals("26.2", properties.getProperty("minecraft_version"));
    }

    @Test
    void canonicalConfigurationExampleIsYaml() throws IOException {
        String readme = Files.readString(ROOT.resolve("README.md"));
        String design = Files.readString(ROOT.resolve("DESIGN.md"));

        assertTrue(Files.isRegularFile(ROOT.resolve("config/jlt_worldz.example.yaml")));
        assertFalse(Files.exists(ROOT.resolve("config/jlt_worldz.example.json")));
        assertTrue(readme.contains("config/jlt_worldz.yaml"));
        assertFalse(readme.contains("config/jlt_worldz.example.json"));
        assertTrue(design.contains("Config `config/jlt_worldz.yaml`"));
        assertTrue(readme.contains("`resizeRateBlocks: 64` and `resizeRateDays: 5`"));
        assertTrue(readme.contains("`resizeDelayDays` holds the initial radius"));
        assertTrue(readme.contains("### Ocean and void exteriors"));
        assertTrue(readme.contains("Nether supports\nnormal and void only"));
        assertFalse(readme.contains("Nether and End generation remain vanilla"));
    }

    @Test
    void clientPresetEditorIsDeclaredForFabricAndImplementedForNeoForge() throws IOException {
        JsonObject fabricMetadata = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/fabric.mod.json")
        )).getAsJsonObject();
        JsonObject mixinConfig = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/jlt_worldz.mixins.json")
        )).getAsJsonObject();

        assertEquals("jlt_worldz.mixins.json", fabricMetadata.getAsJsonArray("mixins").get(0).getAsString());
        assertEquals(
            "client.WorldCreationUiStateMixin",
            mixinConfig.getAsJsonArray("client").get(0).getAsString()
        );
        assertTrue(Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        ).contains("RegisterPresetEditorsEvent"));
    }

    @Test
    void envelopedGeneratorCodecIsRegisteredByBothLoaders() throws IOException {
        String fabric = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/WorldzFabric.java")
        );
        String neoForge = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForge.java")
        );

        assertTrue(fabric.contains("BuiltInRegistries.CHUNK_GENERATOR"));
        assertTrue(fabric.contains("EnvelopedChunkGenerator.CODEC"));
        assertTrue(neoForge.contains("Registries.CHUNK_GENERATOR"));
        assertTrue(neoForge.contains("CHUNK_GENERATORS.register(\"enveloped\""));
    }

    @Test
    void delayedBordersUseBothLoaderTickHooks() throws IOException {
        String fabric = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/WorldzFabric.java")
        );
        String neoForge = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForge.java")
        );
        String manager = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/WorldLimitManager.java"
        ));

        assertTrue(fabric.contains("ServerTickEvents.END_SERVER_TICK.register(WorldLimitManager::onServerTick)"));
        assertTrue(neoForge.contains("onServerTick(ServerTickEvent.Post event)"));
        assertTrue(manager.contains("state.pendingStartTick(overworld)"));
        assertTrue(manager.contains("state.clearPendingStart(overworld)"));
    }

    @Test
    void customizationScreensExposeExteriorAndRateFields() throws IOException {
        String customize = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzCustomizeScreen.java"
        ));
        String border = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzBorderScreen.java"
        ));
        String exterior = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzExteriorScreen.java"
        ));

        assertTrue(customize.contains("WorldzExteriorScreen"));
        assertTrue(customize.contains("WorldzStarterLandScreen"));
        assertTrue(customize.contains("this.overworldExterior"));
        assertTrue(customize.contains("this.netherExterior"));
        assertTrue(border.contains("resizeRateBlocks.getValue()"));
        assertTrue(border.contains("resizeRateDays.getValue()"));
        assertTrue(border.contains("resizeDelayDays.getValue()"));
        assertTrue(exterior.contains("ExteriorSettings.fromText"));
        assertTrue(exterior.contains("case NORMAL -> this.overworld ? ExteriorMode.OCEAN : ExteriorMode.VOID"));
    }

    @Test
    void customizeScreenScrollsAndClarifiesBorderVersusExteriorIndependence() throws IOException {
        String customize = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzCustomizeScreen.java"
        ));
        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();

        assertTrue(customize.contains("new ScrollableLayout(this.minecraft, form, SCROLL_AREA_MIN_HEIGHT)"));
        assertTrue(customize.contains("this.scrollArea.setMaxHeight(this.scrollArea.getHeight() + availableExtraHeight)"));
        assertTrue(customize.contains(".tooltip(borderTooltip)"));
        assertTrue(customize.contains(".tooltip(exteriorTooltip)"));
        assertTrue(lang.has("jlt_worldz.customize.border.tooltip"));
        assertTrue(lang.has("jlt_worldz.customize.exterior.tooltip"));
    }

    @Test
    void starterLandPlanIsPersistedWithCompatibilityFallback() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));
        String codecs = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/StarterLandCodecs.java"
        ));

        assertTrue(source.contains("optionalFieldOf(\"starter_land\")"));
        assertTrue(source.contains("encodedStarterLand.orElseGet(StarterLandPlan::disabled)"));
        assertTrue(codecs.contains("fieldOf(\"transition_width\")"));
        assertTrue(codecs.contains("fieldOf(\"foundation_depth\")"));
        assertTrue(codecs.contains("optionalFieldOf(\"profile_version\", StarterLandPlan.LEGACY_PROFILE_VERSION)"));
    }

    @Test
    void starterLandGenerationRunsAtConsistentChunkStages() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains("applyStarterLand(chunk, randomState, false)"));
        assertTrue(generator.contains("applyStarterLand(chunk, randomState, true)"));
        assertTrue(generator.contains("StarterLandProfile.targetHeight"));
        assertTrue(generator.contains("Heightmap.Types.OCEAN_FLOOR_WG"));
        assertTrue(generator.contains("Noises.SURFACE_SECONDARY"));
        assertTrue(generator.contains("Math.max(layoutHeight, starterLandTargetHeight"));
        assertTrue(generator.contains("super.createStructures("));
    }

    @Test
    void layoutAdjustmentRunsAtConsistentChunkStagesAndSkipsVoid() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains("applyLayoutAdjustment(chunk, randomState, false)"));
        assertTrue(generator.contains("applyLayoutAdjustment(chunk, randomState, true)"));
        assertTrue(generator.contains("LayoutTerrainProfile.targetHeight"));
        assertTrue(generator.contains("plan.mode() == LayoutMode.LEGACY || plan.mode() == LayoutMode.VOID"));
        assertTrue(generator.contains("layoutAdjustedHeight(x, z, naturalHeight, heightAccessor, randomState)"));
        // fillFromNoise raises before buildSurface runs, and applyCarvers only repairs
        // (never lowers) so a carved-through raise still preserves the surface shell.
        int fillFromNoiseIndex = generator.indexOf("public CompletableFuture<ChunkAccess> fillFromNoise(");
        int applyCarversIndex = generator.indexOf("public void applyCarvers(");
        int layoutInFillFromNoise = generator.indexOf("applyLayoutAdjustment(chunk, randomState, false)");
        int layoutInApplyCarvers = generator.indexOf("applyLayoutAdjustment(chunk, randomState, true)");
        assertTrue(fillFromNoiseIndex >= 0 && applyCarversIndex >= 0);
        assertTrue(layoutInApplyCarvers > applyCarversIndex && layoutInApplyCarvers < fillFromNoiseIndex);
        assertTrue(layoutInFillFromNoise > fillFromNoiseIndex);
    }

    @Test
    void limitedBiomeSourceConsumesTheLayoutPlanForNonLegacyModes() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains("this.worldLayoutPlan.mode() != LayoutMode.LEGACY"));
        assertTrue(source.contains("this.worldLayoutPlan.sampleAt(blockX - originX, blockZ - originZ).biomeId()"));
        assertTrue(source.contains("resolveLayoutBiomes(worldLayoutPlan, biomeGetter)"));
        assertTrue(source.contains("possible.addAll(layoutBiomes.values())"));
    }

    @Test
    void starterZoneBeachRingPrefersALayoutBeachBiome() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains("isInStarterTransitionRing(quartX, quartZ)"));
        assertTrue(source.contains("this.worldLayoutPlan.sampleRole(BiomeRole.BEACH, blockX - originX, blockZ - originZ)"));
        assertTrue(source.contains("StarterZone.inRingQuart("));
    }

    @Test
    void progressionObjectivesRequireLayoutSupportiveTerrain() throws IOException {
        String guarantees = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/ProgressionGuarantees.java"
        ));
        String manager = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/WorldLimitManager.java"
        ));

        assertTrue(guarantees.contains(
            "ObjectiveSite.isSupportiveColumn(layoutPlan, natural.getX() - originX, natural.getZ() - originZ)"
        ));
        assertTrue(guarantees.contains(
            "ObjectiveSite.supportiveFallbackZ(layoutPlan, relativeX, radius, NATURAL_STRUCTURE_MARGIN)"
        ));
        assertTrue(manager.contains("limitedSource.worldLayoutPlan()"));
        assertTrue(manager.contains("limitedSource.originBlockX()"));
    }

    @Test
    void voidLayoutModeForcesASkyIslandExteriorAroundTheStarterZone() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains("resolveEnvelope(delegate, dimension, envelope)"));
        assertTrue(generator.contains("source.worldLayoutPlan().mode() != LayoutMode.VOID"));
        assertTrue(generator.contains("new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, Math.max(islandRadius, 1), 0)"));
    }

    @Test
    void structuresAreSuppressedNearAMixedLayoutRoleBoundary() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));
        String layoutPlan = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/logic/WorldLayoutPlan.java"
        ));

        assertTrue(generator.contains(
            "if (!isEntirelyExterior(centerChunk.getPos()) && !isNearLayoutRoleBoundary(centerChunk.getPos())) {"
        ));
        assertTrue(generator.contains("plan.isNearRoleBoundary(minX, minZ)"));
        assertTrue(generator.contains("plan.isNearRoleBoundary(maxX, maxZ)"));
        assertTrue(layoutPlan.contains("public boolean isNearRoleBoundary(int blockX, int blockZ) {"));
        assertTrue(layoutPlan.contains("if (mode != LayoutMode.MIXED) {"));
    }

    @Test
    void landOnlyModeUsesTheGentlerRiverPreservingTarget() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains("plan.mode() == LayoutMode.LAND_ONLY"));
        assertTrue(generator.contains("LayoutTerrainProfile.landOnlyTarget(naturalFloor, seaLevel)"));
    }

    @Test
    void starterLandTransitionBlendsTowardTheLayoutAdjustedFloor() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains(
            "int blendBaseline = this.layout.isPresent()\n"
                + "            ? layoutFloorFor(this.layout.get().plan(), x - originX, z - originZ, naturalFloor, getSeaLevel())\n"
                + "            : naturalFloor;"
        ));
    }

    @Test
    void layoutScreenIsWiredIntoCustomizeAndPresetEditor() throws IOException {
        String customize = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzCustomizeScreen.java"
        ));
        String layoutScreen = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzLayoutScreen.java"
        ));
        String presetEditor = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzPresetEditor.java"
        ));

        assertTrue(customize.contains("WorldzLayoutScreen"));
        assertTrue(customize.contains("this.worldLayout"));
        assertTrue(layoutScreen.contains("WorldzCustomization.LayoutSettings.fromText("));
        assertTrue(layoutScreen.contains("this.parent.setLayout(settings)"));
        assertTrue(presetEditor.contains("customization.worldLayoutPlan(new Random().nextLong())"));
        assertTrue(presetEditor.contains("fromPlan(source.worldLayoutPlan())"));
    }

    @Test
    void limitedBiomeSourcePersistsSpawnStrategyAndMutableOrigin() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains("Codec.STRING.optionalFieldOf(\"spawn_strategy\")"));
        assertTrue(source.contains("private volatile int originBlockX;"));
        assertTrue(source.contains("private volatile int originBlockZ;"));
        assertTrue(source.contains("public void setOrigin(int blockX, int blockZ) {"));
        assertTrue(source.contains(
            "SpawnStrategy spawnStrategy = encodedStarterRadius.isPresent()\n"
                + "            ? encodedSpawnStrategy.map(SpawnStrategy::parse).orElse(SpawnStrategy.STARTER_AT_ORIGIN)\n"
                + "            : encodedSpawnStrategy.map(SpawnStrategy::parse).orElseGet(() -> config.spawn.strategy);"
        ));
    }

    @Test
    void envelopedChunkGeneratorRecentersOnTheOverworldOriginSourceOnly() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains("private final Optional<LimitedBiomeSource> originSource;"));
        assertTrue(generator.contains(
            "this.originSource = dimension == Dimension.OVERWORLD && delegate.getBiomeSource() instanceof LimitedBiomeSource source"
        ));
        assertTrue(generator.contains("this.originSource.map(LimitedBiomeSource::originBlockX).orElse(0);"));
        assertTrue(generator.contains("this.originSource.map(LimitedBiomeSource::originBlockZ).orElse(0);"));
    }

    @Test
    void spawnOriginManagerHasSeparateReapplyAndFreshResolutionEntryPoints() throws IOException {
        String manager = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/SpawnOriginManager.java"
        ));
        String state = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/SpawnOriginState.java"
        ));

        assertTrue(manager.contains("public static void reapplyPersistedOrigin(ServerLevel overworld) {"));
        assertTrue(manager.contains("public static Optional<BlockPos> resolveFreshOrigin(ServerLevel overworld) {"));
        assertTrue(manager.contains("limitedSource.spawnStrategy() != SpawnStrategy.PREFERRED_NATURAL_BIOME"));
        assertTrue(manager.contains("RandomState.create("));
        assertTrue(manager.contains("MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD"));
        assertTrue(manager.contains("SpawnSearchPlan.defaults().offsetsInSearchOrder()"));
        assertTrue(state.contains("Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, \"spawn_origin\")"));
    }

    @Test
    void neoForgeWiresLevelLoadAndCreateSpawnPositionHooks() throws IOException {
        String neoForge = Files.readString(ROOT.resolve(
            "neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForge.java"
        ));

        assertTrue(neoForge.contains("NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onLevelLoad);"));
        assertTrue(neoForge.contains("NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onCreateSpawnPosition);"));
        assertTrue(neoForge.contains("SpawnOriginManager.reapplyPersistedOrigin(level);"));
        assertTrue(neoForge.contains("SpawnOriginManager.resolveFreshOrigin(level).ifPresent(pos -> {"));
        assertTrue(neoForge.contains("event.setCanceled(true);"));
    }

    @Test
    void fabricWiresServerLevelLoadAndTheInitialSpawnMixin() throws IOException {
        String fabric = Files.readString(ROOT.resolve(
            "fabric/src/main/java/media/jlt/minecraft/mods/worldz/WorldzFabric.java"
        ));
        String mixinConfig = Files.readString(ROOT.resolve(
            "fabric/src/main/resources/jlt_worldz.mixins.json"
        ));
        String mixin = Files.readString(ROOT.resolve(
            "fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/MinecraftServerMixin.java"
        ));

        assertTrue(fabric.contains("ServerLevelEvents.LOAD.register((server, level) -> {"));
        assertTrue(fabric.contains("SpawnOriginManager.reapplyPersistedOrigin(level);"));
        assertTrue(mixinConfig.contains("\"MinecraftServerMixin\""));
        assertTrue(mixin.contains("@Inject(method = \"setInitialSpawn\", at = @At(\"HEAD\"), cancellable = true)"));
        assertTrue(mixin.contains("SpawnOriginManager.resolveFreshOrigin(level)"));
        assertTrue(mixin.contains("callback.cancel();"));
    }

    @Test
    void customizeScreenExposesASpawnStrategyCycleButton() throws IOException {
        String customize = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/client/WorldzCustomizeScreen.java"
        ));

        assertTrue(customize.contains("private SpawnStrategy spawnStrategy;"));
        assertTrue(customize.contains("private Button spawnStrategyButton;"));
        assertTrue(customize.contains("private void cycleSpawnStrategy() {"));
        assertTrue(customize.contains(
            "this.spawnStrategy = values[(this.spawnStrategy.ordinal() + 1) % values.length];"
        ));
    }

    private static Properties projectProperties() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(ROOT.resolve("gradle.properties"))) {
            properties.load(reader);
        }
        return properties;
    }
}
