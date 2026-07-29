package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.FlatLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.FlatPlan;
import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandOceanProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.LayoutTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandProfile;
import media.jlt.minecraft.mods.worldz.logic.StackedLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.StackedPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterLandProfile;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.StructureDistancePlan;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Delegates vanilla generation, then replaces columns outside a persisted square envelope. */
public final class EnvelopedChunkGenerator extends ChunkGenerator {
    private static final int PRESERVED_SURFACE_SHELL_BLOCKS = 5;
    /**
     * Foundation repair depth used for a pure-layout raise with no starter-land
     * guarantee active. Starter-land raises keep using their own configured
     * {@code foundationDepthBlocks} instead.
     */
    private static final int DEFAULT_LAYOUT_FOUNDATION_DEPTH_BLOCKS = 8;
    /** Fallback sky-void island radius when {@code VOID} mode has no configured starter biome. */
    private static final int DEFAULT_VOID_ISLAND_RADIUS_BLOCKS = 256;
    /**
     * Buried stacked-layer decoration scatter density (GOALS 35, DESIGN §34.4): a fixed per-
     * chunk-per-feature attempt count rather than a config knob -- a simplification found while
     * implementing (the design pass's own "points-per-chunk knob" idea turned out to be more
     * ceremony than a first pass needs); revisit only if Jason's acceptance testing finds the
     * density visibly wrong.
     */
    private static final int BURIED_LAYER_DECORATION_ATTEMPTS = 4;
    /** Codec registered as {@code jlt_worldz:enveloped}. */
    public static final MapCodec<EnvelopedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ChunkGenerator.CODEC.fieldOf("delegate").forGetter(generator -> generator.delegate),
        Dimension.CODEC.fieldOf("dimension").forGetter(generator -> generator.dimension),
        ExteriorCodecs.DIMENSION_CODEC.optionalFieldOf("exterior").forGetter(generator -> Optional.of(generator.envelope)),
        StripCodecs.PLAN_CODEC.optionalFieldOf("strip").forGetter(generator -> Optional.of(generator.strip)),
        SkyIslandCodecs.PLAN_CODEC.optionalFieldOf("nether_sky_island")
            .forGetter(generator -> Optional.of(generator.netherSkyIsland)),
        ChunkIslandCodecs.PLAN_CODEC.optionalFieldOf("chunk_island")
            .forGetter(generator -> Optional.of(generator.nonOverworldChunkIsland)),
        CaveCodecs.PLAN_CODEC.optionalFieldOf("cave").forGetter(generator -> Optional.of(generator.cave)),
        NetherStartCodecs.PLAN_CODEC.optionalFieldOf("nether_start")
            .forGetter(generator -> Optional.of(generator.netherStart)),
        EndStartCodecs.PLAN_CODEC.optionalFieldOf("end_start").forGetter(generator -> Optional.of(generator.endStart)),
        FlatCodecs.PLAN_CODEC.optionalFieldOf("flat").forGetter(generator -> Optional.of(generator.flat)),
        DeepFlatCodecs.PLAN_CODEC.optionalFieldOf("deep_flat").forGetter(generator -> Optional.of(generator.deepFlat)),
        StackedCodecs.PLAN_CODEC.optionalFieldOf("stacked").forGetter(generator -> Optional.of(generator.stacked)),
        // Fieldless-preset hint only (DESIGN §30.1/§45), mirroring LimitedBiomeSource's own
        // write-never "world_type" field exactly. Plan resolution below is deliberately
        // allow-listed by identity: no typed preset inherits another preset's config merely
        // because both happen to use this wrapper.
        Codec.STRING.optionalFieldOf("world_type").forGetter(generator -> Optional.<String>empty())
    ).apply(instance, EnvelopedChunkGenerator::resolve));

    private final ChunkGenerator delegate;
    private final Dimension dimension;
    /**
     * Not {@code final}: Phase 5c's soft-void border (GOAL 38) needs the boundary radius to
     * change while the world runs, read from worldgen worker threads. A plain volatile
     * reference is enough -- every read site already re-reads {@code this.envelope} fresh per
     * call rather than caching it across a chunk's generation, and swapping in a whole new
     * immutable {@link ExteriorPlan.DimensionEnvelope} instance is inherently atomic. No
     * {@code SavedData} reads happen here; {@link #setEnvelope} is called from the main
     * server thread by whatever schedule driver ends up owning the live radius.
     */
    private volatile ExteriorPlan.DimensionEnvelope envelope;
    /**
     * Cached from whichever of {@link #applyCarvers}/{@link #buildSurface}/{@link #fillFromNoise}
     * runs first for this generator (DESIGN §28.4's natural-biome floating islands): {@link
     * #applyBiomeDecoration} runs later in vanilla's own chunk-status pipeline but is never handed
     * a {@link RandomState} directly, unlike every other override here, so {@link
     * #skyIslandHitAtForTerrain} needs a fallback source for the real seed's {@code
     * Climate.Sampler}. A plain volatile reference is enough for the same reason {@link #envelope}
     * is: this generator resolves one seed for its whole lifetime, so whichever call wins the
     * race writes the same value any other would have.
     */
    private volatile RandomState cachedRandomState;
    /**
     * A narrow corridor's width constraint (GOALS 32), layered additively on top of
     * {@link #envelope} rather than replacing it -- see DESIGN §23. Static for the corridor's
     * whole lifetime, unlike {@link #envelope}: nothing schedules it to change yet.
     */
    private final StripPlan strip;
    private final Optional<StarterLandContext> starterLand;
    private final Optional<LayoutContext> layout;
    private final Optional<LimitedBiomeSource> originSource;
    /**
     * A natural ocean island (GOALS 01, DESIGN §24), read live from {@link #originSource}
     * rather than persisted on this generator's own codec -- {@code LimitedBiomeSource} is
     * already the single source of truth both the biome and terrain code paths share, so
     * there is nothing to keep in sync by duplicating it here.
     */
    private final IslandPlan island;
    /**
     * A true floating island (GOALS 05, DESIGN §27), read live from {@link #originSource} the
     * same way {@link #island} is -- disabled for every preset except {@code sky_island}.
     * Overworld-only, unlike {@link #netherSkyIsland}: this dimension always has a real {@link
     * LimitedBiomeSource} to read from, since only the Overworld ever wraps one.
     */
    private final SkyIslandPlan skyIsland;
    /**
     * The Nether half of a sky island world (GOALS 06, DESIGN §27.6). Unlike {@link #skyIsland},
     * the Nether has no {@code LimitedBiomeSource} to read a live plan from (its biome source is
     * plain vanilla {@code MultiNoiseBiomeSource}), so this is persisted directly on this
     * generator's own codec instead -- mirroring {@link #strip}'s exact precedent (a per-
     * dimension-resolved plan requiring no biome-source involvement at all).
     */
    private final SkyIslandPlan netherSkyIsland;
    /**
     * The real Minecraft world seed for the Nether sky island's footprint shape, set once at
     * {@code ChunkMap} construction (mirrors {@code LimitedBiomeSource.setLayoutSeed}'s timing
     * exactly, DESIGN §27.6) -- there is no {@code LimitedBiomeSource} on this dimension to hold
     * it instead. Irrelevant (never read) for the Overworld instance, which sources its seed from
     * {@link #originSource} unchanged.
     */
    private volatile long netherSkyIslandSeed;
    /**
     * The Overworld's own chunk-island plan (GOALS 09/37, DESIGN §29), read live from {@link
     * #originSource} exactly like {@link #island}/{@link #skyIsland}.
     */
    private final ChunkIslandPlan chunkIsland;
    /**
     * The Nether's or the End's own chunk-island plan, persisted directly on this generator's
     * own codec -- neither dimension has a {@code LimitedBiomeSource} to read a live plan from,
     * mirroring {@link #netherSkyIsland}'s exact precedent, just shared by two dimensions instead
     * of one since chunk islands never need a dimension-specific material palette (DESIGN §29.5).
     */
    private final ChunkIslandPlan nonOverworldChunkIsland;
    /**
     * The real Minecraft world seed for a non-Overworld chunk island's grid (mirrors {@link
     * #netherSkyIslandSeed}'s exact timing/precedent). Irrelevant for the Overworld instance,
     * which sources its seed from {@link #originSource} unchanged.
     */
    private volatile long chunkIslandSeed;
    /**
     * Chunks force-selected as present islands because a seed-search found them naturally
     * showcasing underground content (GOALS 37, DESIGN §29.6) -- lush/dripstone/deep-dark cave
     * biomes or a structure like an ancient city -- resolved once, at world start, by {@code
     * ChunkIslandShowcaseSearch} via {@code WorldLimitManager}, mirroring the guaranteed
     * portal-room cell's own "resolve once, force present" precedent. Coordinates are relative
     * to the origin chunk, matching every other chunk-island coordinate in this class. Always
     * full-column ({@code topOnly} false) regardless of the plan's own setting, since truncating
     * a showcased cave chunk would defeat the point of showcasing it.
     */
    private volatile Set<ChunkPos> chunkIslandShowcaseCells = Set.of();
    /**
     * The Overworld's underground-spawn/sealed-surface/mega-cavern plan (GOALS 25-26, DESIGN
     * §30), persisted directly on this generator's own codec -- mirrors {@link #netherSkyIsland}'s
     * exact precedent (a generator-owned plan needing no biome-source involvement), just for the
     * Overworld instead of the Nether since cave has no Nether/End variant in scope. Threaded
     * through every {@code customized(...)} overload the same way regardless of dimension; only
     * the Overworld instance ever actually uses it (constructor picks based on {@link #dimension}).
     */
    private final CavePlan cave;
    /**
     * The Nether's safe-spawn/starter-chest plan (GOALS 27, DESIGN §31), persisted directly on
     * this generator's own codec -- mirrors {@link #cave}'s exact precedent (a generator-owned
     * plan needing no biome-source involvement), just for the Nether instead of the Overworld
     * since {@code nether_start} leaves the Overworld ordinary vanilla terrain (DESIGN §31.5).
     * Threaded through every {@code customized(...)} overload the same way regardless of
     * dimension; only the Nether instance ever actually uses it (constructor picks based on
     * {@link #dimension}).
     */
    private final NetherStartPlan netherStart;
    /**
     * The End's guaranteed safe-spawn/starter-chest plan (GOALS 34, DESIGN §32), persisted
     * directly on this generator's own codec -- mirrors {@link #netherStart}'s exact precedent
     * (a generator-owned plan needing no biome-source involvement), just for the End instead of
     * the Nether since {@code end_start} leaves both the Overworld and the Nether ordinary
     * vanilla terrain (DESIGN §32.3). Threaded through every {@code customized(...)} overload the
     * same way regardless of dimension; only the End instance ever actually uses it (constructor
     * picks based on {@link #dimension}).
     */
    private final EndStartPlan endStart;
    /**
     * The Overworld's classic-flat plan (GOAL 15, DESIGN §33.2), persisted directly on this
     * generator's own codec -- mirrors {@link #cave}'s exact precedent (a generator-owned plan
     * needing no biome-source involvement), just for the flat-fill shape instead of a spawn/
     * chest mechanism. Threaded through every {@code customized(...)} overload the same way
     * regardless of dimension; only the Overworld instance ever actually uses it (constructor
     * picks based on {@link #dimension}).
     */
    private final FlatPlan flat;
    /**
     * The Overworld's deep-flat plan (GOAL 16, DESIGN §33.4), persisted directly on this
     * generator's own codec -- mirrors {@link #flat}'s exact precedent, just capping real
     * generated terrain to a flat surface instead of replacing it outright.
     */
    private final DeepFlatPlan deepFlat;
    /**
     * The Overworld's stacked-biome-layers plan (GOAL 35, DESIGN §34.1), persisted directly on
     * this generator's own codec -- mirrors {@link #flat}'s exact precedent. Unlike every other
     * generator-owned plan, this one is also pushed onto {@link #originSource} (a live,
     * non-codec setter, {@link LimitedBiomeSource#setStackedLayers}) right after {@link
     * #originSource} itself is resolved below -- real per-Y biome reporting can only happen on
     * {@code LimitedBiomeSource}, since vanilla always asks it for a column's biome, never this
     * generator directly (DESIGN §34.3).
     */
    private final StackedPlan stacked;

    private EnvelopedChunkGenerator(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart,
        EndStartPlan endStart,
        FlatPlan flat,
        DeepFlatPlan deepFlat,
        StackedPlan stacked
    ) {
        super(delegate.getBiomeSource());
        this.delegate = delegate;
        this.dimension = dimension;
        this.envelope = resolveEnvelope(delegate, dimension, envelope);
        this.strip = strip;
        this.starterLand = resolveStarterLand(delegate, dimension);
        this.layout = resolveLayout(delegate, dimension);
        this.originSource = dimension == Dimension.OVERWORLD && delegate.getBiomeSource() instanceof LimitedBiomeSource source
            ? Optional.of(source)
            : Optional.empty();
        this.island = this.originSource.map(LimitedBiomeSource::island).orElse(IslandPlan.disabled());
        this.skyIsland = this.originSource.map(LimitedBiomeSource::skyIsland).orElse(SkyIslandPlan.disabled());
        this.netherSkyIsland = dimension == Dimension.NETHER ? netherSkyIsland : SkyIslandPlan.disabled();
        this.chunkIsland = this.originSource.map(LimitedBiomeSource::chunkIsland).orElse(ChunkIslandPlan.disabled());
        this.nonOverworldChunkIsland = dimension != Dimension.OVERWORLD ? nonOverworldChunkIsland : ChunkIslandPlan.disabled();
        this.cave = dimension == Dimension.OVERWORLD ? cave : CavePlan.disabled();
        this.netherStart = dimension == Dimension.NETHER ? netherStart : NetherStartPlan.disabled();
        this.endStart = dimension == Dimension.END ? endStart : EndStartPlan.disabled();
        this.flat = dimension == Dimension.OVERWORLD ? flat : FlatPlan.disabled();
        this.deepFlat = dimension == Dimension.OVERWORLD ? deepFlat : DeepFlatPlan.disabled();
        this.stacked = dimension == Dimension.OVERWORLD ? stacked : StackedPlan.disabled();
        this.originSource.ifPresent(source -> source.setStackedLayers(this.stacked));
        // GOAL 42, DESIGN §37.3: flat's own underground band, same non-codec setter precedent as
        // setStackedLayers just above. Harmless no-op (blank id) for every non-flat preset and for
        // flat with the band left unconfigured, since FlatPlan.disabled()/an unconfigured
        // FlatConfig both default undergroundBiome to "".
        this.originSource.ifPresent(source -> source.setFlatUnderground(
            this.flat.undergroundBiome(), FlatConfig.OVERWORLD_MIN_Y + this.flat.totalHeightBlocks(),
            this.flat.undergroundBelowSurfaceBlocks()
        ));
    }

    /**
     * Returns the sky island plan actually active for this dimension instance: the Overworld's
     * own (read live from {@link #originSource}) or the Nether's (persisted directly on this
     * generator). Exactly one of {@link #skyIsland}/{@link #netherSkyIsland} is ever enabled for
     * a given instance, since each is only ever populated for its own dimension.
     */
    private SkyIslandPlan activeSkyIsland() {
        return this.dimension == Dimension.OVERWORLD ? this.skyIsland : this.netherSkyIsland;
    }

    /**
     * Returns the sky island plan active for this dimension (GOALS 05/06, DESIGN §27.6),
     * disabled for every other preset.
     *
     * @return resolved sky island plan for this dimension
     */
    public SkyIslandPlan skyIsland() {
        return activeSkyIsland();
    }

    /**
     * Returns the chunk-island plan actually active for this dimension instance: the Overworld's
     * own (read live from {@link #originSource}) or the Nether's/End's (persisted directly on
     * this generator) -- see {@link #chunkIsland}/{@link #nonOverworldChunkIsland}.
     */
    private ChunkIslandPlan activeChunkIsland() {
        return this.dimension == Dimension.OVERWORLD ? this.chunkIsland : this.nonOverworldChunkIsland;
    }

    /**
     * Returns the chunk-island plan active for this dimension (GOALS 09/37, DESIGN §29),
     * disabled for every other preset.
     *
     * @return resolved chunk island plan for this dimension
     */
    public ChunkIslandPlan chunkIsland() {
        return activeChunkIsland();
    }

    /**
     * Returns the cave plan active for this dimension (GOALS 25-26, DESIGN §30), disabled for
     * every other preset and for every non-Overworld instance.
     *
     * @return resolved cave plan
     */
    public CavePlan cave() {
        return this.cave;
    }

    /**
     * Returns the Nether-start plan active for this dimension (GOALS 27, DESIGN §31), disabled
     * for every other preset and for every non-Nether instance.
     *
     * @return resolved Nether-start plan
     */
    public NetherStartPlan netherStart() {
        return this.netherStart;
    }

    /**
     * Returns the End-start plan active for this dimension (GOALS 34, DESIGN §32), disabled for
     * every other preset and for every non-End instance.
     *
     * @return resolved End-start plan
     */
    public EndStartPlan endStart() {
        return this.endStart;
    }

    /**
     * Returns the flat plan active for this dimension (GOAL 15, DESIGN §33.2), disabled for every
     * other preset and for every non-Overworld instance.
     *
     * @return resolved flat plan
     */
    public FlatPlan flat() {
        return this.flat;
    }

    /**
     * Returns the deep-flat plan active for this dimension (GOAL 16, DESIGN §33.4), disabled for
     * every other preset and for every non-Overworld instance.
     *
     * @return resolved deep-flat plan
     */
    public DeepFlatPlan deepFlat() {
        return this.deepFlat;
    }

    /**
     * Returns the stacked-biome-layers plan active for this dimension (GOAL 35, DESIGN §34.1),
     * disabled for every other preset and for every non-Overworld instance.
     *
     * @return resolved stacked plan
     */
    public StackedPlan stacked() {
        return this.stacked;
    }

    /**
     * Resolves the real Minecraft world seed for a non-Overworld chunk island's grid. Mirrors
     * {@link #setSkyIslandSeed(long)}'s timing exactly -- called from the same {@code
     * ChunkMapMixin} injection, once per level load.
     *
     * @param seed the real Minecraft world seed
     */
    public void setChunkIslandSeed(long seed) {
        this.chunkIslandSeed = seed;
    }

    /**
     * Returns the real Minecraft world seed for {@link #activeChunkIsland()}'s grid: the
     * Overworld shares {@code LimitedBiomeSource}'s biome-classification seed exactly; the
     * Nether/End have no such source, so they use {@link #chunkIslandSeed} instead.
     */
    private long chunkIslandSeed() {
        return this.dimension == Dimension.OVERWORLD
            ? this.originSource.orElseThrow().effectiveLayoutPlan().seed()
            : this.chunkIslandSeed;
    }

    /**
     * Resolves the real Minecraft world seed for the Nether sky island's footprint shape.
     * Mirrors {@code LimitedBiomeSource.setLayoutSeed(long)}'s timing exactly -- called from the
     * same {@code ChunkMapMixin} injection, once per level load.
     *
     * @param seed the real Minecraft world seed
     */
    public void setSkyIslandSeed(long seed) {
        this.netherSkyIslandSeed = seed;
    }

    /**
     * Returns the current layout origin's X coordinate (see {@code SpawnOriginManager}).
     *
     * @return origin block X, {@code 0} unless a {@code PREFERRED_NATURAL_BIOME}
     *     search has resolved one for this Overworld
     */
    private int originX() {
        return this.originSource.map(LimitedBiomeSource::originBlockX).orElse(0);
    }

    /**
     * Returns the current layout origin's Z coordinate (see {@code SpawnOriginManager}).
     *
     * @return origin block Z, {@code 0} unless a {@code PREFERRED_NATURAL_BIOME}
     *     search has resolved one for this Overworld
     */
    private int originZ() {
        return this.originSource.map(LimitedBiomeSource::originBlockZ).orElse(0);
    }

    /**
     * Wraps a generator with an explicit envelope selected during world creation, and no
     * strip-world corridor.
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param overworld whether this is the Overworld rather than the Nether
     * @param envelope resolved terrain envelope
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        boolean overworld,
        ExteriorPlan.DimensionEnvelope envelope
    ) {
        return customized(delegate, overworld, envelope, StripPlan.disabled());
    }

    /**
     * Wraps a generator with an explicit envelope and strip-world plan selected during world
     * creation.
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param overworld whether this is the Overworld rather than the Nether
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        boolean overworld,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip
    ) {
        return customized(delegate, overworld, envelope, strip, SkyIslandPlan.disabled());
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, and Nether sky island plan
     * selected during world creation (GOALS 06, DESIGN §27.6).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param overworld whether this is the Overworld rather than the Nether
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for the Overworld instance
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        boolean overworld,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland
    ) {
        return customized(
            delegate, overworld ? Dimension.OVERWORLD : Dimension.NETHER, envelope, strip, netherSkyIsland,
            ChunkIslandPlan.disabled()
        );
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan, and
     * chunk-island plan selected during world creation (GOALS 09/37, DESIGN §29). The only
     * overload that accepts an explicit {@link Dimension} rather than an {@code overworld}
     * boolean, since this is the one call site that also needs to wrap {@code LevelStem.END}
     * for the first time (DESIGN §29.5) -- every other preset editor only ever wraps
     * Overworld/Nether and keeps using the narrower boolean-based overloads above.
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland
    ) {
        return customized(delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, CavePlan.disabled());
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, and cave plan selected during world creation (GOALS 25-26, DESIGN §30).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave
    ) {
        return customized(delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, NetherStartPlan.disabled());
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, cave plan, Nether-start plan, and End-start plan selected during world
     * creation (GOALS 34, DESIGN §32).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param netherStart resolved Nether-start plan, disabled for every other preset and ignored
     *     entirely for non-Nether instances
     * @param endStart resolved End-start plan, disabled for every other preset and ignored
     *     entirely for non-End instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart,
        EndStartPlan endStart
    ) {
        return customized(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, endStart,
            FlatPlan.disabled()
        );
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, cave plan, Nether-start plan, End-start plan, and flat plan selected
     * during world creation (GOAL 15, DESIGN §33.2).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param netherStart resolved Nether-start plan, disabled for every other preset and ignored
     *     entirely for non-Nether instances
     * @param endStart resolved End-start plan, disabled for every other preset and ignored
     *     entirely for non-End instances
     * @param flat resolved flat plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart,
        EndStartPlan endStart,
        FlatPlan flat
    ) {
        return customized(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, endStart, flat,
            DeepFlatPlan.disabled()
        );
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, cave plan, Nether-start plan, End-start plan, flat plan, and deep-flat
     * plan selected during world creation (GOAL 16, DESIGN §33.4).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param netherStart resolved Nether-start plan, disabled for every other preset and ignored
     *     entirely for non-Nether instances
     * @param endStart resolved End-start plan, disabled for every other preset and ignored
     *     entirely for non-End instances
     * @param flat resolved flat plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param deepFlat resolved deep-flat plan, disabled for every other preset and ignored
     *     entirely for non-Overworld instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart,
        EndStartPlan endStart,
        FlatPlan flat,
        DeepFlatPlan deepFlat
    ) {
        return customized(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, endStart, flat, deepFlat,
            StackedPlan.disabled()
        );
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, cave plan, Nether-start plan, End-start plan, flat plan, deep-flat plan,
     * and stacked plan selected during world creation (GOAL 35, DESIGN §34.1).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param netherStart resolved Nether-start plan, disabled for every other preset and ignored
     *     entirely for non-Nether instances
     * @param endStart resolved End-start plan, disabled for every other preset and ignored
     *     entirely for non-End instances
     * @param flat resolved flat plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param deepFlat resolved deep-flat plan, disabled for every other preset and ignored
     *     entirely for non-Overworld instances
     * @param stacked resolved stacked plan, disabled for every other preset and ignored entirely
     *     for non-Overworld instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart,
        EndStartPlan endStart,
        FlatPlan flat,
        DeepFlatPlan deepFlat,
        StackedPlan stacked
    ) {
        return new EnvelopedChunkGenerator(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, endStart, flat, deepFlat,
            stacked
        );
    }

    /**
     * Wraps a generator with an explicit envelope, strip-world plan, Nether sky island plan,
     * chunk-island plan, cave plan, and Nether-start plan selected during world creation (GOALS
     * 27, DESIGN §31).
     *
     * @param delegate vanilla or modded generator to delegate to
     * @param dimension which dimension this instance wraps
     * @param envelope resolved terrain envelope
     * @param strip resolved strip-world corridor plan
     * @param netherSkyIsland resolved Nether sky island plan, disabled for every other preset
     *     and ignored entirely for non-Nether instances
     * @param nonOverworldChunkIsland resolved Nether/End chunk-island plan, disabled for every
     *     other preset and ignored entirely for the Overworld instance
     * @param cave resolved cave plan, disabled for every other preset and ignored entirely for
     *     non-Overworld instances
     * @param netherStart resolved Nether-start plan, disabled for every other preset and ignored
     *     entirely for non-Nether instances
     * @return delegating generator
     */
    public static EnvelopedChunkGenerator customized(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland,
        ChunkIslandPlan nonOverworldChunkIsland,
        CavePlan cave,
        NetherStartPlan netherStart
    ) {
        return customized(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, EndStartPlan.disabled()
        );
    }

    /**
     * Returns the wrapped generator.
     *
     * @return delegated generator
     */
    public ChunkGenerator delegate() {
        return this.delegate;
    }

    /**
     * Returns the currently active envelope (not necessarily what was persisted at world
     * creation -- see {@link #setEnvelope}).
     *
     * @return live dimension envelope
     */
    public ExteriorPlan.DimensionEnvelope envelope() {
        return this.envelope;
    }

    /**
     * Replaces the live envelope, changing the exterior boundary radius newly generated
     * chunks will use from this point on. Does not touch chunks that already exist -- see
     * TODO Phase 5c for the (currently unimplemented, spike-only) sweep/backfill machinery
     * that would need to run alongside this for already-generated terrain to actually follow
     * the new radius.
     *
     * @param envelope replacement envelope
     */
    public void setEnvelope(ExteriorPlan.DimensionEnvelope envelope) {
        this.envelope = envelope;
    }

    /**
     * Returns the strip-world corridor plan (GOALS 32), disabled unless this world is a strip.
     *
     * @return strip plan
     */
    public StripPlan strip() {
        return this.strip;
    }

    /**
     * Classifies a column, combining the strip-world width constraint (if enabled) with the
     * square envelope -- the strip's own verdict wins whenever it applies, since a narrow
     * corridor's width is normally much smaller than any configured square boundary (DESIGN
     * §23). Callers pass coordinates already relative to the origin.
     *
     * @param relativeX block X relative to the origin
     * @param relativeZ block Z relative to the origin
     * @return terrain to generate at the column
     */
    private ExteriorMode effectiveModeAt(int relativeX, int relativeZ) {
        if (activeSkyIsland().enabled()) {
            // Uniformly VOID both inside and outside the footprint (DESIGN §27.2) -- the
            // slab-vs-void distinction happens one level down, in skyIslandStateAt/
            // skyIslandBaseHeight, exactly like OCEAN varies its seabed depth per column
            // via islandOceanDepthAt without ever needing a second ExteriorMode value.
            return ExteriorMode.VOID;
        }
        if (activeChunkIsland().enabled()) {
            // Unlike every other island-shaped preset, a selected chunk's real vanilla terrain
            // is never replaced (DESIGN §29.1) -- NORMAL lets the delegate/layout/starter-land
            // pipeline run exactly as it would with no Worldz masking at all. Only unselected
            // chunks mask to VOID. The TOP_ONLY depth cutoff is layered on separately, after
            // generation, in applyEnvelope's own additive pass (§29.3) -- no single ExteriorMode
            // can express "NORMAL above a cutoff, VOID below it" in the same column.
            return chunkIslandHitAt(relativeX, relativeZ).present() ? ExteriorMode.NORMAL : ExteriorMode.VOID;
        }
        if (this.island.enabled() && this.island.withinExclusionZone(relativeX, relativeZ)) {
            // Inside the exclusion zone (or it's disabled, the GOALS 01 default): the island
            // interior and its shore ring are real, unmasked generation (NORMAL); only open
            // ocean beyond the shore is masked. Beyond the exclusion zone (GOALS 04), island
            // shaping releases entirely and falls through to strip/envelope below, which stay
            // disabled/normal for this preset -- so natural terrain resumes there. Land-free
            // (GOALS 03, DESIGN §25.2) never has a NORMAL branch here at all -- every column
            // within the exclusion zone is OCEAN. Natural land (GOALS 02, DESIGN §25.4) has no
            // separate shore-ring width -- the real seed's own terrain is left completely
            // unmasked out to radiusBlocks, then ocean begins immediately past it.
            if (!this.island.hasLand()) {
                return ExteriorMode.OCEAN;
            }
            double distance = this.island.distanceFromShore(relativeX, relativeZ, islandSeed());
            int landMaskWidth = this.island.syntheticLand() ? this.island.shoreWidthBlocks() : 0;
            return distance > landMaskWidth ? ExteriorMode.OCEAN : ExteriorMode.NORMAL;
        }
        ExteriorMode stripMode = this.strip.modeAt(relativeZ);
        return stripMode != ExteriorMode.NORMAL ? stripMode : this.envelope.modeAt(relativeX, relativeZ);
    }

    /**
     * Returns whether the square envelope, the strip-world width constraint, or the ocean
     * island changes delegated terrain anywhere in this dimension.
     *
     * @return whether any exterior masking is active
     */
    private boolean hasActiveExterior() {
        return this.envelope.mode() != ExteriorMode.NORMAL || this.strip.enabled()
            || this.island.enabled() || activeSkyIsland().enabled() || activeChunkIsland().enabled()
            || this.cave.enabled();
    }

    /**
     * Classifies one column's containing chunk against {@link #activeChunkIsland()}'s grid.
     *
     * @param relativeX block X relative to the origin
     * @param relativeZ block Z relative to the origin
     * @return the containing chunk's resolved hit
     */
    private ChunkIslandPlan.Hit chunkIslandHitAt(int relativeX, int relativeZ) {
        int chunkX = Math.floorDiv(relativeX, 16);
        int chunkZ = Math.floorDiv(relativeZ, 16);
        if (this.chunkIslandShowcaseCells.contains(new ChunkPos(chunkX, chunkZ))) {
            return new ChunkIslandPlan.Hit(true, false, activeChunkIsland().topOnlyDepthBlocks());
        }
        return activeChunkIsland().at(chunkX, chunkZ, chunkIslandSeed());
    }

    /**
     * Sets the underground-content showcase cells found by {@code ChunkIslandShowcaseSearch}
     * (GOALS 37, DESIGN §29.6), forcing them present regardless of the plan's own hash-picked
     * grid. Called once, at world start, from {@code WorldLimitManager} -- harmless no-op for
     * every other preset (the set stays empty).
     *
     * @param relativeChunkCells cells to force present, relative to the origin chunk
     */
    public void setChunkIslandShowcaseCells(Set<ChunkPos> relativeChunkCells) {
        this.chunkIslandShowcaseCells = relativeChunkCells;
    }

    /**
     * Returns the real Minecraft world seed shared with {@code LimitedBiomeSource}'s biome
     * classification (DESIGN §24.2), already resolved live via {@link #originSource} -- island
     * shaping is Overworld-only, so this is only ever called when {@link #island} is enabled,
     * which itself is only ever true when {@link #originSource} is present.
     */
    private long islandSeed() {
        return this.originSource.orElseThrow().effectiveLayoutPlan().seed();
    }

    /**
     * Returns the real Minecraft world seed for {@link #activeSkyIsland()}'s footprint shape:
     * the Overworld shares {@code LimitedBiomeSource}'s biome-classification seed exactly
     * (only ever called when {@link #originSource} is present); the Nether has no such source
     * to read from, so it uses its own {@link #netherSkyIslandSeed} instead (DESIGN §27.6).
     */
    private long skyIslandSeed() {
        return this.dimension == Dimension.OVERWORLD
            ? this.originSource.orElseThrow().effectiveLayoutPlan().seed()
            : this.netherSkyIslandSeed;
    }

    private static EnvelopedChunkGenerator resolve(
        ChunkGenerator delegate,
        Dimension dimension,
        Optional<ExteriorPlan.DimensionEnvelope> encodedEnvelope,
        Optional<StripPlan> encodedStrip,
        Optional<SkyIslandPlan> encodedNetherSkyIsland,
        Optional<ChunkIslandPlan> encodedChunkIsland,
        Optional<CavePlan> encodedCave,
        Optional<NetherStartPlan> encodedNetherStart,
        Optional<EndStartPlan> encodedEndStart,
        Optional<FlatPlan> encodedFlat,
        Optional<DeepFlatPlan> encodedDeepFlat,
        Optional<StackedPlan> encodedStacked,
        Optional<String> worldType
    ) {
        WorldzConfig sharedConfig = WorldzCommon.config();
        ExteriorPlan defaults = ExteriorPlan.fromConfig(sharedConfig);
        // DESIGN §34.7: this generator's own exterior envelope (drives VOID/OCEAN terrain
        // masking beyond the boundary, independent of the actual WorldBorder object -- see
        // LimitedBiomeSource.resolve's identical stackedDefaults fix for that half) needs the
        // same worldSizeChunks-derived override for a never-customized "select preset, Create
        // World" stacked world, or terrain would keep generating normally past the border even
        // though players can't walk there. TODO 25.5: the override backs off whenever the user
        // has explicitly written their own overworldBorder/overworldExterior section (see
        // StackedConfig#effectiveOverworldExterior), so this path's shared-section presence check
        // must match LimitedBiomeSource.resolve's exactly.
        boolean stackedDefaults = dimension == Dimension.OVERWORLD && worldType.filter("stacked"::equals).isPresent();
        ExteriorPlan.DimensionEnvelope envelope = encodedEnvelope.orElseGet(() -> switch (dimension) {
            case OVERWORLD -> stackedDefaults
                ? ExteriorPlan.DimensionEnvelope.fromConfig(
                    sharedConfig.stacked.effectiveOverworldExterior(
                        sharedConfig.overworldExterior,
                        sharedConfig.present("overworldBorder") || sharedConfig.present("overworldExterior")
                    ),
                    sharedConfig.stacked.effectiveOverworldBorder(sharedConfig.overworldBorder)
                )
                : defaults.overworld();
            case NETHER -> defaults.nether();
            // The End has no config-default exterior of its own (ExteriorPlan is an Overworld/
            // Nether pair only, DESIGN §29.5) -- masking there is fully handled by the chunk-
            // island branch in effectiveModeAt, ahead of this envelope ever being consulted.
            case END -> ExteriorPlan.DimensionEnvelope.normal();
        });
        StripPlan strip = encodedStrip.orElseGet(() -> StripPlan.fromPresetConfig(
            sharedConfig.stripWorld, dimension == Dimension.OVERWORLD, worldType.orElse("")
        ));
        SkyIslandPlan netherSkyIsland = encodedNetherSkyIsland.orElseGet(() -> {
            var skyIslandConfig = WorldzCommon.config().skyIsland;
            return dimension == Dimension.NETHER && skyIslandConfig.applyToNether
                ? SkyIslandPlan.fromConfig(skyIslandConfig)
                : SkyIslandPlan.disabled();
        });
        ChunkIslandPlan nonOverworldChunkIsland = encodedChunkIsland.orElseGet(() -> {
            var chunkIslandConfig = WorldzCommon.config().chunkIsland;
            return switch (dimension) {
                case NETHER -> ChunkIslandPlan.fromConfig(chunkIslandConfig, ChunkIslandPlan.Dimension.NETHER);
                case END -> ChunkIslandPlan.fromConfig(chunkIslandConfig, ChunkIslandPlan.Dimension.END);
                case OVERWORLD -> ChunkIslandPlan.disabled();
            };
        });
        CavePlan cave = encodedCave.orElseGet(
            () -> dimension == Dimension.OVERWORLD && worldType.filter("cave"::equals).isPresent()
                ? CavePlan.fromConfig(WorldzCommon.config().cave)
                : CavePlan.disabled()
        );
        NetherStartPlan netherStart = encodedNetherStart.orElseGet(
            () -> dimension == Dimension.NETHER && worldType.filter("nether_start"::equals).isPresent()
                ? NetherStartPlan.fromConfig(WorldzCommon.config().netherStart)
                : NetherStartPlan.disabled()
        );
        EndStartPlan endStart = encodedEndStart.orElseGet(
            () -> dimension == Dimension.END && worldType.filter("end_start"::equals).isPresent()
                ? EndStartPlan.fromConfig(WorldzCommon.config().endStart)
                : EndStartPlan.disabled()
        );
        FlatPlan flat = encodedFlat.orElseGet(
            () -> dimension == Dimension.OVERWORLD && worldType.filter("flat"::equals).isPresent()
                ? FlatPlan.fromConfig(WorldzCommon.config().flat)
                : FlatPlan.disabled()
        );
        DeepFlatPlan deepFlat = encodedDeepFlat.orElseGet(
            () -> dimension == Dimension.OVERWORLD && worldType.filter("deep_flat"::equals).isPresent()
                ? DeepFlatPlan.fromConfig(WorldzCommon.config().deepFlat)
                : DeepFlatPlan.disabled()
        );
        StackedPlan stacked = encodedStacked.orElseGet(
            () -> dimension == Dimension.OVERWORLD && worldType.filter("stacked"::equals).isPresent()
                ? StackedPlan.fromConfig(WorldzCommon.config().stacked)
                : StackedPlan.disabled()
        );
        return new EnvelopedChunkGenerator(
            delegate, dimension, envelope, strip, netherSkyIsland, nonOverworldChunkIsland, cave, netherStart, endStart, flat, deepFlat,
            stacked
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void validate() {
        this.delegate.validate();
    }

    @Override
    public ChunkGeneratorStructureState createState(
        HolderLookup<StructureSet> structureSets,
        RandomState randomState,
        long legacyLevelSeed
    ) {
        if (this.flat.enabled()) {
            // Mirrors FlatLevelSource.createState exactly (GOAL 15, DESIGN §33.1's correction):
            // an empty structureOverrides list means every registered set is eligible, otherwise
            // only the configured ones -- the same "absent means all" default vanilla flat worlds
            // use, just resolved from FlatPlan's own plain-string ids instead of a HolderSet.
            Stream<Holder<StructureSet>> structures = this.flat.structureOverrides().isEmpty()
                ? structureSets.listElements().map(e -> (Holder<StructureSet>) e)
                : structureSets.listElements()
                    .filter(holder -> this.flat.structureOverrides().contains(structureSetId(holder)))
                    .map(e -> (Holder<StructureSet>) e);
            ChunkGeneratorStructureState state =
                ChunkGeneratorStructureState.createForFlat(randomState, legacyLevelSeed, this.getBiomeSource(), structures);
            // GOALS 15/22 clarification (2026-07-26): an explicitly curated structureOverrides
            // entry can be biome-gated in real vanilla data (e.g. ancient_cities needs
            // deep_dark, DESIGN §33.2's own documented case) while flat.biome never matches --
            // vanilla's own structure placement then silently never places it, with nothing to
            // tell the player why. Only checked for an explicit list, not the empty "every
            // registered set eligible" default, which deliberately mirrors vanilla's own
            // all-eligible default and would otherwise warn on every biome-gated vanilla
            // structure set (~10 of them) on every single default flat world.
            if (!this.flat.structureOverrides().isEmpty()) {
                warnUnreachableFlatStructureOverrides(state.possibleStructureSets());
            }
            return state;
        }
        return this.delegate.createState(structureSets, randomState, legacyLevelSeed);
    }

    private static String structureSetId(Holder<StructureSet> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElseGet(holder::getRegisteredName);
    }

    /**
     * Warns about a {@code flat.structureOverrides} entry that real vanilla data says can never
     * generate under the configured {@code flat.biome} (GOALS 15/22 clarification, 2026-07-26) --
     * entirely derived from real registry data ({@link Structure#biomes()}), never a hardcoded
     * structure-to-biome table, matching every other biome-eligibility check in this project.
     * {@link LimitedBiomeSource#allowedBiomes()} is the raw configured biome set (exactly {@code
     * flat.biome} alone for flat, {@code resolveFlatAllowed}), not the broader, fallback-widened
     * {@link net.minecraft.world.level.biome.BiomeSource#possibleBiomes()}.
     */
    private void warnUnreachableFlatStructureOverrides(List<Holder<StructureSet>> configured) {
        Set<Holder<Biome>> reachable = this.originSource.map(LimitedBiomeSource::allowedBiomes)
            .map(set -> Set.copyOf(set.stream().toList()))
            .orElse(Set.of());
        if (reachable.isEmpty()) {
            // flat.biome itself is unresolvable -- LimitedBiomeSource already warned about that
            // specifically ("Unknown flat.biome '{}'."); don't pile redundant warnings on top.
            return;
        }
        for (Holder<StructureSet> holder : configured) {
            boolean eligible = holder.value().structures().stream()
                .flatMap(entry -> entry.structure().value().biomes().stream())
                .anyMatch(reachable::contains);
            if (!eligible) {
                WorldzCommon.LOGGER.warn(
                    "flat.structureOverrides lists '{}', but it is only ever eligible in biomes flat.biome ('{}') "
                        + "doesn't include; it will never generate. Remove it, or change flat.biome to a biome it's eligible for.",
                    structureSetId(holder), this.flat.biome()
                );
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(
        RandomState randomState,
        Blender blender,
        StructureManager structureManager,
        ChunkAccess protoChunk
    ) {
        return this.delegate.createBiomes(randomState, blender, structureManager, protoChunk);
    }

    @Override
    public void applyCarvers(
        WorldGenRegion region,
        long seed,
        RandomState randomState,
        BiomeManager biomeManager,
        StructureManager structureManager,
        ChunkAccess chunk
    ) {
        // Flat has no carving of any kind (mirrors FlatLevelSource.applyCarvers's own no-op,
        // GOAL 15, DESIGN §33.1's correction) -- skipping the delegate's real cave carvers here
        // is what keeps a flat-fill chunk (built in fillFromNoise) from having real vanilla caves
        // carved into it after the fact.
        if (!this.flat.enabled() && !this.stacked.enabled()) {
            this.delegate.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk);
            applyTerrainAdjustments(chunk, randomState, true);
        }
        applyEnvelope(chunk, randomState);
    }

    @Override
    public void buildSurface(
        WorldGenRegion level,
        StructureManager structureManager,
        RandomState randomState,
        ChunkAccess protoChunk
    ) {
        // Same reasoning as applyCarvers: skip the delegate's real biome-specific surface rules
        // (sand/grass placement etc.) for flat -- fillFromNoise's own flat-fill is already the
        // final surface, mirroring FlatLevelSource.buildSurface's own no-op.
        if (!this.flat.enabled() && !this.stacked.enabled()) {
            this.delegate.buildSurface(level, structureManager, randomState, protoChunk);
        }
        if (this.deepFlat.enabled()) {
            // Runs right after the delegate's own real buildSurface call (GOAL 16, DESIGN
            // §33.4): late enough that real terrain/caves/surface materials already exist below
            // the cap, early enough that biome decoration (trees, grass) plants on the fresh
            // capped surface next, not the hidden original one.
            applyDeepFlatCap(level, protoChunk);
        }
        applyEnvelope(protoChunk, randomState);
    }

    /**
     * Caps real, unmodified terrain to a flat surface (GOAL 16, DESIGN §33.4): clears everything
     * at or above {@link DeepFlatPlan#surfaceY()} to air, paints the cap layer stack immediately
     * below it -- a water fill instead of the land cap where the real biome is a river/ocean
     * (unless within {@link DeepFlatPlan#riverExclusionRadiusBlocks()} of the origin, or {@link
     * DeepFlatPlan#riversEnabled()} is off). Every column also gets {@link #sealBeneathCap} run
     * immediately below the band, so a shallow natural pond/lake doesn't punch a hole through the
     * flat grass surface, and a river/ocean's freshly painted water doesn't drain straight down
     * into a real cave breach sitting right beneath it. Everything beyond that bounded seal is
     * untouched real terrain: caves, cave biomes, aquifers, ores, and structures at their natural
     * depth.
     */
    private void applyDeepFlatCap(WorldGenRegion level, ChunkAccess chunk) {
        DeepFlatPlan plan = this.deepFlat;
        List<BlockState> capStates = flatLayerStates(plan.capLayers());
        int capThickness = capStates.size();
        int surfaceY = plan.surfaceY();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int localX = x - chunkPos.getMinBlockX();
                int localZ = z - chunkPos.getMinBlockZ();
                boolean waterCap = plan.riversEnabled()
                    && !withinDeepFlatRiverExclusion(x, z, plan)
                    && isRiverOrOceanBiomeAt(level, x, surfaceY, z);

                for (int y = surfaceY; y <= chunk.getMaxY(); y++) {
                    setBlockIfDifferent(chunk, pos.set(x, y, z), air);
                }
                for (int i = 0; i < capThickness; i++) {
                    int y = surfaceY - capThickness + i;
                    if (y < chunk.getMinY()) {
                        continue;
                    }
                    BlockState state = waterCap ? water : capStates.get(i);
                    setBlockIfDifferent(chunk, pos.set(x, y, z), state);
                    // Heightmap.update takes chunk-LOCAL x/z (0-15, packed via x + z*16 into a
                    // 256-slot bit storage) unlike ChunkAccess.setBlockState above, which accepts
                    // absolute world coordinates -- passing the same absolute x/z here (crash
                    // found 2026-07-26, Jason's real config 69 retest at chunk (-2,-2)) produces a
                    // wildly out-of-range bit-storage index the moment either coordinate is
                    // negative or >= 16, throwing IllegalArgumentException.
                    oceanFloor.update(localX, y, localZ, state);
                    worldSurface.update(localX, y, localZ, state);
                }
                sealBeneathCap(chunk, pos, x, z, surfaceY - capThickness);
            }
        }
    }

    /** How far below the painted cap band {@link #sealBeneathCap} will patch an open pocket. */
    private static final int SEAL_DEPTH_BLOCKS = 8;

    /**
     * Two bugs found and fixed 2026-07-26, Jason's real config 69 retest, both from the same root
     * cause: {@link #applyDeepFlatCap}'s band paint only ever overwrites its own configured
     * thickness and never checked what's immediately beneath it. First, a small natural pond/lake
     * (unrelated to any river/ocean biome -- just ordinary terrain-noise low ground) whose real
     * depth dipped below the shallow default land cap (4 blocks, {@code dirt:3}+{@code
     * grass_block:1}) punched a visible hole with real water straight through the otherwise-flat
     * grass surface. Second -- confirming DESIGN §33.4's previously-only-theoretical "water
     * draining into caves" gap -- a river/ocean's water-cap band, painted directly above whatever
     * real terrain already existed there, drained straight down via ordinary fluid physics
     * whenever a real cave breach sat right beneath it. Continues downward from directly below
     * the freshly painted band (either kind), replacing any immediately-connected open pocket (air
     * or a liquid) with solid stone until hitting the first genuinely solid block, bounded to
     * {@link #SEAL_DEPTH_BLOCKS} so real caves still start a little further down exactly as GOAL
     * 16 intends ("dig through the cap... into real stone, then real caves eventually appear") --
     * a deeper real cave or aquifer beyond that bound is left completely untouched, so a river or
     * ocean sitting over a genuinely deep cave system still floods it, same as any ordinary body
     * of water in vanilla; this only closes the immediate, cap-boundary-adjacent breach.
     */
    private static void sealBeneathCap(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int z, int bandBottomY) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        int minY = Math.max(chunk.getMinY(), bandBottomY - SEAL_DEPTH_BLOCKS);
        for (int y = bandBottomY - 1; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return;
            }
            setBlockIfDifferent(chunk, pos, stone);
        }
    }

    private static void setBlockIfDifferent(ChunkAccess chunk, BlockPos.MutableBlockPos pos, BlockState state) {
        BlockState oldState = chunk.getBlockState(pos);
        if (oldState != state) {
            if (oldState.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }
            chunk.setBlockState(pos, state, 0);
        }
    }

    private static boolean isRiverOrOceanBiomeAt(WorldGenRegion level, int x, int y, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        return biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN);
    }

    /** Shared by {@link #applyDeepFlatCap} and {@link #deepFlatWaterCapAt} so the two never disagree. */
    private boolean withinDeepFlatRiverExclusion(int x, int z, DeepFlatPlan plan) {
        long dx = x - originX();
        long dz = z - originZ();
        long exclusionRadiusSq = (long) plan.riverExclusionRadiusBlocks() * plan.riverExclusionRadiusBlocks();
        return dx * dx + dz * dz <= exclusionRadiusSq;
    }

    /**
     * Reimplements {@link #applyDeepFlatCap}'s own water-cap test for {@link #getBaseHeight}/
     * {@link #getBaseColumn} (GOAL 22 fix, 2026-07-26 Jason's real config 71 test), which only
     * ever get a {@link RandomState}, not the {@link WorldGenRegion} the paint pass reads the
     * already-generated biome from. Sampling the delegate's own biome source directly at this
     * column (same mechanism {@link #skyIslandHitAtForTerrain} already established) gives the same
     * answer {@code applyDeepFlatCap} will paint later in the pipeline, since {@code createBiomes}
     * has already run by the time either query lands.
     */
    private boolean deepFlatWaterCapAt(int x, int z, DeepFlatPlan plan, RandomState randomState) {
        if (!plan.riversEnabled() || withinDeepFlatRiverExclusion(x, z, plan)) {
            return false;
        }
        int quartX = QuartPos.fromBlock(x);
        int quartZ = QuartPos.fromBlock(z);
        int quartY = QuartPos.fromBlock(plan.surfaceY());
        Holder<Biome> biome = this.delegate.getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, randomState.sampler());
        return biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN);
    }

    /**
     * Mirrors {@link #flatBaseHeight} for deep-flat's own cap band (GOAL 22 fix, 2026-07-26
     * Jason's real config 71 test): {@code getBaseHeight} previously fell straight through to the
     * delegate's real, pre-cap terrain height (DESIGN §33.4's original "acceptable first-pass gap"
     * reasoning) -- harmless for underground structures (their placement never consults surface
     * height at all), but wrong for any surface-anchored structure (desert pyramid, shipwreck,
     * etc.), which vanilla places by querying exactly this method. Reading the real, uncapped
     * height meant that wherever it differed from {@code surfaceY}, the structure generated at the
     * wrong height entirely -- floating above the flat cap, or sunk through it -- since the cap
     * itself paints purely by absolute Y afterward with no knowledge of where a structure landed.
     * A land-capped column's topmost cap block (the last {@code capLayers} entry, ordinarily
     * grass) is solid under every {@link Heightmap.Types} predicate including {@code OCEAN_FLOOR}'s
     * opaque-only test, so it always resolves at {@code surfaceY} regardless of {@code type}. A
     * water-capped column only resolves there for predicates that count a fluid as blocking
     * ({@code WORLD_SURFACE}, {@code MOTION_BLOCKING}); an opaque-only predicate has to fall
     * through the whole water band to the real solid ground immediately beneath it, which {@link
     * #sealBeneathCap} guarantees is solid and fluid-free right at {@code surfaceY - capThickness}.
     */
    private static int deepFlatBaseHeight(DeepFlatPlan plan, boolean waterCap, Heightmap.Types type) {
        int surfaceY = plan.surfaceY();
        if (!waterCap) {
            return surfaceY;
        }
        if (type.isOpaque().test(Blocks.WATER.defaultBlockState())) {
            return surfaceY;
        }
        int capThickness = flatLayerStates(plan.capLayers()).size();
        return surfaceY - capThickness;
    }

    /**
     * Mirrors {@link #flatBaseColumn} for deep-flat: same reasoning as {@link
     * #deepFlatBaseHeight}, but for the full-column callers ({@code getBaseColumn}) rather than
     * just the topmost height. Starts from the delegate's real column (so caves/ores/structures
     * below the cap still read correctly for any caller that inspects the whole column, not just
     * its top) and overlays exactly what {@link #applyDeepFlatCap} will later paint: air at and
     * above {@code surfaceY}, the cap band (land or water) immediately below it. Left deliberately
     * unaware of {@link #sealBeneathCap}'s own bounded patch beneath the band -- a real cave breach
     * sitting there is already a documented, accepted gap (DESIGN §33.4's "known, accepted gaps"),
     * and this method only needs to agree with the *painted* surface, not every runtime patch.
     */
    private NoiseColumn deepFlatBaseColumn(DeepFlatPlan plan, int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        NoiseColumn natural = this.delegate.getBaseColumn(x, z, heightAccessor, randomState);
        BlockState[] states = copyColumn(natural, heightAccessor);
        int minY = heightAccessor.getMinY();
        int surfaceY = plan.surfaceY();
        boolean waterCap = deepFlatWaterCapAt(x, z, plan, randomState);
        List<BlockState> capStates = flatLayerStates(plan.capLayers());
        int capThickness = capStates.size();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int y = surfaceY; y <= heightAccessor.getMaxY(); y++) {
            int index = y - minY;
            if (index >= 0 && index < states.length) {
                states[index] = air;
            }
        }
        for (int i = 0; i < capThickness; i++) {
            int y = surfaceY - capThickness + i;
            int index = y - minY;
            if (index >= 0 && index < states.length) {
                states[index] = waterCap ? water : capStates.get(i);
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();
        // isEntirelyExteriorOcean already implies isEntirelyExterior (OCEAN != NORMAL).
        boolean decorateExteriorOcean = decoratesExteriorOcean(chunkPos);
        if (!isEntirelyExterior(chunkPos) || decorateExteriorOcean) {
            if (!this.flat.enabled() || this.flat.decoration()) {
                // Flat's own decoration toggle (GOAL 15, mirrors FlatLevelGeneratorSettings.
                // decoration's all-or-nothing behavior) gates the delegate's real biome decoration
                // entirely -- trees/ore veins/etc. still come from the real (single, fixed)
                // biome's own feature list when enabled, same mechanism every other preset already
                // uses.
                this.delegate.applyBiomeDecoration(level, chunk, structureManager);
            } else {
                // Bug found 2026-07-26 (Jason's first real in-game test of config 66, GOAL 15):
                // the all-or-nothing skip above, when decoration is off (the default), silently
                // dropped *structure placement* too -- vanilla's own applyBiomeDecoration
                // interleaves ordinary per-biome feature decoration with the actual block-writing
                // for every structure whose start was already resolved by the earlier
                // STRUCTURE_STARTS pass (confirmed against real decompiled source:
                // ChunkGenerator.applyBiomeDecoration calls structureManager.startsForStructure(...)
                // .forEach(start -> start.placeInChunk(...)) directly alongside its biome-feature
                // loop, not gated by any decoration flag of its own). Confirmed directly against a
                // real Worldz-66 save: village structure *starts* existed and every touched chunk
                // had reached `full` status, but every section was still pure flat-layer palette --
                // no village block was ever written. Vanilla's own FlatLevelSource never overrides
                // applyBiomeDecoration at all, so vanilla flat worlds always place structures
                // regardless of their own decoration setting -- placeStructuresOnly replicates just
                // that half of the vanilla method (DESIGN §36's "faithful reimplementation using
                // only public API" precedent, mirrors createStructuresRespectingDistance), skipping
                // only the ordinary per-biome feature loop this toggle is actually meant to control.
                placeStructuresOnly(level, chunk, structureManager);
            }
        }
        // Stacked (GOAL 35, DESIGN §34.4): the delegate call above already runs real vanilla
        // decoration correctly for every fixed-Y-range feature (ore veins, etc.) once biome
        // varies by Y (DESIGN §34.3) -- only heightmap-based features (trees) need this extra
        // pass, since a chunk's persisted heightmap can only ever report the topmost layer.
        if (this.stacked.enabled() && (!isEntirelyExterior(chunkPos) || decorateExteriorOcean)) {
            applyStackedBuriedDecoration(level, chunk, resolvedStackedLayers());
        }
        // Re-painting the exterior profile here would immediately erase whatever decoration
        // (kelp, seagrass, structure pieces) just placed -- skip it for a chunk we deliberately
        // decorated; the earlier applyCarvers/buildSurface passes already shaped its terrain.
        if (!decorateExteriorOcean) {
            applyEnvelope(chunk, this.cachedRandomState);
            if (activeSkyIsland().enabled()) {
                applyFloatingIslandOre(level, chunk);
                applyFloatingIslandLoot(level, chunk);
            }
            if (activeChunkIsland().enabled()) {
                applyChunkIslandGeode(level, chunk);
            }
        }
    }

    /**
     * Faithful reimplementation of just the structure-placement half of vanilla's {@code
     * ChunkGenerator.applyBiomeDecoration} using only public API (mirrors this class's own {@code
     * createStructuresRespectingDistance}/{@code tryGenerateRestrictedStructure} precedent, DESIGN
     * §36) -- writes {@code StructureStart.placeInChunk} for every structure whose start the
     * earlier STRUCTURE_STARTS pass already resolved, skipping vanilla's own per-biome ordinary
     * feature-decoration loop entirely. Used only when {@link FlatPlan#enabled()} is true and
     * {@link FlatPlan#decoration()} is false (GOAL 15's classic-flat "Decoration" toggle, off by
     * default): that toggle is meant to suppress trees/ore veins/etc. only, not structures --
     * vanilla's own {@code FlatLevelSource} never overrides {@code applyBiomeDecoration} at all, so
     * an ordinary vanilla flat world always places structures regardless of its own decoration
     * setting.
     */
    private void placeStructuresOnly(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos centerPos = chunk.getPos();
        if (SharedConstants.debugVoidTerrain(centerPos) || !structureManager.shouldGenerateStructures()) {
            return;
        }
        SectionPos sectionPos = SectionPos.of(centerPos, level.getMinSectionY());
        BlockPos origin = sectionPos.origin();
        Registry<Structure> structuresRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Map<Integer, List<Structure>> structuresByStep = structuresRegistry.stream()
            .collect(Collectors.groupingBy(structure -> structure.step().ordinal()));
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
        long decorationSeed = random.setDecorationSeed(level.getSeed(), origin.getX(), origin.getZ());
        BoundingBox writableArea = writableArea(chunk);
        for (int stepIndex = 0; stepIndex < GenerationStep.Decoration.values().length; stepIndex++) {
            int index = 0;
            for (Structure structure : structuresByStep.getOrDefault(stepIndex, Collections.emptyList())) {
                random.setFeatureSeed(decorationSeed, index, stepIndex);
                structureManager.startsForStructure(sectionPos, structure)
                    .forEach(start -> start.placeInChunk(level, structureManager, this, random, writableArea, centerPos));
                index++;
            }
        }
    }

    /** Mirrors vanilla's own private {@code ChunkGenerator.getWritableArea}, unreachable from a subclass. */
    private static BoundingBox writableArea(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int targetBlockX = chunkPos.getMinBlockX();
        int targetBlockZ = chunkPos.getMinBlockZ();
        LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
        int minY = heightAccessor.getMinY() + 1;
        int maxY = heightAccessor.getMaxY();
        return new BoundingBox(targetBlockX, minY, targetBlockZ, targetBlockX + 15, maxY, targetBlockZ + 15);
    }

    /**
     * Scatters each buried layer's own biome's {@code VEGETAL_DECORATION} features directly into
     * its air gap (GOAL 35, DESIGN §34.4) -- the topmost layer needs nothing here, since its real
     * heightmap-based decoration already ran correctly in the delegate call above. Calls {@link
     * ConfiguredFeature#place} directly (verified real, public, and placement-modifier-free,
     * DESIGN §34.4) rather than {@code PlacedFeature.placeWithBiomeCheck}, since a heightmap-based
     * placement modifier would otherwise snap back to the chunk's real (topmost) surface --
     * exactly the mechanical limit this bypass exists to work around. Not an attempt at full
     * per-feature placement-modifier fidelity (scatter density is fixed, {@link
     * #BURIED_LAYER_DECORATION_ATTEMPTS}), matching {@link #placeOreFeature}'s own precedent for
     * a direct, exact-position feature placement elsewhere in this class.
     */
    private void applyStackedBuriedDecoration(WorldGenLevel level, ChunkAccess chunk, List<StackedLayerSpec> layers) {
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        ChunkPos chunkPos = chunk.getPos();
        int cursor = chunk.getMinY();
        int stepIndex = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        for (int layerIndex = 0; layerIndex < layers.size() - 1; layerIndex++) {
            StackedLayerSpec layer = layers.get(layerIndex);
            int layerSurfaceY = cursor + layer.blocksHeightBlocks();
            cursor = layerSurfaceY + layer.airGapBlocks();
            if (layerSurfaceY > chunk.getMaxY() || layer.airGapBlocks() <= 0) {
                continue;
            }
            Holder<Biome> biome = biomes.get(ResourceKey.create(Registries.BIOME, Identifier.parse(layer.biome()))).orElse(null);
            if (biome == null) {
                continue;
            }
            List<HolderSet<PlacedFeature>> featuresByStep = biome.value().getGenerationSettings().features();
            if (stepIndex >= featuresByStep.size()) {
                continue;
            }
            RandomSource random = RandomSource.create(
                level.getSeed() ^ (((long) chunkPos.getMinBlockX()) << 32 ^ (chunkPos.getMinBlockZ() & 0xFFFFFFFFL)) ^ layerIndex
            );
            for (Holder<PlacedFeature> placedFeature : featuresByStep.get(stepIndex)) {
                ConfiguredFeature<?, ?> feature = placedFeature.value().feature().value();
                for (int attempt = 0; attempt < BURIED_LAYER_DECORATION_ATTEMPTS; attempt++) {
                    int x = chunkPos.getMinBlockX() + random.nextInt(16);
                    int z = chunkPos.getMinBlockZ() + random.nextInt(16);
                    // DESIGN §34.7: the painted surface at this column may sit a few blocks above
                    // layerSurfaceY (relief bump) -- placing at the un-bumped Y would bury or
                    // float this feature relative to what's actually there. Clamped to this
                    // (non-top) layer's own air gap, mirroring stackedColumnStates's own clamp,
                    // so this always matches what was actually painted.
                    int bump = Math.min(
                        StackedPlan.reliefBlocksAt(stackedSeed(), layerIndex, x, z, this.stacked.reliefBlocks()),
                        layer.airGapBlocks()
                    );
                    feature.place(level, this, random, new BlockPos(x, layerSurfaceY + bump, z));
                }
            }
        }
    }

    /**
     * Force-places one amethyst geode on the reserved geode showcase cell (GOALS 37, DESIGN
     * §29.6), reusing {@link #placeOreFeature} directly -- despite the name, it forces any
     * {@code ConfiguredFeature} at an exact position, exactly what a geode needs too.
     */
    private void applyChunkIslandGeode(WorldGenLevel level, ChunkAccess chunk) {
        List<String> geodeFeatureIds = WorldzCommon.config().chunkIsland.geodeFeatureIds;
        if (geodeFeatureIds.isEmpty()) {
            return;
        }
        ChunkIslandPlan active = activeChunkIsland();
        long seed = chunkIslandSeed();
        ChunkIslandPlan.PortalCell geodeCell = active.reservedGeodeCell(seed);
        int[] center = geodeCell.centerBlock(active.cellSizeChunks());
        int centerBlockX = center[0] + originX();
        int centerBlockZ = center[1] + originZ();
        ChunkPos chunkPos = chunk.getPos();
        if (chunkPos.x() != Math.floorDiv(centerBlockX, 16) || chunkPos.z() != Math.floorDiv(centerBlockZ, 16)) {
            return;
        }
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, centerBlockX, centerBlockZ);
        int y = Math.clamp(surfaceY - 20, chunk.getMinY() + 5, surfaceY - 5);
        String featureId = geodeFeatureIds.get(
            Math.floorMod((int) (splitmix64(seed) >>> 33), geodeFeatureIds.size())
        );
        placeOreFeature(level, featureId, new BlockPos(centerBlockX, y, centerBlockZ), seed, centerBlockX, centerBlockZ);
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /**
     * One scattered island whose own (jittered) center falls inside a specific chunk -- the
     * "exactly once per island, whichever chunk happens to own it" unit every floating-island
     * resource (an ore deposit, a loot chest -- DESIGN §28.2) is placed against.
     */
    private record OwnedIsland(FloatingIslandsPlan.ResolvedIsland island, int centerBlockX, int centerBlockZ) {
    }

    /**
     * Resolves every scattered island this chunk owns the center of (DESIGN §28.2/§28.3): checks
     * the 3x3 cell neighborhood around this chunk's own center (not just "which cell does my
     * center belong to"), since a jittered center can land in a different chunk than that naive
     * lookup would suggest.
     */
    private List<OwnedIsland> floatingIslandsOwnedByChunk(ChunkPos chunkPos) {
        SkyIslandPlan active = activeSkyIsland();
        FloatingIslandsPlan floating = active.floatingIslands();
        if (!floating.enabled()) {
            return List.of();
        }
        long seed = skyIslandSeed();
        int centerX = chunkPos.getMinBlockX() + 8 - originX();
        int centerZ = chunkPos.getMinBlockZ() + 8 - originZ();
        List<OwnedIsland> owned = new ArrayList<>();
        for (FloatingIslandsPlan.ResolvedIsland island : floating.nearbyIslands(centerX, centerZ, seed, active.islandBiome())) {
            int blockX = (int) Math.round(island.centerX()) + originX();
            int blockZ = (int) Math.round(island.centerZ()) + originZ();
            if (blockX >= chunkPos.getMinBlockX() && blockX <= chunkPos.getMaxBlockX()
                && blockZ >= chunkPos.getMinBlockZ() && blockZ <= chunkPos.getMaxBlockZ()) {
                owned.add(new OwnedIsland(island, blockX, blockZ));
            }
        }
        return owned;
    }

    /**
     * Embeds one vanilla ore-vein feature on each present scattered floating island (GOALS 08,
     * DESIGN §28.2), exactly once per island regardless of chunk generation order: the deposit's
     * position is always the island's own center, clamped between its slab's floor and surface.
     */
    private void applyFloatingIslandOre(WorldGenLevel level, ChunkAccess chunk) {
        SkyIslandPlan active = activeSkyIsland();
        if (!active.floatingIslands().oreDepositsEnabled()) {
            return;
        }
        List<String> oreFeatureIds = WorldzCommon.config().skyIsland.floatingIslands.oreFeatureIds;
        if (oreFeatureIds.isEmpty()) {
            return;
        }
        int minY = active.bottomY() + 1;
        int maxY = active.surfaceY() - 1;
        if (minY > maxY) {
            // Slab too thin (thicknessBlocks 1) to fit any ore between its floor and surface.
            return;
        }

        long seed = skyIslandSeed();
        for (OwnedIsland owned : floatingIslandsOwnedByChunk(chunk.getPos())) {
            String featureId = owned.island().pick(oreFeatureIds, seed, "floating_island_ore_feature");
            int y = owned.island().pickY(minY, maxY, seed, "floating_island_ore_y");
            placeOreFeature(
                level, featureId, new BlockPos(owned.centerBlockX(), y, owned.centerBlockZ()), seed, owned.centerBlockX(), owned.centerBlockZ()
            );
        }
    }

    /**
     * Places one filled loot chest on each present scattered floating island (GOALS 08, DESIGN
     * §28.2), reusing {@link StarterKitPlan} exactly like the starter island's own necessities
     * chest (DESIGN §27.8) -- on the island's walkable surface, at the same X/Z as its ore
     * deposit (if any) but a different Y, so the two never collide.
     */
    private void applyFloatingIslandLoot(WorldGenLevel level, ChunkAccess chunk) {
        SkyIslandPlan active = activeSkyIsland();
        if (!active.floatingIslands().lootChestEnabled()) {
            return;
        }
        long seed = skyIslandSeed();
        for (OwnedIsland owned : floatingIslandsOwnedByChunk(chunk.getPos())) {
            placeLootChest(level, new BlockPos(owned.centerBlockX(), active.surfaceY(), owned.centerBlockZ()), seed, owned.centerBlockX(), owned.centerBlockZ());
        }
    }

    private void placeLootChest(WorldGenLevel level, BlockPos pos, long seed, int centerX, int centerZ) {
        StarterKitPlan plan = StarterKitDeployment.resolvePlan(WorldzCommon.config().skyIsland.floatingIslands.lootKit);
        long islandSeed = seed ^ (((long) centerX) << 32 ^ (centerZ & 0xFFFFFFFFL));
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(islandSeed);

        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 0);
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            WorldzCommon.LOGGER.warn("Could not create a GOALS 08 floating-island loot chest at {}.", pos);
            return;
        }
        int slot = 0;
        for (StarterKitPlan.ItemAmount amount : resolved) {
            if (slot >= chest.getContainerSize()) {
                break;
            }
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(amount.itemId()));
            chest.setItem(slot, new ItemStack(item, amount.count()));
            slot++;
        }
    }

    private void placeOreFeature(WorldGenLevel level, String featureId, BlockPos pos, long seed, int centerX, int centerZ) {
        Registry<ConfiguredFeature<?, ?>> registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> feature = registry.getValue(ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.parse(featureId)));
        if (feature == null) {
            WorldzCommon.LOGGER.warn("Unknown floating-island ore feature '{}'; skipping.", featureId);
            return;
        }
        RandomSource random = RandomSource.create(seed ^ (((long) centerX) << 32 ^ (centerZ & 0xFFFFFFFFL)));
        feature.place(level, this, random, pos);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos center = region.getCenter();
        if (!isEntirelyExterior(center) || decoratesExteriorOcean(center)) {
            this.delegate.spawnOriginalMobs(region);
        }
    }

    @Override
    public void createStructures(
        RegistryAccess registryAccess,
        ChunkGeneratorStructureState state,
        StructureManager structureManager,
        ChunkAccess centerChunk,
        StructureTemplateManager structureTemplateManager,
        ResourceKey<Level> level
    ) {
        ChunkPos centerPos = centerChunk.getPos();
        if (isEntirelyExterior(centerPos) && !decoratesExteriorOcean(centerPos)) {
            return;
        }
        StructureDistancePlan distancePlan = StructureDistancePlan.fromConfig(WorldzCommon.config().structureDistance);
        if (!distancePlan.enabled()) {
            super.createStructures(registryAccess, state, structureManager, centerChunk, structureTemplateManager, level);
            return;
        }
        createStructuresRespectingDistance(
            registryAccess, state, structureManager, centerChunk, structureTemplateManager, level, distancePlan
        );
    }

    /**
     * Reimplements {@code ChunkGenerator.createStructures}'s own per-set loop (GOAL 24, DESIGN
     * §36) with one inserted condition -- a restricted structure set is skipped entirely for a
     * chunk too close to spawn -- rather than the all-or-nothing gate above (which can only ever
     * suppress every structure in a chunk, not one family at a time). Only entered when {@link
     * StructureDistancePlan#enabled()}, so the default (disabled) path is byte-for-byte the
     * original {@code super.createStructures} call, zero risk to every existing preset.
     *
     * <p>Generation itself ({@code structure.generate} + {@code structureManager.
     * setStartForStructure}) reuses the exact technique already proven by {@code
     * FloatingIslandsDeployment.placeGuaranteedVillage} (verified against {@code
     * PlaceCommand.placeStructure}) instead of vanilla's own {@code private
     * ChunkGenerator.tryGenerateStructure}, which this class cannot call.
     */
    private void createStructuresRespectingDistance(
        RegistryAccess registryAccess,
        ChunkGeneratorStructureState state,
        StructureManager structureManager,
        ChunkAccess centerChunk,
        StructureTemplateManager structureTemplateManager,
        ResourceKey<Level> level,
        StructureDistancePlan distancePlan
    ) {
        ChunkPos sourceChunkPos = centerChunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(centerChunk);
        RandomState randomState = state.randomState();
        int distanceFromOrigin = Math.max(
            Math.abs(sourceChunkPos.getMinBlockX() - originX()), Math.abs(sourceChunkPos.getMinBlockZ() - originZ())
        );
        state.possibleStructureSets().forEach(set -> {
            StructurePlacement placement = set.value().placement();
            List<StructureSet.StructureSelectionEntry> structures = set.value().structures();
            for (StructureSet.StructureSelectionEntry structure : structures) {
                StructureStart existingStart = structureManager.getStartForStructure(sectionPos, structure.structure().value(), centerChunk);
                if (existingStart != null && existingStart.isValid()) {
                    return;
                }
            }
            if (distancePlan.isRestricted(structureSetId(set), distanceFromOrigin)) {
                return;
            }
            if (!placement.isStructureChunk(state, sourceChunkPos.x(), sourceChunkPos.z())) {
                return;
            }
            if (structures.size() == 1) {
                tryGenerateRestrictedStructure(
                    structures.get(0), registryAccess, structureManager, randomState, structureTemplateManager,
                    state.getLevelSeed(), centerChunk, sourceChunkPos, sectionPos, level
                );
                return;
            }
            List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(structures);
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(state.getLevelSeed(), sourceChunkPos.x(), sourceChunkPos.z());
            int total = options.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
            while (!options.isEmpty()) {
                int choice = random.nextInt(total);
                int index = 0;
                for (StructureSet.StructureSelectionEntry option : options) {
                    choice -= option.weight();
                    if (choice < 0) {
                        break;
                    }
                    index++;
                }
                StructureSet.StructureSelectionEntry selected = options.get(index);
                if (tryGenerateRestrictedStructure(
                    selected, registryAccess, structureManager, randomState, structureTemplateManager,
                    state.getLevelSeed(), centerChunk, sourceChunkPos, sectionPos, level
                )) {
                    return;
                }
                options.remove(index);
                total -= selected.weight();
            }
        });
    }

    /**
     * Faithful reimplementation of vanilla's private {@code ChunkGenerator.tryGenerateStructure}
     * using only public API (DESIGN §36) -- same shape as {@code FloatingIslandsDeployment}'s
     * already-shipped, Jason-tested call to {@code structure.generate}, but with the real
     * biome-matching predicate instead of an always-true one, since this is an ordinary
     * "try, may fail" attempt, not a forced placement.
     */
    private boolean tryGenerateRestrictedStructure(
        StructureSet.StructureSelectionEntry selected,
        RegistryAccess registryAccess,
        StructureManager structureManager,
        RandomState randomState,
        StructureTemplateManager structureTemplateManager,
        long seed,
        ChunkAccess centerChunk,
        ChunkPos sourceChunkPos,
        SectionPos sectionPos,
        ResourceKey<Level> level
    ) {
        Structure structure = selected.structure().value();
        StructureStart previous = structureManager.getStartForStructure(sectionPos, structure, centerChunk);
        int references = previous != null ? previous.getReferences() : 0;
        Predicate<Holder<Biome>> biomePredicate = structure.biomes()::contains;
        StructureStart start = structure.generate(
            selected.structure(), level, registryAccess, this, this.getBiomeSource(), randomState,
            structureTemplateManager, seed, sourceChunkPos, references, centerChunk, biomePredicate
        );
        if (!start.isValid()) {
            return false;
        }
        structureManager.setStartForStructure(sectionPos, structure, start, centerChunk);
        return true;
    }

    /**
     * Whether an entirely-exterior chunk should still get vanilla decoration, original mob
     * population, and structures (kelp/seagrass, fish, shipwrecks, ocean ruins, monuments) --
     * true only for the ocean island's own artificial ocean (GOALS 01/02/03), which unlike
     * every other preset's exterior ocean is real, meant-to-be-explored space, not an
     * incidental boundary. Scoped to {@code island.enabled()} so strip_world/single_biome/
     * chaos_biomes's existing exterior-ocean behavior (a silent, decoration-free boundary,
     * unchanged since before this phase) is completely untouched.
     *
     * <p>Excludes {@link IslandFluid#LAVA} (GOALS 28): Jason found ocean monuments and
     * shipwrecks sitting in a sea of lava looks wrong (config 36 testing, 2026-07-20), so a lava
     * exterior reverts to the same silent, decoration-free boundary as every non-ocean-island
     * preset. {@link IslandFluid#NONE} (GOALS 31, drained basin) is unaffected -- config 37's own
     * acceptance steps already expect structures to generate normally there.
     */
    private boolean decoratesExteriorOcean(ChunkPos chunkPos) {
        return this.island.enabled() && this.island.fluid() != IslandFluid.LAVA && isEntirelyExteriorOcean(chunkPos);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        ChunkAccess centerChunk
    ) {
        if (this.flat.enabled()) {
            // Skips the delegate's real (comparatively expensive) noise-based terrain shaping
            // entirely -- GOAL 15, DESIGN §33.1's correction -- instead of calling
            // this.delegate.fillFromNoise, which for the NoiseBasedChunkGenerator+
            // LimitedBiomeSource delegate flat now uses (needed only for WorldLimitManager/
            // border integration, DESIGN §33.1) would otherwise generate a full real terrain
            // column just to discard it.
            fillFlatColumns(centerChunk, this.flat);
            applyEnvelope(centerChunk, randomState);
            return CompletableFuture.completedFuture(centerChunk);
        }
        if (this.stacked.enabled()) {
            // Same reasoning as flat above (GOAL 35, DESIGN §34.2): the delegate's real terrain
            // is never used for stacked either, just its NoiseBasedChunkGenerator+
            // LimitedBiomeSource shape (WorldLimitManager/border integration).
            fillStackedColumns(centerChunk, resolvedStackedLayers(), stackedSeed(), this.stacked.reliefBlocks());
            applyEnvelope(centerChunk, randomState);
            return CompletableFuture.completedFuture(centerChunk);
        }
        return this.delegate.fillFromNoise(blender, randomState, structureManager, centerChunk)
            .thenApply(chunk -> {
                applyTerrainAdjustments(chunk, randomState, false);
                applyEnvelope(chunk, randomState);
                return chunk;
            });
    }

    /**
     * Paints every column with the same fixed layer stack, bottom to top (GOAL 15, DESIGN
     * §33.2) -- mirrors {@code FlatLevelSource.fillFromNoise}'s own real loop almost verbatim,
     * reimplemented directly rather than delegated to (DESIGN §33.1's correction).
     */
    private static void fillFlatColumns(ChunkAccess chunk, FlatPlan flat) {
        List<BlockState> states = flatLayerStates(flat.layers());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int layerIndex = 0; layerIndex < Math.min(chunk.getHeight(), states.size()); layerIndex++) {
            BlockState state = states.get(layerIndex);
            int y = chunk.getMinY() + layerIndex;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(pos.set(x, y, z), state);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }
    }

    /**
     * Expands an ordered layer list into one {@link BlockState} per Y, mirroring {@code
     * FlatLevelGeneratorSettings.updateLayers()}'s own expansion exactly. Shared by {@link
     * #flat}'s full-height stack and {@link #deepFlat}'s cap-only stack.
     */
    private static List<BlockState> flatLayerStates(List<FlatLayerSpec> layers) {
        List<BlockState> states = new ArrayList<>();
        for (FlatLayerSpec layer : layers) {
            BlockState state = resolveBlockState(layer.blockId());
            for (int i = 0; i < layer.heightBlocks(); i++) {
                states.add(state);
            }
        }
        return states;
    }

    private static BlockState resolveBlockState(String blockId) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId)).defaultBlockState();
    }

    /**
     * Returns {@link #stacked}'s configured layer order, resolved once per call from the real
     * Minecraft world seed (GOAL 35, DESIGN §34.1) -- {@link StackedPlan#resolvedLayers} is a
     * no-op unless {@code seedRandomizedOrder} is set. {@code stacked} is Overworld-only, so this
     * is only ever called when {@link #originSource} is present, mirroring {@link #islandSeed()}/
     * {@link #skyIslandSeed()}'s exact same-precedent seed lookup.
     */
    private List<StackedLayerSpec> resolvedStackedLayers() {
        return this.stacked.resolvedLayers(stackedSeed());
    }

    /**
     * Returns the real Minecraft world seed backing {@code stacked}'s layer order (GOAL 35,
     * DESIGN §34.1) and, since DESIGN §34.7, its per-column relief -- shared by {@link
     * #resolvedStackedLayers()} and every {@link StackedPlan#reliefBlocksAt} call site so all of
     * them agree on the same seed.
     */
    private long stackedSeed() {
        return this.originSource.orElseThrow().effectiveLayoutPlan().seed();
    }

    /**
     * Paints every column with the same fixed stack of layers, bottom to top (GOAL 35, DESIGN
     * §34.2) -- mirrors {@link #fillFlatColumns} almost exactly, except each layer's own block
     * stack is followed by an explicit skip across its air gap rather than an unbroken solid
     * column: a freshly generated {@link ChunkAccess} is already all-air by default, so the gap
     * needs no block writes of its own, only advancing {@code y} past it. Since DESIGN §34.7,
     * each layer's own topmost block state can also bump a few blocks into that gap, deterministic
     * per column via {@link StackedPlan#reliefBlocksAt} -- {@link #stackedColumnStates} is the
     * single source of truth for exactly what a column's own bump looks like, shared with the
     * heightmap/base-column query paths below so painted terrain and reported height never disagree.
     */
    private static void fillStackedColumns(ChunkAccess chunk, List<StackedLayerSpec> layers, long seed, int reliefBlocks) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                List<BlockState> states = stackedColumnStates(layers, seed, reliefBlocks, minBlockX + x, minBlockZ + z);
                for (int index = 0; index < Math.min(chunk.getHeight(), states.size()); index++) {
                    BlockState state = states.get(index);
                    if (state.isAir()) {
                        continue;
                    }
                    int y = chunk.getMinY() + index;
                    chunk.setBlockState(pos.set(x, y, z), state);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }
    }

    /**
     * Expands a resolved stacked layer order into one {@link BlockState} per Y for a single
     * column, mirroring {@link #flatLayerStates}'s per-layer expansion but with each layer's own
     * air-gap entries partly replaced by its own topmost block (DESIGN §34.7's relief bump, {@link
     * StackedPlan#reliefBlocksAt}) -- deliberately a within-budget reallocation (the bump is
     * clamped to a non-top layer's own {@code airGapBlocks}) so a layer's cumulative-height zone
     * boundary never moves, keeping {@link StackedPlan#layerAt}'s Y-only biome lookup correct
     * without needing to know about relief at all. Shared by {@link #fillStackedColumns} and
     * {@link #stackedBaseHeight}/{@link #stackedBaseColumn} so painted terrain and reported
     * height/heightmap can never disagree.
     */
    private static List<BlockState> stackedColumnStates(
        List<StackedLayerSpec> layers, long seed, int reliefBlocks, int blockX, int blockZ
    ) {
        List<BlockState> states = new ArrayList<>();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
            StackedLayerSpec layer = layers.get(layerIndex);
            List<BlockState> layerStates = flatLayerStates(layer.blocks());
            states.addAll(layerStates);
            boolean isTopLayer = layerIndex == layers.size() - 1;
            int bump = StackedPlan.reliefBlocksAt(seed, layerIndex, blockX, blockZ, reliefBlocks);
            int clampedBump = isTopLayer ? bump : Math.min(bump, layer.airGapBlocks());
            BlockState topState = layerStates.get(layerStates.size() - 1);
            for (int i = 0; i < clampedBump; i++) {
                states.add(topState);
            }
            for (int i = 0; i < layer.airGapBlocks() - clampedBump; i++) {
                states.add(air);
            }
        }
        return states;
    }

    /** Mirrors {@link #flatBaseHeight} exactly, fed one column's own resolved stacked states. */
    private static int stackedBaseHeight(
        List<StackedLayerSpec> layers, Heightmap.Types type, LevelHeightAccessor heightAccessor,
        long seed, int reliefBlocks, int blockX, int blockZ
    ) {
        List<BlockState> states = stackedColumnStates(layers, seed, reliefBlocks, blockX, blockZ);
        for (int layerIndex = Math.min(states.size() - 1, heightAccessor.getMaxY()); layerIndex >= 0; layerIndex--) {
            BlockState state = states.get(layerIndex);
            if (type.isOpaque().test(state)) {
                return heightAccessor.getMinY() + layerIndex + 1;
            }
        }
        return heightAccessor.getMinY();
    }

    /** Mirrors {@link #flatBaseColumn} exactly, fed one column's own resolved stacked states. */
    private static NoiseColumn stackedBaseColumn(
        List<StackedLayerSpec> layers, LevelHeightAccessor heightAccessor, long seed, int reliefBlocks, int blockX, int blockZ
    ) {
        List<BlockState> layerStates = stackedColumnStates(layers, seed, reliefBlocks, blockX, blockZ);
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int index = 0; index < states.length; index++) {
            states[index] = index < layerStates.size() ? layerStates.get(index) : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    /**
     * Scans the layer stack from the top down for the first block opaque under {@code type},
     * mirroring {@code FlatLevelSource.getBaseHeight}'s own exact logic -- almost always just the
     * very top layer, but a configured non-opaque top layer (e.g. a deliberate glass ceiling)
     * correctly falls through to the next solid one below it, same as vanilla.
     */
    private static int flatBaseHeight(FlatPlan flat, Heightmap.Types type, LevelHeightAccessor heightAccessor) {
        List<BlockState> states = flatLayerStates(flat.layers());
        for (int layerIndex = Math.min(states.size() - 1, heightAccessor.getMaxY()); layerIndex >= 0; layerIndex--) {
            BlockState state = states.get(layerIndex);
            if (type.isOpaque().test(state)) {
                return heightAccessor.getMinY() + layerIndex + 1;
            }
        }
        return heightAccessor.getMinY();
    }

    /** Mirrors {@code FlatLevelSource.getBaseColumn} exactly: the layer stack, air above it. */
    private static NoiseColumn flatBaseColumn(FlatPlan flat, LevelHeightAccessor heightAccessor) {
        List<BlockState> layerStates = flatLayerStates(flat.layers());
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int index = 0; index < states.length; index++) {
            states[index] = index < layerStates.size() ? layerStates.get(index) : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    @Override
    public int getGenDepth() {
        return this.delegate.getGenDepth();
    }

    @Override
    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return this.delegate.getMinY();
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        // Mirrors FlatLevelSource.getSpawnHeight exactly (GOAL 15, DESIGN §33.3): the top of the
        // layer stack -- avoiding slimes (Y 40 cutoff, Slime.checkSlimeSpawnRules) is purely a
        // property of how tall the configured layers are, no separate spawnY field needed.
        if (this.flat.enabled()) {
            return heightAccessor.getMinY() + Math.min(heightAccessor.getHeight(), this.flat.totalHeightBlocks());
        }
        // Stacked (GOAL 35, DESIGN §34.1): spawn lands on the topmost layer's own surface, not
        // including its own trailing air gap (that's just open sky above, not headroom to stand
        // in mid-air over) -- mirrors flat's "top of the layer stack" spawn semantic exactly.
        if (this.stacked.enabled()) {
            return StackedPlan.surfaceY(resolvedStackedLayers(), heightAccessor.getMinY(), heightAccessor.getHeight());
        }
        // Deep-flat (GOAL 16, DESIGN §33.4): getSpawnHeight has no x/z of its own (it's a
        // dimension-wide constant, not per-column, verified from the real ChunkGenerator/
        // NoiseBasedChunkGenerator signature) -- SpawnOriginManager.safeSpawnNear reads it
        // directly as the spawn Y with no further per-column height lookup, so returning the
        // configured surfaceY here is what actually places the player on the flat cap instead
        // of wherever the real, uncapped delegate terrain happens to be tall.
        if (this.deepFlat.enabled()) {
            return this.deepFlat.surfaceY();
        }
        return this.delegate.getSpawnHeight(heightAccessor);
    }

    @Override
    public int getBaseHeight(
        int x,
        int z,
        Heightmap.Types type,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        ExteriorMode mode = this.effectiveModeAt(x - originX(), z - originZ());
        if (mode == ExteriorMode.NORMAL) {
            if (this.flat.enabled()) {
                return flatBaseHeight(this.flat, type, heightAccessor);
            }
            if (this.stacked.enabled()) {
                return stackedBaseHeight(resolvedStackedLayers(), type, heightAccessor, stackedSeed(), this.stacked.reliefBlocks(), x, z);
            }
            if (this.deepFlat.enabled()) {
                boolean waterCap = deepFlatWaterCapAt(x, z, this.deepFlat, randomState);
                return deepFlatBaseHeight(this.deepFlat, waterCap, type);
            }
            int naturalHeight = this.delegate.getBaseHeight(x, z, type, heightAccessor, randomState);
            int naturalFloor = naturalOceanFloorHeight(x, z, heightAccessor, randomState);
            int layoutFloor = layoutFloorOrNatural(x, z, naturalFloor, randomState);
            int layoutHeight = naturalHeight + (layoutFloor - naturalFloor);
            int raisedHeight = Math.max(layoutHeight, starterLandTargetHeight(x, z, heightAccessor, randomState, naturalFloor, layoutFloor));
            return Math.max(raisedHeight, islandTargetHeight(x, z, heightAccessor, randomState, naturalFloor, layoutFloor));
        }
        if (activeSkyIsland().enabled()) {
            return skyIslandBaseHeight(x - originX(), z - originZ(), heightAccessor);
        }
        return this.island.enabled()
            ? ExteriorTerrainProfile.baseHeight(
                mode, isOceanFloor(type), heightAccessor.getMinY(), heightAccessor.getMaxY(), getSeaLevel(),
                islandOceanDepthAt(x - originX(), z - originZ())
            )
            : ExteriorTerrainProfile.baseHeight(
                mode, isOceanFloor(type), heightAccessor.getMinY(), heightAccessor.getMaxY(), getSeaLevel()
            );
    }

    /**
     * Returns the sky island's first-free height (GOALS 05, DESIGN §27.2): {@code surfaceY}
     * inside the footprint (so spawn search, structure placement, and heightmaps all see the
     * slab's top as "the ground"), or true void ({@code heightAccessor.getMinY()}) outside it --
     * the same shape {@link ExteriorTerrainProfile#baseHeight}'s {@code VOID} case already
     * returns unconditionally today, just no longer uniform across the whole dimension.
     */
    private int skyIslandBaseHeight(int relativeX, int relativeZ, LevelHeightAccessor heightAccessor) {
        SkyIslandPlan active = activeSkyIsland();
        SkyIslandHit hit = skyIslandHitAt(relativeX, relativeZ, active);
        return hit.present()
            ? Math.min(active.surfaceY(), heightAccessor.getMaxY() + 1)
            : heightAccessor.getMinY();
    }

    /**
     * One column's result against the sky island footprint (GOALS 05/06) or, failing that, the
     * scattered floating-island grid beyond it (GOALS 07-08, DESIGN §28.1) -- the starter island
     * always wins when both would apply (it never does, since scattered islands respect their own
     * exclusion zone, but checking the starter first is cheaper and needs no coordination either
     * way). {@code distanceFromShore} and {@code biome} are meaningless when {@link #present} is
     * {@code false}. {@code village} mirrors {@link FloatingIslandsPlan.Hit#village()} -- always
     * {@code false} for the starter footprint, which is never the guaranteed village. {@code
     * centerX}/{@code centerZ} mirror {@link FloatingIslandsPlan.Hit#centerX()}/{@link
     * FloatingIslandsPlan.Hit#centerZ()} -- meaningless (0) for the starter footprint, which never
     * needs them (natural-biome resampling always bails out for it before reading them).
     */
    private record SkyIslandHit(boolean present, double distanceFromShore, String biome, boolean village, double centerX, double centerZ) {
    }

    private SkyIslandHit skyIslandHitAt(int relativeX, int relativeZ, SkyIslandPlan active) {
        double starterDistance = active.distanceFromShore(relativeX, relativeZ, skyIslandSeed());
        if (starterDistance <= 0.0) {
            return new SkyIslandHit(true, starterDistance, active.islandBiome(), false, 0.0, 0.0);
        }
        FloatingIslandsPlan.Hit scattered = active.floatingIslands().at(relativeX, relativeZ, skyIslandSeed(), active.islandBiome());
        return scattered.present()
            ? new SkyIslandHit(true, scattered.distanceFromShore(), scattered.biome(), scattered.village(), scattered.centerX(), scattered.centerZ())
            : new SkyIslandHit(false, starterDistance, active.islandBiome(), false, 0.0, 0.0);
    }

    /**
     * Same as {@link #skyIslandHitAt(int, int, SkyIslandPlan)}, except a scattered floating
     * island's biome is resolved from the real seed (DESIGN §28.4) when {@code naturalBiome} is
     * set, instead of {@link FloatingIslandsPlan.Hit#biome()}'s hash-picked placeholder -- needed
     * only by callers that actually consume {@link SkyIslandHit#biome()} for the terrain palette
     * ({@link #skyIslandStateAt}), not by height-only callers like {@link #skyIslandBaseHeight}.
     * The guaranteed village's own reserved island ({@link SkyIslandHit#village()}) is deliberately
     * exempt (2026-07-25 fix): it needs a real vanilla village to legally place its jigsaw pieces,
     * which only works on the specific structure-compatible biome {@link
     * FloatingIslandsPlan#resolveCell} already forced -- substituting the real seed's biome there
     * (frequently unrelated, e.g. deep ocean) doesn't change what actually got force-generated but
     * did make the biome reading/surface palette visibly disagree with it.
     */
    private SkyIslandHit skyIslandHitAtForTerrain(int relativeX, int relativeZ, SkyIslandPlan active, RandomState randomState) {
        SkyIslandHit hit = skyIslandHitAt(relativeX, relativeZ, active);
        if (!hit.present() || hit.village() || !active.floatingIslands().naturalBiome() || randomState == null) {
            // A null randomState only happens if this generator's very first chunk call ever
            // lands on applyBiomeDecoration before any of fillFromNoise/applyCarvers/buildSurface
            // populated the cache -- not expected given vanilla's own chunk-status ordering, but
            // falling back to the placeholder biome is harmless (just skips natural-biome for
            // this one call) rather than crashing worldgen outright.
            return hit;
        }
        double starterDistance = active.distanceFromShore(relativeX, relativeZ, skyIslandSeed());
        if (starterDistance <= 0.0) {
            // The starter footprint itself always keeps its own configured biome, natural-biome
            // mode only applies to scattered floating islands beyond it.
            return hit;
        }
        // Sampled once at the island's own (fixed) center, not at this query column (2026-07-25
        // fix #2, mirrors LimitedBiomeSource#getNoiseBiome's matching fix): the real biome noise
        // varies over much shorter distances than one island's own footprint, so per-column
        // sampling made a single island's surface palette shift between unrelated biomes a few
        // blocks apart instead of reading as one coherent region.
        int quartX = QuartPos.fromBlock((int) Math.round(hit.centerX()) + originX());
        int quartZ = QuartPos.fromBlock((int) Math.round(hit.centerZ()) + originZ());
        int quartY = QuartPos.fromBlock(active.surfaceY());
        Holder<Biome> natural = this.delegate.getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, randomState.sampler());
        String naturalId = natural.unwrapKey().map(key -> key.identifier().toString()).orElseGet(natural::getRegisteredName);
        return new SkyIslandHit(true, hit.distanceFromShore(), naturalId, false, hit.centerX(), hit.centerZ());
    }

    @Override
    public NoiseColumn getBaseColumn(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        ExteriorMode mode = this.effectiveModeAt(x - originX(), z - originZ());
        if (mode == ExteriorMode.NORMAL) {
            if (this.flat.enabled()) {
                return flatBaseColumn(this.flat, heightAccessor);
            }
            if (this.stacked.enabled()) {
                return stackedBaseColumn(resolvedStackedLayers(), heightAccessor, stackedSeed(), this.stacked.reliefBlocks(), x, z);
            }
            if (this.deepFlat.enabled()) {
                return deepFlatBaseColumn(this.deepFlat, x, z, heightAccessor, randomState);
            }
            NoiseColumn naturalColumn = this.delegate.getBaseColumn(x, z, heightAccessor, randomState);
            int naturalFloor = naturalOceanFloorHeight(x, z, heightAccessor, randomState);
            int layoutFloor = layoutFloorOrNatural(x, z, naturalFloor, randomState);
            BlockState[] states = null;

            if (this.layout.isPresent()) {
                if (layoutFloor > naturalFloor) {
                    states = copyColumn(naturalColumn, heightAccessor);
                    int minY = StarterLandProfile.foundationMinY(
                        naturalFloor, DEFAULT_LAYOUT_FOUNDATION_DEPTH_BLOCKS, heightAccessor.getMinY()
                    );
                    fillStarterColumn(states, heightAccessor.getMinY(), minY, layoutFloor - 1, naturalFloor);
                } else if (layoutFloor < naturalFloor) {
                    states = copyColumn(naturalColumn, heightAccessor);
                    lowerColumn(states, heightAccessor.getMinY(), layoutFloor, naturalFloor - 1, getSeaLevel());
                }
            }

            int targetHeight = starterLandTargetHeight(x, z, heightAccessor, randomState, naturalFloor, layoutFloor);
            if (targetHeight > naturalFloor) {
                if (states == null) {
                    states = copyColumn(naturalColumn, heightAccessor);
                }
                int minY = StarterLandProfile.foundationMinY(
                    naturalFloor,
                    this.starterLand.orElseThrow().plan().foundationDepthBlocks(),
                    heightAccessor.getMinY()
                );
                fillStarterColumn(states, heightAccessor.getMinY(), minY, targetHeight - 1, naturalFloor);
            }

            int islandHeight = islandTargetHeight(x, z, heightAccessor, randomState, naturalFloor, layoutFloor);
            if (islandHeight > naturalFloor) {
                if (states == null) {
                    states = copyColumn(naturalColumn, heightAccessor);
                }
                int minY = StarterLandProfile.foundationMinY(
                    naturalFloor, DEFAULT_LAYOUT_FOUNDATION_DEPTH_BLOCKS, heightAccessor.getMinY()
                );
                fillStarterColumn(states, heightAccessor.getMinY(), minY, islandHeight - 1, naturalFloor);
            }

            return states == null ? naturalColumn : new NoiseColumn(heightAccessor.getMinY(), states);
        }
        if (activeSkyIsland().enabled()) {
            SkyIslandHit hit = skyIslandHitAtForTerrain(x - originX(), z - originZ(), activeSkyIsland(), randomState);
            BlockState[] skyStates = new BlockState[heightAccessor.getHeight()];
            int skyMinY = heightAccessor.getMinY();
            for (int index = 0; index < skyStates.length; index++) {
                skyStates[index] = skyIslandStateAt(hit, skyMinY + index);
            }
            return new NoiseColumn(skyMinY, skyStates);
        }
        int depthBlocks = this.island.enabled()
            ? islandOceanDepthAt(x - originX(), z - originZ())
            : ExteriorTerrainProfile.OCEAN_DEPTH;
        IslandFluid fluid = this.island.enabled() ? this.island.fluid() : IslandFluid.WATER;
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        int minY = heightAccessor.getMinY();
        for (int index = 0; index < states.length; index++) {
            states[index] = exteriorState(mode, minY + index, heightAccessor, depthBlocks, fluid);
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        this.delegate.addDebugScreenInfo(result, randomState, feetPos);
        result.add("Worldz exterior: " + this.envelope.mode().serializedName());
        if (this.strip.enabled()) {
            result.add(
                "Worldz strip: width=" + this.strip.width()
                    + ", widthMode=" + this.strip.widthMode().serializedName()
            );
        }
        if (this.island.enabled()) {
            result.add(
                "Worldz island: radius=" + this.island.radiusBlocks()
                    + ", amplitude=" + this.island.shapeAmplitude()
                    + ", shoreWidth=" + this.island.shoreWidthBlocks()
                    + ", exclusionZone=" + (this.island.exclusionZoneEnabled()
                        ? "radius=" + this.island.exclusionZoneRadiusBlocks() : "<disabled>")
            );
        }
        if (activeSkyIsland().enabled()) {
            SkyIslandPlan active = activeSkyIsland();
            result.add(
                "Worldz sky island: radius=" + active.radiusBlocks()
                    + ", surfaceY=" + active.surfaceY()
                    + ", thickness=" + active.thicknessBlocks()
            );
        }
        if (activeChunkIsland().enabled()) {
            ChunkIslandPlan active = activeChunkIsland();
            result.add(
                "Worldz chunk island: spawnChance=" + active.spawnChance()
                    + ", cellSizeChunks=" + active.cellSizeChunks()
                    + ", topOnly=" + active.topOnly()
            );
        }
        this.starterLand.ifPresent(context -> result.add(
            "Worldz starter land: radius=" + context.radiusBlocks()
                + ", transition=" + context.plan().transitionWidthBlocks()
        ));
        this.layout.ifPresent(context -> result.add("Worldz layout: mode=" + context.plan().mode().serializedName()));
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
        ServerLevel level,
        HolderSet<Structure> wantedStructures,
        BlockPos pos,
        int maxSearchRadius,
        boolean createReference
    ) {
        return this.delegate.findNearestMapStructure(level, wantedStructures, pos, maxSearchRadius, createReference);
    }

    @Override
    public net.minecraft.util.random.WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(
        Holder<Biome> biome,
        StructureManager structureManager,
        MobCategory mobCategory,
        BlockPos pos
    ) {
        return this.delegate.getMobsAt(biome, structureManager, mobCategory, pos);
    }

    @Override
    @Deprecated
    public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> biome) {
        return this.delegate.getBiomeGenerationSettings(biome);
    }

    private void applyEnvelope(ChunkAccess chunk, RandomState randomState) {
        if (randomState != null) {
            this.cachedRandomState = randomState;
        }
        if (!hasActiveExterior()) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int relativeX = x - originX();
                int relativeZ = z - originZ();
                ExteriorMode mode = this.effectiveModeAt(relativeX, relativeZ);
                if (mode != ExteriorMode.NORMAL) {
                    if (activeSkyIsland().enabled()) {
                        SkyIslandHit hit = skyIslandHitAtForTerrain(relativeX, relativeZ, activeSkyIsland(), this.cachedRandomState);
                        for (int y = minY; y <= maxY; y++) {
                            pos.set(x, y, z);
                            BlockState state = skyIslandStateAt(hit, y);
                            BlockState oldState = chunk.getBlockState(pos);
                            if (oldState != state) {
                                if (oldState.hasBlockEntity()) {
                                    chunk.removeBlockEntity(pos);
                                }
                                chunk.setBlockState(pos, state, 0);
                            }
                        }
                        continue;
                    }
                    int depthBlocks = this.island.enabled()
                        ? islandOceanDepthAt(relativeX, relativeZ)
                        : ExteriorTerrainProfile.OCEAN_DEPTH;
                    IslandFluid fluid = this.island.enabled() ? this.island.fluid() : IslandFluid.WATER;
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = exteriorState(mode, y, chunk, depthBlocks, fluid);
                        BlockState oldState = chunk.getBlockState(pos);
                        if (oldState != state) {
                            if (oldState.hasBlockEntity()) {
                                chunk.removeBlockEntity(pos);
                            }
                            chunk.setBlockState(pos, state, 0);
                        }
                    }
                }
            }
        }
        if (activeChunkIsland().enabled() && activeChunkIsland().topOnly()) {
            applyChunkIslandDepthCutoff(chunk, chunkPos, pos, minY);
        }
        if (this.cave.enabled() && this.cave.sealedSurface()) {
            applyCaveSealedSurface(chunk, chunkPos, pos, maxY);
        }
        if (this.cave.enabled() && this.cave.cavernEnabled()) {
            applyCaveMegaCavern(chunk, chunkPos, pos);
        }
    }

    /**
     * Seals off sky access everywhere (GOALS 25, DESIGN §30.4): a solid roof at {@code
     * cave.sealedSurfaceY()}, applied uniformly to every column regardless of X/Z or any
     * border/exterior/island state -- unlike every shaped exterior mode, this has no footprint
     * concept at all. Layered additively after {@link #applyEnvelope}'s ordinary masking loop,
     * mirroring {@link #applyChunkIslandDepthCutoff}'s "runs again unconditionally" placement.
     * No custom lighting/heightmap code needed: ordinary {@code setBlockState} calls already
     * recompute both automatically during chunk generation (DESIGN §21.2's void-border spike
     * finding), and skylight naturally stops propagating below a solid roof.
     *
     * <p>Block and thickness are both configurable (Jason, 2026-07-25) -- originally a fixed
     * 5-thick stone layer, easily breached by a determined player; {@code cave.sealedSurfaceBlock()}
     * can now be raised to deepslate or bedrock, and {@code cave.sealedSurfaceThicknessBlocks()}
     * widened, without changing this method's shape.
     */
    private void applyCaveSealedSurface(ChunkAccess chunk, ChunkPos chunkPos, BlockPos.MutableBlockPos pos, int maxY) {
        int roofY = Math.min(this.cave.sealedSurfaceY(), maxY);
        int roofTop = Math.min(maxY, roofY + this.cave.sealedSurfaceThicknessBlocks() - 1);
        BlockState roofState = sealedSurfaceBlockState(this.cave.sealedSurfaceBlock());
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                for (int y = roofY; y <= roofTop; y++) {
                    pos.set(x, y, z);
                    BlockState oldState = chunk.getBlockState(pos);
                    if (oldState != roofState) {
                        if (oldState.hasBlockEntity()) {
                            chunk.removeBlockEntity(pos);
                        }
                        chunk.setBlockState(pos, roofState, 0);
                    }
                }
            }
        }
    }

    /** Maps the cave preset's configured roof material (Jason, 2026-07-25) to its real block. */
    private static BlockState sealedSurfaceBlockState(SealedSurfaceBlock block) {
        return switch (block) {
            case STONE -> Blocks.STONE.defaultBlockState();
            case DEEPSLATE -> Blocks.DEEPSLATE.defaultBlockState();
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
        };
    }

    /**
     * Carves a large natural-looking cavern around spawn (GOALS 26, DESIGN §30.5): reuses {@link
     * IslandShapeProfile#distanceFromShore} for a perturbed horizontal footprint (the same
     * coastline-shaping math every other footprint in this project shares) bounded vertically by
     * {@code cave.cavernHeightBlocks()} above/below {@code cave.spawnDepthY()}. Air-only,
     * one-directional: a column already air or fluid inside the footprint is left exactly as
     * vanilla generated it (this is what "blended into the natural cave systems at its edges"
     * means in practice) -- only solid, non-fluid blocks become air. Never fills.
     *
     * <p>One deliberate exception to "never fills": when the configured height reaches (or, once
     * clamped, would have reached) the world's own floor, the carve leaves the single bottom-most
     * layer untouched instead of carving it away -- found in-game (Jason, config 55, whose
     * {@code spawnDepthY=-32}/{@code cavernHeightBlocks=32} lands exactly on the Overworld's
     * {@code minY=-64}): carving that layer removed vanilla's own guaranteed-solid bottom bedrock
     * along with everything else, opening the cavern floor straight into the void below the
     * world. Vanilla always generates a fully solid bedrock block at the literal minimum Y for
     * every column (only the few layers just above it are randomized), so simply never carving
     * that one layer is sufficient -- no explicit fill needed to restore a solid floor.
     */
    private void applyCaveMegaCavern(ChunkAccess chunk, ChunkPos chunkPos, BlockPos.MutableBlockPos pos) {
        int worldMinY = chunk.getMinY();
        int minY = Math.max(worldMinY, this.cave.spawnDepthY() - this.cave.cavernHeightBlocks());
        int maxY = Math.min(chunk.getMaxY(), this.cave.spawnDepthY() + this.cave.cavernHeightBlocks());
        int carveFromY = minY == worldMinY ? worldMinY + 1 : minY;
        long seed = caveSeed();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                double distance = IslandShapeProfile.distanceFromShore(
                    x - originX(), z - originZ(), this.cave.cavernRadiusBlocks(), IslandShapeProfile.DEFAULT_AMPLITUDE, seed
                );
                if (distance > 0) {
                    continue;
                }
                for (int y = carveFromY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState oldState = chunk.getBlockState(pos);
                    if (!oldState.isAir() && oldState.getFluidState().isEmpty()) {
                        if (oldState.hasBlockEntity()) {
                            chunk.removeBlockEntity(pos);
                        }
                        chunk.setBlockState(pos, air, 0);
                    }
                }
            }
        }
    }

    /**
     * Returns the real Minecraft world seed for the mega-cavern's footprint shape. Cave has no
     * Nether/End variant in scope (DESIGN §30.6), so this is only ever called for the Overworld
     * instance, which always has a real {@link #originSource} to read from -- same precondition
     * as {@link #islandSeed()}.
     */
    private long caveSeed() {
        return this.originSource.orElseThrow().effectiveLayoutPlan().seed();
    }

    /**
     * Voids everything below a {@code TOP_ONLY} chunk island's own real generated surface, down
     * to its configured depth (GOALS 09's "like 5 deep to ensure access to stone") -- layered
     * additively after {@link #applyEnvelope}'s ordinary per-column masking loop above, since the
     * same column is {@code NORMAL} above the cutoff and voided below it, which no single {@link
     * ExteriorMode} can express in one column (DESIGN §29.3). Re-runs every time {@link
     * #applyEnvelope} does (once per generation stage) -- harmless and idempotent, the same
     * "runs again unconditionally" pattern every other exterior mode already relies on.
     */
    private void applyChunkIslandDepthCutoff(ChunkAccess chunk, ChunkPos chunkPos, BlockPos.MutableBlockPos pos, int minY) {
        int chunkX = Math.floorDiv(chunkPos.getMinBlockX() - originX(), 16);
        int chunkZ = Math.floorDiv(chunkPos.getMinBlockZ() - originZ(), 16);
        ChunkIslandPlan.Hit hit = activeChunkIsland().at(chunkX, chunkZ, chunkIslandSeed());
        if (!hit.present() || !hit.topOnly()) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int cutoffY = surfaceY - hit.topOnlyDepthBlocks();
                for (int y = minY; y < cutoffY; y++) {
                    pos.set(x, y, z);
                    BlockState oldState = chunk.getBlockState(pos);
                    if (oldState != air) {
                        if (oldState.hasBlockEntity()) {
                            chunk.removeBlockEntity(pos);
                        }
                        chunk.setBlockState(pos, air, 0);
                    }
                }
            }
        }
    }

    /**
     * Applies both the layout raise/lower and the starter-land raise in one pass per
     * column. Merged (rather than two separate per-column loops, as before) so the
     * natural ocean-floor height -- a real vanilla noise-based terrain query, not a
     * cheap lookup -- is computed once per column instead of twice; see MEMORY.md's
     * 2026-07-17 performance entry for the in-game symptoms this fixed.
     */
    private void applyTerrainAdjustments(ChunkAccess chunk, RandomState randomState, boolean repairOnly) {
        if (this.layout.isEmpty() && this.starterLand.isEmpty() && !this.island.enabled()) {
            return;
        }
        WorldLayoutPlan plan = this.layout.map(LayoutContext::plan).orElse(null);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int seaLevel = getSeaLevel();
        int originX = originX();
        int originZ = originZ();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                if (this.effectiveModeAt(x - originX, z - originZ) != ExteriorMode.NORMAL) {
                    continue;
                }
                int naturalFloor = naturalOceanFloorHeight(x, z, chunk, randomState);
                int layoutFloor = naturalFloor;

                if (plan != null) {
                    layoutFloor = layoutFloorFor(plan, x, z, originX, originZ, naturalFloor, seaLevel, randomState);
                    if (layoutFloor > naturalFloor) {
                        int minY = StarterLandProfile.foundationMinY(naturalFloor, DEFAULT_LAYOUT_FOUNDATION_DEPTH_BLOCKS, chunk.getMinY());
                        int maxY = repairOnly ? layoutFloor - 1 - PRESERVED_SURFACE_SHELL_BLOCKS : layoutFloor - 1;
                        fillStarterColumn(chunk, pos, x, z, minY, maxY, naturalFloor);
                    } else if (layoutFloor < naturalFloor && !repairOnly) {
                        // Lowering only clears solid ground down to open water/air; there is nothing
                        // for a carver-stage repair pass to restore, unlike a starter-land raise.
                        lowerColumn(chunk, pos, x, z, layoutFloor, naturalFloor - 1, seaLevel);
                    }
                }

                if (this.starterLand.isPresent()) {
                    int targetHeight = starterLandTargetHeight(x, z, chunk, randomState, naturalFloor, layoutFloor);
                    if (targetHeight > naturalFloor) {
                        int minY = StarterLandProfile.foundationMinY(
                            naturalFloor, this.starterLand.get().plan().foundationDepthBlocks(), chunk.getMinY()
                        );
                        int maxY = repairOnly ? targetHeight - 1 - PRESERVED_SURFACE_SHELL_BLOCKS : targetHeight - 1;
                        fillStarterColumn(chunk, pos, x, z, minY, maxY, naturalFloor);
                    }
                }

                if (this.island.enabled()) {
                    int islandHeight = islandTargetHeight(x, z, chunk, randomState, naturalFloor, layoutFloor);
                    if (islandHeight > naturalFloor) {
                        int minY = StarterLandProfile.foundationMinY(naturalFloor, DEFAULT_LAYOUT_FOUNDATION_DEPTH_BLOCKS, chunk.getMinY());
                        int maxY = repairOnly ? islandHeight - 1 - PRESERVED_SURFACE_SHELL_BLOCKS : islandHeight - 1;
                        fillStarterColumn(chunk, pos, x, z, minY, maxY, naturalFloor);
                    }
                }
            }
        }
    }

    /**
     * Computes the layout's target floor height by blending the sampled land/ocean factor,
     * unless the vanilla pass-through (DESIGN §20.5, GOALS 13/14) applies at this column --
     * then the natural floor is returned untouched, since a passed-through river/ocean is
     * real, natural vanilla terrain, not something to raise toward guaranteed land.
     *
     * @param x absolute block X
     * @param z absolute block Z
     */
    private int layoutFloorFor(
        WorldLayoutPlan plan, int x, int z, int originX, int originZ, int naturalFloor, int seaLevel, RandomState randomState
    ) {
        if (this.originSource.isPresent()
            && this.originSource.get().isNaturalPassThroughAt(x, naturalFloor, z, randomState.sampler())) {
            return naturalFloor;
        }
        double landFactor = plan.sampleAt(x - originX, z - originZ).landFactor();
        return LayoutTerrainProfile.targetHeight(naturalFloor, landFactor, seaLevel);
    }

    /** Returns the layout-adjusted floor, or {@code naturalFloor} unchanged with no active layout. */
    private int layoutFloorOrNatural(int x, int z, int naturalFloor, RandomState randomState) {
        return this.layout.isPresent()
            ? layoutFloorFor(this.layout.get().plan(), x, z, originX(), originZ(), naturalFloor, getSeaLevel(), randomState)
            : naturalFloor;
    }

    /**
     * Computes the starter-land target height for one column. Blends back toward
     * {@code blendBaseline} (the layout-adjusted floor when a layout is active, otherwise
     * the natural floor -- callers already have this value, so it is passed in rather than
     * recomputed) so the starter island's transition connects to what generation will
     * actually leave beyond it, rather than jumping to unrelated natural shape.
     */
    private int starterLandTargetHeight(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState,
        int naturalFloor,
        int blendBaseline
    ) {
        if (this.starterLand.isEmpty()) {
            return heightAccessor.getMinY();
        }
        StarterLandContext context = this.starterLand.get();
        int originX = originX();
        int originZ = originZ();
        double strength = StarterLandProfile.strengthAt(
            x - originX, z - originZ, context.radiusBlocks(), context.plan().transitionWidthBlocks()
        );
        int target;
        if (strength <= 0.0) {
            // Outside the starter zone and its transition, StarterLandProfile.targetHeight
            // always returns blendBaseline unchanged regardless of relief noise -- skip
            // sampling it. This is the common case for every column away from the starter
            // zone (i.e. almost the entire generated world).
            target = blendBaseline;
        } else {
            double reliefNoise = context.plan().profileVersion() <= StarterLandPlan.LEGACY_PROFILE_VERSION
                ? 0.0
                : randomState.getOrCreateNoise(Noises.SURFACE_SECONDARY).getValue(
                    x * StarterLandProfile.RELIEF_NOISE_SCALE,
                    0.0,
                    z * StarterLandProfile.RELIEF_NOISE_SCALE
                );
            target = StarterLandProfile.targetHeight(
                x - originX,
                z - originZ,
                context.radiusBlocks(),
                context.plan().transitionWidthBlocks(),
                blendBaseline,
                getSeaLevel(),
                context.plan().profileVersion(),
                reliefNoise
            );
        }
        return Math.min(target, heightAccessor.getMaxY() + 1);
    }

    /**
     * Computes the ocean island's guaranteed-land raise (GOALS 01, DESIGN §24.3), mirroring
     * {@link #starterLandTargetHeight}'s shape exactly but keyed on the perturbed shore
     * distance instead of a raw circular one. Returns {@code heightAccessor.getMinY()} (a
     * harmless no-op once combined with {@code Math.max}) whenever the island is disabled,
     * beyond the exclusion zone, or at/beyond the shore's own outer edge -- the last case is
     * already masked to {@code OCEAN} by {@link #effectiveModeAt}, so there is nothing to
     * raise there regardless.
     */
    private int islandTargetHeight(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState,
        int naturalFloor,
        int blendBaseline
    ) {
        int relativeX = x - originX();
        int relativeZ = z - originZ();
        // Natural land (GOALS 02) is never artificially raised -- the real seed's own terrain
        // is left completely unmodified, unlike the artificially shaped island (GOALS 01).
        if (!this.island.enabled() || !this.island.hasLand() || !this.island.syntheticLand()
            || !this.island.withinExclusionZone(relativeX, relativeZ)) {
            return heightAccessor.getMinY();
        }
        double distance = this.island.distanceFromShore(relativeX, relativeZ, islandSeed());
        if (distance >= this.island.shoreWidthBlocks()) {
            return heightAccessor.getMinY();
        }
        double reliefNoise = randomState.getOrCreateNoise(Noises.SURFACE_SECONDARY).getValue(
            x * IslandShapeProfile.RELIEF_NOISE_SCALE, 0.0, z * IslandShapeProfile.RELIEF_NOISE_SCALE
        );
        int target = IslandShapeProfile.targetHeight(
            distance, this.island.shoreWidthBlocks(), blendBaseline, getSeaLevel(), reliefNoise
        );
        return Math.min(target, heightAccessor.getMaxY() + 1);
    }

    /**
     * Computes the ocean island's shallow-to-deep seabed depth at one column (GOALS 01, 02, 03,
     * DESIGN §24.5, §25.2, §25.4). Only ever called once {@link #island} is confirmed enabled.
     * When {@link IslandPlan#hasLand} is {@code false} (GOALS 03) or {@link
     * IslandPlan#syntheticLand} is {@code false} (GOALS 02) there is no shore ring to subtract,
     * so the raw distance from origin stands in for "distance beyond the shore" directly.
     */
    private int islandOceanDepthAt(int relativeX, int relativeZ) {
        double distance = this.island.distanceFromShore(relativeX, relativeZ, islandSeed());
        boolean hasShoreRing = this.island.hasLand() && this.island.syntheticLand();
        double beyondShore = hasShoreRing ? distance - this.island.shoreWidthBlocks() : distance;
        return IslandOceanProfile.floorDepthBelowSeaLevel(
            beyondShore,
            this.island.oceanShallowWidthBlocks(),
            this.island.oceanDeepenWidthBlocks(),
            this.island.oceanShallowDepthBlocks(),
            this.island.oceanDeepDepthBlocks()
        );
    }

    private int naturalOceanFloorHeight(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        return this.delegate.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
    }

    private static void fillStarterColumn(
        ChunkAccess chunk,
        BlockPos.MutableBlockPos pos,
        int x,
        int z,
        int minY,
        int maxY,
        int naturalFloor
    ) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int y = minY; y <= maxY; y++) {
            pos.set(x, y, z);
            BlockState oldState = chunk.getBlockState(pos);
            if (y >= naturalFloor || isReplaceableFoundation(oldState)) {
                if (oldState.hasBlockEntity()) {
                    chunk.removeBlockEntity(pos);
                }
                chunk.setBlockState(pos, stone, 0);
            }
        }
    }

    private static void fillStarterColumn(
        BlockState[] states,
        int columnMinY,
        int minY,
        int maxY,
        int naturalFloor
    ) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int y = minY; y <= maxY; y++) {
            int index = y - columnMinY;
            if (index >= 0 && index < states.length && (y >= naturalFloor || isReplaceableFoundation(states[index]))) {
                states[index] = stone;
            }
        }
    }

    private static void lowerColumn(
        ChunkAccess chunk,
        BlockPos.MutableBlockPos pos,
        int x,
        int z,
        int minY,
        int maxY,
        int seaLevel
    ) {
        for (int y = minY; y <= maxY; y++) {
            pos.set(x, y, z);
            BlockState newState = y < seaLevel ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            BlockState oldState = chunk.getBlockState(pos);
            if (oldState != newState) {
                if (oldState.hasBlockEntity()) {
                    chunk.removeBlockEntity(pos);
                }
                chunk.setBlockState(pos, newState, 0);
            }
        }
    }

    private static void lowerColumn(
        BlockState[] states,
        int columnMinY,
        int minY,
        int maxY,
        int seaLevel
    ) {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = minY; y <= maxY; y++) {
            int index = y - columnMinY;
            if (index >= 0 && index < states.length) {
                states[index] = y < seaLevel ? water : air;
            }
        }
    }

    private static BlockState[] copyColumn(NoiseColumn column, LevelHeightAccessor heightAccessor) {
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        int minY = heightAccessor.getMinY();
        for (int index = 0; index < states.length; index++) {
            states[index] = column.getBlock(minY + index);
        }
        return states;
    }

    private static boolean isReplaceableFoundation(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    /**
     * Forces a sky-void exterior around the starter island when the layout mode is
     * {@code VOID}, overriding any explicitly configured exterior for that dimension.
     * The island radius matches the starter zone plus its natural-land transition so
     * the guaranteed land is never cut off by the void boundary.
     */
    private static ExteriorPlan.DimensionEnvelope resolveEnvelope(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope configured
    ) {
        if (dimension != Dimension.OVERWORLD || !(delegate.getBiomeSource() instanceof LimitedBiomeSource source)) {
            return configured;
        }
        // Sky island (GOALS 05, DESIGN §27.5): reporting envelope() as a plain VOID envelope at
        // the island's own radius costs nothing extra and, for free, makes ObjectiveSite's
        // existing envelope-based supportiveRadius overload narrow the fallback End-portal
        // guarantee correctly -- no new ObjectiveSite code needed, unlike ocean island's IslandPlan
        // (which needed its own 4-arg overload because its shape isn't expressible this way).
        if (source.skyIsland().enabled()) {
            return new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, Math.max(source.skyIsland().radiusBlocks(), 1), 0);
        }
        if (source.worldLayoutPlan().mode() != LayoutMode.VOID) {
            return configured;
        }
        int islandRadius = source.starterBiome().isPresent()
            ? source.starterRadiusBlocks() + source.starterLandPlan().transitionWidthBlocks()
            : DEFAULT_VOID_ISLAND_RADIUS_BLOCKS;
        return new ExteriorPlan.DimensionEnvelope(ExteriorMode.VOID, Math.max(islandRadius, 1), 0);
    }

    private static Optional<StarterLandContext> resolveStarterLand(ChunkGenerator delegate, Dimension dimension) {
        if (dimension != Dimension.OVERWORLD || !(delegate.getBiomeSource() instanceof LimitedBiomeSource source)) {
            return Optional.empty();
        }
        StarterLandPlan plan = source.starterLandPlan();
        if (!plan.enabled() || source.starterBiome().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StarterLandContext(source.starterRadiusBlocks(), plan));
    }

    private static Optional<LayoutContext> resolveLayout(ChunkGenerator delegate, Dimension dimension) {
        if (dimension != Dimension.OVERWORLD || !(delegate.getBiomeSource() instanceof LimitedBiomeSource source)) {
            return Optional.empty();
        }
        LayoutMode mode = source.worldLayoutPlan().mode();
        // VOID's sky-island overlay is Phase 15.5 work; its placeholder sample
        // always reports full land factor, which would wrongly raise the whole
        // world instead of leaving it void. Skip terrain adjustment until then.
        // CHAOS (Phase 4.1) and STRIP_BANDS (Phase 6.3, GOALS 36) deliberately never
        // adjust terrain at all -- both require terrain shape to stay exactly vanilla,
        // only biome identity changes.
        // Mode is invariant regardless of when the real world seed resolves, so
        // gating on it here (rather than through the live effective plan) is safe.
        if (mode == LayoutMode.LEGACY || mode == LayoutMode.VOID || mode == LayoutMode.CHAOS || mode == LayoutMode.STRIP_BANDS) {
            return Optional.empty();
        }
        return Optional.of(new LayoutContext(source));
    }

    /**
     * Classifies one exterior block, substituting the ocean island's own fluid (GOALS 28/31,
     * DESIGN §26.1) for the "wet" layer -- lava instead of water, or air for a drained basin.
     * {@code fluid} is only ever non-{@code WATER} for island-driven columns; every other
     * exterior mode (strip_world's own OCEAN option, etc.) always passes {@code WATER}
     * unchanged.
     */
    private BlockState exteriorState(
        ExteriorMode mode, int y, LevelHeightAccessor heightAccessor, int depthBlocks, IslandFluid fluid
    ) {
        if (mode == ExteriorMode.VOID) {
            return Blocks.AIR.defaultBlockState();
        }
        return switch (ExteriorTerrainProfile.oceanLayerAt(
            y,
            heightAccessor.getMinY(),
            heightAccessor.getMaxY(),
            getSeaLevel(),
            depthBlocks
        )) {
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case STONE -> Blocks.STONE.defaultBlockState();
            case WATER -> switch (fluid) {
                case WATER -> Blocks.WATER.defaultBlockState();
                case LAVA -> Blocks.LAVA.defaultBlockState();
                case NONE -> Blocks.AIR.defaultBlockState();
            };
            case AIR -> Blocks.AIR.defaultBlockState();
        };
    }

    /**
     * Classifies one sky island block (GOALS 05/06/07/08, DESIGN §27.2/27.3/27.6/28.2): air
     * outside every footprint (starter or scattered) or outside the slab's vertical band,
     * otherwise a top/subsoil/core block. The Overworld picks its palette from {@link
     * SkyIslandProfile}'s biome-family classification (§27.3), keyed off {@code hit}'s own biome
     * so a scattered island with biome variety (GOALS 08) gets its own palette instead of always
     * reusing the starter island's; the Nether has no meaningful biome to key off (DESIGN §27.6
     * deliberately doesn't force one) and always uses a simple netherrack-family palette instead.
     * Neither dimension's chunk ever runs the delegate's biome-aware surface builder (§27.2), so
     * this is the sky island's own surface-material choice either way.
     */
    private BlockState skyIslandStateAt(SkyIslandHit hit, int y) {
        if (!hit.present()) {
            return Blocks.AIR.defaultBlockState();
        }
        SkyIslandPlan active = activeSkyIsland();
        SkyIslandProfile.Layer layer = SkyIslandProfile.layerAt(y, active.surfaceY(), active.thicknessBlocks());
        if (this.dimension == Dimension.NETHER) {
            return switch (layer) {
                case VOID -> Blocks.AIR.defaultBlockState();
                case TOP, SUBSOIL -> Blocks.NETHERRACK.defaultBlockState();
                case CORE -> Blocks.BASALT.defaultBlockState();
            };
        }
        SkyIslandProfile.BiomeFamily family = SkyIslandProfile.familyFor(hit.biome());
        return switch (layer) {
            case VOID -> Blocks.AIR.defaultBlockState();
            case TOP -> skyIslandTopBlock(family);
            case SUBSOIL -> skyIslandSubsoilBlock(family);
            case CORE -> Blocks.STONE.defaultBlockState();
        };
    }

    private static BlockState skyIslandTopBlock(SkyIslandProfile.BiomeFamily family) {
        return switch (family) {
            case DESERT -> Blocks.SAND.defaultBlockState();
            case SNOWY -> Blocks.SNOW_BLOCK.defaultBlockState();
            case MUSHROOM -> Blocks.MYCELIUM.defaultBlockState();
            case WATER -> Blocks.PACKED_ICE.defaultBlockState();
            case DEFAULT -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    private static BlockState skyIslandSubsoilBlock(SkyIslandProfile.BiomeFamily family) {
        return family == SkyIslandProfile.BiomeFamily.DESERT
            ? Blocks.SANDSTONE.defaultBlockState()
            : Blocks.DIRT.defaultBlockState();
    }

    private static boolean isOceanFloor(Heightmap.Types type) {
        return type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
    }

    private boolean isEntirelyExterior(ChunkPos chunkPos) {
        return allCornersMatch(chunkPos, mode -> mode != ExteriorMode.NORMAL);
    }

    /** Narrower than {@link #isEntirelyExterior}: every corner must specifically be OCEAN, not VOID. */
    private boolean isEntirelyExteriorOcean(ChunkPos chunkPos) {
        return allCornersMatch(chunkPos, mode -> mode == ExteriorMode.OCEAN);
    }

    private boolean allCornersMatch(ChunkPos chunkPos, Predicate<ExteriorMode> predicate) {
        int originX = originX();
        int originZ = originZ();
        int minX = chunkPos.getMinBlockX() - originX;
        int maxX = chunkPos.getMaxBlockX() - originX;
        int minZ = chunkPos.getMinBlockZ() - originZ;
        int maxZ = chunkPos.getMaxBlockZ() - originZ;
        return predicate.test(this.effectiveModeAt(minX, minZ))
            && predicate.test(this.effectiveModeAt(minX, maxZ))
            && predicate.test(this.effectiveModeAt(maxX, minZ))
            && predicate.test(this.effectiveModeAt(maxX, maxZ));
    }

    /** Which dimension one {@code EnvelopedChunkGenerator} instance wraps. */
    public enum Dimension {
        /** The Overworld. */
        OVERWORLD("overworld"),
        /** The Nether. */
        NETHER("nether"),
        /** The End -- only ever wrapped by the {@code sky_chunk} preset (DESIGN §29.5). */
        END("end");

        private static final Codec<Dimension> CODEC = Codec.STRING.xmap(Dimension::parse, value -> value.serializedName);
        private final String serializedName;

        Dimension(String serializedName) {
            this.serializedName = serializedName;
        }

        private static Dimension parse(String value) {
            return switch (value) {
                case "overworld" -> OVERWORLD;
                case "nether" -> NETHER;
                case "end" -> END;
                default -> throw new IllegalArgumentException("Unknown Worldz generator dimension: " + value);
            };
        }
    }

    private record StarterLandContext(int radiusBlocks, StarterLandPlan plan) {
    }

    /**
     * Holds the source rather than a snapshotted plan so every read reflects the real
     * world seed once {@link LimitedBiomeSource#setLayoutSeed(long)} resolves it --
     * this context is captured at codec-decode time, before that resolution happens.
     */
    private record LayoutContext(LimitedBiomeSource source) {
        WorldLayoutPlan plan() {
            return source.effectiveLayoutPlan();
        }
    }
}
