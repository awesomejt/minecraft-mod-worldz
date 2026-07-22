package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.EndStartCustomization;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
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

/** Small preset editor for the {@code jlt_worldz:end_start} typed preset (GOALS 34). */
public final class EndStartPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> END_START_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":end_start")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new EndStartPresetEditor();
    /** Ordinary vanilla biome variety -- End-start is a spawn-placement mechanism, not a biome restriction. */
    private static final TagKey<Biome> OVERWORLD_BIOMES = TagKey.create(
        Registries.BIOME, Identifier.withDefaultNamespace("is_overworld")
    );

    private EndStartPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new EndStartCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions. Wraps {@code LevelStem.END} with
     * {@link EnvelopedChunkGenerator} the same way {@code sky_chunk} first established (DESIGN
     * §29.5) -- the second preset to do so.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with an ordinary Overworld/Nether and an explicit End-start plan
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        EndStartCustomization customization
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
            ExteriorPlan.normal(),
            WorldLayoutPlan.legacy(),
            SpawnStrategy.STARTER_AT_ORIGIN,
            false,
            false,
            false,
            biomes
        );
        NoiseBasedChunkGenerator customizedGenerator = new NoiseBasedChunkGenerator(source, noiseGenerator.generatorSettings());
        var replaced = new LinkedHashMap<>(dimensions.dimensions());
        LevelStem overworld = replaced.get(LevelStem.OVERWORLD);
        replaced.put(
            LevelStem.OVERWORLD,
            new LevelStem(
                overworld.type(),
                EnvelopedChunkGenerator.customized(
                    customizedGenerator, EnvelopedChunkGenerator.Dimension.OVERWORLD, ExteriorPlan.DimensionEnvelope.normal(),
                    StripPlan.disabled(), SkyIslandPlan.disabled(), ChunkIslandPlan.disabled(), CavePlan.disabled(),
                    NetherStartPlan.disabled(), EndStartPlan.disabled()
                )
            )
        );
        LevelStem nether = replaced.get(LevelStem.NETHER);
        if (nether != null) {
            replaced.put(
                LevelStem.NETHER,
                new LevelStem(
                    nether.type(),
                    EnvelopedChunkGenerator.customized(
                        unwrap(nether.generator()), EnvelopedChunkGenerator.Dimension.NETHER, ExteriorPlan.DimensionEnvelope.normal(),
                        StripPlan.disabled(), SkyIslandPlan.disabled(), ChunkIslandPlan.disabled(), CavePlan.disabled(),
                        NetherStartPlan.disabled(), EndStartPlan.disabled()
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
                        unwrap(end.generator()), EnvelopedChunkGenerator.Dimension.END, ExteriorPlan.DimensionEnvelope.normal(),
                        StripPlan.disabled(), SkyIslandPlan.disabled(), ChunkIslandPlan.disabled(), CavePlan.disabled(),
                        NetherStartPlan.disabled(), customization.endStartPlan()
                    )
                )
            );
        }
        return new WorldDimensions(Map.copyOf(replaced));
    }

    private static EndStartCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator endGenerator = settings.selectedDimensions().get(LevelStem.END)
            .map(LevelStem::generator)
            .orElse(null);
        if (!(endGenerator instanceof EnvelopedChunkGenerator enveloped) || !enveloped.endStart().enabled()) {
            return EndStartCustomization.fromConfig(WorldzCommon.config());
        }

        EndStartPlan endStart = enveloped.endStart();
        ChunkGenerator overworldGenerator = settings.selectedDimensions().overworld();
        LimitedBiomeSource source = overworldGenerator instanceof EnvelopedChunkGenerator overworldEnveloped
            && overworldEnveloped.delegate().getBiomeSource() instanceof LimitedBiomeSource limitedSource
            ? limitedSource
            : null;
        if (source == null) {
            return EndStartCustomization.fromConfig(WorldzCommon.config());
        }
        WorldLimitPlan plan = source.worldLimits();
        return new EndStartCustomization(
            endStart.chestTier(),
            fromPlan(plan.overworld()), fromPlan(plan.nether()), fromPlan(plan.end())
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

    private static ChunkGenerator unwrap(ChunkGenerator generator) {
        return generator instanceof EnvelopedChunkGenerator enveloped ? enveloped.delegate() : generator;
    }
}
