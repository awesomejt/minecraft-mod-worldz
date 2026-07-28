package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.ExteriorConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

import java.util.List;
import java.util.function.Function;

/**
 * Schema for {@link ExteriorConfig} (GOALS 17) -- parameterized by a per-parent legality flag
 * ({@code oceanAllowed}: true for Overworld, false for Nether, DESIGN §41.5) and by an accessor
 * onto the sibling {@link BorderConfig} the boundary is derived from when none is explicit
 * (DESIGN R2, the one section that needs {@link SanitizeContext#root()}).
 *
 * <p>{@code mode}'s own ocean-legality rejection is a per-setting rule (composed with the usual
 * null-fallback) so it runs inline, before {@code boundaryRadius}/{@code
 * oceanTransitionWidth}'s own clamps -- matching {@code sanitizeExterior}'s exact top-to-
 * bottom order. The remaining two checks (boundary-required-or-enabled-border, and the ocean-
 * transition-vs-resolved-boundary clamp) read the sibling border via {@code ctx.root()}, so they
 * cannot be per-setting rules; they live in {@link #postValidate}, in the same order as today's
 * interleaved {@code sanitizeExterior} tail (R2: "cannot be deferred to a root-level post-pass...
 * the exterior's own clamps and its cross-section checks interleave").
 */
public final class ExteriorSchema extends SchemaSection<ExteriorConfig> {
    private final Function<WorldzConfig, BorderConfig> borderAccessor;
    private final boolean oceanAllowed;

    public ExteriorSchema(String path, Function<WorldzConfig, BorderConfig> borderAccessor, boolean oceanAllowed) {
        super(path, ExteriorConfig::new);
        this.borderAccessor = borderAccessor;
        this.oceanAllowed = oceanAllowed;
    }

    @Override
    protected List<Setting<ExteriorConfig, ?>> declare() {
        Rule<ExteriorConfig, ExteriorMode> modeRule = oceanAllowed
            ? new Rule.NullFallback<>(() -> ExteriorMode.NORMAL)
            : Rule.of(
                new Rule.NullFallback<>(() -> ExteriorMode.NORMAL),
                new Rule.RejectValue<>(
                    ExteriorMode.OCEAN, ExteriorMode.NORMAL,
                    "Ignoring unsupported ocean mode for " + path() + "; using normal terrain."
                )
            );
        return List.of(
            Setting.<ExteriorConfig, ExteriorMode>enumeration(
                    "mode", c -> c.mode, (c, v) -> c.mode = v,
                    ExteriorMode::parse, ExteriorMode::serializedName, ExteriorMode.NORMAL
                )
                .rule(modeRule)
                .doc("Terrain generated outside the central envelope: normal, ocean or void.")
                .build(),
            Setting.<ExteriorConfig>integer(
                    "boundaryRadius", c -> c.boundaryRadiusBlocks, (c, v) -> c.boundaryRadiusBlocks = v
                )
                .range(0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Outer envelope half-width, or zero to derive it from an enabled border.")
                .build(),
            Setting.<ExteriorConfig>integer(
                    "oceanTransitionWidth", c -> c.oceanTransitionWidthBlocks, (c, v) -> c.oceanTransitionWidthBlocks = v
                )
                .range(0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Ocean width inside the outer boundary; ignored by normal and void modes.")
                .build()
        );
    }

    /**
     * Cross-section checks reading the sibling border via {@code ctx.root()} (DESIGN R2): first,
     * fall back to normal terrain when a non-normal mode has neither an explicit boundary nor an
     * enabled border to derive one from; then resolve the effective boundary (the explicit value,
     * or the border's larger radius); then clamp the ocean transition width against it. This exact
     * order matches {@code sanitizeExterior}'s tail, which runs immediately after the two int
     * clamps above.
     */
    @Override
    protected void postValidate(ExteriorConfig value, SanitizeContext ctx) {
        BorderConfig border = borderAccessor.apply(ctx.root());
        if (value.mode != ExteriorMode.NORMAL && value.boundaryRadiusBlocks == 0 && !border.enabled) {
            ctx.logger().warn("{} requires an explicit boundary or an enabled border; using normal terrain.", path());
            value.mode = ExteriorMode.NORMAL;
        }
        int resolvedBoundary = value.boundaryRadiusBlocks == 0
            ? Math.max(border.initialRadiusBlocks, border.finalRadiusBlocks)
            : value.boundaryRadiusBlocks;
        if (value.mode == ExteriorMode.OCEAN && value.oceanTransitionWidthBlocks > resolvedBoundary) {
            ctx.logger().warn(
                "Clamped {}.oceanTransitionWidth from {} to {}.", path(), value.oceanTransitionWidthBlocks, resolvedBoundary
            );
            value.oceanTransitionWidthBlocks = resolvedBoundary;
        }
    }

    /**
     * Overridden: {@code mode} gates the whole line ({@code "<normal>"} when normal), the surviving
     * segments are relabeled ({@code "boundary="} renders {@code "auto"} for zero), and {@code
     * transition=} only appears for ocean mode -- not mechanically derivable.
     */
    @Override
    public String summary(ExteriorConfig value) {
        if (value.mode == ExteriorMode.NORMAL) {
            return "<normal>";
        }
        return value.mode.serializedName() + ", boundary="
            + (value.boundaryRadiusBlocks == 0 ? "auto" : value.boundaryRadiusBlocks)
            + (value.mode == ExteriorMode.OCEAN ? ", transition=" + value.oceanTransitionWidthBlocks : "");
    }
}
