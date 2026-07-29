package media.jlt.minecraft.mods.worldz.config.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The stock {@link ValueCodec} instances (DESIGN §41.3). {@link #INT} and {@link #DOUBLE} are
 * verbatim copies of {@code WorldzConfig.readInt}/{@code readDouble} (:1628-1655 as measured
 * against the pre-refactor tree) -- same six accepted numeric types, same {@code intValueExact}
 * leniency, same {@code ArithmeticException} rethrow. That leniency is load-bearing
 * ({@code WorldzConfigTest.fractionalRadiusMakesTheFileInvalidWithoutOverwritingIt}) and exactly
 * the kind of detail a rewrite could silently lose, so it is duplicated here rather than
 * refactored, until the legacy copies are deleted at TODO 25.2h.
 */
public final class Codecs {
    /** Integer values, accepting the same widened numeric types SnakeYAML can hand back. */
    public static final ValueCodec<Integer> INT = new ValueCodec<>() {
        @Override
        public Integer read(Object raw, ParseContext ctx) {
            String name = ctx.path();
            try {
                return switch (raw) {
                    case Integer integer -> integer;
                    case Byte byteValue -> byteValue.intValue();
                    case Short shortValue -> shortValue.intValue();
                    case Long longValue -> Math.toIntExact(longValue);
                    case BigInteger bigInteger -> bigInteger.intValueExact();
                    case BigDecimal bigDecimal -> bigDecimal.intValueExact();
                    default -> throw new IllegalArgumentException(name + " must be an integer");
                };
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(name + " must be an integer", exception);
            }
        }

        @Override
        public Object write(Integer value) {
            return value;
        }
    };

    /** Floating-point values, accepting the same widened numeric types SnakeYAML can hand back. */
    public static final ValueCodec<Double> DOUBLE = new ValueCodec<>() {
        @Override
        public Double read(Object raw, ParseContext ctx) {
            String name = ctx.path();
            return switch (raw) {
                case Double doubleValue -> doubleValue;
                case Float floatValue -> floatValue.doubleValue();
                case Integer integer -> integer.doubleValue();
                case Byte byteValue -> byteValue.doubleValue();
                case Short shortValue -> shortValue.doubleValue();
                case Long longValue -> longValue.doubleValue();
                case BigInteger bigInteger -> bigInteger.doubleValue();
                case BigDecimal bigDecimal -> bigDecimal.doubleValue();
                default -> throw new IllegalArgumentException(name + " must be a number");
            };
        }

        @Override
        public Object write(Double value) {
            return value;
        }
    };

    /** Boolean flags. */
    public static final ValueCodec<Boolean> BOOLEAN = new ValueCodec<>() {
        @Override
        public Boolean read(Object raw, ParseContext ctx) {
            if (!(raw instanceof Boolean booleanValue)) {
                throw new IllegalArgumentException(ctx.path() + " must be a boolean");
            }
            return booleanValue;
        }

        @Override
        public Object write(Boolean value) {
            return value;
        }
    };

    /** Plain strings. */
    public static final ValueCodec<String> STRING = new ValueCodec<>() {
        @Override
        public String read(Object raw, ParseContext ctx) {
            if (!(raw instanceof String string)) {
                throw new IllegalArgumentException(ctx.path() + " must be a string");
            }
            return string;
        }

        @Override
        public Object write(String value) {
            return value;
        }
    };

    /** Lists of strings; non-string entries are dropped with a warning rather than failing the
     * whole list (today's {@code readStringList} behavior). */
    public static final ValueCodec<List<String>> STRING_LIST = new ValueCodec<>() {
        @Override
        public List<String> read(Object raw, ParseContext ctx) {
            String name = ctx.path();
            if (!(raw instanceof List<?> list)) {
                throw new IllegalArgumentException(name + " must be a sequence");
            }
            List<String> values = new ArrayList<>();
            for (int index = 0; index < list.size(); index++) {
                Object entry = list.get(index);
                if (entry instanceof String string) {
                    values.add(string);
                } else {
                    ctx.logger().warn("Ignoring non-string {} entry at index {}.", name, index);
                }
            }
            return values;
        }

        @Override
        public Object write(List<String> value) {
            return value;
        }
    };

    /** String-to-string maps. */
    public static final ValueCodec<Map<String, String>> STRING_MAP = new ValueCodec<>() {
        @Override
        public Map<String, String> read(Object raw, ParseContext ctx) {
            String name = ctx.path();
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException(name + " must be a mapping");
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String stringValue)) {
                    throw new IllegalArgumentException(name + " keys and values must be strings");
                }
                values.put(key, stringValue);
            }
            return values;
        }

        @Override
        public Object write(Map<String, String> value) {
            return value;
        }
    };

    private Codecs() {
    }

    /**
     * Builds a codec for an enum that follows the project's {@code parse}/{@code serializedName}
     * convention. Read goes through {@link #STRING} first, so a non-string value fails with the
     * usual "must be a string" message before {@code parse} ever sees it.
     *
     * @param parse parses the stable lowercase configuration name, throwing
     *     {@link IllegalArgumentException} on an unrecognized value
     * @param serialize returns the stable lowercase configuration name
     */
    public static <T> ValueCodec<T> enumeration(Function<String, T> parse, Function<T, String> serialize) {
        return new ValueCodec<>() {
            @Override
            public T read(Object raw, ParseContext ctx) {
                return parse.apply(STRING.read(raw, ctx));
            }

            @Override
            public Object write(T value) {
                return serialize.apply(value);
            }
        };
    }

    /**
     * Builds a codec that delegates to a nested {@link SectionCodec}, for {@code Setting.section}.
     *
     * @param section the nested section's schema (or registry entry)
     */
    public static <C> ValueCodec<C> section(SectionCodec<C> section) {
        return new ValueCodec<>() {
            @Override
            public Shape shape() {
                return Shape.SECTION;
            }

            @Override
            public SectionCodec<?> nestedSchema() {
                return section;
            }

            @Override
            public C read(Object raw, ParseContext ctx) {
                return section.read(raw, ctx);
            }

            @Override
            public Object write(C value) {
                return section.toMap(value);
            }
        };
    }

    /**
     * Builds a codec for a polymorphic leaf that is either a bare name (a reference into a shared
     * library, e.g. {@code kits}) or an inline mapping -- {@code StarterKitSchema.reference}'s
     * mechanism (DESIGN §44.3.3). A raw {@link String} value parses via {@code fromName}; anything
     * else (typically a {@link java.util.Map}) falls through to {@code section.read}, unchanged
     * from today -- including the "must be a mapping" exception a non-string/non-map value still
     * throws. Write mirrors this: {@code nameOf} returning non-{@code null} emits the bare name,
     * otherwise {@code section.toMap} emits the inline mapping exactly as today.
     *
     * @param section the inline definition's own codec, used verbatim for both read and write
     * @param fromName builds a reference-only value from the trimmed name
     * @param nameOf returns the name to re-emit for a referenced value, or {@code null} for an
     *     inline one
     */
    public static <C> ValueCodec<C> namedOrSection(
        SectionCodec<C> section, Function<String, C> fromName, Function<C, String> nameOf
    ) {
        return new ValueCodec<>() {
            @Override
            public Shape shape() {
                return Shape.POLYMORPHIC;
            }

            @Override
            public SectionCodec<?> nestedSchema() {
                return section;
            }

            @Override
            public C read(Object raw, ParseContext ctx) {
                if (raw instanceof String name) {
                    return fromName.apply(name.trim());
                }
                return section.read(raw, ctx);
            }

            @Override
            public Object write(C value) {
                String name = nameOf.apply(value);
                return name != null ? name : section.toMap(value);
            }
        };
    }

    /**
     * Builds a codec for a dynamic-key section -- a name-to-entry map where every value shares one
     * entry shape, e.g. the {@code kits} library (DESIGN §44.4.2). Each raw map key becomes a name
     * in the returned {@link Map}; each raw value parses through {@code entry.read}, scoped one
     * level deeper via {@link ParseContext#child(String)} so per-entry warnings/exceptions carry
     * the entry's own name; presence is recorded for the full dotted path of each name.
     *
     * @param entry the shared entry shape's own codec, used verbatim for both read and write
     */
    public static <C> ValueCodec<Map<String, C>> sectionMap(SectionCodec<C> entry) {
        return new ValueCodec<>() {
            @Override
            public Shape shape() {
                return Shape.DYNAMIC_MAP;
            }

            @Override
            public SectionCodec<?> nestedSchema() {
                return entry;
            }

            @Override
            public Map<String, C> read(Object raw, ParseContext ctx) {
                if (!(raw instanceof Map<?, ?> map)) {
                    throw new IllegalArgumentException(ctx.path() + " must be a mapping");
                }
                Map<String, C> values = new LinkedHashMap<>();
                for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                    if (!(mapEntry.getKey() instanceof String name)) {
                        throw new IllegalArgumentException(ctx.path() + " keys must be strings");
                    }
                    ParseContext child = ctx.child(name);
                    values.put(name, entry.read(mapEntry.getValue(), child));
                    child.markPresent(child.path());
                }
                return values;
            }

            @Override
            public Object write(Map<String, C> value) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (Map.Entry<String, C> mapEntry : value.entrySet()) {
                    values.put(mapEntry.getKey(), entry.toMap(mapEntry.getValue()));
                }
                return values;
            }
        };
    }
}
