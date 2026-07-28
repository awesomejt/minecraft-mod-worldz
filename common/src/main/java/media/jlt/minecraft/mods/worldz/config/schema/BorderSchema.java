package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;

import java.util.List;

/**
 * Schema for {@link BorderConfig} (GOALS 17/33) -- one POJO field, two YAML key names depending on
 * whether the instance is {@code overworldBorder} ({@code ensureEndPortal}) or {@code
 * netherBorder} ({@code ensureBlazeAccess}), DESIGN §41.5's parameterized-instance case and R1.
 * {@code resize: {days, delayDays, style, rate: {blocks, days}}} is a group inside a group (TODO
 * 25.6c, DESIGN §42.2's "groups nest") over this section's own {@link BorderConfig} type -- every
 * field stays flat on {@link BorderConfig} itself, exactly as before; only the YAML shape and key
 * names change.
 *
 * <p>{@code postValidate} carries the incomplete-rate-pair check and the stepped-without-a-rate
 * fallback, in that exact order, matching {@code sanitizeBorder}'s own tail (after all six int
 * clamps have already run as per-setting rules): the pair check runs first because it can zero
 * {@code resizeRateBlocks}, which the stepped-style check then reads. Reordering these would
 * change which WARN lines fire for an input that trips both.
 */
public final class BorderSchema extends SchemaSection<BorderConfig> {
    private final String objectiveKey;
    private final String summaryObjectiveName;
    private final ResizeSchema resize;

    public BorderSchema(String path, String objectiveKey, String summaryObjectiveName) {
        super(path, BorderConfig::new);
        this.objectiveKey = objectiveKey;
        this.summaryObjectiveName = summaryObjectiveName;
        this.resize = new ResizeSchema(path + ".resize");
    }

