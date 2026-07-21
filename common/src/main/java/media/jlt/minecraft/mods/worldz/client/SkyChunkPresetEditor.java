package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyChunkCustomization;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Small preset editor for the {@code jlt_worldz:sky_chunk} typed preset (GOALS 09/37, DESIGN §29). */
public final class SkyChunkPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> SKY_CHUNK_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":sky_chunk")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new SkyChunkPresetEditor();
    /** Ordinary vanilla biome variety -- a chunk island is a shape, not a biome restriction. */
    private static final TagKey<Biome> OVERWORLD_BIOMES = TagKey.create(
        Registries.BIOME, Identifier.withDefaultNamespace("is_overworld")
    );

    private SkyChunkPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new SkyChunkCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions. Unlike every other typed
     * preset, this one also wraps {@code LevelStem.END} with {@link EnvelopedChunkGenerator}
     * (DESIGN §29.5's finding: no preset has ever needed to before).
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with an explicit Overworld chunk island and Nether/End wrapped for
     *     their own chunk-island toggles
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        SkyChunkCustomization customization
    ) {
        ChunkGenerator currentGenerator = unwrap(dimensions.overworld());
        if (!(currentGenerator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalArgumentException("Worldz customization requires the Worldz noise generator.");
        }

        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        HolderSet<Biome> allowed = biomes.get(OVERWORLD_BIOMES)
            .orElseThrow(() -> new IllegalStateException("Missing #minecraft:is_overworld biome tag."));
        var sharedConfig = WorldzCommon.config();
        LimitedBiomeSource source = LimitedBiomeSource.customized(
            allowed,
            Optional.empty(),
            sharedConfig.starterRadiusBlocks,
            StarterLandPlan.disabled(),
            customization.worldLimitPlan(),
            customization.exteriorPlan(),
            WorldLayoutPlan.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN,
            false,
            false,
            false,
            IslandPlan.disabled(),
            SkyIslandPlan.disabled(),
            customization.chunkIslandPlan(),
            biomes
        );
        NoiseBasedChunkGenerator customizedGenerator = new NoiseBasedChunkGenerator(source, noiseGenerator.generatorSettings());
        var exterior = customization.exteriorPlan();
        var replaced = new LinkedHashMap<>(dimensions.dimensions());
        LevelStem overworld = replaced.get(LevelStem.OVERWORLD);
        replaced.put(
            LevelStem.OVERWORLD,
            new LevelStem(overworld.type(), EnvelopedChunkGenerator.customized(customizedGenerator, true, exterior.overworld()))
        );
        LevelStem nether = replaced.get(LevelStem.NETHER);
        if (nether != null) {
            replaced.put(
                LevelStem.NETHER,
                new LevelStem(
                    nether.type(),
                    EnvelopedChunkGenerator.customized(
                        unwrap(nether.generator()), EnvelopedChunkGenerator.Dimension.NETHER, exterior.nether(),
                        StripPlan.disabled(), SkyIslandPlan.disabled(), customization.netherChunkIslandPlan()
                    )
                )
            );
        }
        LevelStem end = replaced.get(LevelStem.END);
        if (end != null) {
            replaced.put(
                LevelStem.END,
                new LevelStem(
                    end.type(),
                    EnvelopedChunkGenerator.customized(
                        unwrap(end.generator()), EnvelopedChunkGenerator.Dimension.END,
                        ExteriorPlan.DimensionEnvelope.normal(),
                        StripPlan.disabled(), SkyIslandPlan.disabled(), customization.endChunkIslandPlan()
                    )
                )
            );
        }
        return new WorldDimensions(Map.copyOf(replaced));
    }

    private static SkyChunkCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator.getBiomeSource() instanceof LimitedBiomeSource source) || source.usesConfigDefaults()) {
            return SkyChunkCustomization.fromConfig(WorldzCommon.config());
        }

        ChunkIslandPlan chunkIsland = source.chunkIsland();
        WorldLimitPlan plan = source.worldLimits();
        var exterior = source.exteriorPlan();
        boolean applyToNether = settings.selectedDimensions().get(LevelStem.NETHER)
            .map(LevelStem::generator)
            .filter(netherGenerator -> netherGenerator instanceof EnvelopedChunkGenerator enveloped
                && enveloped.chunkIsland().enabled())
            .isPresent();
        boolean applyToEnd = settings.selectedDimensions().get(LevelStem.END)
            .map(LevelStem::generator)
            .filter(endGenerator -> endGenerator instanceof EnvelopedChunkGenerator enveloped
                && enveloped.chunkIsland().enabled())
            .isPresent();
        return new SkyChunkCustomization(
            chunkIsland.spawnChance(),
            chunkIsland.cellSizeChunks(),
            chunkIsland.topOnly(),
            chunkIsland.topOnlyDepthBlocks(),
            chunkIsland.exclusionZone().enabled(),
            chunkIsland.exclusionZone().radiusBlocks(),
            chunkIsland.scatteredTopOnlyChance(),
            applyToNether,
            applyToEnd,
            fromPlan(plan.overworld()), fromPlan(plan.nether()), fromPlan(plan.end()),
            fromPlan(exterior.nether())
        );
    }

    private static WorldzCustomization.BorderSettings fromPlan(WorldLimitPlan.DimensionLimit limit) {
        return new WorldzCustomization.BorderSettings(
            limit.enabled(),
            limit.initialRadiusBlocks(),
            limit.finalRadiusBlocks(),
            limit.resizeDays(),
            limit.resizeDelayDays(),
            limit.resizeRateBlocks(),
            limit.resizeRateDays(),
            limit.ensureObjective()
        );
    }

    private static WorldzCustomization.EndBorderSettings fromPlan(WorldLimitPlan.EndLimit limit) {
        return new WorldzCustomization.EndBorderSettings(limit.carryFromOverworld(), limit.minimumRadiusBlocks());
    }

    private static WorldzCustomization.ExteriorSettings fromPlan(ExteriorPlan.DimensionEnvelope envelope) {
        return new WorldzCustomization.ExteriorSettings(
            envelope.mode(), envelope.boundaryRadiusBlocks(), envelope.oceanTransitionWidthBlocks()
        );
    }

    private static ChunkGenerator unwrap(ChunkGenerator generator) {
        return generator instanceof EnvelopedChunkGenerator enveloped ? enveloped.delegate() : generator;
    }
}
