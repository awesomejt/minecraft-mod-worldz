package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ForeverNightConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link ForeverNightConfig} (GOAL 30, DESIGN §35.1) -- a shared world-hazard runtime
 * rule, root-level, composable with any world type.
 */
public final class ForeverNightSchema extends SchemaSection<ForeverNightConfig> {
    public ForeverNightSchema(String path) {
        super(path, ForeverNightConfig::new);
    }

    @Override
    protected List<Setting<ForeverNightConfig, ?>> declare() {
        return List.of(
            Setting.<ForeverNightConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .live()
                .doc("Whether night eventually locks permanently.")
                .build(),
            Setting.<ForeverNightConfig>integer("lockAfterDays", c -> c.lockAfterDays, (c, v) -> c.lockAfterDays = v)
                .range(0, WorldzConfig.MAX_BORDER_RESIZE_DAYS)
                .unit(Unit.DAYS).live()
                .doc("In-game days before night locks; zero locks immediately at world creation.")
                .build(),
            Setting.<ForeverNightConfig>flag("relaxInsomnia", c -> c.relaxInsomnia, (c, v) -> c.relaxInsomnia = v)
                .live()
                .doc("Whether phantom/insomnia pressure is suppressed once night is locked.")
                .build()
        );
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments -- not mechanically derivable (no section-level "disabled" gate in the framework).
     */
    @Override
    public String summary(ForeverNightConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "lockAfterDays=" + value.lockAfterDays + ", relaxInsomnia=" + value.relaxInsomnia;
    }
}
