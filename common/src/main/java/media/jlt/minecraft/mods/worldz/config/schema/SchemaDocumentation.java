package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ConfigFile;
import media.jlt.minecraft.mods.worldz.config.ConfigLayout;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable documentation view of the same schema that parses and emits the configuration.
 *
 * <p>The view resolves inherited applicability, sanitized defaults, split-file ownership,
 * dynamic-map wildcards and polymorphic kit-reference leaves once. The reference YAML and README
 * renderers both consume this model, so neither keeps an independent setting inventory.
 */
public final class SchemaDocumentation {
    public static final String DYNAMIC_NAME = "<name>";

    private final Node root;
    private final List<Node> leaves;
    private final Map<String, Node> byPath;

    private SchemaDocumentation(Node root) {
        this.root = root;
        List<Node> collected = new ArrayList<>();
        Map<String, Node> indexed = new LinkedHashMap<>();
        index(root, collected, indexed);
        this.leaves = List.copyOf(collected);
        this.byPath = Map.copyOf(indexed);
    }

    /** Builds documentation from sanitized built-in defaults. */
    public static SchemaDocumentation create() {
        WorldzRootSchema schema = new WorldzRootSchema();
        WorldzConfig defaults = schema.defaults();
        schema.sanitize(defaults, new SanitizeContext(NOPLogger.NOP_LOGGER, defaults));
        Node root = new Node(
            "", "", Docs.NONE, null, null, null, "", List.of(), Kind.SECTION, List.of()
        );
        List<Node> children = buildSection(schema, defaults, null, false, "");
        return new SchemaDocumentation(root.withChildren(children));
    }

    public Node root() {
        return root;
    }

    /** Every scalar/list/map/polymorphic setting, with dynamic kit fields represented once. */
    public List<Node> leaves() {
        return leaves;
    }

    public Node node(String canonicalPath) {
        Node found = byPath.get(canonicalPath);
        if (found == null) {
            throw new IllegalArgumentException("Unknown documented config path: " + canonicalPath);
        }
        return found;
    }

