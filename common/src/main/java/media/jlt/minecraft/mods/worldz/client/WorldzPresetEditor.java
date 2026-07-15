package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Shared Worldz preset editor registered by each loader on the client. */
public final class WorldzPresetEditor implements PresetEditor {
    /** World-preset key used by loader-specific registration. */
    public static final ResourceKey<WorldPreset> WORLDZ_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        Identifier.parse(WorldzCommon.MOD_ID + ":worldz")
    );
    /** Stateless editor instance shared by both loaders. */
    public static final PresetEditor INSTANCE = new WorldzPresetEditor();

    private WorldzPresetEditor() {
    }

    @Override
    public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext settings) {
        return new WorldzCustomizeScreen(parent, currentCustomization(settings));
    }

    /**
     * Applies explicit choices to the current Worldz dimensions.
     *
     * @param registries loaded world-generation registries
     * @param dimensions currently selected dimensions
     * @param customization validated player choices
     * @return dimensions with only the overworld generator replaced
     */
    public static WorldDimensions apply(
        RegistryAccess.Frozen registries,
        WorldDimensions dimensions,
        WorldzCustomization customization
    ) {
        ChunkGenerator currentGenerator = dimensions.overworld();
        if (!(currentGenerator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalArgumentException("Worldz customization requires the Worldz noise generator.");
        }

        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        HolderSet<Biome> allowed = resolveAllowedBiomes(customization.allowedBiomes(), biomes);
        Optional<Holder<Biome>> starter = resolveStarterBiome(customization.starterBiome(), biomes);
        LimitedBiomeSource source = LimitedBiomeSource.customized(
            allowed,
            starter,
            customization.starterRadiusBlocks(),
            customization.worldLimitPlan(),
            biomes
        );
        NoiseBasedChunkGenerator customizedGenerator = new NoiseBasedChunkGenerator(source, noiseGenerator.generatorSettings());
        return dimensions.replaceOverworldGenerator(registries, customizedGenerator);
    }

    private static WorldzCustomization currentCustomization(WorldCreationContext settings) {
        ChunkGenerator generator = settings.selectedDimensions().overworld();
        if (!(generator.getBiomeSource() instanceof LimitedBiomeSource source) || source.usesConfigDefaults()) {
            return WorldzCustomization.fromConfig(WorldzCommon.config());
        }

        List<String> allowed = source.allowedBiomes().stream().map(WorldzPresetEditor::registeredName).toList();
        String starter = source.starterBiome().map(WorldzPresetEditor::registeredName).orElse("");
        WorldLimitPlan plan = source.worldLimits();
        return new WorldzCustomization(
            allowed,
            starter,
            source.starterRadiusBlocks(),
            fromPlan(plan.overworld()),
            fromPlan(plan.nether())
        );
    }

    private static HolderSet<Biome> resolveAllowedBiomes(List<String> values, Registry<Biome> biomes) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        for (BiomeListSpec.Entry entry : BiomeListSpec.parse(values).entries()) {
            Identifier id = Identifier.parse(entry.id());
            if (entry.tag()) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
                HolderSet.Named<Biome> holders = biomes.get(tag)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown biome tag: #" + id));
                holders.stream().forEach(resolved::add);
            } else {
                ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
                Holder.Reference<Biome> holder = biomes.get(key)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown biome: " + id));
                resolved.add(holder);
            }
        }
        return HolderSet.direct(List.copyOf(resolved));
    }

    private static Optional<Holder<Biome>> resolveStarterBiome(String value, Registry<Biome> biomes) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        Identifier id = Identifier.parse(value);
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return Optional.of(biomes.get(key).orElseThrow(() -> new IllegalArgumentException("Unknown starter biome: " + id)));
    }

    private static String registeredName(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElseGet(holder::getRegisteredName);
    }

    private static WorldzCustomization.BorderSettings fromPlan(WorldLimitPlan.DimensionLimit limit) {
        return new WorldzCustomization.BorderSettings(
            limit.enabled(),
            limit.initialRadiusBlocks(),
            limit.finalRadiusBlocks(),
            limit.resizeDays(),
            limit.ensureObjective()
        );
    }
}
