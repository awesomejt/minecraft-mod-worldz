package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandOceanProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.LayoutTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandProfile;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterLandProfile;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.MobCategory;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
    /** Codec registered as {@code jlt_worldz:enveloped}. */
    public static final MapCodec<EnvelopedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ChunkGenerator.CODEC.fieldOf("delegate").forGetter(generator -> generator.delegate),
        Dimension.CODEC.fieldOf("dimension").forGetter(generator -> generator.dimension),
        ExteriorCodecs.DIMENSION_CODEC.optionalFieldOf("exterior").forGetter(generator -> Optional.of(generator.envelope)),
        StripCodecs.PLAN_CODEC.optionalFieldOf("strip").forGetter(generator -> Optional.of(generator.strip)),
        SkyIslandCodecs.PLAN_CODEC.optionalFieldOf("nether_sky_island")
            .forGetter(generator -> Optional.of(generator.netherSkyIsland))
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

    private EnvelopedChunkGenerator(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope,
        StripPlan strip,
        SkyIslandPlan netherSkyIsland
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
        return new EnvelopedChunkGenerator(
            delegate, overworld ? Dimension.OVERWORLD : Dimension.NETHER, envelope, strip, netherSkyIsland
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
            || this.island.enabled() || activeSkyIsland().enabled();
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
        Optional<SkyIslandPlan> encodedNetherSkyIsland
    ) {
        ExteriorPlan defaults = ExteriorPlan.fromConfig(WorldzCommon.config());
        ExteriorPlan.DimensionEnvelope envelope = encodedEnvelope.orElseGet(
            () -> dimension == Dimension.OVERWORLD ? defaults.overworld() : defaults.nether()
        );
        StripPlan strip = encodedStrip.orElseGet(
            () -> StripPlan.fromConfig(WorldzCommon.config().strip, dimension == Dimension.OVERWORLD)
        );
        SkyIslandPlan netherSkyIsland = encodedNetherSkyIsland.orElseGet(() -> {
            var skyIslandConfig = WorldzCommon.config().skyIsland;
            return dimension == Dimension.NETHER && skyIslandConfig.applyToNether
                ? SkyIslandPlan.fromConfig(skyIslandConfig)
                : SkyIslandPlan.disabled();
        });
        return new EnvelopedChunkGenerator(delegate, dimension, envelope, strip, netherSkyIsland);
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
        return this.delegate.createState(structureSets, randomState, legacyLevelSeed);
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
        this.delegate.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk);
        applyTerrainAdjustments(chunk, randomState, true);
        applyEnvelope(chunk);
    }

    @Override
    public void buildSurface(
        WorldGenRegion level,
        StructureManager structureManager,
        RandomState randomState,
        ChunkAccess protoChunk
    ) {
        this.delegate.buildSurface(level, structureManager, randomState, protoChunk);
        applyEnvelope(protoChunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();
        // isEntirelyExteriorOcean already implies isEntirelyExterior (OCEAN != NORMAL).
        boolean decorateExteriorOcean = decoratesExteriorOcean(chunkPos);
        if (!isEntirelyExterior(chunkPos) || decorateExteriorOcean) {
            this.delegate.applyBiomeDecoration(level, chunk, structureManager);
        }
        // Re-painting the exterior profile here would immediately erase whatever decoration
        // (kelp, seagrass, structure pieces) just placed -- skip it for a chunk we deliberately
        // decorated; the earlier applyCarvers/buildSurface passes already shaped its terrain.
        if (!decorateExteriorOcean) {
            applyEnvelope(chunk);
        }
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
        if (!isEntirelyExterior(centerPos) || decoratesExteriorOcean(centerPos)) {
            super.createStructures(registryAccess, state, structureManager, centerChunk, structureTemplateManager, level);
        }
    }

    /**
     * Whether an entirely-exterior chunk should still get vanilla decoration, original mob
     * population, and structures (kelp/seagrass, fish, shipwrecks, ocean ruins, monuments) --
     * true only for the ocean island's own artificial ocean (GOALS 01/02/03), which unlike
     * every other preset's exterior ocean is real, meant-to-be-explored space, not an
     * incidental boundary. Scoped to {@code island.enabled()} so strip_world/single_biome/
     * chaos_biomes's existing exterior-ocean behavior (a silent, decoration-free boundary,
     * unchanged since before this phase) is completely untouched.
     */
    private boolean decoratesExteriorOcean(ChunkPos chunkPos) {
        return this.island.enabled() && isEntirelyExteriorOcean(chunkPos);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        ChunkAccess centerChunk
    ) {
        return this.delegate.fillFromNoise(blender, randomState, structureManager, centerChunk)
            .thenApply(chunk -> {
                applyTerrainAdjustments(chunk, randomState, false);
                applyEnvelope(chunk);
                return chunk;
            });
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
     * {@code false}.
     */
    private record SkyIslandHit(boolean present, double distanceFromShore, String biome) {
    }

    private SkyIslandHit skyIslandHitAt(int relativeX, int relativeZ, SkyIslandPlan active) {
        double starterDistance = active.distanceFromShore(relativeX, relativeZ, skyIslandSeed());
        if (starterDistance <= 0.0) {
            return new SkyIslandHit(true, starterDistance, active.islandBiome());
        }
        FloatingIslandsPlan.Hit scattered = active.floatingIslands().at(relativeX, relativeZ, skyIslandSeed(), active.islandBiome());
        return scattered.present()
            ? new SkyIslandHit(true, scattered.distanceFromShore(), scattered.biome())
            : new SkyIslandHit(false, starterDistance, active.islandBiome());
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
            SkyIslandHit hit = skyIslandHitAt(x - originX(), z - originZ(), activeSkyIsland());
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
                "Worldz strip: widthRadius=" + this.strip.widthRadiusBlocks()
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

    private void applyEnvelope(ChunkAccess chunk) {
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
                        SkyIslandHit hit = skyIslandHitAt(relativeX, relativeZ, activeSkyIsland());
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

    private enum Dimension {
        OVERWORLD("overworld"),
        NETHER("nether");

        private static final Codec<Dimension> CODEC = Codec.STRING.xmap(Dimension::parse, value -> value.serializedName);
        private final String serializedName;

        Dimension(String serializedName) {
            this.serializedName = serializedName;
        }

        private static Dimension parse(String value) {
            return switch (value) {
                case "overworld" -> OVERWORLD;
                case "nether" -> NETHER;
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
