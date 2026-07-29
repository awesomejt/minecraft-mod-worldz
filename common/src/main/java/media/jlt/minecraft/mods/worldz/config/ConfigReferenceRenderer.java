package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation;
import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation.Kind;
import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure, deterministic renderer for the generated commented reference bundle. */
public final class ConfigReferenceRenderer {
    private static final int COMMENT_WIDTH = 116;
    private static final Pattern SAFE_PLAIN_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");
    private static final Set<String> YAML_RESERVED_KEYS = Set.of(
        "false", "n", "no", "null", "off", "on", "true", "y", "yes"
    );

    private ConfigReferenceRenderer() {
    }

    public static String render(SchemaDocumentation documentation) {
        StringBuilder out = new StringBuilder();
        renderHeader(out);
        for (Node node : documentation.root().children()) {
            renderNode(out, node, node.encodedDefault(), 0, node.path());
        }
        return out.toString();
    }

    static String renderNode(Node node, Object value) {
        StringBuilder out = new StringBuilder();
        renderNode(out, node, value, 0, node.path());
        return out.toString();
    }

    private static void renderHeader(StringBuilder out) {
        out.append("""
            # jlt_worldz reference config -- GENERATED, do not edit.
            # Rewritten from the mod's schema on every launch; the mod never reads this file.
            # Every setting is shown at its sanitized built-in default. Copy the parts you want
            # into config/jlt_worldz/ or copy this whole file to config/jlt_worldz/all.yaml.
            #
            # Split-file map (runtime.yaml is live after relaunch; every other file is baked into
            # newly created worlds):
            """);
        for (ConfigFile file : ConfigLayout.FILES) {
            appendComment(
                out,
                0,
                String.join("/", file.rootKeys())
                    + " -> config/jlt_worldz/"
                    + file.relativePath()
                    + (file.unwrapped() ? " (unwrapped)" : " (wrapped)")
            );
        }
        out.append("# See README.md for wrapped/unwrapped shapes and live-vs-baked behavior.\n\n");
    }

