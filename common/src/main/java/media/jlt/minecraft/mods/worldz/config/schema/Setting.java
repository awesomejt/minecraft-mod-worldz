package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * One declarative setting descriptor, from which parse, sanitize, YAML emit and the summary
 * segment are all derived (DESIGN §41.2). Built through a fluent builder so authoring ~165
 * settings across the config package stays terse; no reflection anywhere -- {@link #accessor}
 * is always an explicit getter/setter lambda pair over a public field.
 *
 * @param key leaf YAML key, e.g. {@code "sealedSurfaceY"}
 * @param accessor getter/setter pair over the owning POJO's field
 * @param codec YAML {@code Object} &lt;-&gt; value, both directions
 * @param rule the sanitize step
 * @param docs one-line doc, unit, rendered default and rendered range
 * @param applies where this setting takes effect (live/world-default/preset) and Customize exposure
 * @param summary how this setting renders into the section's {@code summary()} line
 */
public record Setting<S, T>(
    String key,
    Accessor<S, T> accessor,
    ValueCodec<T> codec,
    Rule<S, T> rule,
    Docs docs,
    Applicability applies,
    SummarySpec<S, T> summary
) {
    /** Starts building an integer setting. */
    public static <S> IntBuilder<S> integer(String key, Function<S, Integer> get, BiConsumer<S, Integer> set) {
        return new IntBuilder<>(key, new Accessor<>(get, set));
    }

    /** Starts building a floating-point setting. */
    public static <S> DoubleBuilder<S> decimal(String key, Function<S, Double> get, BiConsumer<S, Double> set) {
        return new DoubleBuilder<>(key, new Accessor<>(get, set));
    }

    /** Starts building a boolean flag setting. */
    public static <S> PlainBuilder<S, Boolean> flag(String key, Function<S, Boolean> get, BiConsumer<S, Boolean> set) {
        return new PlainBuilder<>(key, new Accessor<>(get, set), Codecs.BOOLEAN);
    }

    /** Starts building a plain string setting. */
    public static <S> PlainBuilder<S, String> text(String key, Function<S, String> get, BiConsumer<S, String> set) {
        return new PlainBuilder<>(key, new Accessor<>(get, set), Codecs.STRING);
    }

    /** Starts building a string-list setting. */
    public static <S> PlainBuilder<S, List<String>> stringList(
        String key, Function<S, List<String>> get, BiConsumer<S, List<String>> set
    ) {
        return new PlainBuilder<>(key, new Accessor<>(get, set), Codecs.STRING_LIST);
    }

    /** Starts building a string-to-string map setting. */
    public static <S> PlainBuilder<S, Map<String, String>> stringMap(
        String key, Function<S, Map<String, String>> get, BiConsumer<S, Map<String, String>> set
    ) {
        return new PlainBuilder<>(key, new Accessor<>(get, set), Codecs.STRING_MAP);
    }

    /**
     * Starts building an enum setting, following the project's {@code parse}/{@code
     * serializedName} convention. A {@code NullFallback} rule to {@code fallback} is pre-installed,
     * matching every {@code sanitizeX}'s {@code field == null ? DEFAULT : field} pattern. The
     * summary segment also renders via {@code serialize} by default (e.g. {@code "glowstone"}),
     * not {@code Enum::toString} (which would render the upper-case constant name) -- every
     * existing {@code xSummary} renders enums through their own {@code serializedName()}.
     */
    public static <S, T> PlainBuilder<S, T> enumeration(
        String key, Function<S, T> get, BiConsumer<S, T> set,
        Function<String, T> parse, Function<T, String> serialize, T fallback
    ) {
        PlainBuilder<S, T> builder = new PlainBuilder<>(
            key, new Accessor<>(get, set), Codecs.enumeration(parse, serialize), new Rule.NullFallback<>(() -> fallback)
        );
        builder.summaryRender = serialize;
        return builder;
    }

    /**
     * Starts building a nested-section setting. Sanitizing recurses into {@code section}'s own
     * {@link SectionCodec#sanitize}, pre-installed as this setting's rule -- no further rule
     * configuration is needed for the common case (see the worked {@code CaveSchema} example,
     * DESIGN §41.6). Accepts a {@link SectionCodec} rather than a {@link SchemaSection} directly:
     * every already-converted section is a {@code SchemaSection} (which implements {@code
     * SectionCodec}), but the root's own settings (TODO 25.2g) bind {@code SchemaSections}
     * registry entries, whose declared return type is {@code SectionCodec}.
     */
    public static <S, C> PlainBuilder<S, C> section(
        String key, Function<S, C> get, BiConsumer<S, C> set, SectionCodec<C> section
    ) {
        return new PlainBuilder<>(key, new Accessor<>(get, set), Codecs.section(section), new Rule.Nested<>(section));
    }

    /**
     * Shared fluent state for every setting shape: docs, applicability and summary rendering. Type
     * -specific subclasses ({@link IntBuilder}, {@link DoubleBuilder}, {@link PlainBuilder}) add
     * the methods that build {@link #buildRule()}; call those first, generic methods after, ending
     * in {@link #build()} -- exactly the order the worked {@code CaveSchema} example uses.
     */
    public abstract static class Builder<S, T> {
        final String key;
        final Accessor<S, T> accessor;
        final ValueCodec<T> codec;
        String doc = "";
        Unit unit = Unit.NONE;
        String rangeText = "";
        String defaultText = "";
        Applicability applicability = Applicability.worldDefault();
        String summaryLabel;
        Function<T, String> summaryRender = String::valueOf;
        Predicate<S> summaryInclude = owner -> true;
        boolean summaryShown = true;

        Builder(String key, Accessor<S, T> accessor, ValueCodec<T> codec) {
            this.key = key;
            this.accessor = accessor;
            this.codec = codec;
            this.summaryLabel = key;
        }

        /** One-line description of what the setting does, for the 25.4 reference generator. */
        public Builder<S, T> doc(String doc) {
            this.doc = doc;
            return this;
        }

        /** The physical/conceptual unit the value is measured in. */
        public Builder<S, T> unit(Unit unit) {
            this.unit = unit;
            return this;
        }

        /** Overrides the rendered range text (for dependent bounds the builder can't render itself). */
        public Builder<S, T> rangeText(String rangeText) {
            this.rangeText = rangeText;
            return this;
        }

        /** Overrides the rendered default text (for defaults the builder can't render itself). */
        public Builder<S, T> defaultText(String defaultText) {
            this.defaultText = defaultText;
            return this;
        }

        /** Marks this setting as a live global runtime rule (re-read every tick/day). */
        public Builder<S, T> live() {
            this.applicability = Applicability.live();
            return this;
        }

        /** Marks this setting as scoped to the given typed preset id(s); empty means the generic
         * {@code worldz} preset only. */
        public Builder<S, T> preset(String... presetIds) {
            this.applicability = Applicability.preset(presetIds);
            return this;
        }

        /** Marks this setting as exposed on the in-game Customize screen. */
        public Builder<S, T> customizeExposed() {
            this.applicability = this.applicability.exposedInCustomize();
            return this;
        }

        /** Overrides the summary segment's label; defaults to the setting's own key. */
        public Builder<S, T> label(String label) {
            this.summaryLabel = label;
            return this;
        }

        /** Overrides how the value renders into the summary segment; defaults to {@code String::valueOf}. */
        public Builder<S, T> render(Function<T, String> render) {
            this.summaryRender = render;
            return this;
        }

        /** Restricts the summary segment to owners for which the predicate is true (e.g.
         * {@code chunkIsland.topOnlyDepthBlocks}, shown only when {@code topOnly} is set). */
        public Builder<S, T> includeInSummaryWhen(Predicate<S> include) {
            this.summaryInclude = include;
            return this;
        }

        /** Excludes this setting from the derived summary entirely. */
        public Builder<S, T> hiddenFromSummary() {
            this.summaryShown = false;
            return this;
        }

        abstract Rule<S, T> buildRule();

        /** Builds the immutable {@link Setting}. */
        public Setting<S, T> build() {
            Docs docs = new Docs(doc, unit, defaultText, rangeText);
            SummarySpec<S, T> summarySpec = new SummarySpec<>(summaryLabel, summaryRender, summaryInclude, summaryShown);
            return new Setting<>(key, accessor, codec, buildRule(), docs, applicability, summarySpec);
        }
    }

    /** Builder for {@link Codecs#INT}-backed settings: fixed or dependent bounds, an optional
     * conditional gate, and optional odd-rounding applied before the range clamp (DESIGN R3, R5). */
    public static final class IntBuilder<S> extends Builder<S, Integer> {
        private ToIntFunction<S> min;
        private ToIntFunction<S> max;
        private Predicate<S> when = owner -> true;
        private boolean rangeSet;
        private Rule<S, Integer> preRule;

        IntBuilder(String key, Accessor<S, Integer> accessor) {
            super(key, accessor, Codecs.INT);
        }

        /** Clamps to a fixed lower bound only (upper bound stays {@code Integer.MAX_VALUE}). */
        public IntBuilder<S> min(int min) {
            return min(owner -> min);
        }

        /** Clamps to a bound that depends on a sibling field. */
        public IntBuilder<S> min(ToIntFunction<S> min) {
            this.min = min;
            this.max = this.max == null ? owner -> Integer.MAX_VALUE : this.max;
            this.rangeSet = true;
            return this;
        }

        /** Clamps to a fixed upper bound only (lower bound stays {@code Integer.MIN_VALUE}). */
        public IntBuilder<S> max(int max) {
            return max(owner -> max);
        }

        /** Clamps to a bound that depends on a sibling field. */
        public IntBuilder<S> max(ToIntFunction<S> max) {
            this.max = max;
            this.min = this.min == null ? owner -> Integer.MIN_VALUE : this.min;
            this.rangeSet = true;
            return this;
        }

        /** Clamps to a fixed {@code [min, max]}. */
        public IntBuilder<S> range(int min, int max) {
            return range(owner -> min, owner -> max);
        }

        /** Clamps to bounds that depend on a sibling field, e.g. {@code maxRadiusBlocks} floored
         * at the sibling {@code minRadiusBlocks}. */
        public IntBuilder<S> range(ToIntFunction<S> min, ToIntFunction<S> max) {
            this.min = min;
            this.max = max;
            this.rangeSet = true;
            return this;
        }

        /** Restricts the clamp to owners for which the predicate is true -- the one conditional
         * clamp in the codebase, {@code cave.sealedSurfaceY} (DESIGN R5). */
        public IntBuilder<S> when(Predicate<S> when) {
            this.when = when;
            return this;
        }

        /** Rounds up to the next odd value before the range clamp is applied, regardless of call
         * order (DESIGN R3). */
        public IntBuilder<S> oddRounding() {
            this.preRule = new Rule.OddRounding<>();
            return this;
        }

        @Override
        Rule<S, Integer> buildRule() {
            Rule<S, Integer> rangeRule = rangeSet ? new Rule.IntRange<>(min, max, when) : new Rule.None<>();
            return preRule == null ? rangeRule : Rule.of(preRule, rangeRule);
        }
    }

    /** Builder for {@link Codecs#DOUBLE}-backed settings: a fixed {@code [min, max]}. */
    public static final class DoubleBuilder<S> extends Builder<S, Double> {
        private double min = Double.NEGATIVE_INFINITY;
        private double max = Double.POSITIVE_INFINITY;
        private boolean rangeSet;

        DoubleBuilder(String key, Accessor<S, Double> accessor) {
            super(key, accessor, Codecs.DOUBLE);
        }

        /** Clamps to a fixed {@code [min, max]}. */
        public DoubleBuilder<S> range(double min, double max) {
            this.min = min;
            this.max = max;
            this.rangeSet = true;
            return this;
        }

        @Override
        Rule<S, Double> buildRule() {
            return rangeSet ? new Rule.DoubleRange<>(min, max) : new Rule.None<>();
        }
    }

    /** Builder for every other shape (flags, text, lists, maps, enums, nested sections): an
     * explicit {@link Rule}, defaulting to {@link Rule.None} unless a factory method installs one
     * (enum's {@code NullFallback}, section's {@code Nested}). */
    public static final class PlainBuilder<S, T> extends Builder<S, T> {
        private Rule<S, T> rule;

        PlainBuilder(String key, Accessor<S, T> accessor, ValueCodec<T> codec) {
            this(key, accessor, codec, new Rule.None<>());
        }

        PlainBuilder(String key, Accessor<S, T> accessor, ValueCodec<T> codec, Rule<S, T> rule) {
            super(key, accessor, codec);
            this.rule = rule;
        }

        /** Installs an arbitrary sanitize rule, replacing any default. */
        public PlainBuilder<S, T> rule(Rule<S, T> rule) {
            this.rule = rule;
            return this;
        }

        /** Convenience for the common "replace null with a fresh instance" case (DESIGN R11: the
         * fallback is a {@link Supplier}, never a shared constant, so list defaults never alias). */
        public PlainBuilder<S, T> nullFallback(Supplier<T> fallback) {
            this.rule = new Rule.NullFallback<>(fallback);
            return this;
        }

        @Override
        Rule<S, T> buildRule() {
            return rule;
        }
    }
}
