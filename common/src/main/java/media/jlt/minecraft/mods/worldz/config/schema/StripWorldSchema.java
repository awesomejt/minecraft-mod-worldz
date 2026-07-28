package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.StripBandsConfig;
import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;

import java.util.List;

/**
 * Schema for {@link StripWorldConfig} (GOALS 32, DESIGN §23; TODO 25.2d) -- defaults for the {@code
 * jlt_worldz:strip_world} typed preset. Only two fields, both nested sections, sanitizing in the
 * same order they read and emit -- no cross-field hook needed. The corridor width itself lives in
 * the separate top-level {@link media.jlt.minecraft.mods.worldz.config.StripConfig} (TODO 25.7's
 * D2/D10 job to merge; explicitly out of scope here).
 *
 * <p>Summary is fully derivable: both settings render via their nested schema's own {@code
 * summary()} ({@link SpawnSchema}'s bare strategy name, {@link StripBandsSchema}'s own {@code
 * <disabled>}-gated line), reproducing {@code stripWorldSummary}'s {@code "spawn=..., bands=..."}
 * exactly with no override needed here.
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
            Setting.<StripWorldConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
                .render(spawnSchema::summary)
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<StripWorldConfig, StripBandsConfig>section("bands", c -> c.bands, (c, v) -> c.bands = v, bandsSchema)
                .render(bandsSchema::summary)
                .doc("Optional ordered biome-band sequence along the strip's length.")
                .build()
        );
    }
}
