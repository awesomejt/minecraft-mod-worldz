package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import media.jlt.minecraft.mods.worldz.config.RisingLavaConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link RisingLavaConfig} (GOAL 29, DESIGN §35.2) -- a shared world-hazard runtime
 * rule, root-level, composable with any world type. Overworld only for this phase (DESIGN §35.3).
 *
 * <p>{@code rateBlocks}/{@code rateDays} declare {@link Rule.None} and are instead clamped inside
 * {@link #postValidate}, <em>after</em> the {@code maxY >= startY} cross-field check -- matching
 * {@code sanitizeRisingLava}'s own interleaved order exactly (delay/start/max clamps, then the
 * cross-check, then the rate clamps). A plain per-setting declaration order would run the rate
 * clamps before {@code postValidate}, reordering the WARN lines whenever both fire for the same
 * input.
 */
public final class RisingLavaSchema extends SchemaSection<RisingLavaConfig> {
    private static final int MIN_Y = FlatConfig.OVERWORLD_MIN_Y;
    private static final int MAX_BUILD_Y = FlatConfig.OVERWORLD_MIN_Y + FlatConfig.MAX_TOTAL_HEIGHT_BLOCKS - 1;

    public RisingLavaSchema(String path) {
        super(path, RisingLavaConfig::new);
    }

    @Override
    protected List<Setting<RisingLavaConfig, ?>> declare() {
        return List.of(
            Setting.<RisingLavaConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .live()
                .doc("Whether the lava level rises over time.")
                .build(),
            Setting.<RisingLavaConfig>integer("delayDays", c -> c.delayDays, (c, v) -> c.delayDays = v)
                .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                .unit(Unit.DAYS).live()
                .doc("In-game days before the level starts rising.")
                .build(),
            Setting.<RisingLavaConfig>integer("startY", c -> c.startY, (c, v) -> c.startY = v)
                .range(MIN_Y, MAX_BUILD_Y)
                .unit(Unit.Y_LEVEL).live()
                .doc("Y the lava level starts at, before any rise.")
                .build(),
            Setting.<RisingLavaConfig>integer("maxY", c -> c.maxY, (c, v) -> c.maxY = v)
                .range(MIN_Y, MAX_BUILD_Y)
                .unit(Unit.Y_LEVEL).live()
                .doc("Y the lava level stops rising at.")
                .build(),
            Setting.<RisingLavaConfig>integer("rateBlocks", c -> c.rateBlocks, (c, v) -> c.rateBlocks = v)
                .unit(Unit.BLOCKS).live()
                .rangeText(rangeText(1, WorldzConfig.MAX_BORDER_RATE_BLOCKS))
                .doc("Blocks the level rises per rateDays.")
                .build(),
            Setting.<RisingLavaConfig>integer("rateDays", c -> c.rateDays, (c, v) -> c.rateDays = v)
                .unit(Unit.DAYS).live()
                .rangeText(rangeText(1, WorldzConfig.MAX_BORDER_RESIZE_DAYS))
                .doc("In-game days per rateBlocks of rise.")
                .build()
        );
    }

    private static String rangeText(int min, int max) {
        return min + ".." + max;
    }

    /**
     * Cross-field check ({@code maxY >= startY}), then the two rate clamps -- in that exact order,
     * matching {@code sanitizeRisingLava} (DESIGN's postValidate hook, per TODO 25.2b).
     */
    @Override
    protected void postValidate(RisingLavaConfig value, SanitizeContext ctx) {
        if (value.maxY < value.startY) {
            ctx.logger().warn("risingLava.maxY ({}) was below startY ({}); raised to match.", value.maxY, value.startY);
            value.maxY = value.startY;
        }
        value.rateBlocks = clampWithWarning(value.rateBlocks, 1, WorldzConfig.MAX_BORDER_RATE_BLOCKS, path() + ".rateBlocks", ctx);
        value.rateDays = clampWithWarning(value.rateDays, 1, WorldzConfig.MAX_BORDER_RESIZE_DAYS, path() + ".rateDays", ctx);
    }

    private static int clampWithWarning(int value, int minimum, int maximum, String name, SanitizeContext ctx) {
        int clamped = Math.clamp(value, minimum, maximum);
        if (clamped != value) {
            ctx.logger().warn("Clamped {} from {} to {}.", name, value, clamped);
        }
        return clamped;
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments -- not mechanically derivable (no section-level "disabled" gate in the framework).
     */
    @Override
    public String summary(RisingLavaConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "delayDays=" + value.delayDays + ", startY=" + value.startY + ", maxY=" + value.maxY
            + ", rateBlocks=" + value.rateBlocks + ", rateDays=" + value.rateDays;
    }
}
