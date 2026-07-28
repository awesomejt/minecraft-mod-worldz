package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StructureDistanceConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link StructureDistanceConfig} (GOAL 24, DESIGN §36) -- a shared runtime rule,
 * root-level, composable with any world type.
 */
public final class StructureDistanceSchema extends SchemaSection<StructureDistanceConfig> {
    public StructureDistanceSchema(String path) {
        super(path, StructureDistanceConfig::new);
    }

    @Override
    protected List<Setting<StructureDistanceConfig, ?>> declare() {
        return List.of(
            Setting.<StructureDistanceConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .live()
                .doc("Whether vanilla structure sets are held back from spawn at all.")
                .build(),
            Setting.<StructureDistanceConfig>integer("minDistance", c -> c.minDistanceBlocks, (c, v) -> c.minDistanceBlocks = v)
                .range(0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS).live()
                .doc("Minimum block distance (Chebyshev) from spawn.")
                .build(),
            Setting.<StructureDistanceConfig>stringList(
                    "exemptStructureSets", c -> c.exemptStructureSets, (c, v) -> c.exemptStructureSets = v
                )
                .nullFallback(() -> new StructureDistanceConfig().exemptStructureSets)
                .live()
                .doc("Structure set ids always allowed at their normal vanilla distance.")
                .build()
        );
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments -- not mechanically derivable (no section-level "disabled" gate in the framework).
     */
    @Override
    public String summary(StructureDistanceConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "minDistance=" + value.minDistanceBlocks + ", exemptStructureSets=" + value.exemptStructureSets;
    }
}