    /** Maps {@code kits.cave-easy.essentials} to {@code kits.<name>.essentials}. */
    public static String normalizeDynamicPath(String path) {
        if (!path.startsWith("kits.")) {
            return path;
        }
        int nextDot = path.indexOf('.', "kits.".length());
        return nextDot < 0 ? "kits." + DYNAMIC_NAME : "kits." + DYNAMIC_NAME + path.substring(nextDot);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<Node> buildSection(
        SchemaSection section, Object owner, Applicability inherited, boolean inheritedCustomize, String parentPath
    ) {
        List<Node> nodes = new ArrayList<>();
        for (Object rawSetting : section.settings()) {
            Setting setting = (Setting) rawSetting;
            String path = parentPath.isEmpty() ? setting.key() : parentPath + "." + setting.key();
            Applicability effective = resolve(setting.applies(), inherited, inheritedCustomize, path);
            boolean childCustomize = inheritedCustomize || setting.applies().customizeDescendantsExposed();
            Object typedDefault = setting.accessor().get(owner);
            Object encodedDefault = setting.codec().write(typedDefault);
            ConfigFile file = ConfigLayout.owning(firstSegment(path)).orElseThrow(
                () -> new IllegalStateException("No ConfigLayout owner for " + path)
            );
            String range = rangeText(setting.docs(), setting.rule(), owner);
            List<Node> children = List.of();
            Kind kind = Kind.LEAF;
            List<String> inlineShape = List.of();

            if (setting.codec().shape() == ValueCodec.Shape.SECTION
                && setting.codec().nestedSchema() instanceof SchemaSection childSchema) {
                children = buildSection(childSchema, typedDefault, effective, childCustomize, path);
                kind = Kind.SECTION;
            } else if (setting.codec().shape() == ValueCodec.Shape.DYNAMIC_MAP
                && setting.codec().nestedSchema() instanceof SchemaSection entrySchema) {
                Map<?, ?> entries = (Map<?, ?>) typedDefault;
                Object entryValue = entries.isEmpty() ? entrySchema.defaults() : entries.values().iterator().next();
                children = buildSection(entrySchema, entryValue, effective, childCustomize, path + "." + DYNAMIC_NAME);
                kind = Kind.DYNAMIC_MAP;
            } else if (setting.codec().shape() == ValueCodec.Shape.POLYMORPHIC
                && setting.codec().nestedSchema() instanceof SchemaSection inlineSchema) {
                inlineShape = relativeLeafPaths(inlineSchema, inlineSchema.defaults());
            }

            String defaultText = setting.docs().defaultText().isBlank()
                ? flowValue(encodedDefault)
                : setting.docs().defaultText();
            nodes.add(new Node(
                setting.key(),
                path,
                setting.docs(),
                effective,
                file,
                encodedDefault,
                defaultText,
                children,
                kind,
                inlineShape,
                range
            ));
        }
        return List.copyOf(nodes);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> relativeLeafPaths(SchemaSection section, Object owner) {
        List<String> result = new ArrayList<>();
        for (Object rawSetting : section.settings()) {
            Setting setting = (Setting) rawSetting;
            if (setting.rule() instanceof Rule.Nested nested
                && nested.section() instanceof SchemaSection child) {
                for (String childPath : relativeLeafPaths(child, setting.accessor().get(owner))) {
                    result.add(setting.key() + "." + childPath);
                }
            } else {
                result.add((String) setting.key());
            }
        }
        return List.copyOf(result);
    }

    private static Applicability resolve(
        Applicability own, Applicability inherited, boolean inheritedCustomize, String path
    ) {
        Objects.requireNonNull(own, path + " has no applicability");
        if (own.scope() != Applicability.Scope.INHERIT) {
            return new Applicability(
                own.scope(),
                own.presets(),
                own.customizeExposed() || inheritedCustomize,
                own.customizeDescendantsExposed()
            );
        }
        if (inherited == null) {
            throw new IllegalStateException("Root setting " + path + " must declare an explicit applicability scope");
        }
        return new Applicability(
            inherited.scope(),
            inherited.presets(),
            own.customizeExposed() || inheritedCustomize,
            own.customizeDescendantsExposed()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String rangeText(Docs docs, Rule rule, Object owner) {
        if (!docs.rangeText().isBlank()) {
            return docs.rangeText();
        }
        Rule candidate = rule;
        boolean odd = false;
        if (candidate instanceof Rule.Compose compose) {
            odd = compose.first() instanceof Rule.OddRounding || compose.second() instanceof Rule.OddRounding;
            candidate = compose.first() instanceof Rule.IntRange ? compose.first() : compose.second();
        }
        String range = "";
        if (candidate instanceof Rule.IntRange intRange) {
            int min = intRange.min().applyAsInt(owner);
            int max = intRange.max().applyAsInt(owner);
            range = boundedRange(min, max);
        } else if (candidate instanceof Rule.DoubleRange doubleRange) {
            range = number(doubleRange.min()) + ".." + number(doubleRange.max());
        }
        if (odd) {
            return range.isBlank() ? "odd" : range + "; odd";
        }
        return range;
    }

    private static String boundedRange(int min, int max) {
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            return "";
        }
        if (max == Integer.MAX_VALUE) {
            return min + "+";
        }
        if (min == Integer.MIN_VALUE) {
            return "≤" + max;
        }
        return min + ".." + max;
    }

    private static String firstSegment(String path) {
        int dot = path.indexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private static void index(Node node, List<Node> leaves, Map<String, Node> indexed) {
        if (!node.path().isEmpty()) {
            if (indexed.put(node.path(), node) != null) {
                throw new IllegalStateException("Duplicate documented config path: " + node.path());
            }
            if (node.kind() == Kind.LEAF) {
                leaves.add(node);
            }
        }
        node.children().forEach(child -> index(child, leaves, indexed));
    }

    /** Stable flow-style value used in tables and comment metadata. */
    public static String flowValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "'" + string.replace("'", "''") + "'";
        }
        if (value instanceof Double number) {
            return number(number);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> list) {
            return "[" + list.stream().map(SchemaDocumentation::flowValue).reduce((a, b) -> a + ", " + b).orElse("") + "]";
        }
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("Only string-keyed config maps can be documented");
                }
                entries.add(flowKey(key) + ": " + flowValue(entry.getValue()));
            }
            return "{" + String.join(", ", entries) + "}";
        }
        throw new IllegalArgumentException("Unsupported documented config value: " + value.getClass().getName());
    }

    private static String flowKey(String key) {
        return key.matches("[A-Za-z0-9_-]+") ? key : flowValue(key);
    }

    private static String number(double number) {
        if (Double.isInfinite(number)) {
            return number < 0 ? "-∞" : "∞";
        }
        if (number == Math.rint(number)) {
            return Long.toString((long) number);
        }
        return Double.toString(number);
    }

    public enum Kind {
        SECTION,
        DYNAMIC_MAP,
        LEAF
    }

    /**
     * One setting. Section and dynamic-map nodes contain children; leaf nodes contain a rendered
     * default and optional inline-shape fingerprint.
     */
    public record Node(
        String key,
        String path,
        Docs docs,
        Applicability applicability,
        ConfigFile file,
        Object encodedDefault,
        String defaultText,
        List<Node> children,
        Kind kind,
        List<String> inlineShape,
        String rangeText
    ) {
        private Node(
            String key,
            String path,
            Docs docs,
            Applicability applicability,
            ConfigFile file,
            Object encodedDefault,
            String defaultText,
            List<Node> children,
            Kind kind,
            List<String> inlineShape
        ) {
            this(key, path, docs, applicability, file, encodedDefault, defaultText, children, kind, inlineShape, "");
        }

        public Node {
            children = List.copyOf(children);
            inlineShape = List.copyOf(inlineShape);
        }

        private Node withChildren(List<Node> newChildren) {
            return new Node(
                key, path, docs, applicability, file, encodedDefault, defaultText,
                newChildren, kind, inlineShape, rangeText
            );
        }

        public boolean leaf() {
            return kind == Kind.LEAF;
        }

        public String splitFile() {
            return "config/jlt_worldz/" + file.relativePath();
        }

        public String appliesText() {
            String customize = applicability.customizeExposed() ? "; Customize" : "";
            return switch (applicability.scope()) {
                case LIVE -> "Live; existing worlds after relaunch" + customize;
                case WORLD_DEFAULT -> "Baked: all presets; new worlds only" + customize;
                case PRESET -> {
                    Set<String> presets = applicability.presets();
                    String names = presets.isEmpty() ? "worldz" : String.join(", ", presets);
                    yield "Baked: " + names + "; new worlds only" + customize;
                }
                case INHERIT -> throw new IllegalStateException("Unresolved applicability at " + path);
            };
        }

        public String unitAndRangeText() {
            String unit = switch (docs.unit()) {
                case BLOCKS -> "blocks";
                case CHUNKS -> "chunks";
                case DAYS -> "days";
                case Y_LEVEL -> "Y level";
                case CHANCE -> "chance";
                case COUNT -> "count";
                case FACTOR -> "factor";
                case BIOME_ID -> "biome id";
                case ITEM_LIST -> "item list";
                case NONE -> "";
            };
            if (unit.isBlank()) {
                return rangeText;
            }
            return rangeText.isBlank() ? unit : unit + "; " + rangeText;
        }
    }
}
