package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.EndBorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;

import java.util.OptionalInt;

/**
 * Border and progression settings serialized into each new Worldz world.
 *
 * @param overworld overworld limit plan
 * @param nether Nether limit plan
 * @param end End limit plan (GOALS 17's Overworld-to-End carry-over)
 */
public record WorldLimitPlan(DimensionLimit overworld, DimensionLimit nether, EndLimit end) {
    /**
     * Returns a plan that leaves every dimension unlimited.
     *
     * @return disabled plan
     */
    public static WorldLimitPlan disabled() {
        return new WorldLimitPlan(DimensionLimit.disabled(), DimensionLimit.disabled(), EndLimit.disabled());
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
            DimensionLimit.fromConfig(config.netherBorder),
            EndLimit.fromConfig(config.endBorder)
        );
    }

    /**
     * Returns whether any dimension has a managed border.
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
     * @param resizeStyle whether the rate fields drive one smooth lerp or abrupt jumps
     */
    public record DimensionLimit(
        boolean enabled,
        int initialRadiusBlocks,
        int finalRadiusBlocks,
        int resizeDays,
        int resizeDelayDays,
        int resizeRateBlocks,
        int resizeRateDays,
        boolean ensureObjective,
        ResizeStyle resizeStyle
    ) {
        /** Validates persisted values before they reach vanilla's border API. */
        public DimensionLimit {
            if (initialRadiusBlocks <= 0 || finalRadiusBlocks <= 0 || resizeDays < 0 || resizeDelayDays < 0
                || resizeRateBlocks < 0 || resizeRateDays < 0
                || ((resizeRateBlocks == 0) != (resizeRateDays == 0))) {
                throw new IllegalArgumentException("invalid persisted world-limit values");
            }
            if (resizeStyle == ResizeStyle.STEPPED && resizeRateBlocks == 0) {
                throw new IllegalArgumentException("stepped resize requires resizeRateBlocks and resizeRateDays");
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
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, 0, 0, ensureObjective, ResizeStyle.CONTINUOUS);
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
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, 0, resizeRateBlocks, resizeRateDays, ensureObjective, ResizeStyle.CONTINUOUS);
        }

        /**
         * Creates continuous-style values with an initial delay.
         *
         * @param enabled whether the border is managed
         * @param initialRadiusBlocks initial center-to-side distance
         * @param finalRadiusBlocks final center-to-side distance
         * @param resizeDays legacy total transition duration
         * @param resizeDelayDays wait at the initial radius before resizing
         * @param resizeRateBlocks radius blocks per rate interval
         * @param resizeRateDays Minecraft days per rate interval
         * @param ensureObjective whether the progression objective is guaranteed
         */
        public DimensionLimit(
            boolean enabled,
            int initialRadiusBlocks,
            int finalRadiusBlocks,
            int resizeDays,
            int resizeDelayDays,
            int resizeRateBlocks,
            int resizeRateDays,
            boolean ensureObjective
        ) {
            this(enabled, initialRadiusBlocks, finalRadiusBlocks, resizeDays, resizeDelayDays, resizeRateBlocks, resizeRateDays, ensureObjective, ResizeStyle.CONTINUOUS);
        }

        /**
         * Returns a disabled static plan with safe placeholder radii.
         *
         * @return disabled dimension plan
         */
        public static DimensionLimit disabled() {
            return new DimensionLimit(false, 512, 512, 0, 0, 0, 0, false, ResizeStyle.CONTINUOUS);
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
                config.ensureObjective,
                config.resizeStyle
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
                resizeRateDays,
                resizeStyle
            );
        }

        /** Preferred spawn offset from the origin: the center of the origin chunk, not its corner. */
        private static final int PREFERRED_SPAWN_OFFSET_BLOCKS = 8;
        /** Kept clear of the border so vanilla's push-back/damage never applies right at spawn. */
        private static final int SPAWN_BORDER_SAFETY_MARGIN_BLOCKS = 1;

        /**
         * Returns how far from the origin a fresh spawn may offset (into the center of the
         * origin chunk) without landing beyond this border's initial radius. A disabled border
         * imposes no constraint. Needed since 0.2.13 lowered the border radius floor to 1 --
         * without this clamp, a tiny border would place spawn beyond it, triggering vanilla's
         * own push-back/damage the instant the world loads.
         *
         * @return safe spawn offset in blocks, never more than {@value #PREFERRED_SPAWN_OFFSET_BLOCKS}
         */
        public int safeSpawnOffsetBlocks() {
            if (!enabled) {
                return PREFERRED_SPAWN_OFFSET_BLOCKS;
            }
            int maxSafeOffset = initialRadiusBlocks - SPAWN_BORDER_SAFETY_MARGIN_BLOCKS;
            return Math.clamp(maxSafeOffset, 0, PREFERRED_SPAWN_OFFSET_BLOCKS);
        }
    }

    /**
     * The End's persisted, static border plan: whether to carry the Overworld's eventual
     * (final) radius over, and the floor that keeps the dragon fight winnable even when that
     * carried radius would otherwise be too small (GOALS 17).
     *
     * @param carryFromOverworld whether the End receives a border at all
     * @param minimumRadiusBlocks smallest End border half-width regardless of the carried radius
     */
    public record EndLimit(boolean carryFromOverworld, int minimumRadiusBlocks) {
        /** Validates persisted values before they reach vanilla's border API. */
        public EndLimit {
            if (minimumRadiusBlocks <= 0) {
                throw new IllegalArgumentException("invalid persisted End world-limit values");
            }
        }

        /**
         * Returns a plan that leaves the End unbordered.
         *
         * @return disabled End plan
         */
        public static EndLimit disabled() {
            return new EndLimit(false, 256);
        }

        private static EndLimit fromConfig(EndBorderConfig config) {
            return new EndLimit(config.carryFromOverworld, config.minimumRadiusBlocks);
        }

        /**
         * Resolves the actual End border half-width to apply, carrying the Overworld's eventual
         * radius and clamping it up to {@link #minimumRadiusBlocks}. Empty when disabled or when
         * the Overworld itself has no border to carry.
         *
         * @param overworld the Overworld's persisted limit plan
         * @return the End border half-width to apply, or empty to leave the End unbordered
         */
        public OptionalInt resolveRadiusBlocks(DimensionLimit overworld) {
            if (!carryFromOverworld || !overworld.enabled()) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Math.max(overworld.finalRadiusBlocks(), minimumRadiusBlocks));
        }
    }
}
