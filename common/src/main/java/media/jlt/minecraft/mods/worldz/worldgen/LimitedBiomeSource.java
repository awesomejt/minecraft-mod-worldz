package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.StarterZone;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** A multi-noise biome source restricted to configured overworld biomes. */
public final class LimitedBiomeSource extends BiomeSource {
    /** Codec registered as {@code jlt_worldz:limited}. */
    public static final MapCodec<LimitedBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Biome.LIST_CODEC.optionalFieldOf("biomes").forGetter(source -> Optional.of(source.allowedBiomes)),
        Biome.CODEC.optionalFieldOf("starter_biome").forGetter(source -> source.starterBiome),
        Codec.INT.optionalFieldOf("starter_radius").forGetter(source -> Optional.of(source.starterRadiusBlocks)),
        RegistryOps.retrieveGetter(Registries.BIOME),
        RegistryOps.retrieveGetter(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
    ).apply(instance, LimitedBiomeSource::resolve));

    private final HolderSet<Biome> allowedBiomes;
    private final Optional<Holder<Biome>> starterBiome;
    private final int starterRadiusBlocks;
    private final MultiNoiseBiomeSource delegate;
    private final Set<Holder<Biome>> possibleBiomes;

    private LimitedBiomeSource(
        HolderSet<Biome> allowedBiomes,
        Optional<Holder<Biome>> starterBiome,
        int starterRadiusBlocks,
        Climate.ParameterList<Holder<Biome>> delegateParameters,
        boolean usingFallback
    ) {
        this.allowedBiomes = allowedBiomes;
        this.starterBiome = starterBiome;
        this.starterRadiusBlocks = starterRadiusBlocks;
        this.delegate = MultiNoiseBiomeSource.createFromList(delegateParameters);

        Set<Holder<Biome>> possible = new LinkedHashSet<>();
        if (usingFallback) {
            possible.addAll(this.delegate.possibleBiomes());
        } else {
            allowedBiomes.stream().forEach(possible::add);
        }
        starterBiome.ifPresent(possible::add);
        this.possibleBiomes = Set.copyOf(possible);
    }

    private static LimitedBiomeSource resolve(
        Optional<HolderSet<Biome>> encodedBiomes,
        Optional<Holder<Biome>> encodedStarterBiome,
        Optional<Integer> encodedStarterRadius,
        HolderGetter<Biome> biomeGetter,
        HolderGetter<MultiNoiseBiomeSourceParameterList> parameterListGetter
    ) {
        WorldzConfig config = WorldzCommon.config();
        HolderSet<Biome> allowed = encodedBiomes.orElseGet(() -> resolveConfiguredBiomes(config, biomeGetter));

        // Every encoded instance has starter_radius. Its presence distinguishes a
        // persisted "no starter biome" from the fieldless preset that consults config.
        Optional<Holder<Biome>> starter = encodedStarterRadius.isPresent()
            ? encodedStarterBiome
            : encodedStarterBiome.or(() -> resolveConfiguredStarter(config, biomeGetter));
        int radius = encodedStarterRadius.orElse(config.starterRadiusBlocks);

        Climate.ParameterList<Holder<Biome>> overworld = parameterListGetter
            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
            .value()
            .parameters();
        Set<Holder<Biome>> allowedSet = new LinkedHashSet<>(allowed.stream().toList());
        Set<Holder<Biome>> matched = new LinkedHashSet<>();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> filtered = overworld.values().stream()
            .filter(pair -> {
                boolean keep = allowedSet.contains(pair.getSecond());
                if (keep) {
                    matched.add(pair.getSecond());
                }
                return keep;
            })
            .toList();

        allowedSet.stream()
            .filter(holder -> !matched.contains(holder))
            .forEach(holder -> WorldzCommon.LOGGER.warn(
                "Allowed biome '{}' has no overworld climate entry and will be ignored.",
                holder.getRegisteredName()
            ));

        boolean usingFallback = filtered.isEmpty();
        Climate.ParameterList<Holder<Biome>> delegateParameters;
        if (usingFallback) {
            WorldzCommon.LOGGER.warn(
                "No configured biome matched an overworld climate entry; using the full vanilla overworld biome list as a fail-safe."
            );
            delegateParameters = overworld;
        } else {
            delegateParameters = new Climate.ParameterList<>(filtered);
        }
        return new LimitedBiomeSource(allowed, starter, radius, delegateParameters, usingFallback);
    }

    private static HolderSet<Biome> resolveConfiguredBiomes(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        for (BiomeListSpec.Entry entry : BiomeListSpec.parse(config.allowedBiomes).entries()) {
            Identifier id = Identifier.parse(entry.id());
            if (entry.tag()) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
                Optional<HolderSet.Named<Biome>> holders = biomeGetter.get(tag);
                if (holders.isEmpty()) {
                    WorldzCommon.LOGGER.warn("Unknown configured biome tag '#{}'.", id);
                } else {
                    holders.get().stream().forEach(resolved::add);
                }
            } else {
                ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
                Optional<Holder.Reference<Biome>> holder = biomeGetter.get(key);
                if (holder.isEmpty()) {
                    WorldzCommon.LOGGER.warn("Unknown configured biome '{}'.", id);
                } else {
                    resolved.add(holder.get());
                }
            }
        }
        return HolderSet.direct(List.copyOf(resolved));
    }

    private static Optional<Holder<Biome>> resolveConfiguredStarter(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        if (config.starterBiome.isEmpty()) {
            return Optional.empty();
        }
        Identifier id = Identifier.parse(config.starterBiome);
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        Optional<Holder.Reference<Biome>> holder = biomeGetter.get(key);
        if (holder.isEmpty()) {
            WorldzCommon.LOGGER.warn("Unknown configured starter biome '{}'; starter zone disabled.", id);
        }
        return holder.map(value -> value);
    }

    /**
     * Returns the resolved allowed holder set serialized into the world.
     *
     * @return resolved allowed biomes
     */
    public HolderSet<Biome> allowedBiomes() {
        return this.allowedBiomes;
    }

    /**
     * Returns the resolved optional starter biome serialized into the world.
     *
     * @return resolved starter biome
     */
    public Optional<Holder<Biome>> starterBiome() {
        return this.starterBiome;
    }

    /**
     * Returns the resolved starter radius serialized into the world.
     *
     * @return radius in blocks
     */
    public int starterRadiusBlocks() {
        return this.starterRadiusBlocks;
    }

    /**
     * Tests a quart-coordinate position against this source's starter zone.
     *
     * @param quartX quart X coordinate
     * @param quartZ quart Z coordinate
     * @return whether the source has a starter biome and the position is in its zone
     */
    public boolean isInStarterZone(int quartX, int quartZ) {
        return this.starterBiome.isPresent() && StarterZone.containsQuart(quartX, quartZ, this.starterRadiusBlocks);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.possibleBiomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        if (isInStarterZone(quartX, quartZ)) {
            return this.starterBiome.orElseThrow();
        }
        return this.delegate.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }
}
