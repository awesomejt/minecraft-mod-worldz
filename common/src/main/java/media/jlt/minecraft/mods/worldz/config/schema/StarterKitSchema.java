package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    /**
     * TODO 25.8b: replace with {@code ctx -> ctx.root().kits} once {@code WorldzConfig} gains the
     * {@code kits} field (DESIGN §44.4.1, §44.3.4) -- there is no library to resolve against yet.
     * Safe in the meantime because no 25.8a call site invokes {@link #reference} (no site converts
     * until 25.8c): an always-empty library sends every reference straight through {@link
     * Rule.KitReference}'s own unknown-name fallback to {@code inline}'s definition.
     */
    private static final Function<SanitizeContext, Map<String, StarterKitConfig>> NO_LIBRARY_YET = ctx -> Map.of();

    /**
     * Binds a polymorphic "named reference or inline definition" leaf (DESIGN §44.3.5): the raw
     * YAML value is either a bare {@code kits} library name or an inline {@link StarterKitConfig}
     * mapping, parsed/emitted via {@link Codecs#namedOrSection} and resolved/materialized at
     * sanitize time via {@link Rule.KitReference}. Kit-specific, not a general {@link Setting}
     * factory -- {@code Setting}'s own factories stay value-shape generic; this is the one call
     * site {@code ChestSchema.KitsSchema}, {@code OceanIslandSchema} and {@code
     * FloatingIslandsSchema} bind (TODO 25.8c/d).
     *
     * @param inline the site's own inline schema, used for both the inline path and as the
     *     fallback when neither the referenced name nor {@code defaultName} resolves
     * @param defaultName this site's own shipped default kit name, substituted (with a warning)
     *     for an unknown or blank reference
     */
    public static <S> Setting.PlainBuilder<S, StarterKitConfig> reference(
        String key, Function<S, StarterKitConfig> get, BiConsumer<S, StarterKitConfig> set,
        StarterKitSchema inline, String defaultName
    ) {
        return new Setting.PlainBuilder<>(
            key, new Accessor<>(get, set),
            Codecs.namedOrSection(inline, StarterKitConfig::reference, config -> config.ref),
            new Rule.KitReference<>(inline, NO_LIBRARY_YET, defaultName)
        );
    }
}
