package media.jlt.minecraft.mods.worldz.worldgen;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.AllowedEntryFilter;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.BiomeRole;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.StarterZone;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** A multi-noise biome source restricted to configured overworld biomes. */
public final class LimitedBiomeSource extends BiomeSource {
    /** Codec registered as {@code jlt_worldz:limited}. */
    public static final MapCodec<LimitedBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Biome.LIST_CODEC.optionalFieldOf("biomes").forGetter(source -> Optional.of(source.allowedBiomes())),
        Biome.CODEC.optionalFieldOf("starter_biome").forGetter(source -> source.starterBiome),
        Codec.INT.optionalFieldOf("starter_radius").forGetter(source -> Optional.of(source.starterRadiusBlocks)),
        StarterLandCodecs.PLAN_CODEC.optionalFieldOf("starter_land").forGetter(source -> Optional.of(source.starterLandPlan)),
        WorldLimitCodecs.PLAN_CODEC.optionalFieldOf("world_limits").forGetter(source -> Optional.of(source.worldLimits)),
        ExteriorCodecs.PLAN_CODEC.optionalFieldOf("exterior_plan").forGetter(source -> Optional.of(source.exteriorPlan)),
        LayoutCodecs.PLAN_CODEC.optionalFieldOf("world_layout").forGetter(source -> Optional.of(source.worldLayoutPlan)),
        Codec.STRING.optionalFieldOf("spawn_strategy")
            .forGetter(source -> Optional.of(source.spawnStrategy.serializedName())),
        RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, LimitedBiomeSource::resolve));

    private final Optional<Holder<Biome>> starterBiome;
    private final int starterRadiusBlocks;
    private final StarterLandPlan starterLandPlan;
    private final WorldLimitPlan worldLimits;
    private final ExteriorPlan exteriorPlan;
    private final WorldLayoutPlan worldLayoutPlan;
    private final SpawnStrategy spawnStrategy;
    private final Optional<Holder<Biome>> oceanBiome;
    private final boolean configDefaults;
    private final Supplier<Resolution> resolution;
    private volatile int originBlockX;
    private volatile int originBlockZ;

    private LimitedBiomeSource(
        Supplier<HolderSet<Biome>> allowedBiomes,
        Optional<Holder<Biome>> starterBiome,
        int starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        WorldLimitPlan worldLimits,
        ExteriorPlan exteriorPlan,
        WorldLayoutPlan worldLayoutPlan,
        SpawnStrategy spawnStrategy,
        boolean configDefaults,
        HolderGetter<Biome> biomeGetter
    ) {
        this.starterBiome = starterBiome;
        this.starterRadiusBlocks = starterRadiusBlocks;
        this.starterLandPlan = starterLandPlan;
        this.worldLimits = worldLimits;
        this.exteriorPlan = exteriorPlan;
        this.worldLayoutPlan = worldLayoutPlan;
        this.spawnStrategy = spawnStrategy;
        this.oceanBiome = exteriorPlan.overworld().mode() == ExteriorMode.OCEAN
            ? biomeGetter.get(Biomes.DEEP_OCEAN).map(value -> value)
            : Optional.empty();
        this.configDefaults = configDefaults;
        // World presets are decoded before dynamic-registry tags are bound in
        // 26.2. Defer tag expansion and climate filtering until Minecraft first
        // asks this biome source for its possible biomes or an actual biome.
        this.resolution = Suppliers.memoize(() -> resolveAllowedBiomes(
            allowedBiomes.get(), starterBiome, this.oceanBiome, worldLayoutPlan, biomeGetter
        ));
    }

    private static LimitedBiomeSource resolve(
        Optional<HolderSet<Biome>> encodedBiomes,
        Optional<Holder<Biome>> encodedStarterBiome,
        Optional<Integer> encodedStarterRadius,
        Optional<StarterLandPlan> encodedStarterLand,
        Optional<WorldLimitPlan> encodedWorldLimits,
        Optional<ExteriorPlan> encodedExteriorPlan,
        Optional<WorldLayoutPlan> encodedWorldLayout,
        Optional<String> encodedSpawnStrategy,
        HolderGetter<Biome> biomeGetter
    ) {
        WorldzConfig config = WorldzCommon.config();
        Supplier<HolderSet<Biome>> allowed = encodedBiomes
            .<Supplier<HolderSet<Biome>>>map(value -> () -> value)
            .orElseGet(() -> () -> resolveConfiguredBiomes(config, biomeGetter));

        // Every encoded instance has starter_radius. Its presence distinguishes a
        // persisted "no starter biome" from the fieldless preset that consults config.
        Optional<Holder<Biome>> starter = encodedStarterRadius.isPresent()
            ? encodedStarterBiome
            : encodedStarterBiome.or(() -> resolveConfiguredStarter(config, biomeGetter));
        int radius = encodedStarterRadius.orElse(config.starterRadiusBlocks);
        StarterLandPlan starterLand = encodedStarterRadius.isPresent()
            ? encodedStarterLand.orElseGet(StarterLandPlan::disabled)
            : encodedStarterLand.orElseGet(() -> StarterLandPlan.fromConfig(config));
        WorldLimitPlan limits = encodedStarterRadius.isPresent()
            ? encodedWorldLimits.orElseGet(WorldLimitPlan::disabled)
            : encodedWorldLimits.orElseGet(() -> WorldLimitPlan.fromConfig(config));
        ExteriorPlan exterior = encodedStarterRadius.isPresent()
            ? encodedExteriorPlan.orElseGet(ExteriorPlan::normal)
            : encodedExteriorPlan.orElseGet(() -> ExteriorPlan.fromConfig(config));
        // A fresh random sampling seed is picked once per newly created fieldless-preset
        // world and then persisted, giving distinct worlds distinct layouts even though
        // it is not yet tied to the player's chosen Minecraft world seed string: no
        // decode-time hook here exposes that seed (BiomeSource codecs decode from
        // RegistryOps, not a seed-aware context). Ideally would use the actual world
        // seed; verifying where to obtain it is deferred alongside Phase 16's related
        // finalized-seed-timing investigation.
        WorldLayoutPlan worldLayout = encodedStarterRadius.isPresent()
            ? encodedWorldLayout.orElseGet(WorldLayoutPlan::legacy)
            : encodedWorldLayout.orElseGet(() -> WorldLayoutPlan.fromConfig(config, new Random().nextLong()));
        SpawnStrategy spawnStrategy = encodedStarterRadius.isPresent()
            ? encodedSpawnStrategy.map(SpawnStrategy::parse).orElse(SpawnStrategy.STARTER_AT_ORIGIN)
            : encodedSpawnStrategy.map(SpawnStrategy::parse).orElseGet(() -> config.spawn.strategy);

        return new LimitedBiomeSource(
            allowed, starter, radius, starterLand, limits, exterior, worldLayout, spawnStrategy,
            encodedStarterRadius.isEmpty(), biomeGetter
        );
    }

    /**
     * Creates a source from values selected in the world-creation screen.
     *
     * @param allowedBiomes resolved direct allowed-biome holders
     * @param starterBiome optional resolved starter biome
     * @param starterRadiusBlocks starter-zone radius
     * @param starterLandPlan persisted terrain guarantee
     * @param worldLimits persisted border plan
     * @param exteriorPlan persisted exterior-terrain plan
     * @param worldLayoutPlan persisted coordinated-layout plan
     * @param spawnStrategy persisted layout-origin and spawn strategy
     * @param biomeGetter biome registry lookup used for vanilla climate parameters
     * @return a fully explicit source independent of later YAML changes
     */
    public static LimitedBiomeSource customized(
        HolderSet<Biome> allowedBiomes,
        Optional<Holder<Biome>> starterBiome,
        int starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        WorldLimitPlan worldLimits,
        ExteriorPlan exteriorPlan,
        WorldLayoutPlan worldLayoutPlan,
        SpawnStrategy spawnStrategy,
        HolderGetter<Biome> biomeGetter
    ) {
        return new LimitedBiomeSource(
            () -> allowedBiomes,
            starterBiome,
            starterRadiusBlocks,
            starterLandPlan,
            worldLimits,
            exteriorPlan,
            worldLayoutPlan,
            spawnStrategy,
            false,
            biomeGetter
        );
    }

    private static Resolution resolveAllowedBiomes(
        HolderSet<Biome> allowed,
        Optional<Holder<Biome>> starterBiome,
        Optional<Holder<Biome>> oceanBiome,
        WorldLayoutPlan worldLayoutPlan,
        HolderGetter<Biome> biomeGetter
    ) {
        Climate.ParameterList<Holder<Biome>> overworld = new MultiNoiseBiomeSourceParameterList(
            MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD,
            biomeGetter
        ).parameters();
        Set<Holder<Biome>> allowedSet = new LinkedHashSet<>(allowed.stream().toList());
        AllowedEntryFilter.Result<Pair<Climate.ParameterPoint, Holder<Biome>>, Holder<Biome>> filteredResult =
            AllowedEntryFilter.filter(overworld.values(), Pair::getSecond, allowedSet);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> filtered = filteredResult.entries();
        Set<Holder<Biome>> matched = filteredResult.matchedValues();

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
        MultiNoiseBiomeSource delegate = MultiNoiseBiomeSource.createFromList(delegateParameters);
        Set<Holder<Biome>> possible = new LinkedHashSet<>();
        if (usingFallback) {
            possible.addAll(delegate.possibleBiomes());
        } else {
            possible.addAll(matched);
        }
        starterBiome.ifPresent(possible::add);
        oceanBiome.ifPresent(possible::add);

        Map<String, Holder<Biome>> layoutBiomes = resolveLayoutBiomes(worldLayoutPlan, biomeGetter);
        if (worldLayoutPlan.mode() != LayoutMode.LEGACY) {
            possible.addAll(layoutBiomes.values());
        }
        return new Resolution(HolderSet.direct(List.copyOf(allowedSet)), delegate, Set.copyOf(possible), layoutBiomes);
    }

    private static Map<String, Holder<Biome>> resolveLayoutBiomes(WorldLayoutPlan plan, HolderGetter<Biome> biomeGetter) {
        if (plan.mode() == LayoutMode.LEGACY) {
            return Map.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        plan.landBiomes().forEach(weight -> ids.add(weight.biomeId()));
        plan.oceanBiomes().forEach(weight -> ids.add(weight.biomeId()));
        plan.beachBiomes().forEach(weight -> ids.add(weight.biomeId()));
        plan.singleBiome().ifPresent(ids::add);

        Map<String, Holder<Biome>> resolved = new LinkedHashMap<>();
        for (String id : ids) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
            Optional<Holder.Reference<Biome>> holder = biomeGetter.get(key);
            if (holder.isEmpty()) {
                WorldzCommon.LOGGER.warn("Unknown layout biome '{}'; it will never be selected.", id);
            } else {
                resolved.put(id, holder.get());
            }
        }
        if (resolved.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Layout mode '{}' has no resolved biomes; falling back to climate-filter biomes at every column.",
                plan.mode().serializedName()
            );
        }
        return resolved;
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
        return this.resolution.get().allowedBiomes();
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
     * Returns the terrain guarantee baked into this world.
     *
     * @return immutable starter-land plan
     */
    public StarterLandPlan starterLandPlan() {
        return this.starterLandPlan;
    }

    /**
     * Returns the border plan baked into this world at creation.
     *
     * @return immutable persisted limit settings
     */
    public WorldLimitPlan worldLimits() {
        return this.worldLimits;
    }

    /**
     * Returns the exterior terrain plan baked into this source.
     *
     * @return resolved dimension envelopes
     */
    public ExteriorPlan exteriorPlan() {
        return this.exteriorPlan;
    }

    /**
     * Returns the coordinated-layout plan baked into this source.
     *
     * @return immutable persisted layout plan
     */
    public WorldLayoutPlan worldLayoutPlan() {
        return this.worldLayoutPlan;
    }

    /**
     * Returns the layout-origin and spawn strategy baked into this world.
     *
     * @return persisted spawn strategy
     */
    public SpawnStrategy spawnStrategy() {
        return this.spawnStrategy;
    }

    /**
     * Returns the current layout origin's X coordinate. Always {@code 0} unless
     * {@link #setOrigin(int, int)} has been called (see {@code SpawnOriginManager}).
     *
     * @return origin block X
     */
    public int originBlockX() {
        return this.originBlockX;
    }

    /**
     * Returns the current layout origin's Z coordinate. Always {@code 0} unless
     * {@link #setOrigin(int, int)} has been called (see {@code SpawnOriginManager}).
     *
     * @return origin block Z
     */
    public int originBlockZ() {
        return this.originBlockZ;
    }

    /**
     * Applies a resolved layout origin at runtime. Not part of the codec: the
     * origin is computed after world creation (it needs the real seed and a
     * live level) and is re-applied from persisted {@code SavedData} on every
     * load instead, mirroring how {@code WorldLimitState} re-applies border
     * schedules. Every other Worldz system reads this same instance's origin
     * so border, exterior, layout sampling, and progression placement agree.
     *
     * @param blockX resolved origin X
     * @param blockZ resolved origin Z
     */
    public void setOrigin(int blockX, int blockZ) {
        this.originBlockX = blockX;
        this.originBlockZ = blockZ;
    }

    /**
     * Returns whether this fieldless preset instance still represents YAML defaults.
     *
     * @return true before the player applies explicit Customize values
     */
    public boolean usesConfigDefaults() {
        return this.configDefaults;
    }

    /**
     * Tests a quart-coordinate position against this source's starter zone.
     *
     * @param quartX quart X coordinate
     * @param quartZ quart Z coordinate
     * @return whether the source has a starter biome and the position is in its zone
     */
    public boolean isInStarterZone(int quartX, int quartZ) {
        return this.starterBiome.isPresent() && StarterZone.containsQuart(
            quartX - originQuartX(), quartZ - originQuartZ(), this.starterRadiusBlocks
        );
    }

    /**
     * Tests a quart-coordinate position against the starter-land transition ring
     * just outside the starter zone, where a beach-role layout biome is preferred.
     *
     * @param quartX quart X coordinate
     * @param quartZ quart Z coordinate
     * @return whether the source has a starter biome and the position is in its transition ring
     */
    public boolean isInStarterTransitionRing(int quartX, int quartZ) {
        return this.starterBiome.isPresent() && StarterZone.inRingQuart(
            quartX - originQuartX(), quartZ - originQuartZ(),
            this.starterRadiusBlocks, this.starterRadiusBlocks + this.starterLandPlan.transitionWidthBlocks()
        );
    }

    private int originQuartX() {
        return QuartPos.fromBlock(this.originBlockX);
    }

    private int originQuartZ() {
        return QuartPos.fromBlock(this.originBlockZ);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.resolution.get().possibleBiomes().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        int originX = this.originBlockX;
        int originZ = this.originBlockZ;
        if (this.exteriorPlan.overworld().modeAt(blockX - originX, blockZ - originZ) == ExteriorMode.OCEAN) {
            return this.oceanBiome.orElseThrow(() -> new IllegalStateException("Deep ocean biome is unavailable."));
        }
        if (isInStarterZone(quartX, quartZ)) {
            return this.starterBiome.orElseThrow();
        }
        if (this.worldLayoutPlan.mode() != LayoutMode.LEGACY) {
            if (isInStarterTransitionRing(quartX, quartZ)) {
                Optional<Holder<Biome>> beach = this.worldLayoutPlan.sampleRole(BiomeRole.BEACH, blockX - originX, blockZ - originZ)
                    .map(this.resolution.get().layoutBiomes()::get);
                if (beach.isPresent()) {
                    return beach.get();
                }
            }
            Optional<Holder<Biome>> sampled = this.worldLayoutPlan.sampleAt(blockX - originX, blockZ - originZ).biomeId()
                .map(this.resolution.get().layoutBiomes()::get);
            if (sampled.isPresent()) {
                return sampled.get();
            }
        }
        return this.resolution.get().delegate().getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    private record Resolution(
        HolderSet<Biome> allowedBiomes,
        MultiNoiseBiomeSource delegate,
        Set<Holder<Biome>> possibleBiomes,
        Map<String, Holder<Biome>> layoutBiomes
    ) {
    }
}
