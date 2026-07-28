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
 * -list rule, {@code starterBiome}'s {@link Rule.BiomeId} and the two int clamps are ordinary
 * per-setting rules, declared in exactly {@code chaosBiomesMap}'s order.
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
            Setting.<ChaosBiomesConfig>integer("regionScaleBlocks", c -> c.regionScaleBlocks, (c, v) -> c.regionScaleBlocks = v)
                .range(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Grid-cell edge length in blocks; smaller means more frequent biome changes.")
                .build(),
            Setting.<ChaosBiomesConfig>text("starterBiome", c -> c.starterBiome, (c, v) -> c.starterBiome = v)
                .rule(new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".starterBiome '{}'."))
                .unit(Unit.BIOME_ID)
                .render(ChaosBiomesSchema::renderStarterBiome)
                .doc("Optional biome forced in a circular zone around spawn; empty means chaos starts at spawn.")
                .build(),
            Setting.<ChaosBiomesConfig>integer(
                    "starterRadiusBlocks", c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v
                )
                .range(WorldzConfig.MIN_STARTER_RADIUS_BLOCKS, WorldzConfig.MAX_STARTER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Starter-zone radius, only meaningful when starterBiome is set.")
                .build(),
            Setting.<ChaosBiomesConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawnSchema)
                .render(spawnSchema::summary)
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<ChaosBiomesConfig>flag("allowRivers", c -> c.allowRivers, (c, v) -> c.allowRivers = v)
                .doc("Let vanilla's own river biomes generate where vanilla would place one.")
                .build(),
            Setting.<ChaosBiomesConfig>flag("allowOceans", c -> c.allowOceans, (c, v) -> c.allowOceans = v)
                .doc("Let vanilla's own river/ocean-family biomes generate naturally, additive over allowRivers.")
                .build(),
            Setting.<ChaosBiomesConfig>flag("allowBeaches", c -> c.allowBeaches, (c, v) -> c.allowBeaches = v)
                .doc("Let vanilla's own beach/stony-shore biomes generate where vanilla would place one.")
                .build()
        );
    }

    private static String renderStarterBiome(String value) {
        return value.isEmpty() ? "<none>" : value;
    }
}
