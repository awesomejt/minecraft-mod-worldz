package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.ForeverNightConfig;

/**
 * The world-hazard "forever night" module's resolved settings (GOAL 30, DESIGN §35.1): a shared
 * runtime rule, not a typed preset, composable with any world type -- the Overworld's day/night
 * clock locks at night, immediately or after a configured delay, and vanilla's own sleep-skip
 * logic already refuses to advance time once the underlying gamerule is off (DESIGN §35.0), so no
 * separate "sleeping cannot skip it" mechanism is needed here.
 *
 * @param enabled whether night eventually locks permanently
 * @param lockAfterDays in-game days before night locks; zero locks immediately at world creation
 * @param relaxInsomnia whether phantom/insomnia pressure is suppressed once night is locked
 */
public record ForeverNightPlan(boolean enabled, int lockAfterDays, boolean relaxInsomnia) {
    /** Validates persisted values even while the module is disabled. */
    public ForeverNightPlan {
        if (lockAfterDays < 0) {
            throw new IllegalArgumentException("Forever-night lock delay must not be negative.");
        }
    }

    /**
     * Returns a plan with the module switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static ForeverNightPlan disabled() {
        return new ForeverNightPlan(false, 0, false);
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized forever-night configuration
     * @return resolved plan, disabled unless {@link ForeverNightConfig#enabled} is set
     */
    public static ForeverNightPlan fromConfig(ForeverNightConfig config) {
        if (!config.enabled) {
            return disabled();
        }
        return new ForeverNightPlan(true, config.lockAfterDays, config.relaxInsomnia);
    }

    /**
     * Returns how long night stays unlocked after world creation, in game ticks -- mirrors
     * {@link BorderSchedule#delayTicks()}'s exact same "days times {@link
     * BorderSchedule#TICKS_PER_DAY}" idiom.
     *
     * @return delay in game ticks, zero for an immediate lock
     */
    public long lockDelayTicks() {
        return Math.multiplyExact((long)lockAfterDays, BorderSchedule.TICKS_PER_DAY);
    }
}
