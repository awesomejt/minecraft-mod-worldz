package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatCustomization;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.FlatPlan;
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

/** Small preset editor for the {@code jlt_worldz:deep_flat} typed preset (GOAL 16). */
public final class DeepFlatPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> DEEP_FLAT_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":deep_flat")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new DeepFlatPresetEditor();
    /** Full vanilla biome variety -- deep_flat caps real terrain, it does not restrict biomes. */
    private static final TagKey<Biome> OVERWORLD_BIOMES = TagKey.create(
        Registries.BIOME, Identifier.withDefaultNamespace("is_overworld")
    );

    private DeepFlatPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new DeepFlatCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with an explicit Overworld deep-flat plan and ordinary Nether/End
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        DeepFlatCustomization customization
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
            biomes
        );
        NoiseBasedChunkGenerator customizedGenerator = new NoiseBasedChunkGenerator(source, noiseGenerator.generatorSettings());
        var exterior = customization.exteriorPlan();
        var replaced = new LinkedHashMap<>(dimensions.dimensions());
        LevelStem overworld = replaced.get(LevelStem.OVERWORLD);
        replaced.put(
            LevelStem.OVERWORLD,
            new LevelStem(
                overworld.type(),
                EnvelopedChunkGenerator.customized(
                    customizedGenerator, EnvelopedChunkGenerator.Dimension.OVERWORLD, exterior.overworld(), StripPlan.disabled(),
                    SkyIslandPlan.disabled(), ChunkIslandPlan.disabled(), CavePlan.disabled(), NetherStartPlan.disabled(),
                    EndStartPlan.disabled(), FlatPlan.disabled(), customization.deepFlatPlan()
                )
            )
        );
        LevelStem nether = replaced.get(LevelStem.NETHER);
        if (nether != null) {
            replaced.put(
                LevelStem.NETHER,
                new LevelStem(
                    nether.type(),
                    EnvelopedChunkGenerator.customized(unwrap(nether.generator()), false, exterior.nether())
                )
            );
        }
        return new WorldDimensions(Map.copyOf(replaced));
    }

    private static DeepFlatCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator instanceof EnvelopedChunkGenerator enveloped) || !enveloped.deepFlat().enabled()) {
            return DeepFlatCustomization.fromConfig(WorldzCommon.config());
        }

        DeepFlatPlan deepFlat = enveloped.deepFlat();
        LimitedBiomeSource source = (LimitedBiomeSource) enveloped.delegate().getBiomeSource();
        WorldLimitPlan plan = source.worldLimits();
        var exterior = source.exteriorPlan();
        return new DeepFlatCustomization(
            deepFlat.surfaceY(), deepFlat.capLayers(), deepFlat.riversEnabled(), deepFlat.riverExclusionRadiusBlocks(),
            fromPlan(plan.overworld()), fromPlan(plan.nether()), fromPlan(plan.end()),
            fromPlan(exterior.overworld()), fromPlan(exterior.nether())
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
