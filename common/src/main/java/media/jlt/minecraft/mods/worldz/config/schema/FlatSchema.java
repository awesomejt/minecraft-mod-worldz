package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;

import java.util.List;

/**
 * Schema for {@link FlatConfig} (GOAL 15, DESIGN §33.2) -- defaults for the {@code
 * jlt_worldz:flat} typed preset, consulted only when it resolves without explicit
 * Customize-screen values. Summary is fully derivable: no gate, no relabeling.
 *
 * <p>{@code undergroundBiome}/{@code undergroundBelowSurfaceBlocks} (GOAL 42, DESIGN §37.3) are
 * now wired into the schema as the shared {@code underground: {biome, belowSurface}} group (TODO
 * 25.6g, {@link UndergroundSchema}) -- previously a pre-existing gap ({@code config/tests/97}'s
 * values were silently ignored), now a real, sanitized pair.
 */
public final class FlatSchema extends SchemaSection<FlatConfig> {
    private final UndergroundSchema<FlatConfig> underground;

    public FlatSchema(String path) {
        super(path, FlatConfig::new);
        this.underground = new UndergroundSchema<>(
            path() + ".underground", FlatConfig::new,
            new Accessor<>(c -> c.undergroundBiome, (c, v) -> c.undergroundBiome = v),
            new Accessor<>(c -> c.undergroundBelowSurfaceBlocks, (c, v) -> c.undergroundBelowSurfaceBlocks = v)
        );
    }

    @Override
    protected List<Setting<FlatConfig, ?>> declare() {
        return List.of(
            Setting.<FlatConfig>stringList("layers", c -> c.layers, (c, v) -> c.layers = v)
                .rule(new Rule.EmptyListFallback<>(
                    () -> new FlatConfig().layers, "flat.layers was empty; using the default layer stack."
                ))
                .preset("flat").customizeExposed()
                .doc("Ordered bottom-to-top layer list.")
                .build(),
            Setting.<FlatConfig>text("biome", c -> c.biome, (c, v) -> c.biome = v)
                .rule(new Rule.BlankFallback<>("minecraft:plains", "flat.biome was blank; defaulting to minecraft:plains."))
                .unit(Unit.BIOME_ID).preset("flat").customizeExposed()
                .doc("Single fixed biome for the whole world.")
                .build(),
            Setting.<FlatConfig>flag("decoration", c -> c.decoration, (c, v) -> c.decoration = v)
                .preset("flat").customizeExposed()
                .doc("Whether ordinary biome decoration (trees, flowers, ore veins, etc.) runs.")
                .build(),
            Setting.<FlatConfig>stringList("structureOverrides", c -> c.structureOverrides, (c, v) -> c.structureOverrides = v)
                .nullFallback(() -> new FlatConfig().structureOverrides)
                .preset("flat").customizeExposed()
                .doc("Structure sets eligible to place; empty means every registered set is eligible.")
                .build(),
            Setting.group("underground", underground)
                .render(underground::summary)
                .doc("Synthetic underground-biome band a configured number of blocks below the surface; config-only for now.")
                .build()
        );
    }
}
