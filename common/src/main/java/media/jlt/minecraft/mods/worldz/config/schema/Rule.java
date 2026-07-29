package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.WeightedBiomeListSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * A sanitize-time validation/clamp step for one {@link Setting} (DESIGN §41.4). A sealed
 * interface of data, not a {@code Consumer}, so 25.4's reference generator can render
 * "Range: 64..4096" by introspecting the rule instead of needing prose written by hand.
 *
 * <p>{@code name} is the setting's full dotted path (section {@link SchemaSection#path()} plus
 * leaf key), matching today's hand-built warning prefixes exactly (e.g. {@code "cave.easyKit"}).
 */
public sealed interface Rule<S, T> {
    /**
     * Applies this rule to a value already read onto {@code owner}.
     *
     * @param owner the owning POJO, for rules that consult a sibling field ({@link IntRange#when()})
     * @param value the current value
     * @param name the full dotted path, for warning messages
     * @param ctx sanitize context (logger, root)
     * @return the (possibly clamped/replaced) value
     */
    T apply(S owner, T value, String name, SanitizeContext ctx);

    /**
     * Composes two rules, applying {@code first} then {@code second} in that order. Order
     * matters -- e.g. {@code StarterCapsuleConfig.sizeBlocks} must be odd-rounded <em>before</em>
     * being clamped (DESIGN R3), not the other way around.
     */
    static <S, T> Rule<S, T> of(Rule<S, T> first, Rule<S, T> second) {
        return new Compose<>(first, second);
    }

    /** Clamps an integer to {@code [min(owner), max(owner)]}, warning only when it actually moved.
     * {@code when} restricts the clamp to owners for which it returns {@code true} -- the one
     * conditional clamp in the codebase, {@code cave.sealedSurfaceY} (DESIGN R5). */
    record IntRange<S>(ToIntFunction<S> min, ToIntFunction<S> max, Predicate<S> when) implements Rule<S, Integer> {
        public IntRange(ToIntFunction<S> min, ToIntFunction<S> max) {
            this(min, max, owner -> true);
        }

        @Override
        public Integer apply(S owner, Integer value, String name, SanitizeContext ctx) {
            if (!when.test(owner)) {
                return value;
            }
            int clamped = Math.clamp(value, min.applyAsInt(owner), max.applyAsInt(owner));
            if (clamped != value) {
                ctx.logger().warn("Clamped {} from {} to {}.", name, value, clamped);
            }
            return clamped;
        }
    }

    /** Clamps a double to a fixed {@code [min, max]}, warning only when it actually moved. */
    record DoubleRange<S>(double min, double max) implements Rule<S, Double> {
        @Override
        public Double apply(S owner, Double value, String name, SanitizeContext ctx) {
            double clamped = Math.clamp(value, min, max);
            if (clamped != value) {
                ctx.logger().warn("Clamped {} from {} to {}.", name, value, clamped);
            }
            return clamped;
        }
    }

    /** Rounds an odd-only integer field up by one when it is even, warning when it moved
     * (DESIGN R3; {@code StarterCapsuleConfig.sizeBlocks} must stay odd). Compose with
     * {@link IntRange} via {@link #of} to clamp <em>after</em> rounding. */
    record OddRounding<S>() implements Rule<S, Integer> {
        @Override
        public Integer apply(S owner, Integer value, String name, SanitizeContext ctx) {
            if (value % 2 == 0) {
                int oddened = value + 1;
                ctx.logger().warn("Rounded {} from {} to {} (must be odd).", name, value, oddened);
                return oddened;
            }
            return value;
        }
    }

    /** Replaces a {@code null} value with a fresh instance from {@code fallback}. The fallback is
     * a {@link Supplier}, never a shared constant, so empty-list defaults never alias between
     * sanitize calls (DESIGN R11). */
    record NullFallback<S, T>(Supplier<T> fallback) implements Rule<S, T> {
        @Override
        public T apply(S owner, T value, String name, SanitizeContext ctx) {
            return value == null ? fallback.get() : value;
        }
    }

