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
import media.jlt.minecraft.mods.worldz.logic.BiomeRoles;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandOceanProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;
import media.jlt.minecraft.mods.worldz.logic.StarterZone;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.StackedLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.StackedPlan;
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
    /**
     * {@code stony_shore} has no dedicated vanilla tag the way beaches do
     * ({@link BiomeTags#IS_BEACH} only covers {@code beach}/{@code snowy_beach}),
     * so the beach pass-through checks this specific id directly.
     */
    private static final ResourceKey<Biome> STONY_SHORE = ResourceKey.create(
        Registries.BIOME, Identifier.withDefaultNamespace("stony_shore")
    );

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
        // Nested rather than three flat optional booleans (DESIGN §27.9/PassThroughCodecs):
        // this codec's instance.group(...) was already at the 14-field Function14 ceiling, and
        // the three toggles are always encoded together anyway.
        PassThroughCodecs.FLAGS_CODEC.optionalFieldOf("pass_through")
            .forGetter(source -> Optional.of(new PassThroughCodecs.Flags(source.allowRivers, source.allowOceans, source.allowBeaches))),
        IslandCodecs.PLAN_CODEC.optionalFieldOf("island").forGetter(source -> Optional.of(source.island)),
        SkyIslandCodecs.PLAN_CODEC.optionalFieldOf("sky_island").forGetter(source -> Optional.of(source.skyIsland)),
        Codec.STRING.optionalFieldOf("world_type").forGetter(source -> Optional.<String>empty()),
        // The last spare top-level slot (DESIGN §29.7): this codec's instance.group(...) is now
        // completely full at 14 fields. Any future top-level LimitedBiomeSource field must nest
        // into an existing group instead (the same move made twice now -- pass_through, §27.9).
        ChunkIslandCodecs.PLAN_CODEC.optionalFieldOf("chunk_island").forGetter(source -> Optional.of(source.chunkIsland)),
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
    private final boolean allowBeaches;
    private final IslandPlan island;
    private final SkyIslandPlan skyIsland;
    private final ChunkIslandPlan chunkIsland;
    private final Optional<Holder<Biome>> oceanBiome;
    private final boolean configDefaults;
    private final Supplier<Resolution> resolution;
    private volatile int originBlockX;
    private volatile int originBlockZ;
    private volatile WorldLayoutPlan effectiveLayoutPlan;
    /**
     * Pushed post-construction from {@code EnvelopedChunkGenerator}'s own constructor (GOAL 35,
     * DESIGN §34.3) -- unlike {@link #island}/{@link #skyIsland}/{@link #chunkIsland} (read live
     * FROM this class by the generator), the stacked plan flows the other direction: it is
     * persisted on the generator's own codec (its codec has a free slot; this class's is already
     * full 14/14), but real per-Y biome reporting can only happen here, since vanilla always asks
     * *this* class for a column's biome, never the generator. Mirrors {@link #setLayoutSeed}'s
     * "not part of the codec" precedent -- fully known at construction, no real-seed dependency
     * of its own (layer order resolution reuses {@link #effectiveLayoutPlan}'s own seed).
     */
    private volatile StackedPlan stackedPlan = StackedPlan.disabled();

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
        boolean allowBeaches,
        IslandPlan island,
        SkyIslandPlan skyIsland,
        ChunkIslandPlan chunkIsland,
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
        this.allowBeaches = allowBeaches;
        this.island = island;
        this.skyIsland = skyIsland;
        this.chunkIsland = chunkIsland;
        this.oceanBiome = exteriorPlan.overworld().mode() == ExteriorMode.OCEAN
            ? biomeGetter.get(Biomes.DEEP_OCEAN).map(value -> value)
            : Optional.empty();
        this.configDefaults = configDefaults;
        // World presets are decoded before dynamic-registry tags are bound in
        // 26.2. Defer tag expansion and climate filtering until Minecraft first
        // asks this biome source for its possible biomes or an actual biome.
        this.resolution = Suppliers.memoize(() -> resolveAllowedBiomes(
            allowedBiomes.get(), starterBiome, this.oceanBiome, worldLayoutPlan,
            allowRivers, allowOceans, allowBeaches, island, skyIsland, biomeGetter
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
        Optional<PassThroughCodecs.Flags> encodedPassThrough,
        Optional<IslandPlan> encodedIsland,
        Optional<SkyIslandPlan> encodedSkyIsland,
        Optional<String> encodedWorldType,
        Optional<ChunkIslandPlan> encodedChunkIsland,
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
        // Closes the gap logged in MEMORY.md/TODO.md (Phase 6.2b/6.3): a strip_world
        // created without ever opening Customize (world_preset/strip_world.json's own
        // "world_type": "strip_world" hint) previously fell straight through to the
        // generic preset's own defaults, silently ignoring stripWorld.bands entirely.
        boolean stripWorldDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("strip_world"::equals).orElse(false);
        // Same fix, same reason, for ocean_island (GOALS 01/04, DESIGN §24): without this
        // branch a config-only "select preset, Create World" world would silently get no
        // island at all (IslandPlan.disabled() fallback further down).
        boolean oceanIslandDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("ocean_island"::equals).orElse(false);
        // Same fix, same reason, for sky_island (GOALS 05, DESIGN §27): without this branch a
        // config-only "select preset, Create World" world would silently get no island at all
        // (SkyIslandPlan.disabled() fallback further down).
        boolean skyIslandDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("sky_island"::equals).orElse(false);
        // Same fix, same reason, for sky_chunk (GOALS 09/37, DESIGN §29): without this branch a
        // config-only "select preset, Create World" world would silently get no chunk islands
        // at all (ChunkIslandPlan.disabled() fallback further down) -- the exact "known gap"
        // Phase 6.2b/6.3/6.2c had to fix after the fact for strip_world, closed from day one here.
        boolean skyChunkDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("sky_chunk"::equals).orElse(false);
        // Same fix, same reason, for cave (GOALS 25-26, DESIGN §30): without this branch a
        // config-only "select preset, Create World" world would fall through to the generic
        // preset's own restricted biome list instead of full vanilla variety. Unlike every
        // other typed preset, cave's own plan (CavePlan) is never read from here at all --
        // this hint only affects LimitedBiomeSource's own biome/starter/layout defaults,
        // which for cave are identical to strip_world/ocean_island/sky_island/sky_chunk's
        // (full vanilla variety, no starter, no coordinated layout).
        boolean caveDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("cave"::equals).orElse(false);
        // Same fix, same reason, for nether_start (GOALS 27, DESIGN §31): this hint only
        // affects LimitedBiomeSource's own biome/starter/layout defaults -- NetherStartPlan
        // itself is read from EnvelopedChunkGenerator's own codec, never from here (§31.5),
        // exactly like cave's own hint above.
        boolean netherStartDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("nether_start"::equals).orElse(false);
        // Same fix, same reason, for end_start (GOALS 34, DESIGN §32): this hint only affects
        // LimitedBiomeSource's own biome/starter/layout defaults -- EndStartPlan itself is read
        // from EnvelopedChunkGenerator's own codec, never from here (§32.3), exactly like
        // nether_start's own hint above.
        boolean endStartDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("end_start"::equals).orElse(false);
        // Same fix shape again for flat (GOAL 15, DESIGN §33.2), but unlike cave/nether_start/
        // end_start's "full vanilla variety" hint, flat needs exactly one biome everywhere --
        // mirrors single_biome's own hint below instead, just reading config.flat.biome. FlatPlan
        // itself is read from EnvelopedChunkGenerator's own codec, never from here (DESIGN
        // §33.2), same "hint only affects LimitedBiomeSource's own defaults" split every other
        // generator-owned plan already uses.
        boolean flatDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("flat"::equals).orElse(false);
        // Same fix shape again for deep_flat (GOAL 16, DESIGN §33.4): unlike flat, deep_flat
        // needs full vanilla biome variety (real caves/cave biomes/rivers below the cap), so it
        // mirrors cave/nether_start/end_start's own hint instead of flat's single-biome one.
        // DeepFlatPlan itself is read from EnvelopedChunkGenerator's own codec, never from here.
        boolean deepFlatDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("deep_flat"::equals).orElse(false);
        // Same fix shape again for stacked (GOAL 35, DESIGN §34.1): unlike every other typed
        // preset, stacked needs several biomes at once (one per layer), not one biome (flat) or
        // full vanilla variety (cave/deep_flat/...) -- resolveStackedAllowed returns exactly the
        // configured layers' own biome set. StackedPlan itself is read from
        // EnvelopedChunkGenerator's own codec, never from here, same split as flat/deep_flat.
        boolean stackedDefaults = encodedStarterRadius.isEmpty()
            && encodedWorldType.map("stacked"::equals).orElse(false);

        Supplier<HolderSet<Biome>> allowed = encodedBiomes
            .<Supplier<HolderSet<Biome>>>map(value -> () -> value)
            .orElseGet(() -> chaosBiomesDefaults
                ? () -> resolveChaosBiomesAllowed(config, biomeGetter)
                : singleBiomeDefaults
                    ? () -> resolveSingleBiomeAllowed(config, biomeGetter)
                    : flatDefaults
                        ? () -> resolveFlatAllowed(config, biomeGetter)
                        : stackedDefaults
                            ? () -> resolveStackedAllowed(config, biomeGetter)
                            : stripWorldDefaults || oceanIslandDefaults || skyIslandDefaults || skyChunkDefaults || caveDefaults
                                || netherStartDefaults || endStartDefaults || deepFlatDefaults
                                ? () -> resolveFullVanillaOverworldAllowed(biomeGetter)
                                : () -> resolveConfiguredBiomes(config, biomeGetter));

        // Every encoded instance has starter_radius. Its presence distinguishes a
        // persisted "no starter biome" from the fieldless preset that consults config.
        // strip_world/ocean_island never have a starter biome at all (GOALS 32/01: a shape,
        // not a biome restriction) -- Optional.empty() directly, not a fallback lookup.
        Optional<Holder<Biome>> starter = encodedStarterRadius.isPresent()
            ? encodedStarterBiome
            : stripWorldDefaults || oceanIslandDefaults || skyIslandDefaults || skyChunkDefaults || caveDefaults
                || netherStartDefaults || endStartDefaults || flatDefaults || deepFlatDefaults || stackedDefaults
                ? Optional.empty()
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
        // (DESIGN §20.4) -- this placeholder never reaches actual sampling. ocean_island
        // deliberately always stays LEGACY (DESIGN §24.2) -- its land biome is resolved
        // entirely by IslandPlan, not WorldLayoutPlan, so it never reads config.layout here
        // even though that's the generic preset's own fallback for every other branch.
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
                    : flatDefaults
                        ? WorldLayoutPlan.resolve(
                            LayoutMode.SINGLE_BIOME, List.of(), Map.of(),
                            WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS, config.flat.biome, new Random().nextLong()
                        )
                    : oceanIslandDefaults || skyIslandDefaults || skyChunkDefaults || caveDefaults
                        || netherStartDefaults || endStartDefaults || deepFlatDefaults || stackedDefaults
                        ? WorldLayoutPlan.legacy()
                        : stripWorldDefaults && config.stripWorld.bands.enabled
                            ? WorldLayoutPlan.resolveBands(
                                config.stripWorld.bands.biomes, config.stripWorld.bands.widthBlocks,
                                config.stripWorld.bands.seedRandomOrder, Map.of(), new Random().nextLong()
                            )
                            : WorldLayoutPlan.fromConfig(config, new Random().nextLong()));
        // ocean_island has no spawn-strategy option at all (DESIGN §24.8) -- always
        // STARTER_AT_ORIGIN, regardless of what the generic preset's own config says.
        SpawnStrategy spawnStrategy = encodedStarterRadius.isPresent()
            ? encodedSpawnStrategy.map(SpawnStrategy::parse).orElse(SpawnStrategy.STARTER_AT_ORIGIN)
            : encodedSpawnStrategy.map(SpawnStrategy::parse).orElseGet(() -> chaosBiomesDefaults
                ? config.chaosBiomes.spawn.strategy
                : singleBiomeDefaults
                    ? config.singleBiome.spawn.strategy
                    : stripWorldDefaults
                        ? config.stripWorld.spawn.strategy
                        : oceanIslandDefaults || skyIslandDefaults || skyChunkDefaults || caveDefaults
                            || netherStartDefaults || endStartDefaults || flatDefaults || deepFlatDefaults || stackedDefaults
                            ? SpawnStrategy.STARTER_AT_ORIGIN
                            : config.spawn.strategy);
        // allow_rivers/allow_oceans/allow_beaches come from whichever typed-preset config
        // section is in play (GOALS 13/14, DESIGN §20.5, generalized to CHAOS in Phase 4.1
        // and STRIP_BANDS in the GOALS 36 follow-up); the generic fieldless preset falls
        // back to its own top-level fields (allow_beaches has no such field, so false).
        // ocean_island never uses this pass-through mechanism at all (its own IslandPlan
        // logic resolves every biome directly), so it always stays false here.
        boolean allowRivers = encodedPassThrough.map(PassThroughCodecs.Flags::allowRivers)
            .orElseGet(() -> chaosBiomesDefaults ? config.chaosBiomes.allowRivers
                : singleBiomeDefaults ? config.singleBiome.allowRivers
                    : stripWorldDefaults ? config.stripWorld.bands.allowRivers : config.allowRivers);
        boolean allowOceans = encodedPassThrough.map(PassThroughCodecs.Flags::allowOceans)
            .orElseGet(() -> chaosBiomesDefaults ? config.chaosBiomes.allowOceans
                : singleBiomeDefaults ? config.singleBiome.allowOceans
                    : stripWorldDefaults ? config.stripWorld.bands.allowOceans : config.allowOceans);
        boolean allowBeaches = encodedPassThrough.map(PassThroughCodecs.Flags::allowBeaches)
            .orElseGet(() -> chaosBiomesDefaults ? config.chaosBiomes.allowBeaches
                : singleBiomeDefaults ? config.singleBiome.allowBeaches
                    : stripWorldDefaults ? config.stripWorld.bands.allowBeaches : false);
        IslandPlan island = encodedIsland.orElseGet(() -> {
            if (!oceanIslandDefaults) {
                return IslandPlan.disabled();
            }
            return switch (config.oceanIsland.islandSource) {
                case CHEST_BOAT -> IslandPlan.fromConfigWithoutLand(config.oceanIsland);
                case NATURAL -> IslandPlan.fromConfigNatural(config.oceanIsland);
                case ARTIFICIAL -> IslandPlan.fromConfig(config.oceanIsland);
            };
        });
        SkyIslandPlan skyIsland = encodedSkyIsland.orElseGet(
            () -> skyIslandDefaults ? SkyIslandPlan.fromConfig(config.skyIsland) : SkyIslandPlan.disabled()
        );
        ChunkIslandPlan chunkIsland = encodedChunkIsland.orElseGet(
            () -> skyChunkDefaults
                ? ChunkIslandPlan.fromConfig(config.chunkIsland, ChunkIslandPlan.Dimension.OVERWORLD)
                : ChunkIslandPlan.disabled()
        );

        return new LimitedBiomeSource(
            allowed, starter, radius, starterLand, limits, exterior, worldLayout, spawnStrategy,
            allowRivers, allowOceans, allowBeaches, island, skyIsland, chunkIsland,
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
     * @param allowRivers let vanilla's own river biomes generate naturally (single_biome only, GOALS 13)
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally (single_biome only, GOALS 14)
     * @param allowBeaches let vanilla's own beach/stony-shore biomes generate naturally
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
        boolean allowBeaches,
        HolderGetter<Biome> biomeGetter
    ) {
        return customized(
            allowedBiomes, starterBiome, starterRadiusBlocks, starterLandPlan, worldLimits, exteriorPlan,
            worldLayoutPlan, spawnStrategy, allowRivers, allowOceans, allowBeaches, IslandPlan.disabled(), biomeGetter
        );
    }

    /**
     * Creates a source from values selected in the world-creation screen, including an
     * explicit ocean-island plan (GOALS 01, DESIGN §24).
     *
     * @param allowedBiomes resolved direct allowed-biome holders
     * @param starterBiome optional resolved starter biome
     * @param starterRadiusBlocks starter-zone radius
     * @param starterLandPlan persisted terrain guarantee
     * @param worldLimits persisted border plan
     * @param exteriorPlan persisted exterior-terrain plan
     * @param worldLayoutPlan persisted coordinated-layout plan
     * @param spawnStrategy persisted layout-origin and spawn strategy
     * @param allowRivers let vanilla's own river biomes generate naturally
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
     * @param allowBeaches let vanilla's own beach/stony-shore biomes generate naturally
     * @param island resolved ocean-island plan, disabled for every other preset
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
        boolean allowBeaches,
        IslandPlan island,
        HolderGetter<Biome> biomeGetter
    ) {
        return customized(
            allowedBiomes, starterBiome, starterRadiusBlocks, starterLandPlan, worldLimits, exteriorPlan,
            worldLayoutPlan, spawnStrategy, allowRivers, allowOceans, allowBeaches, island,
            SkyIslandPlan.disabled(), biomeGetter
        );
    }

    /**
     * Creates a source from values selected in the world-creation screen, including an
     * explicit sky-island plan (GOALS 05, DESIGN §27).
     *
     * @param allowedBiomes resolved direct allowed-biome holders
     * @param starterBiome optional resolved starter biome
     * @param starterRadiusBlocks starter-zone radius
     * @param starterLandPlan persisted terrain guarantee
     * @param worldLimits persisted border plan
     * @param exteriorPlan persisted exterior-terrain plan
     * @param worldLayoutPlan persisted coordinated-layout plan
     * @param spawnStrategy persisted layout-origin and spawn strategy
     * @param allowRivers let vanilla's own river biomes generate naturally
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
     * @param allowBeaches let vanilla's own beach/stony-shore biomes generate naturally
     * @param island resolved ocean-island plan, disabled for every other preset
     * @param skyIsland resolved sky-island plan, disabled for every other preset
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
        boolean allowBeaches,
        IslandPlan island,
        SkyIslandPlan skyIsland,
        HolderGetter<Biome> biomeGetter
    ) {
        return customized(
            allowedBiomes, starterBiome, starterRadiusBlocks, starterLandPlan, worldLimits, exteriorPlan,
            worldLayoutPlan, spawnStrategy, allowRivers, allowOceans, allowBeaches, island, skyIsland,
            ChunkIslandPlan.disabled(), biomeGetter
        );
    }

    /**
     * Creates a source from values selected in the world-creation screen, including an
     * explicit chunk-island plan (GOALS 09/37, DESIGN §29).
     *
     * @param allowedBiomes resolved direct allowed-biome holders
     * @param starterBiome optional resolved starter biome
     * @param starterRadiusBlocks starter-zone radius
     * @param starterLandPlan persisted terrain guarantee
     * @param worldLimits persisted border plan
     * @param exteriorPlan persisted exterior-terrain plan
     * @param worldLayoutPlan persisted coordinated-layout plan
     * @param spawnStrategy persisted layout-origin and spawn strategy
     * @param allowRivers let vanilla's own river biomes generate naturally
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
     * @param allowBeaches let vanilla's own beach/stony-shore biomes generate naturally
     * @param island resolved ocean-island plan, disabled for every other preset
     * @param skyIsland resolved sky-island plan, disabled for every other preset
     * @param chunkIsland resolved chunk-island plan, disabled for every other preset
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
        boolean allowBeaches,
        IslandPlan island,
        SkyIslandPlan skyIsland,
        ChunkIslandPlan chunkIsland,
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
            allowBeaches,
            island,
            skyIsland,
            chunkIsland,
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
        boolean allowBeaches,
        IslandPlan island,
        SkyIslandPlan skyIsland,
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
        // Phase 4.1 and STRIP_BANDS in the GOALS 36 follow-up) needs the full, unfiltered
        // overworld source -- the Worldz-restricted `delegate` above essentially never
        // contains river/ocean/beach biomes, so sampling it would defeat the whole feature.
        Optional<MultiNoiseBiomeSource> naturalDelegate = Optional.empty();
        boolean supportsPassThrough = worldLayoutPlan.mode() == LayoutMode.SINGLE_BIOME
            || worldLayoutPlan.mode() == LayoutMode.CHAOS
            || worldLayoutPlan.mode() == LayoutMode.STRIP_BANDS;
        if (supportsPassThrough && (allowRivers || allowOceans || allowBeaches)) {
            naturalDelegate = Optional.of(MultiNoiseBiomeSource.createFromList(overworld));
            if (allowRivers) {
                biomeGetter.get(BiomeTags.IS_RIVER).ifPresent(holders -> holders.stream().forEach(possible::add));
            }
            if (allowOceans) {
                biomeGetter.get(BiomeTags.IS_OCEAN).ifPresent(holders -> holders.stream().forEach(possible::add));
            }
            if (allowBeaches) {
                biomeGetter.get(BiomeTags.IS_BEACH).ifPresent(holders -> holders.stream().forEach(possible::add));
                biomeGetter.get(STONY_SHORE).ifPresent(possible::add);
            }
        }

        Map<String, Holder<Biome>> islandBiomes = resolveIslandBiomes(island, biomeGetter);
        if (island.enabled()) {
            possible.addAll(islandBiomes.values());
        }

        Map<String, Holder<Biome>> skyIslandBiomes = resolveSkyIslandBiomes(skyIsland, biomeGetter);
        if (skyIsland.enabled()) {
            possible.addAll(skyIslandBiomes.values());
        }

        return new Resolution(
            HolderSet.direct(List.copyOf(allowedSet)), delegate, naturalDelegate, Set.copyOf(possible),
            layoutBiomes, islandBiomes, skyIslandBiomes
        );
    }

    /**
     * Resolves every biome id a sky island can ever select (GOALS 05, DESIGN §27): the starter
     * island's own single biome, plus every biome in the scattered floating-island pool (GOALS
     * 07-08, DESIGN §28.2) when that's enabled. Unlike {@link #resolveIslandBiomes} there is no
     * shore ring or ocean-gradient set, since the sky island's exterior is void, not a gradient.
     */
    private static Map<String, Holder<Biome>> resolveSkyIslandBiomes(SkyIslandPlan skyIsland, HolderGetter<Biome> biomeGetter) {
        if (!skyIsland.enabled()) {
            return Map.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        ids.add(skyIsland.islandBiome());
        if (skyIsland.floatingIslands().enabled()) {
            ids.addAll(skyIsland.floatingIslands().islandBiomes());
        }

        Map<String, Holder<Biome>> resolved = new LinkedHashMap<>();
        for (String id : ids) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
            biomeGetter.get(key).ifPresentOrElse(
                holder -> resolved.put(id, holder),
                () -> WorldzCommon.LOGGER.warn("Unknown sky island biome '{}'; it will never be selected.", id)
            );
        }
        return resolved;
    }

    /**
     * Resolves every biome id {@link IslandPlan} can ever select: the island's own land biome,
     * the shore ring's beach/stony-shore pair, and the complete vanilla ocean-biome set (GOALS
     * 01's gradient, DESIGN §24.5).
     */
    private static Map<String, Holder<Biome>> resolveIslandBiomes(IslandPlan island, HolderGetter<Biome> biomeGetter) {
        if (!island.enabled()) {
            return Map.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        ids.add(island.islandBiome());
        ids.add("minecraft:beach");
        ids.add("minecraft:stony_shore");
        ids.addAll(BiomeRoles.oceanIds());

        Map<String, Holder<Biome>> resolved = new LinkedHashMap<>();
        for (String id : ids) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(id));
            Optional<Holder.Reference<Biome>> holder = biomeGetter.get(key);
            if (holder.isEmpty()) {
                WorldzCommon.LOGGER.warn("Unknown island biome '{}'; it will never be selected.", id);
            } else {
                resolved.put(id, holder.get());
            }
        }
        return resolved;
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
        ids.addAll(plan.bandBiomes());

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

    /**
     * A strip world or ocean island is a shape, not a biome restriction (GOALS 32, 01):
     * ordinary vanilla biome variety, matching {@code StripWorldPresetEditor}'s own
     * explicit-customization resolution. Also the fallback ocean_island's own delegate uses
     * beyond an enabled exclusion zone (GOALS 04), so the seed's natural terrain reads with
     * full vanilla variety, not a restricted list.
     */
    private static HolderSet<Biome> resolveFullVanillaOverworldAllowed(HolderGetter<Biome> biomeGetter) {
        return biomeGetter.get(BiomeTags.IS_OVERWORLD)
            .<HolderSet<Biome>>map(value -> value)
            .orElseThrow(() -> new IllegalStateException("Missing #minecraft:is_overworld biome tag."));
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

    private static HolderSet<Biome> resolveFlatAllowed(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        resolveSingleBiomeHolder(config.flat.biome, biomeGetter)
            .ifPresentOrElse(resolved::add, () -> WorldzCommon.LOGGER.warn("Unknown flat.biome '{}'.", config.flat.biome));
        return HolderSet.direct(List.copyOf(resolved));
    }

    /**
     * Unlike every other typed preset's own allowed-biome resolution, stacked needs one holder
     * per configured layer (GOAL 35, DESIGN §34.3) rather than a single biome or full vanilla
     * variety -- this is also what {@link #collectPossibleBiomes()} ends up reporting, which
     * vanilla's own decoration pipeline unions features from (verified in
     * {@code ChunkGenerator.applyBiomeDecoration}, DESIGN §34.4).
     */
    private static HolderSet<Biome> resolveStackedAllowed(WorldzConfig config, HolderGetter<Biome> biomeGetter) {
        Set<Holder<Biome>> resolved = new LinkedHashSet<>();
        for (String raw : config.stacked.layers) {
            String biomeId = StackedLayerSpec.parse(raw).biome();
            resolveSingleBiomeHolder(biomeId, biomeGetter)
                .ifPresentOrElse(resolved::add, () -> WorldzCommon.LOGGER.warn("Unknown stacked layer biome '{}'.", biomeId));
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
     * Pushes the resolved stacked-biome-layers plan onto this biome source (GOAL 35, DESIGN
     * §34.3), called once from {@code EnvelopedChunkGenerator}'s own constructor. Harmless no-op
     * for every other preset -- {@link #stackedPlan} stays {@link StackedPlan#disabled()}.
     *
     * @param plan resolved stacked plan, disabled for every preset except {@code stacked}
     */
    public void setStackedLayers(StackedPlan plan) {
        this.stackedPlan = plan;
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
     * Returns whether vanilla's own beach/stony-shore biomes are allowed to pass through.
     *
     * @return true when the pass-through applies to beaches
     */
    public boolean allowBeaches() {
        return this.allowBeaches;
    }

    /**
     * Returns the ocean-island plan baked into this world (GOALS 01, DESIGN §24), disabled
     * for every preset except {@code ocean_island}.
     *
     * @return resolved island plan
     */
    public IslandPlan island() {
        return this.island;
    }

    /**
     * Returns the sky-island plan baked into this world (GOALS 05, DESIGN §27), disabled for
     * every preset except {@code sky_island}.
     *
     * @return resolved sky island plan
     */
    public SkyIslandPlan skyIsland() {
        return this.skyIsland;
    }

    /**
     * Returns the chunk-island plan baked into this world (GOALS 09/37, DESIGN §29), disabled
     * for every preset except {@code sky_chunk}.
     *
     * @return resolved chunk island plan
     */
    public ChunkIslandPlan chunkIsland() {
        return this.chunkIsland;
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
     * Resolves the ocean-island biome at one column (GOALS 01, 02, 03, DESIGN §24, §25.2,
     * §25.4): the island's own biome inside the coastline, a beach/stony-shore pick in the
     * shore ring, or the shallow-to-deep ocean gradient beyond it. {@link IslandPlan#hasLand}
     * {@code false} (GOALS 03, {@code CHEST_BOAT}) skips the interior/shore-ring branches
     * entirely -- every column falls straight to the ocean gradient, with the raw distance from
     * origin standing in for "distance beyond the shore" since there is no shore ring to
     * subtract. {@link IslandPlan#syntheticLand} {@code false} (GOALS 02, {@code NATURAL})
     * resolves to {@link Optional#empty()} within {@code radiusBlocks} instead -- signaling "no
     * override," so {@link #getNoiseBiome} falls through to the real seed's own biome for that
     * column -- and likewise skips the shore ring beyond it (real terrain already has whatever
     * natural coastline it has). Shares {@link #effectiveLayoutPlan}'s already-resolved real
     * seed with {@code EnvelopedChunkGenerator}'s terrain code (DESIGN §24.2), so biome and
     * terrain height can never disagree about where the coastline is.
     *
     * @param relativeX block X relative to the origin
     * @param relativeZ block Z relative to the origin
     * @return the resolved island biome; empty when the real seed's own biome should show
     *     through instead (GOALS 02), or if the registry lookup for a resolved id failed
     */
    private Optional<Holder<Biome>> islandBiomeAt(int relativeX, int relativeZ) {
        long seed = this.effectiveLayoutPlan.seed();
        double distance = this.island.distanceFromShore(relativeX, relativeZ, seed);
        boolean hasLand = this.island.hasLand();
        boolean syntheticLand = this.island.syntheticLand();
        if (hasLand && !syntheticLand && distance <= 0.0) {
            return Optional.empty();
        }
        Map<String, Holder<Biome>> islandBiomes = this.resolution.get().islandBiomes();
        String biomeId;
        if (hasLand && syntheticLand && distance <= 0.0) {
            biomeId = this.island.islandBiome();
        } else if (hasLand && syntheticLand && distance <= this.island.shoreWidthBlocks()) {
            biomeId = IslandOceanProfile.shoreBiomeAt(relativeX, relativeZ, this.island.radiusBlocks(), seed);
        } else {
            double beyondShore = (hasLand && syntheticLand) ? distance - this.island.shoreWidthBlocks() : distance;
            biomeId = IslandOceanProfile.biomeAt(
                relativeX, relativeZ, beyondShore, this.island.oceanShallowWidthBlocks(),
                this.island.oceanRegionScaleBlocks(), seed
            );
        }
        return Optional.ofNullable(islandBiomes.get(biomeId));
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
        if (this.allowBeaches && (natural.is(BiomeTags.IS_BEACH) || natural.is(STONY_SHORE))) {
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
        if (this.stackedPlan.enabled()) {
            // Checked before every other mode (mirrors sky island's own early short-circuit
            // above) -- stacked does not compose with single_biome/chaos_biomes/pass-through/
            // island shapes, matching every other Overworld-only generator-owned plan's
            // precedent. Applies everywhere in the dimension (no X/Z footprint check), unlike
            // every island-shaped preset -- a stacked world has no island footprint at all.
            List<StackedLayerSpec> resolved = this.stackedPlan.resolvedLayers(this.effectiveLayoutPlan.seed());
            String biomeId = StackedPlan.layerAt(resolved, QuartPos.toBlock(quartY)).biome();
            Optional<Holder<Biome>> stackedBiome = stackedBiomeHolder(biomeId);
            if (stackedBiome.isPresent()) {
                return stackedBiome.get();
            }
        }
        if (this.island.enabled() && this.island.withinExclusionZone(blockX - originX, blockZ - originZ)) {
            Optional<Holder<Biome>> islandResult = islandBiomeAt(blockX - originX, blockZ - originZ);
            if (islandResult.isPresent()) {
                return islandResult.get();
            }
        }
        if (this.skyIsland.enabled()) {
            long skyIslandSeed = this.effectiveLayoutPlan.seed();
            double distance = this.skyIsland.distanceFromShore(blockX - originX, blockZ - originZ, skyIslandSeed);
            String biomeId = this.skyIsland.islandBiome();
            boolean scatteredHit = false;
            if (distance > 0.0) {
                // Outside the starter island's own footprint: check the scattered floating-island
                // grid beyond it (GOALS 07-08, DESIGN §28.1) before giving up.
                FloatingIslandsPlan.Hit hit = this.skyIsland.floatingIslands().at(
                    blockX - originX, blockZ - originZ, skyIslandSeed, this.skyIsland.islandBiome()
                );
                if (hit.present()) {
                    distance = hit.distanceFromShore();
                    biomeId = hit.biome();
                    scatteredHit = true;
                } else if (this.skyIsland.withinBiomeExclusionZone(distance)) {
                    // Beyond every footprint but still inside the starter island's own biome
                    // buffer (DESIGN §27.10): keep reporting islandBiome rather than falling
                    // through to the real seed immediately -- the terrain here is void either way.
                    distance = 0.0;
                }
            }
            if (distance <= 0.0) {
                if (scatteredHit && this.skyIsland.floatingIslands().naturalBiome()) {
                    // DESIGN §28.4: real seed biome instead of a hash-picked pool, the same
                    // delegate this method's own final fallback uses below.
                    return this.resolution.get().delegate().getNoiseBiome(quartX, quartY, quartZ, sampler);
                }
                Holder<Biome> biome = this.resolution.get().skyIslandBiomes().get(biomeId);
                if (biome != null) {
                    return biome;
                }
            }
            // Outside every footprint and buffer (or the lookup failed): fall through to whatever
            // the rest of this method would normally report -- harmless, since nothing ever
            // generates there.
        }
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

    /**
     * Resolves a stacked layer's biome id against {@link #allowedBiomes()} (GOAL 35, DESIGN
     * §34.3) -- {@link #resolveStackedAllowed}/the Customize-screen editor both guarantee every
     * configured layer biome is already present there, exactly the set
     * {@link #collectPossibleBiomes()} reports, so no separate id-to-holder map is needed.
     */
    private Optional<Holder<Biome>> stackedBiomeHolder(String biomeId) {
        return this.resolution.get().allowedBiomes().stream()
            .filter(holder -> holder.unwrapKey().map(key -> key.identifier().toString()).filter(biomeId::equals).isPresent())
            .findFirst();
    }

    private record Resolution(
        HolderSet<Biome> allowedBiomes,
        MultiNoiseBiomeSource delegate,
        Optional<MultiNoiseBiomeSource> naturalDelegate,
        Set<Holder<Biome>> possibleBiomes,
        Map<String, Holder<Biome>> layoutBiomes,
        Map<String, Holder<Biome>> islandBiomes,
        Map<String, Holder<Biome>> skyIslandBiomes
    ) {
    }
}
