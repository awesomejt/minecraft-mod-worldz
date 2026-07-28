package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;

import java.util.List;

/**
 * Schema for {@link StarterKitConfig} (GOALS 03; DESIGN §41.6). Nested inside every {@code
 * easyKit}/{@code mediumKit}/{@code hardKit}/{@code starterKit}/{@code lootKit} field across the
 * config (cave, sky island, ocean island, Nether/End start, floating islands) -- one of the
 * "shared leaf" sections everything else nests, so proving it here de-risks every later
 * conversion that contains one.
 */
public final class StarterKitSchema extends SchemaSection<StarterKitConfig> {
    public StarterKitSchema(String path) {
        super(path, StarterKitConfig::new);
    }

    @Override
    protected List<Setting<StarterKitConfig, ?>> declare() {
        return List.of(
            Setting.<StarterKitConfig>stringList("essentials", c -> c.essentials, (c, v) -> c.essentials = v)
                .nullFallback(() -> new StarterKitConfig().essentials)
                .unit(Unit.ITEM_LIST)
                .doc("Always-included items.")
                .build(),
            Setting.<StarterKitConfig>stringList("extras", c -> c.extras, (c, v) -> c.extras = v)
                .nullFallback(() -> new StarterKitConfig().extras)
                .unit(Unit.ITEM_LIST)
                .doc("Candidate items the random picks draw from.")
                .build(),
            Setting.<StarterKitConfig>integer("extrasCount", c -> c.extrasCount, (c, v) -> c.extrasCount = v)
                .min(0)
                .unit(Unit.COUNT)
                .doc("How many extras to pick, with replacement.")
                .build()
        );
    }

    /**
     * Clamps {@code extrasCount} to {@code 0} when the {@code extras} pool is empty -- a second,
     * cross-field check beyond the plain non-negative clamp declared above, so it belongs here
     * rather than in a {@link Rule}.
     */
    @Override
    protected void postValidate(StarterKitConfig value, SanitizeContext ctx) {
        if (value.extrasCount > 0 && value.extras.isEmpty()) {
            ctx.logger().warn(
                "Clamped {}.extrasCount from {} to 0 because the extras pool is empty.", path(), value.extrasCount
            );
            value.extrasCount = 0;
        }
    }
}
