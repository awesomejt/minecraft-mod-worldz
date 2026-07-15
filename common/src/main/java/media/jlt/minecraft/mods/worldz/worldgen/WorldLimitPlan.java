package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;

/**
 * Border and progression settings serialized into each new Worldz world.
 *
 * @param overworld overworld limit plan
 * @param nether Nether limit plan
 */
public record WorldLimitPlan(DimensionLimit overworld, DimensionLimit nether) {
    /**
     * Returns a plan that leaves both dimensions unlimited.
     *
     * @return disabled plan
     */
    public static WorldLimitPlan disabled() {
        return new WorldLimitPlan(DimensionLimit.disabled(), DimensionLimit.disabled());
    }

    /**
     * Snapshots the startup config for a newly decoded Worldz preset.
     *
     * @param config sanitized startup config
     * @return immutable plan
     */
    public static WorldLimitPlan fromConfig(WorldzConfig config) {
        return new WorldLimitPlan(
            DimensionLimit.fromConfig(config.overworldBorder),
            DimensionLimit.fromConfig(config.netherBorder)
        );
    }

    /**
     * Returns whether either dimension has a managed border.
     *
     * @return whether any limit is enabled
     */
    public boolean enabled() {
        return overworld.enabled() || nether.enabled();
    }

    /**
     * One dimension's immutable, persisted border plan.
     *
     * @param enabled whether the border is managed
     * @param initialRadiusBlocks initial center-to-side distance
     * @param finalRadiusBlocks final center-to-side distance
     * @param resizeDays linear transition duration
     * @param resizeDelayDays wait at the initial radius before resizing
     * @param resizeRateBlocks radius blocks per rate interval
     * @param resizeRateDays Minecraft days per rate interval
     * @param ensureObjective whether the progression objective is guaranteed
     */
    public record DimensionLimit(
        boolean enabled,
        int initialRadiusBlocks,
        int finalRadiusBlocks,
        int resizeDays,
        int resizeDelayDays,
        int resizeRateBlocks,
        int resizeRateDays,
        boolean ensureObjective
    ) {
        /** Validates persisted values before they reach vanilla's border API. */
        public DimensionLimit {
            if (initialRadiusBlocks <= 0 || finalRadiusBlocks <= 0 || resizeDays < 0 || resizeDelayDays < 0
                || resizeRateBlocks < 0 || resizeRateDays < 0
                || ((resizeRateBlocks == 0) != (resizeRateDays == 0))) {
                throw new IllegalArgumentException("invalid persisted world-limit values");
            }
        }

        /**
         * Creates a legacy total-duration limit without rate fields.
         *
         * @param enabled whether the border is managed
         * @param initialRadiusBlocks initial center-to-side distance
         * @param finalRadiusBlocks final center-to-side distance
         * @param resizeDays total transition duration in Minecraft days
         * @param ensureObjective whether the progression objective is guaranteed
         */
        public DimensionLimit(
            boolean enabled,
            int initialRadiusBlocks,
            int finalRadiusBlocks,
            int resizeDays,
            boolean ensureObjective
        ) {
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, 0, 0, ensureObjective);
        }

        /**
         * Creates rate-based values without an initial delay.
         *
         * @param enabled whether the border is managed
         * @param initialRadiusBlocks initial center-to-side distance
         * @param finalRadiusBlocks final center-to-side distance
         * @param resizeDays legacy total transition duration
         * @param resizeRateBlocks radius blocks per rate interval
         * @param resizeRateDays Minecraft days per rate interval
         * @param ensureObjective whether the progression objective is guaranteed
         */
        public DimensionLimit(
            boolean enabled,
            int initialRadiusBlocks,
            int finalRadiusBlocks,
            int resizeDays,
            int resizeRateBlocks,
            int resizeRateDays,
            boolean ensureObjective
        ) {
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, resizeRateBlocks, resizeRateDays, ensureObjective);
        }

        /**
         * Returns a disabled static plan with safe placeholder radii.
         *
         * @return disabled dimension plan
         */
        public static DimensionLimit disabled() {
            return new DimensionLimit(false, 512, 512, 0, 0, 0, 0, false);
        }

        private static DimensionLimit fromConfig(BorderConfig config) {
            return new DimensionLimit(
                config.enabled,
                config.initialRadiusBlocks,
                config.finalRadiusBlocks,
                config.resizeDays,
                config.resizeDelayDays,
                config.resizeRateBlocks,
                config.resizeRateDays,
                config.ensureObjective
            );
        }

        /**
         * Converts the persisted values to the pure sizing helper.
         *
         * @return border schedule
         */
        public BorderSchedule schedule() {
            return new BorderSchedule(
                initialRadiusBlocks,
                finalRadiusBlocks,
                resizeDays,
                resizeDelayDays,
                resizeRateBlocks,
                resizeRateDays
            );
        }
    }
}
