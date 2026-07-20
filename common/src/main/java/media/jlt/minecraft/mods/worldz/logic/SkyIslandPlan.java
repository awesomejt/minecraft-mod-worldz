package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

/**
 * A true floating island (GOALS 05, DESIGN §27): a thin, fixed-thickness slab bounded below,
 * void everywhere else within its own footprint and beyond it. New, additive, sky-island-only
 * mechanism mirroring {@link IslandPlan}/{@link StripPlan}'s precedent (DESIGN §27.1) -- not a
 * retrofit of the shared {@code StarterLandPlan}/exterior-envelope machinery every other typed
 * preset composes with. Unlike {@link IslandPlan} there is no fluid/shore/ocean-gradient axis and
 * no exclusion zone: GOALS 05 surrounds the island with void, not a gradient, and GOALS 07's
 * exclusion-zone-driven villages are deferred to Phase 11 entirely (see TODO Phase 10 header).
 *
 * @param enabled whether the sky island shape applies
 * @param radiusBlocks configured (unperturbed) island radius
 * @param shapeAmplitude coastline perturbation strength, reusing {@link IslandShapeProfile}
 * @param islandBiome the one biome that fills the island's interior
 * @param surfaceY the island's walkable surface Y (GOALS 05 default 64, slime rule)
 * @param thicknessBlocks how many blocks of solid ground extend below {@link #surfaceY}
 * @param chestTier the necessities-chest difficulty tier (GOALS 05, DESIGN §27.8)
 */
public record SkyIslandPlan(
    boolean enabled,
    int radiusBlocks,
    double shapeAmplitude,
    String islandBiome,
    int surfaceY,
    int thicknessBlocks,
    StarterKitTier chestTier
) {
    /** GOALS 05's own default: spawn at Y 64 to avoid slimes. */
    public static final int DEFAULT_SURFACE_Y = 64;
    /** Fixture-verified default slab thickness -- enough for a believable island, still clearly bounded. */
    public static final int DEFAULT_THICKNESS_BLOCKS = 6;
    /** Smallest supported slab thickness. */
    public static final int MIN_THICKNESS_BLOCKS = 1;
    /** Largest supported slab thickness -- a generous ceiling, not a tuned limit. */
    public static final int MAX_THICKNESS_BLOCKS = 64;

    /** Validates persisted values even while the island is disabled. */
    public SkyIslandPlan {
        if (radiusBlocks < WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS || radiusBlocks > WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Sky island radius must be between " + WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS
                    + " and " + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS + "."
            );
        }
        if (shapeAmplitude < 0.0 || shapeAmplitude > IslandShapeProfile.MAX_AMPLITUDE) {
            throw new IllegalArgumentException(
                "Sky island shape amplitude must be between 0 and " + IslandShapeProfile.MAX_AMPLITUDE + "."
            );
        }
        if (islandBiome == null || islandBiome.isBlank()) {
            throw new IllegalArgumentException("Sky island biome is required.");
        }
        if (thicknessBlocks < MIN_THICKNESS_BLOCKS || thicknessBlocks > MAX_THICKNESS_BLOCKS) {
            throw new IllegalArgumentException(
                "Sky island thickness must be between " + MIN_THICKNESS_BLOCKS + " and " + MAX_THICKNESS_BLOCKS + "."
            );
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
    }

    /**
     * Returns a plan with the sky island shape switched off.
     *
     * @return disabled sky island plan with safe placeholder values
     */
    public static SkyIslandPlan disabled() {
        return new SkyIslandPlan(
            false, WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, 0.0, "minecraft:plains",
            DEFAULT_SURFACE_Y, DEFAULT_THICKNESS_BLOCKS, StarterKitTier.MEDIUM
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized sky-island configuration
     * @return resolved, enabled plan
     */
    public static SkyIslandPlan fromConfig(SkyIslandConfig config) {
        return new SkyIslandPlan(
            true, config.radiusBlocks, config.shapeAmplitude, config.islandBiome, config.surfaceY, config.thicknessBlocks,
            config.chestTier
        );
    }

    /**
     * Computes the signed distance from the perturbed coastline at one column, reusing {@link
     * IslandShapeProfile} directly (DESIGN §27.4) so a sky island's footprint edge reads exactly
     * like ocean island's coastline.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param seed sampling seed (the world's real seed, resolved live)
     * @return negative inside the island, positive outside, zero at the edge
     */
    public double distanceFromShore(int x, int z, long seed) {
        return IslandShapeProfile.distanceFromShore(x, z, radiusBlocks, shapeAmplitude, seed);
    }

    /**
     * Returns the lowest solid Y of the slab (inclusive).
     *
     * @return bottom of the slab
     */
    public int bottomY() {
        return surfaceY - thicknessBlocks;
    }
}
