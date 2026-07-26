package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;

/**
 * The {@code nether_start} typed preset's resolved settings (GOALS 27, DESIGN §31): a guaranteed
 * safe Nether spawn site plus a difficulty-tiered starter chest there, Overworld otherwise
 * ordinary vanilla terrain. Unlike every other typed preset except {@code cave} this plan is never
 * read from {@code LimitedBiomeSource} -- persisted entirely on {@code EnvelopedChunkGenerator}'s
 * own codec instead (DESIGN §31.5), since none of its pieces need per-column biome-source
 * consultation and {@code LimitedBiomeSource}'s own codec is already full.
 *
 * @param enabled whether the Nether-start shape applies
 * @param spawnY target Y for the safe-site search (GOALS 27's "spawn-point safety")
 * @param chestTier the starter chest's difficulty tier (GOALS 27's own worked example: easy
 *     includes obsidian and flint and steel)
 * @param forceCapsule whether to always build the guaranteed capsule instead of only falling back
 *     to it when the natural search comes up empty (GOALS 41.1, DESIGN §31.9: "explicit,
 *     requestable option", not only a fallback)
 * @param capsuleSizeBlocks the capsule's total exterior footprint width/depth, walls included
 *     (GOALS 41, "decent sized enclosure" -- must be odd; interior footprint is this minus 2)
 * @param capsuleHeightBlocks the capsule's interior height
 * @param capsuleLightSource which block lights the capsule (GOALS 41.2)
 * @param capsuleLightSpacingBlocks spacing, in blocks, between embedded/hung light sources
 */
public record NetherStartPlan(
    boolean enabled,
    int spawnY,
    StarterKitTier chestTier,
    boolean forceCapsule,
    int capsuleSizeBlocks,
    int capsuleHeightBlocks,
    LightSource capsuleLightSource,
    int capsuleLightSpacingBlocks
) {
    /** Fixture-verified default: comfortably between the Y-0 floor and the Y-128 bedrock ceiling. */
    public static final int DEFAULT_SPAWN_Y = 32;
    /** Smallest supported search-target Y -- keeps well clear of the Nether's own bedrock floor. */
    public static final int MIN_SPAWN_Y = 1;
    /** Largest supported search-target Y -- keeps well clear of the Nether's own bedrock ceiling. */
    public static final int MAX_SPAWN_Y = 120;
    /** Default capsule interior footprint (DESIGN §31.9's "decent sized enclosure"). */
    public static final int DEFAULT_CAPSULE_SIZE_BLOCKS = 5;
    /** Smallest supported capsule footprint -- the original 1x1-interior shape (a 1-thick shell around a single column). */
    public static final int MIN_CAPSULE_SIZE_BLOCKS = 3;
    /** Largest supported capsule footprint -- generous but still "a small base", not a hall. */
    public static final int MAX_CAPSULE_SIZE_BLOCKS = 15;
    /** Default capsule interior height. */
    public static final int DEFAULT_CAPSULE_HEIGHT_BLOCKS = 3;
    /** Smallest supported capsule height -- the original 2-tall shape. */
    public static final int MIN_CAPSULE_HEIGHT_BLOCKS = 2;
    /** Largest supported capsule height. */
    public static final int MAX_CAPSULE_HEIGHT_BLOCKS = 8;
    /** Default light spacing (DESIGN §31.9: "every 5 blocks"). */
    public static final int DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS = 5;
    /** Smallest supported light spacing. */
    public static final int MIN_CAPSULE_LIGHT_SPACING_BLOCKS = 1;
    /** Largest supported light spacing. */
    public static final int MAX_CAPSULE_LIGHT_SPACING_BLOCKS = 15;

    /** Validates persisted values even while the shape is disabled. */
    public NetherStartPlan {
        if (spawnY < MIN_SPAWN_Y || spawnY > MAX_SPAWN_Y) {
            throw new IllegalArgumentException("Nether-start spawn Y must be between " + MIN_SPAWN_Y + " and " + MAX_SPAWN_Y + ".");
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
        if (capsuleSizeBlocks < MIN_CAPSULE_SIZE_BLOCKS || capsuleSizeBlocks > MAX_CAPSULE_SIZE_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule size must be between " + MIN_CAPSULE_SIZE_BLOCKS + " and " + MAX_CAPSULE_SIZE_BLOCKS + "."
            );
        }
        if (capsuleSizeBlocks % 2 == 0) {
            throw new IllegalArgumentException("Capsule size must be odd (a symmetric room needs a true center column).");
        }
        if (capsuleHeightBlocks < MIN_CAPSULE_HEIGHT_BLOCKS || capsuleHeightBlocks > MAX_CAPSULE_HEIGHT_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule height must be between " + MIN_CAPSULE_HEIGHT_BLOCKS + " and " + MAX_CAPSULE_HEIGHT_BLOCKS + "."
            );
        }
        if (capsuleLightSource == null) {
            throw new IllegalArgumentException("Capsule light source is required.");
        }
        if (capsuleLightSpacingBlocks < MIN_CAPSULE_LIGHT_SPACING_BLOCKS || capsuleLightSpacingBlocks > MAX_CAPSULE_LIGHT_SPACING_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule light spacing must be between " + MIN_CAPSULE_LIGHT_SPACING_BLOCKS + " and "
                    + MAX_CAPSULE_LIGHT_SPACING_BLOCKS + "."
            );
        }
    }

    /**
     * Returns a plan with the Nether-start shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static NetherStartPlan disabled() {
        return new NetherStartPlan(
            false, DEFAULT_SPAWN_Y, StarterKitTier.MEDIUM, false,
            DEFAULT_CAPSULE_SIZE_BLOCKS, DEFAULT_CAPSULE_HEIGHT_BLOCKS, LightSource.GLOWSTONE, DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized Nether-start configuration
     * @return resolved, enabled plan
     */
    public static NetherStartPlan fromConfig(NetherStartConfig config) {
        return new NetherStartPlan(
            true, config.spawnY, config.chestTier, config.forceCapsule,
            config.capsule.sizeBlocks, config.capsule.heightBlocks, config.capsule.lightSource, config.capsule.lightSpacingBlocks
        );
    }
}
