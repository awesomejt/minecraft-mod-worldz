package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.SingleBiomeConfig;
import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link SingleBiomeConfig} (DESIGN §20.2's Phase 2.1 subsection; TODO 25.2d) --
 * defaults for the {@code jlt_worldz:single_biome} typed preset. {@code landBiome} is the one
 * "double warning" shape in this section (DESIGN R7): {@code sanitizeSingleBiomeId} can itself warn
 * and blank the value, then the caller warns a second time and substitutes a concrete fallback --
 * modeled as {@link Rule.BiomeId} composed with {@link Rule.BlankFallback} via {@link Rule#of},
 * silent-null-then-loud-blank exactly like {@code strip.widthMode}'s {@code NullFallback}-then-
 * {@code RejectValue} composition.
 *
 * <p>Summary is fully derivable (DESIGN §41.6's table: "derived + a paired renderer"): {@code
 * starterBiome} renders {@code <none>} for the unset empty string, and {@code spawn} renders as the
 * bare strategy name by delegating to the nested {@link SpawnSchema}'s own summary -- both are
 * per-setting {@code .render(...)} overrides, not a {@code summary()} override on this class.
 */
public final class SingleBiomeSchema extends SchemaSection<SingleBiomeConfig> {
    public SingleBiomeSchema(String path) {
        super(path, SingleBiomeConfig::new);
    }

    @Override
    protected List<Setting<SingleBiomeConfig, ?>> declare() {
        SpawnSchema spawnSchema = new SpawnSchema(path() + ".spawn");
        return List.of(
            Setting.<SingleBiomeConfig>text("landBiome", c -> c.landBiome, (c, v) -> c.landBiome = v)
                .rule(Rule.of(
                    new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".landBiome '{}'."),
                    new Rule.BlankFallback<>(
                        "minecraft:plains", path() + ".landBiome must be one biome ID; using minecraft:plains instead."
                    )
                ))
                .unit(Unit.BIOME_ID)
                .doc("The one biome that fills the generated world.")
                .build(),
            Setting.<SingleBiomeConfig>text("starterBiome", c -> c.starterBiome, (c, v) -> c.starterBiome = v)
                .rule(new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".starterBiome '{}'."))
                .unit(Unit.BIOME_ID)
                .render(SingleBiomeSchema::renderStarterBiome)
                .doc("Optional different biome forced around spawn; empty means same as landBiome.")
                .build(),
            Setting.<SingleBiomeConfig>integer(
                    "starterRadiusBlocks", c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v
                )
                .range(WorldzConfig.MIN_STARTER_RADIUS_BLOCKS, WorldzConfig.MAX_STARTER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Starter-zone radius, only meaningful when starterBiome is set.")
                .build(),
            Setting.<SingleBiomeConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
                .render(spawnSchema::summary)
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<SingleBiomeConfig>flag("allowRivers", c -> c.allowRivers, (c, v) -> c.allowRivers = v)
                .doc("Let vanilla's own river biomes generate where vanilla would place one.")
                .build(),
            Setting.<SingleBiomeConfig>flag("allowOceans", c -> c.allowOceans, (c, v) -> c.allowOceans = v)
                .doc("Let vanilla's own river/ocean-family biomes generate naturally, additive over allowRivers.")
                .build(),
            Setting.<SingleBiomeConfig>flag("allowBeaches", c -> c.allowBeaches, (c, v) -> c.allowBeaches = v)
                .doc("Let vanilla's own beach/stony-shore biomes generate where vanilla would place one.")
                .build()
        );
    }

    private static String renderStarterBiome(String value) {
        return value.isEmpty() ? "<none>" : value;
    }
}
