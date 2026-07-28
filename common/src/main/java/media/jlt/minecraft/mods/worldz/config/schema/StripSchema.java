package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StripConfig;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

import java.util.List;

/**
 * Schema for {@link StripConfig} (GOALS 32). Root-level section for a narrow strip-world
 * corridor. {@code widthMode} composes a {@code null}-fallback with a {@link Rule.RejectValue}
 * check, since the corridor's wall can only be void or ocean, never normal terrain.
 */
public final class StripSchema extends SchemaSection<StripConfig> {
    public StripSchema(String path) {
        super(path, StripConfig::new);
    }

    @Override
    protected List<Setting<StripConfig, ?>> declare() {
        return List.of(
            Setting.<StripConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether the corridor's width constraint applies.")
                .build(),
            Setting.<StripConfig>integer("widthRadiusBlocks", c -> c.widthRadiusBlocks, (c, v) -> c.widthRadiusBlocks = v)
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Half-width from the origin; the corridor is twice this wide.")
                .build(),
            Setting.<StripConfig, ExteriorMode>enumeration(
                    "widthMode", c -> c.widthMode, (c, v) -> c.widthMode = v,
                    ExteriorMode::parse, ExteriorMode::serializedName, ExteriorMode.VOID
                )
                .rule(Rule.of(
                    new Rule.NullFallback<>(() -> ExteriorMode.VOID),
                    new Rule.RejectValue<>(ExteriorMode.NORMAL, ExteriorMode.VOID, "strip.widthMode cannot be normal; using void instead.")
                ))
                .doc("Terrain generated beyond the width -- void or ocean, never normal.")
                .build(),
            Setting.<StripConfig>flag("applyToNether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                .doc("Whether the same corridor width also applies to the Nether.")
                .build()
        );
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments, and {@code widthRadiusBlocks} renders under the relabeled segment {@code
     * "widthRadius="}, not {@code "widthRadiusBlocks="} -- not mechanically derivable.
     */
    @Override
    public String summary(StripConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "widthRadius=" + value.widthRadiusBlocks
            + ", widthMode=" + value.widthMode.serializedName()
            + ", applyToNether=" + value.applyToNether;
    }
}
