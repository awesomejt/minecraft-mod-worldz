package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.StripWorldCustomization;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Small preset editor for the {@code jlt_worldz:strip_world} typed preset (GOALS 32). */
public final class StripWorldPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> STRIP_WORLD_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":strip_world")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new StripWorldPresetEditor();
    /** Ordinary vanilla biome variety -- a strip world is a shape, not a biome restriction. */
    private static final TagKey<Biome> OVERWORLD_BIOMES = TagKey.create(
        Registries.BIOME, Identifier.withDefaultNamespace("is_overworld")
    );

    private StripWorldPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new StripWorldCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with explicit Overworld and Nether corridors
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        StripWorldCustomization customization
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
            StarterLandPlan.fromConfig(sharedConfig),
            customization.worldLimitPlan(),
            customization.exteriorPlan(),
            customization.layoutPlan(new Random().nextLong()),
            customization.spawnStrategy(),
            customization.bandAllowRivers(),
            customization.bandAllowOceans(),
            customization.bandAllowBeaches(),
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
                EnvelopedChunkGenerator.customized(customizedGenerator, true, exterior.overworld(), customization.stripPlan(true))
            )
        );
        LevelStem nether = replaced.get(LevelStem.NETHER);
        if (nether != null) {
            replaced.put(
                LevelStem.NETHER,
                new LevelStem(
                    nether.type(),
                    EnvelopedChunkGenerator.customized(
                        unwrap(nether.generator()), false, exterior.nether(), customization.stripPlan(false)
                    )
                )
            );
        }
        return new WorldDimensions(Map.copyOf(replaced));
    }

    private static StripWorldCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator instanceof EnvelopedChunkGenerator enveloped)
            || !(generator.getBiomeSource() instanceof LimitedBiomeSource source)
            || source.usesConfigDefaults()) {
            return StripWorldCustomization.fromConfig(WorldzCommon.config());
        }

        StripPlan strip = enveloped.strip();
        WorldLimitPlan plan = source.worldLimits();
        var exterior = source.exteriorPlan();
        WorldLayoutPlan layout = source.worldLayoutPlan();
        boolean bandsEnabled = layout.mode() == LayoutMode.STRIP_BANDS;
        return new StripWorldCustomization(
            strip.widthRadiusBlocks(),
            strip.widthMode(),
            strip.enabled(),
            bandsEnabled,
            bandsEnabled ? layout.bandBiomes() : List.of(),
            bandsEnabled ? layout.regionScaleBlocks() : WorldzCommon.config().stripWorld.bands.widthBlocks,
            false,
            source.allowRivers(),
            source.allowOceans(),
            source.allowBeaches(),
            source.spawnStrategy(),
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
