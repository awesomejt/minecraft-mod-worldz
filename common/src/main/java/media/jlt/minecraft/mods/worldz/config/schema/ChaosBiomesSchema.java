package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ChaosBiomesConfig;
import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link ChaosBiomesConfig} (DESIGN §20.11's Phase 4.1 subsection; TODO 25.2d) --
 * defaults for the {@code jlt_worldz:chaos_biomes} typed preset. Every field sanitizes in the same
 * order it reads and emits (unlike {@link LayoutSchema}, whose {@code roleOverrides} forces a
 * postValidate reorder), so this section needs no cross-field hook at all: {@code biomes}' weighted
 * -list rule, {@code regionScale}'s clamp and the {@code starter} group's own rules are ordinary
 * per-setting rules, declared in exactly {@code chaosBiomesMap}'s pre-25.6 order.
 *
 * <p>{@code starterBiome}/{@code starterRadiusBlocks} and {@code allowRivers}/{@code
 * allowOceans}/{@code allowBeaches} were folded into the shared {@code starter}/{@code
 * naturalBiomes} groups at TODO 25.6b ({@link StarterSchema}, {@link NaturalBiomesSchema});
 * {@code regionScaleBlocks} dropped its {@code Blocks} suffix in the same sub-step.
 *
 * <p>Summary is fully derivable (DESIGN §41.6's table: "derived + a paired renderer"), the same
 * shape as {@link SingleBiomeSchema}.
 */
public final class ChaosBiomesSchema extends SchemaSection<ChaosBiomesConfig> {
    public ChaosBiomesSchema(String path) {
        super(path, ChaosBiomesConfig::new);
    }

    @Override
    protected List<Setting<ChaosBiomesConfig, ?>> declare() {
        SpawnSchema spawnSchema = new SpawnSchema(path() + ".spawn");
        StarterSchema<ChaosBiomesConfig> starterSchema = new StarterSchema<>(
            path() + ".starter", ChaosBiomesConfig::new,
            new Accessor<>(c -> c.starterBiome, (c, v) -> c.starterBiome = v),
            new Accessor<>(c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v)
        );
        NaturalBiomesSchema<ChaosBiomesConfig> naturalBiomesSchema = new NaturalBiomesSchema<>(
            path() + ".naturalBiomes", ChaosBiomesConfig::new,
            new Accessor<>(c -> c.allowRivers, (c, v) -> c.allowRivers = v),
            new Accessor<>(c -> c.allowOceans, (c, v) -> c.allowOceans = v),
            new Accessor<>(c -> c.allowBeaches, (c, v) -> c.allowBeaches = v)
        );
        return List.of(
            Setting.<ChaosBiomesConfig>stringList("biomes", c -> c.biomes, (c, v) -> c.biomes = v)
                .rule(new Rule.WeightedBiomeIdList<>(
                    "Ignoring invalid " + path() + " biome '{}'.",
                    () -> new ChaosBiomesConfig().biomes,
                    path() + ".biomes has no usable entries; using the default biome list instead."
                ))
                .unit(Unit.BIOME_ID)
                .doc("Weighted land biome entries (id or id@weight) shuffled per region.")
                .build(),
            Setting.<ChaosBiomesConfig>integer("regionScale", c -> c.regionScaleBlocks, (c, v) -> c.regionScaleBlocks = v)
                .range(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Grid-cell edge length in blocks; smaller means more frequent biome changes.")
                .build(),
            Setting.group("starter", starterSchema)
                .render(starterSchema::summary)
                .doc("Optional biome forced in a circular zone around spawn; empty means chaos starts at spawn.")
                .build(),
            Setting.<ChaosBiomesConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
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
