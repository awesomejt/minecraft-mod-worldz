package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.RisingLavaConfig;

/**
 * Pure world-hazard rising-lava-floor sizing calculations expressed in Minecraft game ticks
 * (GOAL 29, DESIGN §35.2) -- mirrors {@link BorderSchedule}'s exact shape, continuous only (GOAL
 * 29 never asks for a stepped variant).
 *
 * @param startY Y the lava level starts at, before any rise
 * @param maxY Y the lava level stops rising at
 * @param delayDays in-game days before the level starts rising
 * @param rateBlocks blocks the level rises per {@code rateDays}
 * @param rateDays in-game days per {@code rateBlocks} of rise
 */
public record RisingLavaSchedule(int startY, int maxY, int delayDays, int rateBlocks, int rateDays) {
    /** Rejects nonsensical schedule values. */
    public RisingLavaSchedule {
        if (maxY < startY) {
            throw new IllegalArgumentException("rising lava maxY must not be below startY");
        }
        if (delayDays < 0) {
            throw new IllegalArgumentException("rising lava delay must not be negative");
        }
        if (rateBlocks <= 0 || rateDays <= 0) {
            throw new IllegalArgumentException("rising lava rate values must be positive");
        }
    }

    /**
     * Resolves a schedule from sanitized YAML configuration.
     *
     * @param config sanitized rising-lava configuration
     * @return resolved schedule
     */
    public static RisingLavaSchedule fromConfig(RisingLavaConfig config) {
        return new RisingLavaSchedule(config.startY, config.maxY, config.delayDays, config.rateBlocks, config.rateDays);
    }

    /**
     * Returns how long the level stays at {@link #startY}, in game ticks.
     *
     * @return delay in game ticks
     */
    public long delayTicks() {
        return Math.multiplyExact((long)delayDays, BorderSchedule.TICKS_PER_DAY);
    }

    /**
     * Calculates the lava level at an elapsed game tick, clamped to {@code [startY, maxY]}.
     *
     * @param elapsedTicks ticks elapsed since the schedule began (world creation)
     * @return current lava level in blocks
     */
    public int levelAtTick(long elapsedTicks) {
        long delay = delayTicks();
        if (elapsedTicks < delay) {
            return startY;
        }
        long distance = maxY - startY;
        if (distance == 0L) {
            return maxY;
        }
        long risingTicks = elapsedTicks - delay;
        long ticksPerInterval = Math.multiplyExact((long)rateDays, BorderSchedule.TICKS_PER_DAY);
        long blocksRisen = risingTicks / ticksPerInterval * rateBlocks;
        return (int)Math.min(maxY, startY + blocksRisen);
    }
}
