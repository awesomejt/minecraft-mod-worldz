package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.StripBandsConfig;
import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

import java.util.List;

/**
 * Schema for {@link StripWorldConfig} (GOALS 32, DESIGN §23; TODO 25.2d) -- defaults for the {@code
 * jlt_worldz:strip_world} typed preset. The dedicated preset always applies the configured
 * corridor; {@code enabled} controls only composition into the generic {@code worldz} preset.
 */
public final class StripWorldSchema extends SchemaSection<StripWorldConfig> {
    public StripWorldSchema(String path) {
        super(path, StripWorldConfig::new);
    }

    @Override
    protected List<Setting<StripWorldConfig, ?>> declare() {
        SpawnSchema spawnSchema = new SpawnSchema(path() + ".spawn");
        StripBandsSchema bandsSchema = new StripBandsSchema(path() + ".bands");
        return List.of(
            Setting.<StripWorldConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .preset()
                .doc("Whether the generic worldz preset applies this corridor; the dedicated strip_world preset always does.")
                .build(),
            Setting.<StripWorldConfig>integer("width", c -> c.width, (c, v) -> c.width = v)
                .min(1)
                .unit(Unit.BLOCKS)
                .preset("worldz", "strip_world").customizeExposed()
                .doc("Absolute corridor width in blocks; even widths take the extra block on +Z.")
                .build(),
            Setting.<StripWorldConfig, ExteriorMode>enumeration(
                    "widthMode", c -> c.widthMode, (c, v) -> c.widthMode = v,
                    ExteriorMode::parse, ExteriorMode::serializedName, ExteriorMode.VOID
                )
                .rule(Rule.of(
                    new Rule.NullFallback<>(() -> ExteriorMode.VOID),
                    new Rule.RejectValue<>(
                        ExteriorMode.NORMAL, ExteriorMode.VOID,
                        "stripWorld.widthMode cannot be normal; using void instead."
                    )
                ))
                .preset("worldz", "strip_world").customizeExposed()
                .doc("Terrain generated beyond the corridor -- void or ocean, never normal.")
                .build(),
            Setting.<StripWorldConfig>flag("applyToNether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                .preset("worldz", "strip_world").customizeExposed()
                .doc("Whether the same corridor width also applies to the Nether.")
                .build(),
            Setting.<StripWorldConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
                .render(spawnSchema::summary)
                .preset("strip_world").customizeExposed()
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<StripWorldConfig, StripBandsConfig>section("bands", c -> c.bands, (c, v) -> c.bands = v, bandsSchema)
                .render(bandsSchema::summary)
                .preset("strip_world").customizeExposed()
                .doc("Optional ordered biome-band sequence along the strip's length.")
                .build()
        );
    }
}
