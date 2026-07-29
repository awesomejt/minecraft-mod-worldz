package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;

/**
 * A narrow corridor along the X axis (GOALS 32): columns outside the absolute
 * {@code width} on the Z axis are classified by {@code widthMode}, layered additively on top of whatever the dimension's own
 * square border/exterior envelope already decide (DESIGN §23) rather than replacing it.
 * The corridor's length uses the existing square border/exterior machinery unmodified.
 *
 * @param enabled whether the width constraint applies
 * @param width absolute corridor width in blocks
 * @param widthMode terrain generated beyond the width -- void or ocean, never normal
 */
public record StripPlan(boolean enabled, int width, ExteriorMode widthMode) {
    /** Validates persisted values even while the strip is disabled. */
    public StripPlan {
        if (width <= 0) {
            throw new IllegalArgumentException("strip width must be positive");
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
        return new StripPlan(false, 1, ExteriorMode.VOID);
    }

    /**
     * Resolves a dimension's plan from sanitized YAML values. The Nether only receives the
     * width constraint when {@link StripWorldConfig#applyToNether} is set.
     *
     * @param config sanitized strip configuration
     * @param overworld whether to resolve the Overworld rather than the Nether
     * @return resolved plan, disabled unless applicable
     */
    public static StripPlan fromConfig(StripWorldConfig config, boolean overworld) {
        if (!config.enabled || (!overworld && !config.applyToNether)) {
            return disabled();
        }
        return new StripPlan(true, config.width, config.widthMode);
    }

    /**
     * Resolves the dedicated {@code strip_world} preset, whose Overworld corridor does not depend
     * on the generic preset's {@link StripWorldConfig#enabled} opt-in.
     */
    public static StripPlan fromDedicatedConfig(StripWorldConfig config, boolean overworld) {
        if (!overworld && !config.applyToNether) {
            return disabled();
        }
        return new StripPlan(true, config.width, config.widthMode);
    }

    /**
     * Resolves fieldless generator defaults by explicit preset identity. Generic strip settings
     * must never leak into another typed preset merely because it also uses an enveloped
     * generator.
     *
     * @param config sanitized merged strip-world configuration
     * @param overworld whether to resolve the Overworld rather than the Nether
     * @param worldType explicit generator hint ({@code worldz} or {@code strip_world})
     * @return the preset-appropriate plan, disabled for every unrelated or absent hint
     */
    public static StripPlan fromPresetConfig(StripWorldConfig config, boolean overworld, String worldType) {
        return switch (worldType) {
            case "worldz" -> fromConfig(config, overworld);
            case "strip_world" -> fromDedicatedConfig(config, overworld);
            default -> disabled();
        };
    }

    /**
     * Classifies a column's Z distance from the origin, ignoring X entirely -- the
     * corridor's length is bounded by the existing square border/exterior mechanism, not
     * this plan.
     *
     * @param relativeZ block Z relative to the origin
     * @return {@link #widthMode} beyond the absolute width, otherwise {@link ExteriorMode#NORMAL}
     */
    public ExteriorMode modeAt(int relativeZ) {
        if (!enabled) {
            return ExteriorMode.NORMAL;
        }
        return relativeZ < minZ() || relativeZ > maxZ() ? widthMode : ExteriorMode.NORMAL;
    }

    /** Inclusive negative-Z edge relative to the corridor origin. */
    public int minZ() {
        return -(width - 1) / 2;
    }

    /** Inclusive positive-Z edge relative to the corridor origin. */
    public int maxZ() {
        return width / 2;
    }
}
