package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Persisted, versioned, coordinated world-layout plan (DESIGN §17). Pure and
 * independent of any Minecraft class: it classifies a block column into a
 * {@link BiomeRole}, picks an allowed biome within that role, and reports a
 * continuous land/ocean blend factor, all as deterministic functions of the
 * baked-in world seed. {@code LimitedBiomeSource} and the enveloped chunk
 * generator (Phase 15.4) sample the same plan so biome identity and terrain
 * shape can never disagree.
 *
 * @param mode layout mode
 * @param seed source of all sampling randomness, supplied by the caller at
 *     world creation; wiring it to the actual Minecraft world seed is a
 *     Phase 15.4 integration concern, not this pure model's
 * @param regionScaleBlocks grid-cell edge length in blocks
 * @param oceanCoverageFraction {@code MIXED} target fraction of cells classified ocean, {@code 0..1}
 * @param coastBlendWidthBlocks smoothing distance either side of a role boundary
 * @param landBiomes weighted candidates for the {@code LAND} role
 * @param oceanBiomes weighted candidates for the {@code OCEAN} role
 * @param beachBiomes weighted candidates for the {@code BEACH} coast-transition role
 * @param singleBiome the {@code SINGLE_BIOME} mode's one biome id
 * @param roleOverrides explicit id-to-role overrides layered on {@link BiomeRoles}
 * @param layoutOriginBlockX grid and starter-overlay center X; {@code 0} until Phase 16 (§18)
 * @param layoutOriginBlockZ grid and starter-overlay center Z; {@code 0} until Phase 16 (§18)
 * @param algorithmRevision persisted sampling-algorithm revision
 */
