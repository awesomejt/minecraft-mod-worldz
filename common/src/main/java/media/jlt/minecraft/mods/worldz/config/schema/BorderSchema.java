package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;

import java.util.List;

/**
 * Schema for {@link BorderConfig} (GOALS 17/33) -- one POJO field, two YAML key names depending on
 * whether the instance is {@code overworldBorder} ({@code ensureEndPortal}) or {@code
 * netherBorder} ({@code ensureBlazeAccess}), DESIGN §41.5's parameterized-instance case and R1.
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

    public BorderSchema(String path, String objectiveKey, String summaryObjectiveName) {
        super(path, BorderConfig::new);
        this.objectiveKey = objectiveKey;
        this.summaryObjectiveName = summaryObjectiveName;
    }

    @Override
    protected List<Setting<BorderConfig, ?>> declare() {
        return List.of(
            Setting.<BorderConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether this dimension receives a limited border.")
                .build(),
            Setting.<BorderConfig>integer("initialRadiusBlocks", c -> c.initialRadiusBlocks, (c, v) -> c.initialRadiusBlocks = v)
                .range(WorldzConfig.MIN_BORDER_RADIUS_BLOCKS, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Border half-width when the world is created.")
                .build(),
            Setting.<BorderConfig>integer("finalRadiusBlocks", c -> c.finalRadiusBlocks, (c, v) -> c.finalRadiusBlocks = v)
                .range(WorldzConfig.MIN_BORDER_RADIUS_BLOCKS, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Border half-width after the configured resize period.")
                .build(),
            Setting.<BorderConfig>integer("resizeDays", c -> c.resizeDays, (c, v) -> c.resizeDays = v)
                .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                .unit(Unit.DAYS)
                .doc("In-game days used for the linear transition from initial to final radius.")
                .build(),
            Setting.<BorderConfig>integer("resizeDelayDays", c -> c.resizeDelayDays, (c, v) -> c.resizeDelayDays = v)
                .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                .unit(Unit.DAYS)
                .doc("In-game days to hold the initial radius before resizing.")
                .build(),
            Setting.<BorderConfig>integer("resizeRateBlocks", c -> c.resizeRateBlocks, (c, v) -> c.resizeRateBlocks = v)
                .range(0, WorldzConfig.MAX_BORDER_RATE_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Radius blocks traversed per rate interval, or zero to use resizeDays.")
                .build(),
            Setting.<BorderConfig>integer("resizeRateDays", c -> c.resizeRateDays, (c, v) -> c.resizeRateDays = v)
                .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                .unit(Unit.DAYS)
                .doc("In-game days per rate interval, or zero to use resizeDays.")
                .build(),
            Setting.<BorderConfig>flag(objectiveKey, c -> c.ensureObjective, (c, v) -> c.ensureObjective = v)
                .doc("Whether the dimension's progression objective must be reachable inside the final border.")
                .build(),
            Setting.<BorderConfig, ResizeStyle>enumeration(
                    "resizeStyle", c -> c.resizeStyle, (c, v) -> c.resizeStyle = v,
                    ResizeStyle::parse, ResizeStyle::serializedName, ResizeStyle.CONTINUOUS
                )
                .doc("Whether the rate fields drive one smooth lerp or abrupt jumps.")
                .build()
        );
    }

    /**
     * Cross-field checks (DESIGN R1's sibling of R2): the incomplete-rate-pair reset, then the
     * stepped-without-a-rate fallback -- in that order, matching {@code sanitizeBorder}'s tail.
     */
    @Override
    protected void postValidate(BorderConfig value, SanitizeContext ctx) {
        if ((value.resizeRateBlocks == 0) != (value.resizeRateDays == 0)) {
            ctx.logger().warn(
                "Ignoring incomplete {} resize rate; both resizeRateBlocks and resizeRateDays must be positive.", path()
            );
            value.resizeRateBlocks = 0;
            value.resizeRateDays = 0;
        }
        if (value.resizeStyle == ResizeStyle.STEPPED && value.resizeRateBlocks == 0) {
            ctx.logger().warn("{} resizeStyle 'stepped' needs resizeRateBlocks/resizeRateDays; using 'continuous' instead.", path());
            value.resizeStyle = ResizeStyle.CONTINUOUS;
        }
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments, most labels are relabeled (e.g. {@code "initial="} not {@code
     * "initialRadiusBlocks="}), the objective flag renders under its own {@code summaryObjectiveName}
     * (not its YAML key), and the two rate fields collapse into one composite {@code "rate="}
     * segment -- none of this is mechanically derivable.
     */
    @Override
    public String summary(BorderConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "initial=" + value.initialRadiusBlocks
            + ", final=" + value.finalRadiusBlocks
            + ", days=" + value.resizeDays
            + ", delayDays=" + value.resizeDelayDays
            + ", rate=" + (value.resizeRateBlocks == 0
                ? "<total-days>"
                : value.resizeRateBlocks + " blocks/" + value.resizeRateDays + " days")
            + ", style=" + value.resizeStyle.serializedName()
            + ", " + summaryObjectiveName + "=" + value.ensureObjective;
    }
}
