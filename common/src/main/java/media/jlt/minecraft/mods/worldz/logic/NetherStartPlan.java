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
 */
public record NetherStartPlan(
    boolean enabled,
    int spawnY,
    StarterKitTier chestTier
) {
    /** Fixture-verified default: comfortably between the Y-0 floor and the Y-128 bedrock ceiling. */
    public static final int DEFAULT_SPAWN_Y = 32;
    /** Smallest supported search-target Y -- keeps well clear of the Nether's own bedrock floor. */
    public static final int MIN_SPAWN_Y = 1;
    /** Largest supported search-target Y -- keeps well clear of the Nether's own bedrock ceiling. */
    public static final int MAX_SPAWN_Y = 120;

    /** Validates persisted values even while the shape is disabled. */
    public NetherStartPlan {
        if (spawnY < MIN_SPAWN_Y || spawnY > MAX_SPAWN_Y) {
            throw new IllegalArgumentException("Nether-start spawn Y must be between " + MIN_SPAWN_Y + " and " + MAX_SPAWN_Y + ".");
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
    }

    /**
     * Returns a plan with the Nether-start shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static NetherStartPlan disabled() {
        return new NetherStartPlan(false, DEFAULT_SPAWN_Y, StarterKitTier.MEDIUM);
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized Nether-start configuration
     * @return resolved, enabled plan
     */
    public static NetherStartPlan fromConfig(NetherStartConfig config) {
        return new NetherStartPlan(true, config.spawnY, config.chestTier);
    }
}
