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
import media.jlt.minecraft.mods.worldz.logic.WeightedBiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
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
        Codec.BOOL.optionalFieldOf("allow_rivers").forGetter(source -> Optional.of(source.allowRivers)),
        Codec.BOOL.optionalFieldOf("allow_oceans").forGetter(source -> Optional.of(source.allowOceans)),
        Codec.STRING.optionalFieldOf("world_type").forGetter(source -> Optional.<String>empty()),
        RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, LimitedBiomeSource::resolve));

    private final Optional<Holder<Biome>> starterBiome;
    private final int starterRadiusBlocks;
    private final StarterLandPlan starterLandPlan;
    private final WorldLimitPlan worldLimits;
    private final ExteriorPlan exteriorPlan;
    private final WorldLayoutPlan worldLayoutPlan;
    private final SpawnStrategy spawnStrategy;
    private final boolean allowRivers;
    private final boolean allowOceans;
    private final Optional<Holder<Biome>> oceanBiome;
    private final boolean configDefaults;
    private final Supplier<Resolution> resolution;
    private volatile int originBlockX;
    private volatile int originBlockZ;
    private volatile WorldLayoutPlan effectiveLayoutPlan;

    private LimitedBiomeSource(
        Supplier<HolderSet<Biome>> allowedBiomes,
        Optional<Holder<Biome>> starterBiome,
        int starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        WorldLimitPlan worldLimits,
        ExteriorPlan exteriorPlan,
        WorldLayoutPlan worldLayoutPlan,
        SpawnStrategy spawnStrategy,
        boolean allowRivers,
        boolean allowOceans,
        boolean configDefaults,
        HolderGetter<Biome> biomeGetter
    ) {
        this.starterBiome = starterBiome;
        this.starterRadiusBlocks = starterRadiusBlocks;
        this.starterLandPlan = starterLandPlan;
        this.worldLimits = worldLimits;
        this.exteriorPlan = exteriorPlan;
        this.worldLayoutPlan = worldLayoutPlan;
        this.effectiveLayoutPlan = worldLayoutPlan;
        this.spawnStrategy = spawnStrategy;
        this.allowRivers = allowRivers;
        this.allowOceans = allowOceans;
        this.oceanBiome = exteriorPlan.overworld().mode() == ExteriorMode.OCEAN
            ? biomeGetter.get(Biomes.DEEP_OCEAN).map(value -> value)
            : Optional.empty();
        this.configDefaults = configDefaults;
        // World presets are decoded before dynamic-registry tags are bound in
        // 26.2. Defer tag expansion and climate filtering until Minecraft first
        // asks this biome source for its possible biomes or an actual biome.
        this.resolution = Suppliers.memoize(() -> resolveAllowedBiomes(
            allowedBiomes.get(), starterBiome, this.oceanBiome, worldLayoutPlan,
            allowRivers, allowOceans, biomeGetter
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
        Optional<Boolean> encodedAllowRivers,
        Optional<Boolean> encodedAllowOceans,
        Optional<String> encodedWorldType,
        HolderGetter<Biome> biomeGetter
    ) {
        WorldzConfig config = WorldzCommon.config();
        // world_type is a decode-time-only hint (never round-tripped, see the codec's
        // forGetter) distinguishing which config section a fieldless preset defaults
        // from -- see DESIGN §20.2's Phase 2.1 subsection. Once any field is explicit
        // (Customize screen "Done"), it is meaningless and ignored.
        boolean singleBiomeDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("single_biome"::equals).orElse(false);
        boolean chaosBiomesDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("chaos_biomes"::equals).orElse(false);

        Supplier<HolderSet<Biome>> allowed = encodedBiomes
            .<Supplier<HolderSet<Biome>>>map(value -> () -> value)
            .orElseGet(() -> chaosBiomesDefaults
                ? () -> resolveChaosBiomesAllowed(config, biomeGetter)
                : singleBiomeDefaults
                    ? () -> resolveSingleBiomeAllowed(config, biomeGetter)
                    : () -> resolveConfiguredBiomes(config, biomeGetter));

        // Every encoded instance has starter_radius. Its presence distinguishes a
        // persisted "no starter biome" from the fieldless preset that consults config.
        Optional<Holder<Biome>> starter = encodedStarterRadius.isPresent()
            ? encodedStarterBiome
            : encodedStarterBiome.or(() -> chaosBiomesDefaults
                ? resolveChaosBiomesStarter(config, biomeGetter)
                : singleBiomeDefaults
                    ? resolveSingleBiomeStarter(config, biomeGetter)
                    : resolveConfiguredStarter(config, biomeGetter));
        int radius = encodedStarterRadius.orElse(
            chaosBiomesDefaults ? config.chaosBiomes.starterRadiusBlocks
                : singleBiomeDefaults ? config.singleBiome.starterRadiusBlocks : config.starterRadiusBlocks
        );
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
        // world and then re-seeded to the real Minecraft world seed at generation time
        // (DESIGN §20.4) -- this placeholder never reaches actual sampling.
        WorldLayoutPlan worldLayout = encodedStarterRadius.isPresent()
            ? encodedWorldLayout.orElseGet(WorldLayoutPlan::legacy)
            : encodedWorldLayout.orElseGet(() -> chaosBiomesDefaults
                ? WorldLayoutPlan.resolve(
                    LayoutMode.CHAOS, config.chaosBiomes.biomes, Map.of(),
                    config.chaosBiomes.regionScaleBlocks, null, new Random().nextLong()
                )
                : singleBiomeDefaults
                    ? WorldLayoutPlan.resolve(
                        LayoutMode.SINGLE_BIOME, List.of(), Map.of(),
                        WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS, config.singleBiome.landBiome, new Random().nextLong()
                    )
                    : WorldLayoutPlan.fromConfig(config, new Random().nextLong()));
        SpawnStrategy spawnStrategy = encodedStarterRadius.isPresent()
            ? encodedSpawnStrategy.map(SpawnStrategy::parse).orElse(SpawnStrategy.STARTER_AT_ORIGIN)
            : encodedSpawnStrategy.map(SpawnStrategy::parse).orElseGet(() -> chaosBiomesDefaults
                ? config.chaosBiomes.spawn.strategy
                : singleBiomeDefaults
                    ? config.singleBiome.spawn.strategy
                    : config.spawn.strategy);
        // allow_rivers/allow_oceans come from whichever typed-preset config section is in
        // play (GOALS 13/14, DESIGN §20.5, generalized to CHAOS in Phase 4.1); the generic
        // fieldless preset falls back to its own top-level fields (default false either way).
        boolean allowRivers = encodedAllowRivers.orElseGet(() -> chaosBiomesDefaults ? config.chaosBiomes.allowRivers
            : singleBiomeDefaults ? config.singleBiome.allowRivers : config.allowRivers);
        boolean allowOceans = encodedAllowOceans.orElseGet(() -> chaosBiomesDefaults ? config.chaosBiomes.allowOceans
            : singleBiomeDefaults ? config.singleBiome.allowOceans : config.allowOceans);

        return new LimitedBiomeSource(
            allowed, starter, radius, starterLand, limits, exterior, worldLayout, spawnStrategy,
            allowRivers, allowOceans, encodedStarterRadius.isEmpty(), biomeGetter
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
     * @param allowRivers let vanilla's own river biomes generate naturally (single_biome only, GOALS 13)
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally (single_biome only, GOALS 14)
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
        boolean allowRivers,
        boolean allowOceans,
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
            allowRivers,
            allowOceans,
            false,
            biomeGetter
        );
    }

    private static Resolution resolveAllowedBiomes(
        HolderSet<Biome> allowed,
        Optional<Holder<Biome>> starterBiome,
        Optional<Holder<Biome>> oceanBiome,
        WorldLayoutPlan worldLayoutPlan,
        boolean allowRivers,
        boolean allowOceans,
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

        // DESIGN §20.5: the vanilla pass-through (GOALS 13/14, generalized to CHAOS in
        // Phase 4.1) needs the full, unfiltered overworld source -- the Worldz-restricted
        // `delegate` above essentially never contains river/ocean biomes, so sampling it
        // would defeat the whole feature.
        Optional<MultiNoiseBiomeSource> naturalDelegate = Optional.empty();
        boolean supportsPassThrough = worldLayoutPlan.mode() == LayoutMode.SINGLE_BIOME
            || worldLayoutPlan.mode() == LayoutMode.CHAOS;
        if (supportsPassThrough && (allowRivers || allowOceans)) {
            naturalDelegate = Optional.of(MultiNoiseBiomeSource.createFromList(overworld));
            if (allowRivers) {
                biomeGetter.get(BiomeTags.IS_RIVER).ifPresent(holders -> holders.stream().forEach(possible::add));
            }
            if (allowOceans) {
                biomeGetter.get(BiomeTags.IS_OCEAN).ifPresent(holders -> holders.stream().forEach(possible::add));
            }
        }

        return new Resolution(
            HolderSet.direct(List.copyOf(allowedSet)), delegate, naturalDelegate, Set.copyOf(possible), layoutBiomes
        );
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

    private static HolderSet<Biome> resolveSingleBiomeAllowed(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        resolveSingleBiomeHolder(config.singleBiome.landBiome, biomeGetter)
            .ifPresentOrElse(
                resolved::add,
                () -> WorldzCommon.LOGGER.warn("Unknown singleBiome.landBiome '{}'.", config.singleBiome.landBiome)
            );
        if (!config.singleBiome.starterBiome.isEmpty()) {
            resolveSingleBiomeHolder(config.singleBiome.starterBiome, biomeGetter).ifPresent(resolved::add);
        }
        return HolderSet.direct(List.copyOf(resolved));
    }

    private static Optional<Holder<Biome>> resolveSingleBiomeStarter(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        if (config.singleBiome.starterBiome.isEmpty()) {
            return Optional.empty();
        }
        Optional<Holder<Biome>> holder = resolveSingleBiomeHolder(config.singleBiome.starterBiome, biomeGetter);
        if (holder.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Unknown singleBiome.starterBiome '{}'; starter zone disabled.", config.singleBiome.starterBiome
            );
        }
        return holder;
    }

    private static Optional<Holder<Biome>> resolveSingleBiomeHolder(String id, HolderGetter<Biome> biomeGetter) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
        return biomeGetter.get(key).map(value -> value);
    }

    private static HolderSet<Biome> resolveChaosBiomesAllowed(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        for (WeightedBiomeListSpec.Entry entry : WeightedBiomeListSpec.parse(config.chaosBiomes.biomes).entries()) {
            resolveSingleBiomeHolder(entry.id(), biomeGetter)
                .ifPresentOrElse(
                    resolved::add,
                    () -> WorldzCommon.LOGGER.warn("Unknown chaosBiomes.biomes entry '{}'.", entry.id())
                );
        }
        if (!config.chaosBiomes.starterBiome.isEmpty()) {
            resolveSingleBiomeHolder(config.chaosBiomes.starterBiome, biomeGetter).ifPresent(resolved::add);
        }
        return HolderSet.direct(List.copyOf(resolved));
    }

    private static Optional<Holder<Biome>> resolveChaosBiomesStarter(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        if (config.chaosBiomes.starterBiome.isEmpty()) {
            return Optional.empty();
        }
        Optional<Holder<Biome>> holder = resolveSingleBiomeHolder(config.chaosBiomes.starterBiome, biomeGetter);
        if (holder.isEmpty()) {
            WorldzCommon.LOGGER.warn(
                "Unknown chaosBiomes.starterBiome '{}'; starter zone disabled.", config.chaosBiomes.starterBiome
            );
        }
        return holder;
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
     * Returns the coordinated-layout plan baked into this source, exactly as persisted
     * (round-trips through the codec and the Customize screen). Sampling call sites
     * should use {@link #effectiveLayoutPlan()} instead, which reflects the real world
     * seed once resolved.
     *
     * @return immutable persisted layout plan
     */
    public WorldLayoutPlan worldLayoutPlan() {
        return this.worldLayoutPlan;
    }

    /**
     * Returns the layout plan actually used for sampling: the persisted plan re-seeded
     * with the real Minecraft world seed once {@link #setLayoutSeed(long)} has resolved
     * it, or the persisted plan's own placeholder seed until then. Codecs decode from
     * {@code RegistryOps}, which has no seed-aware hook, so the real seed is applied
     * later, at generation time (see DESIGN §20.4).
     *
     * @return the plan sampling call sites should use
     */
    public WorldLayoutPlan effectiveLayoutPlan() {
        return this.effectiveLayoutPlan;
    }

    /**
     * Resolves the real Minecraft world seed for layout sampling. Not part of the codec:
     * the seed is applied after decode (mirrors {@link #setOrigin(int, int)}), but unlike
     * the origin search it needs no persistence of its own -- {@code ServerLevel.getSeed()}
     * already returns the same deterministic value on every load, so the loader hook can
     * simply call this every time a level's {@code ChunkMap} is constructed.
     *
     * @param seed the real Minecraft world seed
     */
    public void setLayoutSeed(long seed) {
        this.effectiveLayoutPlan = this.worldLayoutPlan.withSeed(seed);
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
     * Returns whether vanilla's own river biomes are allowed to pass through (GOALS 13,
     * {@code single_biome} only).
     *
     * @return true when the pass-through applies to rivers
     */
    public boolean allowRivers() {
        return this.allowRivers;
    }

    /**
     * Returns whether vanilla's own river/ocean-family biomes are allowed to pass through
     * (GOALS 14, {@code single_biome} only).
     *
     * @return true when the pass-through applies to oceans (and rivers)
     */
    public boolean allowOceans() {
        return this.allowOceans;
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

    /**
     * Tests whether the vanilla pass-through (DESIGN §20.5, GOALS 13/14) applies at one
     * column. Used by {@code EnvelopedChunkGenerator}'s terrain adjustment so a passed-
     * through river/ocean's natural depression is left untouched instead of being raised
     * toward guaranteed land -- otherwise {@link #getNoiseBiome} would display a river or
     * ocean biome over terrain that had already been flattened back to dry land.
     *
     * @param blockX absolute block X (never origin-relative -- vanilla's own terrain has
     *     no notion of Worldz's layout origin)
     * @param blockY representative block Y; the natural, unadjusted floor height at this
     *     column is a good choice since it is close to where the real surface will be
     * @param blockZ absolute block Z
     * @param sampler the real world's climate sampler
     * @return true when {@link #getNoiseBiome} would return a passed-through vanilla biome here
     */
    public boolean isNaturalPassThroughAt(int blockX, int blockY, int blockZ, Climate.Sampler sampler) {
        return naturalPassThroughBiome(
            QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockY), QuartPos.fromBlock(blockZ), sampler
        ).isPresent();
    }

    /**
     * Shared by {@link #getNoiseBiome} and {@link #isNaturalPassThroughAt} so the two never
     * disagree about where the pass-through applies.
     */
    private Optional<Holder<Biome>> naturalPassThroughBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        Optional<MultiNoiseBiomeSource> naturalDelegate = this.resolution.get().naturalDelegate();
        if (naturalDelegate.isEmpty()) {
            return Optional.empty();
        }
        Holder<Biome> natural = naturalDelegate.get().getNoiseBiome(quartX, quartY, quartZ, sampler);
        if (this.allowRivers && natural.is(BiomeTags.IS_RIVER)) {
            return Optional.of(natural);
        }
        if (this.allowOceans && natural.is(BiomeTags.IS_OCEAN)) {
            return Optional.of(natural);
        }
        return Optional.empty();
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
        Optional<Holder<Biome>> passThrough = naturalPassThroughBiome(quartX, quartY, quartZ, sampler);
        if (passThrough.isPresent()) {
            return passThrough.get();
        }
        if (this.worldLayoutPlan.mode() != LayoutMode.LEGACY) {
            WorldLayoutPlan layoutPlan = this.effectiveLayoutPlan;
            if (isInStarterTransitionRing(quartX, quartZ)) {
                Optional<Holder<Biome>> beach = layoutPlan.sampleRole(BiomeRole.BEACH, blockX - originX, blockZ - originZ)
                    .map(this.resolution.get().layoutBiomes()::get);
                if (beach.isPresent()) {
                    return beach.get();
                }
            }
            Optional<Holder<Biome>> sampled = layoutPlan.sampleAt(blockX - originX, blockZ - originZ).biomeId()
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
        Optional<MultiNoiseBiomeSource> naturalDelegate,
        Set<Holder<Biome>> possibleBiomes,
        Map<String, Holder<Biome>> layoutBiomes
    ) {
    }
}
