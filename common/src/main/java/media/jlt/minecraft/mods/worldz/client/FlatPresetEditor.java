package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.FlatCustomization;
import media.jlt.minecraft.mods.worldz.logic.FlatPlan;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Small preset editor for the {@code jlt_worldz:flat} typed preset (GOAL 15). */
public final class FlatPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> FLAT_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":flat")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new FlatPresetEditor();

    private FlatPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new FlatCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with an explicit Overworld flat plan and ordinary Nether/End
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        FlatCustomization customization
    ) {
        ChunkGenerator currentGenerator = unwrap(dimensions.overworld());
        if (!(currentGenerator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalArgumentException("Worldz customization requires the Worldz noise generator.");
        }

        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        Holder<Biome> flatBiome = resolveBiome(customization.biome(), biomes);
        HolderSet<Biome> allowed = HolderSet.direct(flatBiome);
        var sharedConfig = WorldzCommon.config();
        // The flat surface itself never reads a real climate sample (DESIGN §33.1's correction),
        // but LimitedBiomeSource still needs to report exactly one biome everywhere -- the same
        // LayoutMode.SINGLE_BIOME mechanism single_biome already uses for its own "one biome
        // fills the world" requirement, reused verbatim rather than inventing a second one.
        WorldLayoutPlan layoutPlan = WorldLayoutPlan.resolve(
            LayoutMode.SINGLE_BIOME, List.of(), Map.of(),
            WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS, customization.biome(), new Random().nextLong()
        );
        LimitedBiomeSource source = LimitedBiomeSource.customized(
            allowed,
            java.util.Optional.empty(),
            sharedConfig.starterRadiusBlocks,
            StarterLandPlan.disabled(),
            customization.worldLimitPlan(),
            customization.exteriorPlan(),
            layoutPlan,
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
                    EndStartPlan.disabled(), customization.flatPlan()
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

    private static FlatCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator instanceof EnvelopedChunkGenerator enveloped) || !enveloped.flat().enabled()) {
            return FlatCustomization.fromConfig(WorldzCommon.config());
        }

        FlatPlan flat = enveloped.flat();
        LimitedBiomeSource source = (LimitedBiomeSource) enveloped.delegate().getBiomeSource();
        WorldLimitPlan plan = source.worldLimits();
        var exterior = source.exteriorPlan();
        return new FlatCustomization(
            flat.layers(), flat.biome(), flat.decoration(), flat.structureOverrides(),
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

    private static Holder<Biome> resolveBiome(String id, Registry<Biome> biomes) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
        return biomes.get(key).orElseThrow(() -> new IllegalArgumentException("Unknown biome: " + id));
    }
}
