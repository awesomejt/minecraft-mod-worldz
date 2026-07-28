package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;

import java.util.ArrayList;
import java.util.List;
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

    /** Sanitizes a list of biome-or-tag ids, dropping invalid entries individually. */
    record BiomeIdList<S>(Mode mode, boolean warnOnTags, Supplier<List<String>> emptyFallback) implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            BiomeListSpec spec = BiomeListSpec.parse(value);
            for (String invalid : spec.invalidEntries()) {
                ctx.logger().warn("Ignoring invalid {} biome '{}'.", name, invalid);
            }
            List<String> resolved;
            if (mode == Mode.REJECT_TAGS) {
                for (BiomeListSpec.Entry entry : spec.entries()) {
                    if (entry.tag() && warnOnTags) {
                        ctx.logger().warn("Ignoring {} biome tag '#{}'; concrete biome ids are required.", name, entry.id());
                    }
                }
                resolved = new ArrayList<>(spec.entries().stream().filter(entry -> !entry.tag()).map(BiomeListSpec.Entry::id).toList());
            } else {
                resolved = new ArrayList<>(spec.entries().stream().map(BiomeListSpec.Entry::configValue).toList());
            }
            return resolved.isEmpty() && emptyFallback != null ? emptyFallback.get() : resolved;
        }

        /** Whether biome tags ({@code #namespace:tag}) are kept or rejected. */
        public enum Mode {
            /** Tags are kept alongside concrete ids. */
            ALLOW_TAGS,
            /** Tags are dropped (with an optional warning); only concrete ids survive. */
            REJECT_TAGS
        }
    }

    /** Trims each entry and drops the ones that become empty. */
    record TrimNonEmpty<S>() implements Rule<S, List<String>> {
        @Override
        public List<String> apply(S owner, List<String> value, String name, SanitizeContext ctx) {
            return new ArrayList<>(value.stream().map(String::trim).filter(entry -> !entry.isEmpty()).toList());
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
     * recursing into its own {@link SchemaSection#sanitize}, not clamping the reference itself. */
    record Nested<S, C>(SchemaSection<C> section) implements Rule<S, C> {
        @Override
        public C apply(S owner, C value, String name, SanitizeContext ctx) {
            return section.sanitize(value, ctx);
        }
    }
}
