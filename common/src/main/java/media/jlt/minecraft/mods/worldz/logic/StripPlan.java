package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StripConfig;

/**
 * A narrow corridor along the X axis (GOALS 32): columns farther than
 * {@code widthRadiusBlocks} from the origin on the Z axis are classified by
 * {@code widthMode}, layered additively on top of whatever the dimension's own
 * square border/exterior envelope already decide (DESIGN §23) rather than replacing it.
 * The corridor's length uses the existing square border/exterior machinery unmodified.
 *
 * @param enabled whether the width constraint applies
 * @param widthRadiusBlocks half-width from the origin; the corridor is twice this wide
 * @param widthMode terrain generated beyond the width -- void or ocean, never normal
 */
public record StripPlan(boolean enabled, int widthRadiusBlocks, ExteriorMode widthMode) {
    /** Validates persisted values even while the strip is disabled. */
    public StripPlan {
        if (widthRadiusBlocks <= 0) {
            throw new IllegalArgumentException("strip width radius must be positive");
        }
        if (widthMode == null || widthMode == ExteriorMode.NORMAL) {
            throw new IllegalArgumentException("strip width mode must be void or ocean");
        }
    }

    /**
     * Returns a plan with the width constraint switched off.
     *
     * @return disabled strip plan with safe placeholder values
     */
    public static StripPlan disabled() {
        return new StripPlan(false, 512, ExteriorMode.VOID);
    }

    /**
     * Resolves a dimension's plan from sanitized YAML values. The Nether only receives the
     * width constraint when {@link StripConfig#applyToNether} is set.
     *
     * @param config sanitized strip configuration
     * @param overworld whether to resolve the Overworld rather than the Nether
     * @return resolved plan, disabled unless applicable
     */
    public static StripPlan fromConfig(StripConfig config, boolean overworld) {
        if (!config.enabled || (!overworld && !config.applyToNether)) {
            return disabled();
        }
        return new StripPlan(true, config.widthRadiusBlocks, config.widthMode);
    }

    /**
     * Classifies a column's Z distance from the origin, ignoring X entirely -- the
     * corridor's length is bounded by the existing square border/exterior mechanism, not
     * this plan.
     *
     * @param relativeZ block Z relative to the origin
     * @return {@link #widthMode} beyond the width radius, otherwise {@link ExteriorMode#NORMAL}
     */
    public ExteriorMode modeAt(int relativeZ) {
        if (!enabled) {
            return ExteriorMode.NORMAL;
        }
        return Math.abs((long)relativeZ) > widthRadiusBlocks ? widthMode : ExteriorMode.NORMAL;
    }
}
