package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FlatConfig;

import java.util.List;

/**
 * Schema for {@link FlatConfig} (GOAL 15, DESIGN §33.2) -- defaults for the {@code
 * jlt_worldz:flat} typed preset, consulted only when it resolves without explicit
 * Customize-screen values. Summary is fully derivable: no gate, no relabeling.
 *
 * <p>{@code undergroundBiome}/{@code undergroundBelowSurfaceBlocks} (GOAL 42, DESIGN §37.3) exist
 * on {@link FlatConfig} but are not wired into today's {@code readFlatConfig}/{@code
 * sanitizeFlat}/{@code flatMap}/{@code flatSummary} either -- out of scope here (nothing moves),
 * carried over unchanged as a pre-existing gap.
 */
public final class FlatSchema extends SchemaSection<FlatConfig> {
    public FlatSchema(String path) {
        super(path, FlatConfig::new);
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
                .build()
        );
    }
}
