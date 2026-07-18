package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ChaosBiomesCustomization;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
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
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Small preset editor for the {@code jlt_worldz:chaos_biomes} typed preset (DESIGN §20.11). */
public final class ChaosBiomesPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> CHAOS_BIOMES_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":chaos_biomes")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new ChaosBiomesPresetEditor();

    private ChaosBiomesPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new ChaosBiomesCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with explicit Overworld and Nether envelopes
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        ChaosBiomesCustomization customization
    ) {
        ChunkGenerator currentGenerator = unwrap(dimensions.overworld());
        if (!(currentGenerator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalArgumentException("Worldz customization requires the Worldz noise generator.");
        }

        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        HolderSet<Biome> allowed = resolveBiomes(customization.allowedBiomeIds(), biomes);
        Optional<Holder<Biome>> starter = customization.starterBiome().isEmpty()
            ? Optional.empty()
            : Optional.of(resolveBiome(customization.starterBiome(), biomes));

        var sharedConfig = WorldzCommon.config();
        WorldLayoutPlan layoutPlan = WorldLayoutPlan.resolve(
            LayoutMode.CHAOS, customization.biomes(), Map.of(),
            customization.regionScaleBlocks(), null, new Random().nextLong()
        );
        LimitedBiomeSource source = LimitedBiomeSource.customized(
            allowed,
            starter,
            customization.starterRadiusBlocks(),
            StarterLandPlan.fromConfig(sharedConfig),
            customization.worldLimitPlan(),
            customization.exteriorPlan(),
            layoutPlan,
            customization.spawnStrategy(),
            customization.allowRivers(),
            customization.allowOceans(),
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
                    EnvelopedChunkGenerator.customized(unwrap(nether.generator()), false, exterior.nether())
                )
            );
        }
        return new WorldDimensions(Map.copyOf(replaced));
    }

    private static ChaosBiomesCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator.getBiomeSource() instanceof LimitedBiomeSource source) || source.usesConfigDefaults()) {
            return ChaosBiomesCustomization.fromConfig(WorldzCommon.config());
        }

        List<String> biomes = source.worldLayoutPlan().landBiomes().stream()
            .map(weight -> weight.weight() == 1.0 ? weight.biomeId() : weight.biomeId() + "@" + weight.weight())
            .toList();
        if (biomes.isEmpty()) {
            biomes = WorldzCommon.config().chaosBiomes.biomes;
        }
        String starter = source.starterBiome().map(ChaosBiomesPresetEditor::registeredName).orElse("");
        WorldLimitPlan plan = source.worldLimits();
        var exterior = source.exteriorPlan();
        return new ChaosBiomesCustomization(
            biomes,
            source.worldLayoutPlan().regionScaleBlocks(),
            starter,
            source.starterRadiusBlocks(),
            source.spawnStrategy(),
            source.allowRivers(),
            source.allowOceans(),
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

    private static HolderSet<Biome> resolveBiomes(List<String> ids, Registry<Biome> biomes) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        for (String id : ids) {
            resolved.add(resolveBiome(id, biomes));
        }
        return HolderSet.direct(List.copyOf(resolved));
    }

    private static Holder<Biome> resolveBiome(String id, Registry<Biome> biomes) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
        return biomes.get(key).orElseThrow(() -> new IllegalArgumentException("Unknown biome: " + id));
    }

    private static String registeredName(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElseGet(holder::getRegisteredName);
    }
}
