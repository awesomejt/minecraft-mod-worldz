package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StackedConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link StackedConfig} (GOAL 35, DESIGN §34.1) -- defaults for the {@code
 * jlt_worldz:stacked} typed preset, consulted only when it resolves without explicit
 * Customize-screen values. Summary is fully derivable: no gate, no relabeling.
 */
public final class StackedSchema extends SchemaSection<StackedConfig> {
    public StackedSchema(String path) {
        super(path, StackedConfig::new);
    }

    @Override
    protected List<Setting<StackedConfig, ?>> declare() {
        return List.of(
            Setting.<StackedConfig>stringList("layers", c -> c.layers, (c, v) -> c.layers = v)
                .rule(new Rule.EmptyListFallback<>(
                    () -> new StackedConfig().layers, "stacked.layers was empty; using the default layer stack."
                ))
                .preset("stacked").customizeExposed()
                .doc("Ordered bottom-to-top layer list: eight bands, deep taiga through surface plains.")
                .build(),
            Setting.<StackedConfig>flag("seedRandomizedOrder", c -> c.seedRandomizedOrder, (c, v) -> c.seedRandomizedOrder = v)
                .preset("stacked").customizeExposed()
                .doc("Whether the configured layer order is shuffled, seeded off the real world seed.")
                .build(),
            Setting.<StackedConfig>integer("worldSizeChunks", c -> c.worldSizeChunks, (c, v) -> c.worldSizeChunks = v)
                .range(0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS / 16)
                .unit(Unit.CHUNKS).preset("stacked").customizeExposed()
                .doc("Overworld exterior half-width in chunks; zero opts out to the shared exterior section.")
                .build(),
            Setting.<StackedConfig>integer("relief", c -> c.reliefBlocks, (c, v) -> c.reliefBlocks = v)
                .range(0, WorldzConfig.MAX_STACKED_RELIEF_BLOCKS)
                .unit(Unit.BLOCKS).preset("stacked").customizeExposed()
                .doc("Maximum per-column height bump applied to each layer's own surface.")
                .build(),
            Setting.<StackedConfig>flag("forceTopVillage", c -> c.forceTopVillage, (c, v) -> c.forceTopVillage = v)
                .preset("stacked").customizeExposed()
                .doc("Whether a real vanilla village is always force-generated near spawn on the top layer.")
                .build()
        );
    }
}
