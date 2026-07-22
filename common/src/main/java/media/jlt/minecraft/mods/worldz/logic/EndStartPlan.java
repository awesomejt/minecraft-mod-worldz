package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;

/**
 * The {@code end_start} typed preset's resolved settings (GOALS 34, DESIGN §32): a guaranteed
 * safe End spawn platform plus a difficulty-tiered starter chest there, Overworld and Nether both
 * otherwise ordinary vanilla terrain. Mirrors {@code NetherStartPlan} (DESIGN §31.5) except for
 * {@code spawnY} -- there is no search, so no search target -- since Jason's decision (DESIGN
 * §32.2) always places a guaranteed platform rather than searching for a natural site first.
 *
 * @param enabled whether the End-start shape applies
 * @param chestTier the starter chest's difficulty tier (GOALS 34's own worked example: enough to
 *     make reaching and defeating the Ender Dragon genuinely achievable)
 */
public record EndStartPlan(
    boolean enabled,
    StarterKitTier chestTier
) {
    /** Validates persisted values even while the shape is disabled. */
    public EndStartPlan {
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
    }

    /**
     * Returns a plan with the End-start shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static EndStartPlan disabled() {
        return new EndStartPlan(false, StarterKitTier.MEDIUM);
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized End-start configuration
     * @return resolved, enabled plan
     */
    public static EndStartPlan fromConfig(EndStartConfig config) {
        return new EndStartPlan(true, config.chestTier);
    }
}
