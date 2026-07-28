package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.EndBorderConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link EndBorderConfig} (GOALS 17). Root-level section: optionally carries the
 * Overworld's border size into the End, clamped up to a floor so the dragon fight stays winnable
 * even at very small Overworld sizes.
 */
public final class EndBorderSchema extends SchemaSection<EndBorderConfig> {
    public EndBorderSchema(String path) {
        super(path, EndBorderConfig::new);
    }

    @Override
    protected List<Setting<EndBorderConfig, ?>> declare() {
        return List.of(
            Setting.<EndBorderConfig>flag("carryFromOverworld", c -> c.carryFromOverworld, (c, v) -> c.carryFromOverworld = v)
                .doc("Whether the End receives a border matching the Overworld's eventual (final) radius.")
                .build(),
            Setting.<EndBorderConfig>integer("minimumRadius", c -> c.minimumRadiusBlocks, (c, v) -> c.minimumRadiusBlocks = v)
                .range(WorldzConfig.MIN_BORDER_RADIUS_BLOCKS, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Smallest End border half-width regardless of the carried Overworld radius.")
                .build()
        );
    }

    /**
     * Overridden: {@code carryFromOverworld} gates the whole line, and the surviving field renders
     * under the relabeled segment {@code "carried, minimum=..."}, not {@code
     * "carryFromOverworld=..., minimumRadius=..."} -- not mechanically derivable.
     */
    @Override
    public String summary(EndBorderConfig value) {
        return value.carryFromOverworld ? "carried, minimum=" + value.minimumRadiusBlocks : "<disabled>";
    }
}
