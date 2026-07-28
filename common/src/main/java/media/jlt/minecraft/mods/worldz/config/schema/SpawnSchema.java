package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;

import java.util.List;

/**
 * Schema for {@link SpawnConfig} (DESIGN §41.10 R12). Nested inside four sections today (the
 * root, {@code singleBiome}, {@code chaosBiomes}, {@code stripWorld}), each of which currently
 * inlines its own copy of {@code spawn == null ? new SpawnConfig() : spawn; if (spawn.strategy ==
 * null) spawn.strategy = STARTER_AT_ORIGIN;}. Routing all four through this one schema collapses
 * that duplication into a single source of truth -- same defaults, same output, just one place
 * that says so.
 */
public final class SpawnSchema extends SchemaSection<SpawnConfig> {
    public SpawnSchema(String path) {
        super(path, SpawnConfig::new);
    }

    @Override
    protected List<Setting<SpawnConfig, ?>> declare() {
        return List.of(
            Setting.<SpawnConfig, SpawnStrategy>enumeration(
                    "strategy", c -> c.strategy, (c, v) -> c.strategy = v,
                    SpawnStrategy::parse, SpawnStrategy::serializedName, SpawnStrategy.STARTER_AT_ORIGIN
                )
                .unit(Unit.NONE)
                .doc("How the layout origin and initial spawn are chosen.")
                .build()
        );
    }

    /**
     * Overridden: every existing call site renders spawn as the bare strategy name (e.g.
     * {@code "starter_at_origin"}), never as a labeled {@code "strategy=..."} segment, so the
     * generically-derived summary (which would add the label) would not be byte-identical.
     */
    @Override
    public String summary(SpawnConfig value) {
        return value.strategy.serializedName();
    }
}