    /** Sanitizes a single biome-or-tag id string. {@code allowEmpty} controls whether a blank/
     * invalid value is a legal "unset" state ({@code ""}) or must fall back to a concrete
     * default; {@code warning} is the exact message template, since existing wordings for this
     * operation differ by call site (DESIGN R6). */
    record BiomeId<S>(boolean allowEmpty, Supplier<String> fallback, String warning) implements Rule<S, String> {
        @Override
        public String apply(S owner, String value, String name, SanitizeContext ctx) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.isEmpty()) {
                return allowEmpty ? "" : fallback.get();
            }
            BiomeListSpec spec = BiomeListSpec.parse(List.of(trimmed));
            if (spec.entries().size() != 1 || spec.entries().getFirst().tag()) {
                ctx.logger().warn(warning, trimmed);
                return allowEmpty ? "" : fallback.get();
            }
            return spec.entries().getFirst().id();
        }
    }

    /**
     * Sanitizes a list of biome-or-tag ids, dropping invalid entries individually. Every warning
     * template is supplied by the caller, never built from {@code name} (DESIGN R6's "carries its
     * own warning text" applied to lists): {@code sanitizeStripBands} (the one current caller,
     * TODO 25.2d) hardcodes its section name only, without the leaf key, so the generic {@code
     * name} passed to {@link #apply} -- always the full {@code section.key} dotted path -- would
     * be the wrong text to interpolate.
     *
     * @param invalidWarning one-arg template for a syntactically invalid entry (the raw value)
     * @param tagWarning one-arg template for a rejected tag entry (the tag id), or {@code null} to
     *     drop tags silently ({@code REJECT_TAGS} only; ignored for {@code ALLOW_TAGS})
     * @param emptyFallback replacement for a fully-empty resolved list, or {@code null} to leave it
     *     empty
     * @param emptyWarning literal (no format args) warning logged when {@code emptyFallback} fires
     */
    record BiomeIdList<S>(
        Mode mode, String invalidWarning, String tagWarning, Supplier<List<String>> emptyFallback, String emptyWarning
    ) implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            BiomeListSpec spec = BiomeListSpec.parse(value);
            for (String invalid : spec.invalidEntries()) {
                ctx.logger().warn(invalidWarning, invalid);
            }
            List<String> resolved;
            if (mode == Mode.REJECT_TAGS) {
                for (BiomeListSpec.Entry entry : spec.entries()) {
                    if (entry.tag() && tagWarning != null) {
                        ctx.logger().warn(tagWarning, entry.id());
                    }
                }
                resolved = new ArrayList<>(spec.entries().stream().filter(entry -> !entry.tag()).map(BiomeListSpec.Entry::id).toList());
            } else {
                resolved = new ArrayList<>(spec.entries().stream().map(BiomeListSpec.Entry::configValue).toList());
            }
            if (resolved.isEmpty() && emptyFallback != null) {
                ctx.logger().warn(emptyWarning);
                return emptyFallback.get();
            }
            return resolved;
        }

        /** Whether biome tags ({@code #namespace:tag}) are kept or rejected. */
        public enum Mode {
            /** Tags are kept alongside concrete ids. */
            ALLOW_TAGS,
            /** Tags are dropped (with an optional warning); only concrete ids survive. */
            REJECT_TAGS
        }
    }

    /**
     * Sanitizes a weighted biome list ({@code id} or {@code id@weight}), dropping invalid entries
     * individually. Distinct from {@link BiomeIdList}: parsing goes through {@link
     * WeightedBiomeListSpec}, which never accepts tags at all (layout roles are resolved per
     * concrete id, with no registry to expand a tag against) -- so there is no tag-rejection
     * warning to configure. Used by {@code layout.biomes} and {@code chaosBiomes.biomes} (TODO
     * 25.2d).
     *
     * @param invalidWarning one-arg template for a syntactically invalid entry (the raw value)
     * @param emptyFallback replacement for a fully-empty resolved list, or {@code null} to leave it
     *     empty (e.g. {@code layout.biomes}, which has no empty-list fallback of its own -- the
     *     mode-vs-roles cross-check handles an empty list instead)
     * @param emptyWarning literal (no format args) warning logged when {@code emptyFallback} fires
     */
    record WeightedBiomeIdList<S>(
        String invalidWarning, Supplier<List<String>> emptyFallback, String emptyWarning
    ) implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            WeightedBiomeListSpec spec = WeightedBiomeListSpec.parse(value);
            for (String invalid : spec.invalidEntries()) {
                ctx.logger().warn(invalidWarning, invalid);
            }
            List<String> resolved = new ArrayList<>(spec.entries().stream().map(WeightedBiomeListSpec.Entry::configValue).toList());
            if (resolved.isEmpty() && emptyFallback != null) {
                ctx.logger().warn(emptyWarning);
                return emptyFallback.get();
            }
            return resolved;
        }
    }

    /**
     * Validates a single biome id like {@link BiomeId}, but -- if the resolved value is empty --
     * warns a <em>second</em> time citing the pre-validation raw value and falls back to a fixed
     * default, rather than silently returning {@code ""}. Two real call sites need this exact
     * "silent-blank-then-loud-original-value" shape: {@code oceanIsland.islandBiome} (WorldzConfig
     * {@code sanitizeOceanIsland} :437-442) and {@code skyIsland.islandBiome} (:487-492) -- distinct
     * from {@code singleBiome.landBiome}'s single warning (DESIGN R7, modeled by composing
     * {@link BiomeId} with {@link BlankFallback}), because the second warning here names the
     * <em>original</em> input, not the post-{@link BiomeId} blank string, so {@link #of} composition
     * (which only ever sees the previous stage's output) cannot express it; both stages must share
     * one rule instead.
     *
     * @param invalidWarning one-arg template for a syntactically invalid, non-blank entry (the
     *     trimmed raw value), or {@code null} to drop invalid entries silently (never used by
     *     today's two call sites, both of which warn)
     * @param defaultWarning one-arg template for falling back to {@code fallback} (the original,
     *     untrimmed raw value), logged whenever the resolved id ends up empty either way
     * @param fallback the concrete replacement id, a fresh value per call (DESIGN R11)
     */
    record BiomeIdOrDefault<S>(String invalidWarning, String defaultWarning, Supplier<String> fallback) implements Rule<S, String> {
        @Override
        public String apply(S owner, String value, String name, SanitizeContext ctx) {
            String original = value == null ? "" : value;
            String trimmed = original.trim();
            String resolved = "";
            if (!trimmed.isEmpty()) {
                BiomeListSpec spec = BiomeListSpec.parse(List.of(trimmed));
                if (spec.entries().size() == 1 && !spec.entries().getFirst().tag()) {
                    resolved = spec.entries().getFirst().id();
                } else if (invalidWarning != null) {
                    ctx.logger().warn(invalidWarning, trimmed);
                }
            }
            if (resolved.isEmpty()) {
                ctx.logger().warn(defaultWarning, original);
                return fallback.get();
            }
            return resolved;
        }
    }

    /** Trims each entry and drops the ones that become empty. */
    record TrimNonEmpty<S>() implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            return new ArrayList<>(value.stream().map(String::trim).filter(entry -> !entry.isEmpty()).toList());
        }
    }

    /** Replaces a {@code null} or empty list with a fresh instance from {@code fallback},
     * warning with a literal message (no format args -- the wording differs per call site, e.g.
     * {@code "flat.layers was empty; using the default layer stack."} vs {@code "deepFlat
     * .capLayers was empty; using the default cap layer stack."}). Distinct from
     * {@link NullFallback}, which never warns and only checks {@code null}, not emptiness. */
    record EmptyListFallback<S>(Supplier<List<String>> fallback, String warning) implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            if (value == null || value.isEmpty()) {
                ctx.logger().warn(warning);
                return fallback.get();
            }
            return value;
        }
    }

    /** Replaces a {@code null} or blank string with a fixed fallback, warning with a literal
     * message (no format args) -- {@code flat.biome}'s "defaulting to minecraft:plains" wording.
     * Distinct from {@link BiomeId}, which additionally parses/validates the string as a biome or
     * tag id; this rule only checks blankness. */
    record BlankFallback<S>(String fallback, String warning) implements Rule<S, String> {
        @Override
        public String apply(S owner, String value, String name, SanitizeContext ctx) {
            if (value == null || value.isBlank()) {
                ctx.logger().warn(warning);
                return fallback;
            }
            return value;
        }
    }

    /** Replaces one specific value with a fallback, warning with a literal message (no format
     * args) -- {@code strip.widthMode} cannot be {@code NORMAL} (the corridor's wall is void or
     * ocean only). Compose with {@link NullFallback} via {@link #of} so a {@code null} is
     * defaulted first, silently, before this check runs. */
    record RejectValue<S, T>(T rejected, T fallback, String warning) implements Rule<S, T> {
        @Override
        public T apply(S owner, T value, String name, SanitizeContext ctx) {
            if (Objects.equals(value, rejected)) {
                ctx.logger().warn(warning);
                return fallback;
            }
            return value;
        }
    }

    /** No-op: the value passes through unchanged. */
    record None<S, T>() implements Rule<S, T> {
        @Override
        public T apply(S owner, T value, String name, SanitizeContext ctx) {
            return value;
        }
    }

    /** {@code first} then {@code second}, in that order. See {@link #of}. */
    record Compose<S, T>(Rule<S, T> first, Rule<S, T> second) implements Rule<S, T> {
        @Override
        public T apply(S owner, T value, String name, SanitizeContext ctx) {
            return second.apply(owner, first.apply(owner, value, name, ctx), name, ctx);
        }
    }

    /** A {@code Setting.section(...)} entry's implicit rule: sanitizing a nested value means
     * recursing into its own {@link SectionCodec#sanitize}, not clamping the reference itself.
     * Accepts a {@link SectionCodec} rather than a {@link SchemaSection} directly so root-level
     * settings can bind a {@code SchemaSections} registry entry (typed {@code SectionCodec}) without
     * an unchecked cast (TODO 25.2g). */
    record Nested<S, C>(SectionCodec<C> section) implements Rule<S, C> {
        @Override
        public C apply(S owner, C value, String name, SanitizeContext ctx) {
            return section.sanitize(value, ctx);
        }
    }

    /** A {@code Codecs.sectionMap(...)} entry's implicit rule (DESIGN §44.4.2): sanitize each named
     * value in place through {@code entry}'s own {@link SectionCodec#sanitize}, the map-of-many
     * analogue of {@link Nested}'s single-section recursion -- the {@code kits} library's own rule,
     * once TODO 25.8b binds it as the root {@code Setting}. A {@code null} map sanitizes to an
     * empty one rather than failing, matching every other {@code Setting} whose value can be
     * legitimately absent. */
    record NestedMap<S, C>(SectionCodec<C> entry) implements Rule<S, Map<String, C>> {
        @Override
        public Map<String, C> apply(S owner, Map<String, C> value, String name, SanitizeContext ctx) {
            Map<String, C> sanitized = value == null ? new LinkedHashMap<>() : value;
            for (Map.Entry<String, C> mapEntry : sanitized.entrySet()) {
                mapEntry.setValue(entry.sanitize(mapEntry.getValue(), ctx));
            }
            return sanitized;
        }
    }

    /**
     * Resolves a {@link StarterKitConfig} that may be a bare reference into the {@code kits}
     * library or an inline definition (DESIGN §44.3.4). Materializes the referenced kit's contents
     * <em>onto the referring instance</em> rather than swapping the reference, so every downstream
     * consumer keeps reading {@code essentials}/{@code extras}/{@code extrasCount} with zero
     * change; {@code ref} survives on the instance purely so {@link Codecs#namedOrSection}'s
     * {@code write} can re-emit the name. Materialized contents are fresh copies (DESIGN R11,
     * never aliased) of an already-sanitized library entry, so they are not re-sanitized here --
     * running {@code StarterKitSchema.postValidate} a second time would double-warn. An unknown
     * name warns and falls back to {@code defaultName} (the referring site's own shipped default,
     * not a generic blank kit); if {@code defaultName} is itself undefined, warns once more and
     * falls back to sanitizing {@code value} as an inline definition instead.
     *
     * <p>{@code library} resolves the sanitized {@code kits} map given a {@link SanitizeContext} --
     * deliberately <strong>not</strong> hardcoded to {@code ctx.root().kits} here, the exact
     * expression DESIGN §44.3.4 specifies, because {@link SanitizeContext#root()} is concretely
     * typed to {@code WorldzConfig} and {@code WorldzConfig} has no {@code kits} field until TODO
     * 25.8b -- that expression cannot compile yet. TODO 25.8b's real call sites (once {@code
     * WorldzConfig.kits} exists) supply {@code ctx -> ctx.root().kits} for {@code library}; until
     * then, {@code StarterKitSchema.reference} plugs in an always-empty placeholder (no 25.8a call
     * site invokes it -- no site is converted yet), and {@code KitReferenceTest} exercises this
     * resolve/fallback/materialize logic for real against a synthetic supplier standing in for the
     * eventual root coupling.
     */
    record KitReference<S>(
        SectionCodec<StarterKitConfig> inline, Function<SanitizeContext, Map<String, StarterKitConfig>> library, String defaultName
    ) implements Rule<S, StarterKitConfig> {
        @Override
        public StarterKitConfig apply(S owner, StarterKitConfig value, String name, SanitizeContext ctx) {
            StarterKitConfig config = value == null ? StarterKitConfig.reference(defaultName) : value;
            if (config.ref == null) {
                return inline.sanitize(config, ctx);
            }
            Map<String, StarterKitConfig> kits = library.apply(ctx);
            StarterKitConfig entry = kits.get(config.ref);
            if (entry == null) {
                ctx.logger().warn(
                    "Unknown starter kit '{}' at {}; using '{}' instead. Defined kits: {}.",
                    config.ref, name, defaultName, kits.keySet()
                );
                config.ref = defaultName;
                entry = kits.get(defaultName);
                if (entry == null) {
                    ctx.logger().warn(
                        "Default starter kit '{}' at {} is also undefined; using its own inline definition instead.",
                        defaultName, name
                    );
                    return inline.sanitize(config, ctx);
                }
            }
            config.essentials = new ArrayList<>(entry.essentials);
            config.extras = new ArrayList<>(entry.extras);
            config.extrasCount = entry.extrasCount;
            return config;
        }
    }
}