    @SuppressWarnings("unchecked")
    private static void renderNode(StringBuilder out, Node node, Object value, int indent, String concretePath) {
        appendMetadata(out, node, indent, concretePath, value);
        if (node.kind() == Kind.SECTION) {
            appendIndent(out, indent).append(yamlKey(node.key())).append(":\n");
            Map<String, Object> map = castMap(value, concretePath);
            for (Node child : node.children()) {
                renderNode(out, child, map.get(child.key()), indent + 2, concreteChildPath(concretePath, child));
            }
            return;
        }
        if (node.kind() == Kind.DYNAMIC_MAP) {
            appendIndent(out, indent).append(yamlKey(node.key())).append(":\n");
            Map<String, Object> entries = castMap(value, concretePath);
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                String entryPath = concretePath + "." + entry.getKey();
                appendComment(out, indent + 2, "Path: " + entryPath);
                appendComment(out, indent + 2, "Named reusable entry; fields use the " + node.path() + "." + SchemaDocumentation.DYNAMIC_NAME + " schema.");
                appendComment(out, indent + 2, "Applies: " + node.appliesText());
                appendComment(out, indent + 2, "Split file: " + node.splitFile());
                appendIndent(out, indent + 2).append(yamlKey(entry.getKey())).append(":\n");
                Map<String, Object> entryMap = castMap(entry.getValue(), entryPath);
                for (Node child : node.children()) {
                    String childPath = entryPath + child.path().substring((node.path() + "." + SchemaDocumentation.DYNAMIC_NAME).length());
                    renderNode(out, child, entryMap.get(child.key()), indent + 4, childPath);
                }
            }
            return;
        }
        renderLeaf(out, node, value, indent, concretePath);
    }

    private static String concreteChildPath(String parent, Node child) {
        if (!child.path().contains(SchemaDocumentation.DYNAMIC_NAME)) {
            return parent + "." + child.key();
        }
        return child.path();
    }

    private static void appendMetadata(
        StringBuilder out, Node node, int indent, String concretePath, Object value
    ) {
        appendComment(out, indent, "Path: " + concretePath);
        appendComment(out, indent, node.docs().doc());
        if (node.kind() == Kind.LEAF) {
            String defaultText = node.docs().defaultText().isBlank()
                ? SchemaDocumentation.flowValue(value)
                : node.docs().defaultText();
            appendComment(out, indent, "Default: " + defaultText);
            if (!node.unitAndRangeText().isBlank()) {
                appendComment(out, indent, "Unit / range: " + node.unitAndRangeText());
            }
        } else {
            int childCount = node.children().size();
            appendComment(
                out, indent,
                "Default: " + (node.kind() == Kind.DYNAMIC_MAP
                    ? castMap(value, concretePath).size() + " named entries shown below"
                    : childCount + " documented child settings shown below")
            );
        }
        appendComment(out, indent, "Applies: " + node.appliesText());
        appendComment(
            out, indent,
            "Customize: " + (node.applicability().customizeExposed() ? "exposed" : "not exposed")
        );
        appendComment(out, indent, "Split file: " + node.splitFile());
    }

    private static void renderLeaf(
        StringBuilder out, Node node, Object value, int indent, String concretePath
    ) {
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                appendIndent(out, indent).append(yamlKey(node.key())).append(": []\n");
                return;
            }
            appendIndent(out, indent).append(yamlKey(node.key())).append(":\n");
            for (Object item : list) {
                appendIndent(out, indent + 2).append("- ").append(yamlScalar(item)).append('\n');
            }
            return;
        }
        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.isEmpty()) {
                appendIndent(out, indent).append(yamlKey(node.key())).append(": {}\n");
                return;
            }
            appendIndent(out, indent).append(yamlKey(node.key())).append(":\n");
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String mapKey)) {
                    throw new IllegalArgumentException("Only string-keyed config maps are supported");
                }
                appendMapEntryMetadata(
                    out, node, indent + 2, concretePath, mapKey, entry.getValue()
                );
                appendIndent(out, indent + 2)
                    .append(yamlKey(mapKey))
                    .append(": ")
                    .append(yamlScalar(entry.getValue()))
                    .append('\n');
            }
            return;
        }
        appendIndent(out, indent)
            .append(yamlKey(node.key()))
            .append(": ")
            .append(yamlScalar(value))
            .append('\n');
    }

    private static void appendMapEntryMetadata(
        StringBuilder out, Node node, int indent, String concretePath, String mapKey, Object value
    ) {
        appendComment(out, indent, "Path: " + concretePath + "[" + SchemaDocumentation.flowValue(mapKey) + "]");
        appendComment(out, indent, node.docs().doc() + " Map entry.");
        appendComment(out, indent, "Default: " + SchemaDocumentation.flowValue(value));
        if (!node.unitAndRangeText().isBlank()) {
            appendComment(out, indent, "Unit / range: " + node.unitAndRangeText());
        }
        appendComment(out, indent, "Applies: " + node.appliesText());
        appendComment(
            out, indent,
            "Customize: " + (node.applicability().customizeExposed() ? "exposed" : "not exposed")
        );
        appendComment(out, indent, "Split file: " + node.splitFile());
    }

    private static String yamlScalar(Object value) {
        if (value instanceof String string) {
            return "'" + string.replace("'", "''") + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> || value instanceof Map<?, ?>) {
            return SchemaDocumentation.flowValue(value);
        }
        if (value == null) {
            return "null";
        }
        throw new IllegalArgumentException("Unsupported reference YAML scalar: " + value.getClass().getName());
    }

    private static String yamlKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (SAFE_PLAIN_KEY.matcher(key).matches() && !YAML_RESERVED_KEYS.contains(normalized)) {
            return key;
        }
        return "'" + key.replace("'", "''") + "'";
    }

    private static void appendComment(StringBuilder out, int indent, String text) {
        List<String> words = new ArrayList<>(List.of(text.trim().split("\\s+")));
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (!line.isEmpty() && indent + 2 + line.length() + 1 + word.length() > COMMENT_WIDTH) {
                appendIndent(out, indent).append("# ").append(line).append('\n');
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        appendIndent(out, indent).append("# ").append(line).append('\n');
    }

    private static StringBuilder appendIndent(StringBuilder out, int indent) {
        return out.append(" ".repeat(indent));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " default must be a mapping");
        }
        return (Map<String, Object>) map;
    }
}