    @Override
    protected List<Setting<BorderConfig, ?>> declare() {
        return List.of(
            Setting.<BorderConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether this dimension receives a limited border.")
                .build(),
            Setting.<BorderConfig>integer("initialRadius", c -> c.initialRadiusBlocks, (c, v) -> c.initialRadiusBlocks = v)
                .range(WorldzConfig.MIN_BORDER_RADIUS_BLOCKS, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Border half-width when the world is created.")
                .build(),
            Setting.<BorderConfig>integer("finalRadius", c -> c.finalRadiusBlocks, (c, v) -> c.finalRadiusBlocks = v)
                .range(WorldzConfig.MIN_BORDER_RADIUS_BLOCKS, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Border half-width after the configured resize period.")
                .build(),
            Setting.group("resize", resize)
                .render(resize::summary)
                .doc("How/when the border transitions from initial to final radius.")
                .build(),
            Setting.<BorderConfig>flag(objectiveKey, c -> c.ensureObjective, (c, v) -> c.ensureObjective = v)
                .doc("Whether the dimension's progression objective must be reachable inside the final border.")
                .build()
        );
    }

    /**
     * Cross-field checks (DESIGN R1's sibling of R2): the incomplete-rate-pair reset, then the
     * stepped-without-a-rate fallback -- in that order, matching {@code sanitizeBorder}'s tail.
     * Only the warning path strings changed (TODO 25.6c); the clamps and their order are untouched.
     */
    @Override
    protected void postValidate(BorderConfig value, SanitizeContext ctx) {
        if ((value.resizeRateBlocks == 0) != (value.resizeRateDays == 0)) {
            ctx.logger().warn(
                "Ignoring incomplete {} resize rate; both resize.rate.blocks and resize.rate.days must be positive.", path()
            );
            value.resizeRateBlocks = 0;
            value.resizeRateDays = 0;
        }
        if (value.resizeStyle == ResizeStyle.STEPPED && value.resizeRateBlocks == 0) {
            ctx.logger().warn("{} resize.style 'stepped' needs resize.rate.blocks/resize.rate.days; using 'continuous' instead.", path());
            value.resizeStyle = ResizeStyle.CONTINUOUS;
        }
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments, most labels are relabeled (e.g. {@code "initial="} not {@code "initialRadius="}),
     * the objective flag renders under its own {@code summaryObjectiveName} (not its YAML key), and
     * {@code resize}'s own fields fold into one {@code "resize="} segment via {@link
     * ResizeSchema#summary} -- none of this is mechanically derivable.
     */
    @Override
    public String summary(BorderConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "initial=" + value.initialRadiusBlocks
            + ", final=" + value.finalRadiusBlocks
            + ", resize=" + resize.summary(value)
            + ", " + summaryObjectiveName + "=" + value.ensureObjective;
    }

    /**
     * {@code overworldBorder.resize}/{@code netherBorder.resize} (TODO 25.6c): {@code days}/{@code
     * delayDays}/{@code style} plus a nested {@code rate} group, a group over {@link BorderSchema}'s
     * own {@link BorderConfig} type rather than a shared/parameterized class -- there is only ever
     * one owner shape, unlike {@link StarterSchema}/{@link NaturalBiomesSchema}. Private, exactly
     * like {@code WorldzRootSchema.StarterLandSchema}.
     */
    private static final class ResizeSchema extends SchemaSection<BorderConfig> {
        private final RateSchema rate;

        ResizeSchema(String path) {
            super(path, BorderConfig::new);
            this.rate = new RateSchema(path + ".rate");
        }

        @Override
        protected List<Setting<BorderConfig, ?>> declare() {
            return List.of(
                Setting.<BorderConfig>integer("days", c -> c.resizeDays, (c, v) -> c.resizeDays = v)
                    .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                    .unit(Unit.DAYS)
                    .doc("In-game days used for the linear transition from initial to final radius.")
                    .build(),
                Setting.<BorderConfig>integer("delayDays", c -> c.resizeDelayDays, (c, v) -> c.resizeDelayDays = v)
                    .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                    .unit(Unit.DAYS)
                    .doc("In-game days to hold the initial radius before resizing.")
                    .build(),
                Setting.<BorderConfig, ResizeStyle>enumeration(
                        "style", c -> c.resizeStyle, (c, v) -> c.resizeStyle = v,
                        ResizeStyle::parse, ResizeStyle::serializedName, ResizeStyle.CONTINUOUS
                    )
                    .doc("Whether the rate fields drive one smooth lerp or abrupt jumps.")
                    .build(),
                Setting.group("rate", rate)
                    .render(rate::summary)
                    .doc("Distance-over-time resize rate, or unset (both zero) to use days instead.")
                    .build()
            );
        }

        /**
         * Overridden: folds {@code days}/{@code delayDays}/{@code style} plus {@code rate}'s own
         * composite segment into one comma-joined line, matching pre-restructure {@code
         * BorderSchema.summary()}'s flat rendering of the same fields -- not mechanically derivable
         * (no section-level custom joining in the framework).
         */
        @Override
        public String summary(BorderConfig value) {
            return "days=" + value.resizeDays
                + ", delayDays=" + value.resizeDelayDays
                + ", style=" + value.resizeStyle.serializedName()
                + ", rate=" + rate.summary(value);
        }
    }

    /**
     * {@code overworldBorder.resize.rate}/{@code netherBorder.resize.rate} (TODO 25.6c): {@code
     * blocks}/{@code days}, a group inside {@link ResizeSchema}'s own group (DESIGN §42.2's "groups
     * nest" -- {@code copyInto} composes because {@code resize}'s setting is itself an
     * identity-accessor {@code Setting<BorderConfig, BorderConfig>}).
     */
    private static final class RateSchema extends SchemaSection<BorderConfig> {
        RateSchema(String path) {
            super(path, BorderConfig::new);
        }

        @Override
        protected List<Setting<BorderConfig, ?>> declare() {
            return List.of(
                Setting.<BorderConfig>integer("blocks", c -> c.resizeRateBlocks, (c, v) -> c.resizeRateBlocks = v)
                    .range(0, WorldzConfig.MAX_BORDER_RATE_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Radius blocks traversed per rate interval, or zero to use resize.days.")
                    .build(),
                Setting.<BorderConfig>integer("days", c -> c.resizeRateDays, (c, v) -> c.resizeRateDays = v)
                    .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                    .unit(Unit.DAYS)
                    .doc("In-game days per rate interval, or zero to use resize.days.")
                    .build()
            );
        }

        /**
         * Overridden: collapses both fields into one composite segment (or {@code "<total-days>"}
         * when unset), matching pre-restructure {@code BorderSchema.summary()}'s {@code "rate="}
         * segment exactly -- not mechanically derivable.
         */
        @Override
        public String summary(BorderConfig value) {
            return value.resizeRateBlocks == 0
                ? "<total-days>"
                : value.resizeRateBlocks + " blocks/" + value.resizeRateDays + " days";
        }
    }
}