public record WorldLayoutPlan(
    LayoutMode mode,
    long seed,
    int regionScaleBlocks,
    double oceanCoverageFraction,
    int coastBlendWidthBlocks,
    List<BiomeWeight> landBiomes,
    List<BiomeWeight> oceanBiomes,
    List<BiomeWeight> beachBiomes,
    Optional<String> singleBiome,
    Map<String, BiomeRole> roleOverrides,
    int layoutOriginBlockX,
    int layoutOriginBlockZ,
    int algorithmRevision
) {
    /** Revision of every plan decoded before Phase 15: no layout sampling occurs. */
    public static final int LEGACY_MODE_REVISION = 0;
    /** Sampling-algorithm revision used by newly created layout-mode worlds. */
    public static final int CURRENT_REVISION = 1;

    /** Fixture-verified default grid-cell edge length; see DESIGN §17. */
    public static final int DEFAULT_REGION_SCALE_BLOCKS = 512;
    /** Fixture-verified default {@code MIXED} ocean coverage target. */
    public static final double DEFAULT_OCEAN_COVERAGE_FRACTION = 0.35;
    /** Fixture-verified default coast-blend width. */
    public static final int DEFAULT_COAST_BLEND_WIDTH_BLOCKS = 128;

    /** Validates the persisted plan. */
    public WorldLayoutPlan {
        if (mode == null) {
            throw new IllegalArgumentException("Layout mode is required.");
        }
        if (regionScaleBlocks < WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
            || regionScaleBlocks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS) {
            throw new IllegalArgumentException("Region scale is outside the supported range.");
        }
        if (coastBlendWidthBlocks < 0 || coastBlendWidthBlocks > WorldzConfig.MAX_LAYOUT_COAST_BLEND_WIDTH_BLOCKS) {
            throw new IllegalArgumentException("Coast blend width is outside the supported range.");
        }
        if (oceanCoverageFraction < 0.0 || oceanCoverageFraction > 1.0) {
            throw new IllegalArgumentException("Ocean coverage fraction must be between 0 and 1.");
        }
        if (algorithmRevision < LEGACY_MODE_REVISION) {
            throw new IllegalArgumentException("Algorithm revision must not be negative.");
        }
        landBiomes = validateWeights(landBiomes, "land");
        oceanBiomes = validateWeights(oceanBiomes, "ocean");
        beachBiomes = validateWeights(beachBiomes, "beach");
        singleBiome = singleBiome == null ? Optional.empty() : singleBiome;
        roleOverrides = Map.copyOf(roleOverrides);

        if (mode == LayoutMode.SINGLE_BIOME && singleBiome.map(String::isBlank).orElse(true)) {
            throw new IllegalArgumentException("Single-biome layouts require a biome id.");
        }
        if (mode == LayoutMode.LAND_ONLY && landBiomes.isEmpty()) {
            throw new IllegalArgumentException("Land-only layouts require at least one land biome.");
        }
        if (mode == LayoutMode.OCEAN && oceanBiomes.isEmpty()) {
            throw new IllegalArgumentException("Ocean layouts require at least one ocean biome.");
        }
        if (mode == LayoutMode.MIXED && (landBiomes.isEmpty() || oceanBiomes.isEmpty())) {
            throw new IllegalArgumentException("Mixed layouts require at least one land biome and one ocean biome.");
        }
    }

    private static List<BiomeWeight> validateWeights(List<BiomeWeight> weights, String roleName) {
        List<BiomeWeight> copy = List.copyOf(weights);
        Set<String> seen = new LinkedHashSet<>();
        for (BiomeWeight weight : copy) {
            if (!seen.add(weight.biomeId())) {
                throw new IllegalArgumentException("Duplicate " + roleName + " biome id '" + weight.biomeId() + "'.");
            }
        }
        return copy;
    }

    /**
     * Returns the compatibility plan decoded for every world created before Phase 15.
     *
     * @return a plan that samples nothing; callers keep using the legacy climate-filter path
     */
    public static WorldLayoutPlan legacy() {
        return new WorldLayoutPlan(
            LayoutMode.LEGACY, 0L, DEFAULT_REGION_SCALE_BLOCKS, 0.0, DEFAULT_COAST_BLEND_WIDTH_BLOCKS,
            List.of(), List.of(), List.of(), Optional.empty(), Map.of(), 0, 0, LEGACY_MODE_REVISION
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration and a caller-supplied seed.
     * Each configured weighted biome id is classified into a role via
     * {@link BiomeRoles#resolve(String, Map)} using the configured overrides.
     *
     * @param config sanitized startup configuration
     * @param seed sampling seed (see {@link #seed()})
     * @return immutable resolved plan for a newly created world
     */
    public static WorldLayoutPlan fromConfig(WorldzConfig config, long seed) {
        var layout = config.layout;
        Map<String, BiomeRole> overrides = new LinkedHashMap<>();
        layout.roleOverrides.forEach((id, role) -> overrides.put(id, BiomeRole.parse(role)));

        List<BiomeWeight> land = new ArrayList<>();
        List<BiomeWeight> ocean = new ArrayList<>();
        List<BiomeWeight> beach = new ArrayList<>();
        for (WeightedBiomeListSpec.Entry entry : WeightedBiomeListSpec.parse(layout.biomes).entries()) {
            BiomeWeight weight = new BiomeWeight(entry.id(), entry.weight());
            switch (BiomeRoles.resolve(entry.id(), overrides)) {
                case LAND -> land.add(weight);
                case OCEAN -> ocean.add(weight);
                case BEACH -> beach.add(weight);
            }
        }

        return new WorldLayoutPlan(
            layout.mode,
            seed,
            layout.regionScaleBlocks,
            layout.oceanCoverageFraction,
            layout.coastBlendWidthBlocks,
            land,
            ocean,
            beach,
            layout.singleBiome.isBlank() ? Optional.empty() : Optional.of(layout.singleBiome),
            overrides,
            0,
            0,
            CURRENT_REVISION
        );
    }

    /**
     * Samples the plan at one block column.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the resolved role, allowed biome (empty only if the role has no candidates), and land factor
     */
    public LayoutSample sampleAt(int blockX, int blockZ) {
        return switch (mode) {
            case LEGACY -> new LayoutSample(BiomeRole.LAND, Optional.empty(), 1.0);
            case LAND_ONLY -> sampleUniform(BiomeRole.LAND, landBiomes, blockX, blockZ, "biome_land");
            case OCEAN -> sampleUniform(BiomeRole.OCEAN, oceanBiomes, blockX, blockZ, "biome_ocean");
            case SINGLE_BIOME -> {
                BiomeRole role = BiomeRoles.resolve(singleBiome.orElseThrow(), roleOverrides);
                yield new LayoutSample(role, singleBiome, roleFactor(role));
            }
            case VOID -> new LayoutSample(BiomeRole.LAND, Optional.empty(), 1.0);
            case MIXED -> sampleMixed(blockX, blockZ);
        };
    }

    private LayoutSample sampleUniform(BiomeRole role, List<BiomeWeight> candidates, int blockX, int blockZ, String salt) {
        long cellX = Math.floorDiv((long) blockX - layoutOriginBlockX, regionScaleBlocks);
        long cellZ = Math.floorDiv((long) blockZ - layoutOriginBlockZ, regionScaleBlocks);
        return new LayoutSample(role, weightedPick(candidates, cellX, cellZ, salt), roleFactor(role));
    }

    private LayoutSample sampleMixed(int blockX, int blockZ) {
        long rx = (long) blockX - layoutOriginBlockX;
        long rz = (long) blockZ - layoutOriginBlockZ;
        long cellX = Math.floorDiv(rx, regionScaleBlocks);
        long cellZ = Math.floorDiv(rz, regionScaleBlocks);
        BiomeRole baseRole = mixedCellRole(cellX, cellZ);

        Boundary nearest = nearestDifferingBoundary(rx, rz, cellX, cellZ, baseRole);
        if (nearest == null) {
            return new LayoutSample(baseRole, weightedPick(rolePool(baseRole), cellX, cellZ, biomeSalt(baseRole)), roleFactor(baseRole));
        }

        double t = clamp((nearest.signedDistance + coastBlendWidthBlocks) / (2.0 * coastBlendWidthBlocks), 0.0, 1.0);
        double s = smoothstep(t);
        double landFactor = lerp(roleFactor(nearest.negativeSideRole), roleFactor(nearest.positiveSideRole), s);
        BiomeRole dominant = s < 0.5 ? nearest.negativeSideRole : nearest.positiveSideRole;
        // Resolve which side's cell coordinates to sample biomes from.
        long sampleCellX = cellX;
        long sampleCellZ = cellZ;
        if (dominant != baseRole) {
            sampleCellX = nearest.neighborCellX;
            sampleCellZ = nearest.neighborCellZ;
        }
        if (!beachBiomes.isEmpty() && nearest.negativeSideRole != nearest.positiveSideRole) {
            long coastKeyA = nearest.axisIsX ? nearest.boundary : cellX;
            long coastKeyB = nearest.axisIsX ? cellZ : nearest.boundary;
            return new LayoutSample(BiomeRole.BEACH, weightedPick(beachBiomes, coastKeyA, coastKeyB, "biome_beach"), landFactor);
        }
        return new LayoutSample(dominant, weightedPick(rolePool(dominant), sampleCellX, sampleCellZ, biomeSalt(dominant)), landFactor);
    }

    private List<BiomeWeight> rolePool(BiomeRole role) {
        return switch (role) {
            case LAND -> landBiomes;
            case OCEAN -> oceanBiomes;
            case BEACH -> beachBiomes;
        };
    }

    private static String biomeSalt(BiomeRole role) {
        return switch (role) {
            case LAND -> "biome_land";
            case OCEAN -> "biome_ocean";
            case BEACH -> "biome_beach";
        };
    }

    private BiomeRole mixedCellRole(long cellX, long cellZ) {
        double u = hash01(seed, "role", cellX, cellZ, 0);
        return u < oceanCoverageFraction ? BiomeRole.OCEAN : BiomeRole.LAND;
    }

    private Boundary nearestDifferingBoundary(long rx, long rz, long cellX, long cellZ, BiomeRole baseRole) {
        long localX = Math.floorMod(rx, regionScaleBlocks);
        long localZ = Math.floorMod(rz, regionScaleBlocks);

        Boundary best = null;
        best = considerBoundary(best, true, cellX * regionScaleBlocks, localX, cellX - 1, cellZ, baseRole, true);
        best = considerBoundary(best, true, (cellX + 1) * regionScaleBlocks, regionScaleBlocks - localX, cellX + 1, cellZ, baseRole, false);
        best = considerBoundary(best, false, cellZ * regionScaleBlocks, localZ, cellX, cellZ - 1, baseRole, true);
        best = considerBoundary(best, false, (cellZ + 1) * regionScaleBlocks, regionScaleBlocks - localZ, cellX, cellZ + 1, baseRole, false);
        return best;
    }

    private Boundary considerBoundary(
        Boundary current,
        boolean axisIsX,
        long boundary,
        long distance,
        long neighborCellX,
        long neighborCellZ,
        BiomeRole baseRole,
        boolean neighborIsLowSide
    ) {
        if (distance >= coastBlendWidthBlocks) {
            return current;
        }
        BiomeRole neighborRole = mixedCellRole(neighborCellX, neighborCellZ);
        if (neighborRole == baseRole) {
            return current;
        }
        if (current != null && current.distance <= distance) {
            return current;
        }
        BiomeRole negativeSide = neighborIsLowSide ? neighborRole : baseRole;
        BiomeRole positiveSide = neighborIsLowSide ? baseRole : neighborRole;
        long signedDistance = neighborIsLowSide ? distance : -distance;
        return new Boundary(axisIsX, boundary, distance, signedDistance, negativeSide, positiveSide, neighborCellX, neighborCellZ);
    }

    private static double roleFactor(BiomeRole role) {
        return role == BiomeRole.OCEAN ? 0.0 : 1.0;
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private Optional<String> weightedPick(List<BiomeWeight> candidates, long cellX, long cellZ, String salt) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        String best = null;
        double bestScore = -1.0;
        for (int index = 0; index < candidates.size(); index++) {
            BiomeWeight candidate = candidates.get(index);
            double u = hash01(seed, salt, cellX, cellZ, index);
            double score = Math.pow(u, 1.0 / candidate.weight());
            if (score > bestScore) {
                bestScore = score;
                best = candidate.biomeId();
            }
        }
        return Optional.ofNullable(best);
    }

    private static double hash01(long seed, String salt, long a, long b, long c) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt.hashCode());
        h = splitmix64(h ^ a);
        h = splitmix64(h ^ b);
        h = splitmix64(h ^ c);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /**
     * One weighted candidate biome within a role.
     *
     * @param biomeId canonical, namespaced biome id
     * @param weight positive selection weight
     */
    public record BiomeWeight(String biomeId, double weight) {
        /** Validates the weighted candidate. */
        public BiomeWeight {
            if (biomeId == null || biomeId.isBlank()) {
                throw new IllegalArgumentException("Biome id is required.");
            }
            if (!(weight > 0)) {
                throw new IllegalArgumentException("Biome weight must be positive.");
            }
        }
    }

    /**
     * One sampled block column.
     *
     * @param role resolved biome role
     * @param biomeId resolved biome id, empty only when the role has no configured candidates
     * @param landFactor {@code 0} fully ocean-shaped, {@code 1} fully land/beach-shaped
     */
    public record LayoutSample(BiomeRole role, Optional<String> biomeId, double landFactor) {
    }

    private record Boundary(
        boolean axisIsX,
        long boundary,
        long distance,
        long signedDistance,
        BiomeRole negativeSideRole,
        BiomeRole positiveSideRole,
        long neighborCellX,
        long neighborCellZ
    ) {
        private int towardNegative() {
            return negativeSideRole == positiveSideRole ? 0 : 1;
        }
    }
}
