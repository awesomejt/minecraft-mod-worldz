package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.SingleBiomeConfig;
import media.jlt.minecraft.mods.worldz.config.SpawnConfig;

import java.util.List;

/**
 * Schema for {@link SingleBiomeConfig} (DESIGN §20.2's Phase 2.1 subsection; TODO 25.2d) --
 * defaults for the {@code jlt_worldz:single_biome} typed preset. {@code biome} (renamed from
 * {@code landBiome}, TODO 25.6b) is the one "double warning" shape in this section (DESIGN R7):
 * {@code sanitizeSingleBiomeId} can itself warn and blank the value, then the caller warns a
 * second time and substitutes a concrete fallback -- modeled as {@link Rule.BiomeId} composed with
 * {@link Rule.BlankFallback} via {@link Rule#of}, silent-null-then-loud-blank exactly like {@code
 * strip.widthMode}'s {@code NullFallback}-then-{@code RejectValue} composition.
 *
 * <p>{@code starterBiome}/{@code starterRadiusBlocks} and {@code allowRivers}/{@code
 * allowOceans}/{@code allowBeaches} were folded into the shared {@code starter}/{@code
 * naturalBiomes} groups at TODO 25.6b ({@link StarterSchema}, {@link NaturalBiomesSchema}).
 *
 * <p>Summary is fully derivable (DESIGN §41.6's table: "derived + a paired renderer"): {@code
 * starter} and {@code naturalBiomes} render via their own group summaries, and {@code spawn}
 * renders as the bare strategy name by delegating to the nested {@link SpawnSchema}'s own summary
 * -- all per-setting {@code .render(...)} overrides, not a {@code summary()} override on this
 * class.
 */
public final class SingleBiomeSchema extends SchemaSection<SingleBiomeConfig> {
    public SingleBiomeSchema(String path) {
        super(path, SingleBiomeConfig::new);
    }

    @Override
    protected List<Setting<SingleBiomeConfig, ?>> declare() {
        SpawnSchema spawnSchema = new SpawnSchema(path() + ".spawn");
        StarterSchema<SingleBiomeConfig> starterSchema = new StarterSchema<>(
            path() + ".starter", SingleBiomeConfig::new,
            new Accessor<>(c -> c.starterBiome, (c, v) -> c.starterBiome = v),
            new Accessor<>(c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v)
        );
        NaturalBiomesSchema<SingleBiomeConfig> naturalBiomesSchema = new NaturalBiomesSchema<>(
            path() + ".naturalBiomes", SingleBiomeConfig::new,
            new Accessor<>(c -> c.allowRivers, (c, v) -> c.allowRivers = v),
            new Accessor<>(c -> c.allowOceans, (c, v) -> c.allowOceans = v),
            new Accessor<>(c -> c.allowBeaches, (c, v) -> c.allowBeaches = v)
        );
        return List.of(
            Setting.<SingleBiomeConfig>text("biome", c -> c.landBiome, (c, v) -> c.landBiome = v)
                .rule(Rule.of(
                    new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".biome '{}'."),
                    new Rule.BlankFallback<>(
                        "minecraft:plains", path() + ".biome must be one biome ID; using minecraft:plains instead."
                    )
                ))
                .unit(Unit.BIOME_ID)
                .doc("The one biome that fills the generated world.")
                .build(),
            Setting.group("starter", starterSchema)
                .render(starterSchema::summary)
                .doc("Optional different biome forced around spawn; empty means same as biome.")
                .build(),
            Setting.<SingleBiomeConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
                .render(spawnSchema::summary)
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.group("naturalBiomes", naturalBiomesSchema)
                .render(naturalBiomesSchema::summary)
                .doc("Let vanilla's own river/ocean/beach biomes generate where vanilla would place one.")
                .build()
        );
    }
}
