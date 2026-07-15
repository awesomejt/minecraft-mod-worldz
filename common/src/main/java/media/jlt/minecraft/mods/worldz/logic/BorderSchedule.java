package media.jlt.minecraft.mods.worldz.logic;

/**
 * Pure world-border sizing calculations expressed in Minecraft game ticks.
 *
 * @param initialRadiusBlocks center-to-side distance at creation
 * @param finalRadiusBlocks center-to-side distance at the end of the schedule
 * @param resizeDays transition duration in normal Minecraft days
 */
public record BorderSchedule(int initialRadiusBlocks, int finalRadiusBlocks, int resizeDays) {
    /** Minecraft ticks in one normal in-game day. */
    public static final long TICKS_PER_DAY = 24_000L;

    /** Rejects nonsensical schedule values. */
    public BorderSchedule {
        if (initialRadiusBlocks <= 0 || finalRadiusBlocks <= 0) {
            throw new IllegalArgumentException("border radii must be positive");
        }
        if (resizeDays < 0) {
            throw new IllegalArgumentException("resizeDays must not be negative");
        }
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
        return Math.multiplyExact((long)resizeDays, TICKS_PER_DAY);
    }

    /**
     * Calculates the radius at an elapsed game tick, clamped to both schedule ends.
     * A zero-day schedule immediately uses the final radius.
     *
     * @param elapsedTicks ticks elapsed since the schedule began
     * @return interpolated radius in blocks
     */
    public double radiusAtTick(long elapsedTicks) {
        long duration = durationTicks();
        if (duration == 0L || elapsedTicks >= duration) {
            return finalRadiusBlocks;
        }
        if (elapsedTicks <= 0L) {
            return initialRadiusBlocks;
        }
        double progress = (double)elapsedTicks / duration;
        return initialRadiusBlocks + (finalRadiusBlocks - initialRadiusBlocks) * progress;
    }
}
