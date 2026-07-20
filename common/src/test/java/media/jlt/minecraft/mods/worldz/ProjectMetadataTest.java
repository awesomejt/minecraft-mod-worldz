package media.jlt.minecraft.mods.worldz;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        assertEquals("0.2.48", properties.getProperty("version"));
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
    void singleBiomeTypedPresetIsWiredIntoBothLoaders() throws IOException {
        String fabricMixin = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/client/WorldCreationUiStateMixin.java")
        );
        assertTrue(fabricMixin.contains("SingleBiomePresetEditor.SINGLE_BIOME_PRESET"));
        assertTrue(fabricMixin.contains("SingleBiomePresetEditor.INSTANCE"));

        String neoForgeClient = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        );
        assertTrue(neoForgeClient.contains("event.register(SingleBiomePresetEditor.SINGLE_BIOME_PRESET, SingleBiomePresetEditor.INSTANCE);"));

        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();
        assertEquals("Worldz: Single Biome", lang.get("generator.jlt_worldz.single_biome").getAsString());
    }

    @Test
    void chaosBiomesTypedPresetIsWiredIntoBothLoaders() throws IOException {
        String fabricMixin = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/client/WorldCreationUiStateMixin.java")
        );
        assertTrue(fabricMixin.contains("ChaosBiomesPresetEditor.CHAOS_BIOMES_PRESET"));
        assertTrue(fabricMixin.contains("ChaosBiomesPresetEditor.INSTANCE"));

        String neoForgeClient = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        );
        assertTrue(neoForgeClient.contains("event.register(ChaosBiomesPresetEditor.CHAOS_BIOMES_PRESET, ChaosBiomesPresetEditor.INSTANCE);"));

        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();
        assertEquals("Worldz: Chaos Biomes", lang.get("generator.jlt_worldz.chaos_biomes").getAsString());
    }

    @Test
    void stripWorldTypedPresetIsWiredIntoBothLoaders() throws IOException {
        String fabricMixin = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/client/WorldCreationUiStateMixin.java")
        );
        assertTrue(fabricMixin.contains("StripWorldPresetEditor.STRIP_WORLD_PRESET"));
        assertTrue(fabricMixin.contains("StripWorldPresetEditor.INSTANCE"));

        String neoForgeClient = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        );
        assertTrue(neoForgeClient.contains("event.register(StripWorldPresetEditor.STRIP_WORLD_PRESET, StripWorldPresetEditor.INSTANCE);"));

        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();
        assertEquals("Worldz: Strip World", lang.get("generator.jlt_worldz.strip_world").getAsString());
    }

    @Test
    void oceanIslandTypedPresetIsWiredIntoBothLoaders() throws IOException {
        String fabricMixin = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/client/WorldCreationUiStateMixin.java")
        );
        assertTrue(fabricMixin.contains("OceanIslandPresetEditor.OCEAN_ISLAND_PRESET"));
        assertTrue(fabricMixin.contains("OceanIslandPresetEditor.INSTANCE"));

        String neoForgeClient = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        );
        assertTrue(neoForgeClient.contains("event.register(OceanIslandPresetEditor.OCEAN_ISLAND_PRESET, OceanIslandPresetEditor.INSTANCE);"));

        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();
        assertEquals("Worldz: Ocean Island", lang.get("generator.jlt_worldz.ocean_island").getAsString());
    }

    @Test
    void skyIslandTypedPresetIsWiredIntoBothLoaders() throws IOException {
        String fabricMixin = Files.readString(
            ROOT.resolve("fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/client/WorldCreationUiStateMixin.java")
        );
        assertTrue(fabricMixin.contains("SkyIslandPresetEditor.SKY_ISLAND_PRESET"));
        assertTrue(fabricMixin.contains("SkyIslandPresetEditor.INSTANCE"));

        String neoForgeClient = Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        );
        assertTrue(neoForgeClient.contains("event.register(SkyIslandPresetEditor.SKY_ISLAND_PRESET, SkyIslandPresetEditor.INSTANCE);"));

        JsonObject lang = JsonParser.parseString(Files.readString(
            ROOT.resolve("common/src/main/resources/assets/jlt_worldz/lang/en_us.json")
        )).getAsJsonObject();
        assertEquals("Worldz: Sky Island", lang.get("generator.jlt_worldz.sky_island").getAsString());
    }

    @Test
    void limitedBiomeSourceAppliesStripWorldBandsWithoutCustomize() throws IOException {
        // Regression guard (Jason found in-game, GOALS 36 follow-up): creating a strip_world
        // directly from world_preset/strip_world.json's fieldless "world_type": "strip_world"
        // hint -- never opening Customize -- previously fell straight through to the generic
        // preset's own defaults (LayoutMode.LEGACY), silently ignoring stripWorld.bands.
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains(
            "boolean stripWorldDefaults = encodedStarterRadius.isEmpty()\n"
                + "            && encodedWorldType.map(\"strip_world\"::equals).orElse(false);"
        ));
        assertTrue(source.contains("resolveFullVanillaOverworldAllowed(biomeGetter)"));
        assertTrue(source.contains("stripWorldDefaults && config.stripWorld.bands.enabled"));
        assertTrue(source.contains("WorldLayoutPlan.resolveBands("));
        assertTrue(source.contains("stripWorldDefaults ? config.stripWorld.bands.allowRivers : config.allowRivers"));
        assertTrue(source.contains("stripWorldDefaults ? config.stripWorld.bands.allowOceans : config.allowOceans"));
        assertTrue(source.contains("stripWorldDefaults ? config.stripWorld.bands.allowBeaches : false"));
    }

    @Test
    void chaosModeIsExcludedFromTerrainAdjustmentAndSharesThePassThroughGate() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        // GOALS 33: terrain shape stays exactly vanilla for chaos worlds -- CHAOS joins
        // LEGACY/VOID in the terrain-adjustment skip list rather than SINGLE_BIOME's
        // guaranteed-land raise.
        assertTrue(generator.contains(
            "if (mode == LayoutMode.LEGACY || mode == LayoutMode.VOID || mode == LayoutMode.CHAOS || mode == LayoutMode.STRIP_BANDS) {"
        ));
        assertTrue(source.contains(
            "boolean supportsPassThrough = worldLayoutPlan.mode() == LayoutMode.SINGLE_BIOME\n"
                + "            || worldLayoutPlan.mode() == LayoutMode.CHAOS\n"
                + "            || worldLayoutPlan.mode() == LayoutMode.STRIP_BANDS;"
        ));
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

        assertTrue(generator.contains("applyTerrainAdjustments(chunk, randomState, false)"));
        assertTrue(generator.contains("applyTerrainAdjustments(chunk, randomState, true)"));
        assertTrue(generator.contains("StarterLandProfile.targetHeight"));
        assertTrue(generator.contains("StarterLandProfile.strengthAt"));
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

        assertTrue(generator.contains("applyTerrainAdjustments(chunk, randomState, false)"));
        assertTrue(generator.contains("applyTerrainAdjustments(chunk, randomState, true)"));
        assertTrue(generator.contains("LayoutTerrainProfile.targetHeight"));
        assertTrue(generator.contains("mode == LayoutMode.LEGACY || mode == LayoutMode.VOID"));
        assertTrue(generator.contains("layoutFloorOrNatural(x, z, naturalFloor, randomState)"));
        // Both stages now share one merged per-column pass (computes the natural floor once
        // instead of twice -- see MEMORY.md's 2026-07-17 performance entry), so what used to
        // be two distinct calls is now one call per stage; still confirm carvers (repair-only)
        // runs before fillFromNoise (the full raise/lower pass).
        int fillFromNoiseIndex = generator.indexOf("public CompletableFuture<ChunkAccess> fillFromNoise(");
        int applyCarversIndex = generator.indexOf("public void applyCarvers(");
        int adjustmentsInFillFromNoise = generator.indexOf("applyTerrainAdjustments(chunk, randomState, false)");
        int adjustmentsInApplyCarvers = generator.indexOf("applyTerrainAdjustments(chunk, randomState, true)");
        assertTrue(fillFromNoiseIndex >= 0 && applyCarversIndex >= 0);
        assertTrue(adjustmentsInApplyCarvers > applyCarversIndex && adjustmentsInApplyCarvers < fillFromNoiseIndex);
        assertTrue(adjustmentsInFillFromNoise > fillFromNoiseIndex);
    }

    @Test
    void limitedBiomeSourceConsumesTheLayoutPlanForNonLegacyModes() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains("this.worldLayoutPlan.mode() != LayoutMode.LEGACY"));
        assertTrue(source.contains("layoutPlan.sampleAt(blockX - originX, blockZ - originZ).biomeId()"));
        assertTrue(source.contains("resolveLayoutBiomes(worldLayoutPlan, biomeGetter)"));
        assertTrue(source.contains("possible.addAll(layoutBiomes.values())"));
        // Regression guard (Jason found in-game, GOALS 36): resolveLayoutBiomes must pull
        // STRIP_BANDS ids into the resolved-holder map same as every other mode's biome
        // lists, or sampleAt's returned biomeId never resolves to a real Holder<Biome> and
        // getNoiseBiome silently falls through to plain vanilla climate-filtered biomes.
        assertTrue(source.contains("ids.addAll(plan.bandBiomes())"));
    }

    @Test
    void limitedBiomeSourceResolvesTheRealSeedThroughAMutableEffectivePlan() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(source.contains("private volatile WorldLayoutPlan effectiveLayoutPlan;"));
        assertTrue(source.contains("public WorldLayoutPlan effectiveLayoutPlan() {"));
        assertTrue(source.contains("public void setLayoutSeed(long seed) {"));
        assertTrue(source.contains("this.effectiveLayoutPlan = this.worldLayoutPlan.withSeed(seed);"));
        assertTrue(source.contains("WorldLayoutPlan layoutPlan = this.effectiveLayoutPlan;"));
        assertTrue(generator.contains("private record LayoutContext(LimitedBiomeSource source) {"));
        assertTrue(generator.contains("return source.effectiveLayoutPlan();"));
    }

    @Test
    void starterZoneBeachRingPrefersALayoutBeachBiome() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));

        assertTrue(source.contains("isInStarterTransitionRing(quartX, quartZ)"));
        assertTrue(source.contains("layoutPlan.sampleRole(BiomeRole.BEACH, blockX - originX, blockZ - originZ)"));
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
            "ObjectiveSite.supportiveFallbackZ(layoutPlan, relativeX, radius, zRadius, NATURAL_STRUCTURE_MARGIN)"
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
    void starterLandTransitionBlendsTowardTheLayoutAdjustedFloor() throws IOException {
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        assertTrue(generator.contains(
            "int targetHeight = starterLandTargetHeight(x, z, chunk, randomState, naturalFloor, layoutFloor);"
        ));
        assertTrue(generator.contains(
            "private int layoutFloorOrNatural(int x, int z, int naturalFloor, RandomState randomState) {\n"
                + "        return this.layout.isPresent()\n"
                + "            ? layoutFloorFor(this.layout.get().plan(), x, z, originX(), originZ(), naturalFloor, getSeaLevel(), randomState)\n"
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
                + "            : encodedSpawnStrategy.map(SpawnStrategy::parse).orElseGet(() -> chaosBiomesDefaults\n"
                + "                ? config.chaosBiomes.spawn.strategy\n"
                + "                : singleBiomeDefaults\n"
                + "                    ? config.singleBiome.spawn.strategy\n"
                + "                    : stripWorldDefaults\n"
                + "                        ? config.stripWorld.spawn.strategy\n"
                + "                        : oceanIslandDefaults || skyIslandDefaults\n"
                + "                            ? SpawnStrategy.STARTER_AT_ORIGIN\n"
                + "                            : config.spawn.strategy);"
        ));
        assertTrue(source.contains("Codec.STRING.optionalFieldOf(\"world_type\").forGetter(source -> Optional.<String>empty())"));
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
        // 2026-07-17: STARTER_AT_ORIGIN now explicitly resolves spawn via safeSpawnNear
        // instead of deferring to vanilla's climate-blind findSpawnPosition() -- only
        // VANILLA_SPAWN still returns Optional.empty() to genuinely defer to vanilla.
        assertTrue(manager.contains("limitedSource.spawnStrategy() == SpawnStrategy.VANILLA_SPAWN"));
        assertTrue(manager.contains("private static BlockPos safeSpawnNear("));
        assertTrue(manager.contains("return Optional.of(safeSpawnNear(overworld, limitedSource, 0, 0));"));
        // 2026-07-18: the spawn offset clamps to the Overworld border's initial radius (see
        // WorldLimitPlan.DimensionLimit.safeSpawnOffsetBlocks) so a very small border (as low
        // as 1 block, since 0.2.13) can never place spawn beyond it.
        assertTrue(manager.contains("limitedSource.worldLimits().overworld().safeSpawnOffsetBlocks()"));
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
    void bothLoadersFixTheChunkMapDummyRandomStateFallback() throws IOException {
        String fabricMixinConfig = Files.readString(ROOT.resolve(
            "fabric/src/main/resources/jlt_worldz.mixins.json"
        ));
        String fabricMixin = Files.readString(ROOT.resolve(
            "fabric/src/main/java/media/jlt/minecraft/mods/worldz/mixin/ChunkMapMixin.java"
        ));
        String neoForgeToml = Files.readString(ROOT.resolve(
            "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
        ));
        String neoForgeMixinConfig = Files.readString(ROOT.resolve(
            "neoforge/src/main/resources/jlt_worldz.neoforge.mixins.json"
        ));
        String neoForgeMixin = Files.readString(ROOT.resolve(
            "neoforge/src/main/java/media/jlt/minecraft/mods/worldz/mixin/ChunkMapMixin.java"
        ));

        assertTrue(fabricMixinConfig.contains("\"ChunkMapMixin\""));
        assertTrue(neoForgeToml.contains("config = \"jlt_worldz.neoforge.mixins.json\""));
        assertTrue(neoForgeMixinConfig.contains("\"ChunkMapMixin\""));
        for (String mixin : List.of(fabricMixin, neoForgeMixin)) {
            assertTrue(mixin.contains("@Mixin(ChunkMap.class)"));
            assertTrue(mixin.contains("private RandomState randomState;"));
            assertTrue(mixin.contains("method = \"<init>\","));
            assertTrue(mixin.contains("if (generator instanceof EnvelopedChunkGenerator enveloped) {"));
            assertTrue(mixin.contains(
                "if (enveloped.delegate() instanceof NoiseBasedChunkGenerator noiseGenerator) {"
            ));
            assertTrue(mixin.contains("this.randomState = RandomState.create("));
            assertTrue(mixin.contains(
                "if (enveloped.getBiomeSource() instanceof LimitedBiomeSource source) {"
            ));
            assertTrue(mixin.contains("source.setLayoutSeed(level.getSeed());"));
            // 2026-07-17: @Inject at @At("INVOKE") fixed this.randomState for every later
            // read, but createState(...) itself still read the stale field value as its own
            // inline argument (evaluated before the injected callback runs) -- switched to
            // @Redirect so the passed-in value is chosen explicitly instead.
            assertTrue(mixin.contains("@Redirect("));
            assertTrue(mixin.contains("return generator.createState(structureSets, this.randomState, legacyLevelSeed);"));
        }
    }

    @Test
    void vanillaPassThroughSkipsTheLayoutRaiseSoRiversAndOceansStayCarved() throws IOException {
        String source = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/LimitedBiomeSource.java"
        ));
        String generator = Files.readString(ROOT.resolve(
            "common/src/main/java/media/jlt/minecraft/mods/worldz/worldgen/EnvelopedChunkGenerator.java"
        ));

        // 2026-07-17: getNoiseBiome correctly returned a passed-through river/ocean, but
        // EnvelopedChunkGenerator's layout terrain raise never consulted the pass-through --
        // single_biome's WorldLayoutPlan.sampleAt always reports landFactor=1.0, so every
        // column (including a real vanilla river's natural depression) was still raised to
        // guaranteed-land height, leaving a "River" biome label over flattened dry land with
        // no water. Confirmed in-game (config 14, world Worldz-14): F3 showed the correct
        // River biome, but the terrain rendered as plains -- trees growing, no water except
        // small cave pools.
        assertTrue(source.contains("public boolean isNaturalPassThroughAt(int blockX, int blockY, int blockZ, Climate.Sampler sampler) {"));
        assertTrue(source.contains("naturalPassThroughBiome(quartX, quartY, quartZ, sampler)"));
        assertTrue(generator.contains(
            "if (this.originSource.isPresent()\n"
                + "            && this.originSource.get().isNaturalPassThroughAt(x, naturalFloor, z, randomState.sampler())) {\n"
                + "            return naturalFloor;"
        ));
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
