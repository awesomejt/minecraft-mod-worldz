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
 * @param landBiomes weighted candidates for the {@code LAND} role
 * @param oceanBiomes weighted candidates for the {@code OCEAN} role
 * @param beachBiomes weighted candidates for the {@code BEACH} coast-transition role
 * @param singleBiome the {@code SINGLE_BIOME} mode's one biome id
 * @param roleOverrides explicit id-to-role overrides layered on {@link BiomeRoles}
 * @param layoutOriginBlockX grid and starter-overlay center X; {@code 0} until Phase 16 (§18)
 * @param layoutOriginBlockZ grid and starter-overlay center Z; {@code 0} until Phase 16 (§18)
 * @param algorithmRevision persisted sampling-algorithm revision
 * @param bandBiomes the {@code STRIP_BANDS} mode's ordered, unweighted biome sequence walked
 *     along X (GOALS 36) -- already resolved to its final walk order at construction time
 *     (a one-time seed-derived shuffle happens in {@link #resolveBands}, not here), so
 *     reloading a saved world always repeats the same order rather than reshuffling
 */
public record WorldLayoutPlan(
    LayoutMode mode,
    long seed,
    int regionScaleBlocks,
    List<BiomeWeight> landBiomes,
    List<BiomeWeight> oceanBiomes,
    List<BiomeWeight> beachBiomes,
    Optional<String> singleBiome,
    Map<String, BiomeRole> roleOverrides,
    int layoutOriginBlockX,
    int layoutOriginBlockZ,
    int algorithmRevision,
    List<String> bandBiomes
) {
    /** Revision of every plan decoded before Phase 15: no layout sampling occurs. */
    public static final int LEGACY_MODE_REVISION = 0;
    /** Sampling-algorithm revision used by newly created layout-mode worlds. */
    public static final int CURRENT_REVISION = 1;

    /** Fixture-verified default grid-cell edge length; see DESIGN §17. */
    public static final int DEFAULT_REGION_SCALE_BLOCKS = 512;

    /** Validates the persisted plan. */
    public WorldLayoutPlan {
        if (mode == null) {
            throw new IllegalArgumentException("Layout mode is required.");
        }
        if (regionScaleBlocks < WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
            || regionScaleBlocks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS) {
            throw new IllegalArgumentException("Region scale is outside the supported range.");
        }
        if (algorithmRevision < LEGACY_MODE_REVISION) {
            throw new IllegalArgumentException("Algorithm revision must not be negative.");
        }
        landBiomes = validateWeights(landBiomes, "land");
        oceanBiomes = validateWeights(oceanBiomes, "ocean");
        beachBiomes = validateWeights(beachBiomes, "beach");
        singleBiome = singleBiome == null ? Optional.empty() : singleBiome;
        roleOverrides = Map.copyOf(roleOverrides);
        bandBiomes = List.copyOf(bandBiomes);

        if (mode == LayoutMode.SINGLE_BIOME && singleBiome.map(String::isBlank).orElse(true)) {
            throw new IllegalArgumentException("Single-biome layouts require a biome id.");
        }
        if (mode == LayoutMode.OCEAN && oceanBiomes.isEmpty()) {
            throw new IllegalArgumentException("Ocean layouts require at least one ocean biome.");
        }
        if (mode == LayoutMode.CHAOS && landBiomes.isEmpty()) {
            throw new IllegalArgumentException("Chaos layouts require at least one land biome.");
        }
        if (mode == LayoutMode.STRIP_BANDS && bandBiomes.isEmpty()) {
            throw new IllegalArgumentException("Strip-band layouts require at least one band biome.");
        }
    }

    /**
     * Creates a plan without a strip-bands sequence -- every mode except
     * {@code STRIP_BANDS}, which is the only mode {@link #bandBiomes} affects.
     *
     * @param mode layout mode
     * @param seed source of all sampling randomness
     * @param regionScaleBlocks grid-cell edge length in blocks
     * @param landBiomes weighted candidates for the {@code LAND} role
     * @param oceanBiomes weighted candidates for the {@code OCEAN} role
     * @param beachBiomes weighted candidates for the {@code BEACH} coast-transition role
     * @param singleBiome the {@code SINGLE_BIOME} mode's one biome id
     * @param roleOverrides explicit id-to-role overrides layered on {@link BiomeRoles}
     * @param layoutOriginBlockX grid and starter-overlay center X
     * @param layoutOriginBlockZ grid and starter-overlay center Z
     * @param algorithmRevision persisted sampling-algorithm revision
     */
    public WorldLayoutPlan(
        LayoutMode mode,
        long seed,
        int regionScaleBlocks,
        List<BiomeWeight> landBiomes,
        List<BiomeWeight> oceanBiomes,
        List<BiomeWeight> beachBiomes,
        Optional<String> singleBiome,
        Map<String, BiomeRole> roleOverrides,
        int layoutOriginBlockX,
        int layoutOriginBlockZ,
        int algorithmRevision
    ) {
        this(
            mode, seed, regionScaleBlocks, landBiomes, oceanBiomes, beachBiomes, singleBiome, roleOverrides,
            layoutOriginBlockX, layoutOriginBlockZ, algorithmRevision, List.of()
        );
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
            LayoutMode.LEGACY, 0L, DEFAULT_REGION_SCALE_BLOCKS,
            List.of(), List.of(), List.of(), Optional.empty(), Map.of(), 0, 0, LEGACY_MODE_REVISION
        );
    }

    /**
     * Returns a copy of this plan sampling with a different seed, otherwise identical.
     * Used to re-seed a decoded plan with the real Minecraft world seed once it becomes
     * available at generation time (see DESIGN §20.4) -- codecs decode from
     * {@code RegistryOps}, which has no seed-aware hook, so the seed baked in at decode
     * time is necessarily a placeholder.
     *
     * @param newSeed replacement sampling seed
     * @return this plan unchanged if the seed already matches, otherwise a re-seeded copy
     */
    public WorldLayoutPlan withSeed(long newSeed) {
        // bandBiomes is already-resolved data (any shuffle happened once, at world creation,
        // via resolveBands), not something to re-derive here -- only future per-cell sampling
        // (CHAOS/OCEAN's weightedPick) actually reads the seed live.
        return newSeed == this.seed ? this : new WorldLayoutPlan(
            mode, newSeed, regionScaleBlocks, landBiomes, oceanBiomes, beachBiomes,
            singleBiome, roleOverrides, layoutOriginBlockX, layoutOriginBlockZ, algorithmRevision, bandBiomes
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration and a caller-supplied seed.
     *
     * @param config sanitized startup configuration
     * @param seed sampling seed (see {@link #seed()})
     * @return immutable resolved plan for a newly created world
     */
    public static WorldLayoutPlan fromConfig(WorldzConfig config, long seed) {
        var layout = config.layout;
        return resolve(
            layout.mode,
            layout.biomes,
            layout.roleOverrides,
            layout.regionScaleBlocks,
            layout.singleBiome,
            seed
        );
    }

    /**
     * Resolves a plan from raw weighted-biome text entries and role overrides, shared by
     * YAML config loading and the world-creation Customize screen. Each weighted biome id
     * is classified into a role via {@link BiomeRoles#resolve(String, Map)} using the
     * given overrides.
     *
     * @param mode layout mode
     * @param weightedBiomeEntries raw {@code id}/{@code id@weight} entries, syntax-validated here
     * @param roleOverrideEntries raw biome id to role-name overrides, syntax-validated here
     * @param regionScaleBlocks grid-cell edge length in blocks
     * @param singleBiome {@code SINGLE_BIOME} biome id, or blank/empty when unused
     * @param seed sampling seed (see {@link #seed()})
     * @return immutable resolved plan
     */
    public static WorldLayoutPlan resolve(
        LayoutMode mode,
        List<String> weightedBiomeEntries,
        Map<String, String> roleOverrideEntries,
        int regionScaleBlocks,
        String singleBiome,
        long seed
    ) {
        Map<String, BiomeRole> overrides = new LinkedHashMap<>();
        roleOverrideEntries.forEach((id, role) -> overrides.put(id, BiomeRole.parse(role)));

        List<BiomeWeight> land = new ArrayList<>();
        List<BiomeWeight> ocean = new ArrayList<>();
        List<BiomeWeight> beach = new ArrayList<>();
        for (WeightedBiomeListSpec.Entry entry : WeightedBiomeListSpec.parse(weightedBiomeEntries).entries()) {
            BiomeWeight weight = new BiomeWeight(entry.id(), entry.weight());
            switch (BiomeRoles.resolve(entry.id(), overrides)) {
                case LAND -> land.add(weight);
                case OCEAN -> ocean.add(weight);
                case BEACH -> beach.add(weight);
            }
        }

        return new WorldLayoutPlan(
            mode,
            seed,
            regionScaleBlocks,
            land,
            ocean,
            beach,
            singleBiome == null || singleBiome.isBlank() ? Optional.empty() : Optional.of(singleBiome),
            overrides,
            0,
            0,
            CURRENT_REVISION
        );
    }

    /**
     * Resolves a {@code STRIP_BANDS} plan (GOALS 36) from an ordered, unweighted biome
     * sequence. When {@code seedRandomOrder} is set, the sequence is shuffled exactly once
     * here (a single seed-derived permutation, not per-band randomness) and that final order
     * is what gets persisted -- reloading a saved world always repeats the same shuffled
     * order rather than reshuffling on every load.
     *
     * @param bandBiomes ordered, unweighted band biome ids (at least one required)
     * @param bandWidthBlocks band width in blocks, reusing the generic grid-cell concept
     * @param seedRandomOrder whether to shuffle the sequence once instead of using it as given
     * @param roleOverrideEntries raw biome id to role-name overrides, syntax-validated here
     * @param seed sampling seed, used only for the one-time shuffle when requested
     * @return immutable resolved plan
     */
    public static WorldLayoutPlan resolveBands(
        List<String> bandBiomes,
        int bandWidthBlocks,
        boolean seedRandomOrder,
        Map<String, String> roleOverrideEntries,
        long seed
    ) {
        Map<String, BiomeRole> overrides = new LinkedHashMap<>();
        roleOverrideEntries.forEach((id, role) -> overrides.put(id, BiomeRole.parse(role)));
        List<String> order = seedRandomOrder ? shuffleOnce(bandBiomes, seed) : List.copyOf(bandBiomes);
        return new WorldLayoutPlan(
            LayoutMode.STRIP_BANDS, seed, bandWidthBlocks, List.of(), List.of(), List.of(),
            Optional.empty(), overrides, 0, 0, CURRENT_REVISION, order
        );
    }

    private static List<String> shuffleOnce(List<String> values, long seed) {
        List<String> shuffled = new ArrayList<>(values);
        for (int index = shuffled.size() - 1; index > 0; index--) {
            int swapWith = (int)Math.floorMod(
                (long)(hash01(seed, "band_order", index, 0, 0) * (index + 1)), (long)(index + 1)
            );
            String moved = shuffled.set(index, shuffled.get(swapWith));
            shuffled.set(swapWith, moved);
        }
        return List.copyOf(shuffled);
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
            case OCEAN -> sampleUniform(BiomeRole.OCEAN, oceanBiomes, blockX, blockZ, "biome_ocean");
            case SINGLE_BIOME -> {
                BiomeRole role = BiomeRoles.resolve(singleBiome.orElseThrow(), roleOverrides);
                yield new LayoutSample(role, singleBiome, roleFactor(role));
            }
            case VOID -> new LayoutSample(BiomeRole.LAND, Optional.empty(), 1.0);
            case CHAOS -> sampleUniform(BiomeRole.LAND, landBiomes, blockX, blockZ, "biome_land");
            case STRIP_BANDS -> sampleBand(blockX);
        };
    }

    /**
     * Samples the ordered band sequence by X position alone -- the strip's length axis;
     * bands repeat (wrap) once the sequence is exhausted, so the pattern is well-defined
     * regardless of how long the corridor's border eventually makes it.
     */
    private LayoutSample sampleBand(int blockX) {
        long bandIndex = Math.floorDiv((long)blockX - layoutOriginBlockX, regionScaleBlocks);
        int position = (int)Math.floorMod(bandIndex, (long)bandBiomes.size());
        String biomeId = bandBiomes.get(position);
        BiomeRole role = BiomeRoles.resolve(biomeId, roleOverrides);
        return new LayoutSample(role, Optional.of(biomeId), roleFactor(role));
    }

    /**
     * Samples one specific role's weighted biome list directly, bypassing the
     * mode's own role classification at this column. Used for starter-zone
     * overlays (e.g. a beach ring) that need a particular role's biome
     * regardless of what the base layout would otherwise place there.
     *
     * @param role role to sample
     * @param blockX block X
     * @param blockZ block Z
     * @return the sampled biome id, empty if that role has no candidates
     */
    public Optional<String> sampleRole(BiomeRole role, int blockX, int blockZ) {
        long cellX = Math.floorDiv((long) blockX - layoutOriginBlockX, regionScaleBlocks);
        long cellZ = Math.floorDiv((long) blockZ - layoutOriginBlockZ, regionScaleBlocks);
        return weightedPick(rolePool(role), cellX, cellZ, biomeSalt(role));
    }

    private LayoutSample sampleUniform(BiomeRole role, List<BiomeWeight> candidates, int blockX, int blockZ, String salt) {
        long cellX = Math.floorDiv((long) blockX - layoutOriginBlockX, regionScaleBlocks);
        long cellZ = Math.floorDiv((long) blockZ - layoutOriginBlockZ, regionScaleBlocks);
        return new LayoutSample(role, weightedPick(candidates, cellX, cellZ, salt), roleFactor(role));
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

    private static double roleFactor(BiomeRole role) {
        return role == BiomeRole.OCEAN ? 0.0 : 1.0;
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
}
