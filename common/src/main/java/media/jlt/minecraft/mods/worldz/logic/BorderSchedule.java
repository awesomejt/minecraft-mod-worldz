package media.jlt.minecraft.mods.worldz.logic;

/**
 * Pure world-border sizing calculations expressed in Minecraft game ticks.
 *
 * @param initialRadiusBlocks center-to-side distance at creation
 * @param finalRadiusBlocks center-to-side distance at the end of the schedule
 * @param resizeDays transition duration in normal Minecraft days
 * @param resizeDelayDays wait at the initial radius before resizing
 * @param resizeRateBlocks radius distance traversed per rate interval
 * @param resizeRateDays normal Minecraft days per rate interval
 */
public record BorderSchedule(
    int initialRadiusBlocks,
    int finalRadiusBlocks,
    int resizeDays,
    int resizeDelayDays,
    int resizeRateBlocks,
    int resizeRateDays
) {
    /** Minecraft ticks in one normal in-game day. */
    public static final long TICKS_PER_DAY = 24_000L;

    /** Rejects nonsensical schedule values. */
    public BorderSchedule {
        if (initialRadiusBlocks <= 0 || finalRadiusBlocks <= 0) {
            throw new IllegalArgumentException("border radii must be positive");
        }
        if (resizeDays < 0 || resizeDelayDays < 0 || resizeRateBlocks < 0 || resizeRateDays < 0) {
            throw new IllegalArgumentException("border timing values must not be negative");
        }
        if ((resizeRateBlocks == 0) != (resizeRateDays == 0)) {
            throw new IllegalArgumentException("both rate values must be zero or positive");
        }
    }

    /**
     * Creates a legacy total-duration schedule without rate fields.
     *
     * @param initialRadiusBlocks center-to-side distance at creation
     * @param finalRadiusBlocks center-to-side distance at completion
     * @param resizeDays total transition duration in Minecraft days
     */
    public BorderSchedule(int initialRadiusBlocks, int finalRadiusBlocks, int resizeDays) {
        this(initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, 0, 0);
    }

    /**
     * Creates a rate schedule without an initial delay.
     *
     * @param initialRadiusBlocks center-to-side distance at creation
     * @param finalRadiusBlocks center-to-side distance at completion
     * @param resizeDays legacy total transition duration
     * @param resizeRateBlocks radius blocks traversed per interval
     * @param resizeRateDays Minecraft days per interval
     */
    public BorderSchedule(
        int initialRadiusBlocks,
        int finalRadiusBlocks,
        int resizeDays,
        int resizeRateBlocks,
        int resizeRateDays
    ) {
        this(initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, resizeRateBlocks, resizeRateDays);
    }

    /**
     * Returns the vanilla border diameter for the initial radius.
     *
     * @return initial diameter in blocks
     */
    public double initialDiameterBlocks() {
        return initialRadiusBlocks * 2.0;
    }

    /**
     * Returns the vanilla border diameter for the final radius.
     *
     * @return final diameter in blocks
     */
    public double finalDiameterBlocks() {
        return finalRadiusBlocks * 2.0;
    }

    /**
     * Returns the transition duration without overflowing 32-bit arithmetic.
     *
     * @return duration in game ticks
     */
    public long durationTicks() {
        long distance = Math.abs((long)finalRadiusBlocks - initialRadiusBlocks);
        if (distance == 0L) {
            return 0L;
        }
        if (usesRate()) {
            long ticksPerInterval = Math.multiplyExact((long)resizeRateDays, TICKS_PER_DAY);
            long wholeIntervals = distance / resizeRateBlocks;
            long remainder = distance % resizeRateBlocks;
            long wholeTicks = Math.multiplyExact(wholeIntervals, ticksPerInterval);
            long partialTicks = remainder == 0L ? 0L : divideCeiling(Math.multiplyExact(remainder, ticksPerInterval), resizeRateBlocks);
            return Math.addExact(wholeTicks, partialTicks);
        }
        return Math.multiplyExact((long)resizeDays, TICKS_PER_DAY);
    }

    /**
     * Returns how long the initial radius remains static.
     *
     * @return delay in game ticks
     */
    public long delayTicks() {
        return Math.multiplyExact((long)resizeDelayDays, TICKS_PER_DAY);
    }

    /**
     * Returns delay plus transition duration.
     *
     * @return complete schedule duration in game ticks
     */
    public long totalDurationTicks() {
        return Math.addExact(delayTicks(), durationTicks());
    }

    /**
     * Returns whether the schedule uses X-blocks-per-Y-days timing.
     *
     * @return whether rate timing overrides the total-duration field
     */
    public boolean usesRate() {
        return resizeRateBlocks > 0;
    }

    /**
     * Calculates the radius at an elapsed game tick, clamped to both schedule ends.
     * A zero-day schedule immediately uses the final radius.
     *
     * @param elapsedTicks ticks elapsed since the schedule began
     * @return interpolated radius in blocks
     */
    public double radiusAtTick(long elapsedTicks) {
        long delay = delayTicks();
        if (elapsedTicks < delay) {
            return initialRadiusBlocks;
        }
        long duration = durationTicks();
        long transitionTicks = elapsedTicks - delay;
        if (duration == 0L) {
            return finalRadiusBlocks;
        }
        if (transitionTicks >= duration) {
            return finalRadiusBlocks;
        }
        double progress = (double)transitionTicks / duration;
        return initialRadiusBlocks + (finalRadiusBlocks - initialRadiusBlocks) * progress;
    }

    private static long divideCeiling(long value, long divisor) {
        return value / divisor + (value % divisor == 0L ? 0L : 1L);
    }
}
