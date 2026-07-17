package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;
import media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.LayoutTerrainProfile;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterLandProfile;
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
        ExteriorCodecs.DIMENSION_CODEC.optionalFieldOf("exterior").forGetter(generator -> Optional.of(generator.envelope))
    ).apply(instance, EnvelopedChunkGenerator::resolve));

    private final ChunkGenerator delegate;
    private final Dimension dimension;
    private final ExteriorPlan.DimensionEnvelope envelope;
    private final Optional<StarterLandContext> starterLand;
    private final Optional<LayoutContext> layout;
    private final Optional<LimitedBiomeSource> originSource;

    private EnvelopedChunkGenerator(
        ChunkGenerator delegate,
        Dimension dimension,
        ExteriorPlan.DimensionEnvelope envelope
    ) {
        super(delegate.getBiomeSource());
        this.delegate = delegate;
        this.dimension = dimension;
        this.envelope = resolveEnvelope(delegate, dimension, envelope);
        this.starterLand = resolveStarterLand(delegate, dimension);
        this.layout = resolveLayout(delegate, dimension);
        this.originSource = dimension == Dimension.OVERWORLD && delegate.getBiomeSource() instanceof LimitedBiomeSource source
            ? Optional.of(source)
            : Optional.empty();
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
     * Wraps a generator with an explicit envelope selected during world creation.
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
        return new EnvelopedChunkGenerator(delegate, overworld ? Dimension.OVERWORLD : Dimension.NETHER, envelope);
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
     * Returns the persisted envelope.
     *
     * @return resolved dimension envelope
     */
    public ExteriorPlan.DimensionEnvelope envelope() {
        return this.envelope;
    }

    private static EnvelopedChunkGenerator resolve(
        ChunkGenerator delegate,
        Dimension dimension,
        Optional<ExteriorPlan.DimensionEnvelope> encodedEnvelope
    ) {
        ExteriorPlan defaults = ExteriorPlan.fromConfig(WorldzCommon.config());
        ExteriorPlan.DimensionEnvelope envelope = encodedEnvelope.orElseGet(
            () -> dimension == Dimension.OVERWORLD ? defaults.overworld() : defaults.nether()
        );
        return new EnvelopedChunkGenerator(delegate, dimension, envelope);
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
        applyLayoutAdjustment(chunk, randomState, true);
        applyStarterLand(chunk, randomState, true);
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
        if (!isEntirelyExterior(chunk.getPos())) {
            this.delegate.applyBiomeDecoration(level, chunk, structureManager);
        }
        applyEnvelope(chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (!isEntirelyExterior(region.getCenter())) {
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
        if (!isEntirelyExterior(centerChunk.getPos())) {
            super.createStructures(registryAccess, state, structureManager, centerChunk, structureTemplateManager, level);
        }
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
                applyLayoutAdjustment(chunk, randomState, false);
                applyStarterLand(chunk, randomState, false);
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
        ExteriorMode mode = this.envelope.modeAt(x - originX(), z - originZ());
        if (mode == ExteriorMode.NORMAL) {
            int naturalHeight = this.delegate.getBaseHeight(x, z, type, heightAccessor, randomState);
            int layoutHeight = layoutAdjustedHeight(x, z, naturalHeight, heightAccessor, randomState);
            return Math.max(layoutHeight, starterLandTargetHeight(x, z, heightAccessor, randomState));
        }
        return ExteriorTerrainProfile.baseHeight(
            mode,
            isOceanFloor(type),
            heightAccessor.getMinY(),
            heightAccessor.getMaxY(),
            getSeaLevel()
        );
    }

    @Override
    public NoiseColumn getBaseColumn(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        ExteriorMode mode = this.envelope.modeAt(x - originX(), z - originZ());
        if (mode == ExteriorMode.NORMAL) {
            NoiseColumn naturalColumn = this.delegate.getBaseColumn(x, z, heightAccessor, randomState);
            int naturalFloor = naturalOceanFloorHeight(x, z, heightAccessor, randomState);
            BlockState[] states = null;

            if (this.layout.isPresent()) {
                int layoutFloor = layoutFloorFor(this.layout.get().plan(), x - originX(), z - originZ(), naturalFloor, getSeaLevel());
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

            int targetHeight = starterLandTargetHeight(x, z, heightAccessor, randomState, naturalFloor);
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

            return states == null ? naturalColumn : new NoiseColumn(heightAccessor.getMinY(), states);
        }
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        int minY = heightAccessor.getMinY();
        for (int index = 0; index < states.length; index++) {
            states[index] = exteriorState(mode, minY + index, heightAccessor);
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        this.delegate.addDebugScreenInfo(result, randomState, feetPos);
        result.add("Worldz exterior: " + this.envelope.mode().serializedName());
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
        if (this.envelope.mode() == ExteriorMode.NORMAL) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                ExteriorMode mode = this.envelope.modeAt(x - originX(), z - originZ());
                if (mode != ExteriorMode.NORMAL) {
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = exteriorState(mode, y, chunk);
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

    private void applyLayoutAdjustment(ChunkAccess chunk, RandomState randomState, boolean repairOnly) {
        if (this.layout.isEmpty()) {
            return;
        }
        WorldLayoutPlan plan = this.layout.get().plan();
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int seaLevel = getSeaLevel();
        int originX = originX();
        int originZ = originZ();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                if (this.envelope.modeAt(x - originX, z - originZ) != ExteriorMode.NORMAL) {
                    continue;
                }
                int naturalFloor = naturalOceanFloorHeight(x, z, chunk, randomState);
                int layoutFloor = layoutFloorFor(plan, x - originX, z - originZ, naturalFloor, seaLevel);
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
        }
    }

    /**
     * Shifts an arbitrary heightmap query by the same delta the layout applies
     * to the ocean-floor baseline, so every {@link Heightmap.Types} agrees with
     * the actual raised or lowered column without special-casing each type.
     */
    private int layoutAdjustedHeight(
        int x,
        int z,
        int naturalHeight,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        if (this.layout.isEmpty()) {
            return naturalHeight;
        }
        int naturalFloor = naturalOceanFloorHeight(x, z, heightAccessor, randomState);
        int layoutFloor = layoutFloorFor(this.layout.get().plan(), x - originX(), z - originZ(), naturalFloor, getSeaLevel());
        return naturalHeight + (layoutFloor - naturalFloor);
    }

    /** Computes the layout's target floor height by blending the sampled land/ocean factor. */
    private static int layoutFloorFor(WorldLayoutPlan plan, int x, int z, int naturalFloor, int seaLevel) {
        double landFactor = plan.sampleAt(x, z).landFactor();
        return LayoutTerrainProfile.targetHeight(naturalFloor, landFactor, seaLevel);
    }

    private void applyStarterLand(ChunkAccess chunk, RandomState randomState, boolean repairOnly) {
        if (this.starterLand.isEmpty()) {
            return;
        }
        StarterLandPlan plan = this.starterLand.get().plan();
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int naturalFloor = naturalOceanFloorHeight(x, z, chunk, randomState);
                int targetHeight = starterLandTargetHeight(x, z, chunk, randomState, naturalFloor);
                if (targetHeight <= naturalFloor) {
                    continue;
                }
                int minY = StarterLandProfile.foundationMinY(
                    naturalFloor, plan.foundationDepthBlocks(), chunk.getMinY()
                );
                int maxY = repairOnly ? targetHeight - 1 - PRESERVED_SURFACE_SHELL_BLOCKS : targetHeight - 1;
                fillStarterColumn(chunk, pos, x, z, minY, maxY, naturalFloor);
            }
        }
    }

    private int starterLandTargetHeight(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState
    ) {
        if (this.starterLand.isEmpty()) {
            return heightAccessor.getMinY();
        }
        int naturalFloor = naturalOceanFloorHeight(x, z, heightAccessor, randomState);
        return starterLandTargetHeight(x, z, heightAccessor, randomState, naturalFloor);
    }

    private int starterLandTargetHeight(
        int x,
        int z,
        LevelHeightAccessor heightAccessor,
        RandomState randomState,
        int naturalFloor
    ) {
        if (this.starterLand.isEmpty()) {
            return heightAccessor.getMinY();
        }
        StarterLandContext context = this.starterLand.get();
        double reliefNoise = context.plan().profileVersion() <= StarterLandPlan.LEGACY_PROFILE_VERSION
            ? 0.0
            : randomState.getOrCreateNoise(Noises.SURFACE_SECONDARY).getValue(
                x * StarterLandProfile.RELIEF_NOISE_SCALE,
                0.0,
                z * StarterLandProfile.RELIEF_NOISE_SCALE
            );
        int originX = originX();
        int originZ = originZ();
        // Blend back toward the layout-adjusted floor (not raw vanilla terrain) so the
        // starter island's transition connects to what generation will actually leave
        // beyond it (e.g. an ocean-mode cap), rather than jumping to unrelated natural shape.
        int blendBaseline = this.layout.isPresent()
            ? layoutFloorFor(this.layout.get().plan(), x - originX, z - originZ, naturalFloor, getSeaLevel())
            : naturalFloor;
        int target = StarterLandProfile.targetHeight(
            x - originX,
            z - originZ,
            context.radiusBlocks(),
            context.plan().transitionWidthBlocks(),
            blendBaseline,
            getSeaLevel(),
            context.plan().profileVersion(),
            reliefNoise
        );
        return Math.min(target, heightAccessor.getMaxY() + 1);
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
        // Mode is invariant regardless of when the real world seed resolves, so
        // gating on it here (rather than through the live effective plan) is safe.
        if (mode == LayoutMode.LEGACY || mode == LayoutMode.VOID) {
            return Optional.empty();
        }
        return Optional.of(new LayoutContext(source));
    }

    private BlockState exteriorState(ExteriorMode mode, int y, LevelHeightAccessor heightAccessor) {
        if (mode == ExteriorMode.VOID) {
            return Blocks.AIR.defaultBlockState();
        }
        return switch (ExteriorTerrainProfile.oceanLayerAt(
            y,
            heightAccessor.getMinY(),
            heightAccessor.getMaxY(),
            getSeaLevel()
        )) {
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case STONE -> Blocks.STONE.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
        };
    }

    private static boolean isOceanFloor(Heightmap.Types type) {
        return type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
    }

    private boolean isEntirelyExterior(ChunkPos chunkPos) {
        int originX = originX();
        int originZ = originZ();
        int minX = chunkPos.getMinBlockX() - originX;
        int maxX = chunkPos.getMaxBlockX() - originX;
        int minZ = chunkPos.getMinBlockZ() - originZ;
        int maxZ = chunkPos.getMaxBlockZ() - originZ;
        return this.envelope.modeAt(minX, minZ) != ExteriorMode.NORMAL
            && this.envelope.modeAt(minX, maxZ) != ExteriorMode.NORMAL
            && this.envelope.modeAt(maxX, minZ) != ExteriorMode.NORMAL
            && this.envelope.modeAt(maxX, maxZ) != ExteriorMode.NORMAL;
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
