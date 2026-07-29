package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation;
import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation.Node;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigReferenceRendererTest {
    private static final SchemaDocumentation DOCUMENTATION = SchemaDocumentation.create();

    @Test
    void referenceIsDeterministicAndParsesToTheExactSanitizedDefaults() {
        String first = ConfigReferenceRenderer.render(DOCUMENTATION);
        String second = ConfigReferenceRenderer.render(SchemaDocumentation.create());
        assertEquals(first, second);

        Yaml yaml = WorldzConfig.createYaml();
        Object expected = yaml.load(new WorldzConfig().sanitize(NOPLogger.NOP_LOGGER).toYaml());
        Object actual = yaml.load(first);
        assertEquals(expected, actual);
    }

    @Test
    void everyFixedNodeAndEveryConcreteKitFieldHasAPathComment() {
        String reference = ConfigReferenceRenderer.render(DOCUMENTATION);
        for (Node node : allNodes()) {
            if (!node.path().contains(SchemaDocumentation.DYNAMIC_NAME)) {
                assertTrue(reference.contains("# Path: " + node.path() + "\n"), node.path());
            }
        }
        for (String kitName : new WorldzConfig().kits.keySet()) {
            assertTrue(reference.contains("# Path: kits." + kitName + "\n"), kitName);
            for (String field : List.of("essentials", "extras", "extrasCount")) {
                assertTrue(
                    reference.contains("# Path: kits." + kitName + "." + field + "\n"),
                    kitName + "." + field
                );
            }
        }
    }

    @Test
    void everyEmittedMappingKeyIsImmediatelyPrecededByComments() {
        String[] lines = ConfigReferenceRenderer.render(DOCUMENTATION).split("\\n");
        for (int index = 0; index < lines.length; index++) {
            String trimmed = lines[index].stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("- ")) {
                continue;
            }
            assertTrue(trimmed.contains(":"), "unexpected YAML line: " + lines[index]);
            assertTrue(index > 0 && lines[index - 1].stripLeading().startsWith("#"), "key lacks comments: " + lines[index]);
        }
    }

    @Test
    void headerFileMapEscapingCollectionsAndCommentWrappingAreStable() {
        String reference = ConfigReferenceRenderer.render(DOCUMENTATION);
        for (ConfigFile file : ConfigLayout.FILES) {
            assertTrue(reference.contains("config/jlt_worldz/" + file.relativePath()), file.relativePath());
        }
        assertTrue(reference.contains("allowedBiomes:\n  - 'minecraft:desert'"));
        assertTrue(reference.contains("roleOverrides: {}"));
        assertTrue(reference.contains("starterKit: 'ocean-island-default'"));
        assertEquals("'Jason''s kit'", SchemaDocumentation.flowValue("Jason's kit"));
        reference.lines()
            .filter(line -> line.stripLeading().startsWith("#"))
            .forEach(line -> assertTrue(line.length() <= 120, "overlong comment: " + line));
    }

    @Test
    void nonemptyStringMapsQuoteUnsafeKeysAnnotateEntriesAndRoundTrip() {
        Map<String, String> roleOverrides = new LinkedHashMap<>();
        roleOverrides.put("true", "land");
        roleOverrides.put("123", "ocean");
        roleOverrides.put("owner's", "beach");
        roleOverrides.put("safe_key", "land");

        String rendered = ConfigReferenceRenderer.renderNode(
            DOCUMENTATION.node("layout.roleOverrides"),
            roleOverrides
        );

        assertTrue(rendered.contains("  'true': 'land'"));
        assertTrue(rendered.contains("  '123': 'ocean'"));
        assertTrue(rendered.contains("  'owner''s': 'beach'"));
        assertTrue(rendered.contains("  safe_key: 'land'"));
        for (String key : roleOverrides.keySet()) {
            assertTrue(
                rendered.contains("# Path: layout.roleOverrides[" + SchemaDocumentation.flowValue(key) + "]"),
                key
            );
        }
        assertTrue(rendered.contains("# Default: 'ocean'"));
        assertTrue(rendered.contains("# Applies: "));
        assertTrue(rendered.contains("# Customize: "));
        assertTrue(rendered.contains("# Split file: "));

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("roleOverrides", roleOverrides);
        assertEquals(expected, WorldzConfig.createYaml().load(rendered));

        String[] lines = rendered.split("\\n");
        for (int index = 0; index < lines.length; index++) {
            String trimmed = lines[index].stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            assertTrue(index > 0 && lines[index - 1].stripLeading().startsWith("#"), lines[index]);
        }
    }

    private static List<Node> allNodes() {
        java.util.ArrayList<Node> nodes = new java.util.ArrayList<>();
        DOCUMENTATION.root().children().forEach(child -> collect(child, nodes));
        return nodes;
    }

    private static void collect(Node node, List<Node> nodes) {
        nodes.add(node);
        node.children().forEach(child -> collect(child, nodes));
    }
}
