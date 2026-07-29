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
     * The real {@code kits} library, resolved through the root config each sanitize call (DESIGN
     * §44.3.4, §44.4.1; wired at TODO 25.8b once {@code WorldzConfig.kits} existed to compile
     * against). Consulted by all 14 sites since TODO 25.8c (the 12 tiered sites) and 25.8d
     * ({@code oceanIsland.starterKit}/{@code skyIsland.floatingIslands.lootChest.kit}).
     */
    private static final Function<SanitizeContext, Map<String, StarterKitConfig>> LIBRARY = ctx -> ctx.root().kits;

    /**
     * Binds a polymorphic "named reference or inline definition" leaf (DESIGN §44.3.5): the raw
     * YAML value is either a bare {@code kits} library name or an inline {@link StarterKitConfig}
     * mapping, parsed/emitted via {@link Codecs#namedOrSection} and resolved/materialized at
     * sanitize time via {@link Rule.KitReference}. Kit-specific, not a general {@link Setting}
     * factory -- {@code Setting}'s own factories stay value-shape generic; this is the one call
     * site {@code ChestSchema.KitsSchema} (TODO 25.8c), {@code OceanIslandSchema} and {@code
     * FloatingIslandsSchema} (TODO 25.8d) bind.
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
            new Rule.KitReference<>(inline, LIBRARY, defaultName)
        );
    }

    /**
     * The render function every {@code reference(...)} call site's {@code .render(...)} passes
     * (DESIGN §44.7): a referenced kit's summary segment is the bare name, not its materialized
     * contents -- the WARN for an unknown name already lists the defined names, so the summary is
     * the other place a user checks what exists. An inline definition (still legal at every site)
     * keeps rendering its full contents exactly as before, via this same schema instance's own
     * inherited {@link #summary}.
     */
    public String summaryOrReference(StarterKitConfig value) {
        return value.ref != null ? value.ref : summary(value);
    }
}
