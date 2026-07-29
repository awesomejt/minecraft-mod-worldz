package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadmeConfigDocumentationTest {
    private static final Path README = Path.of("../README.md");
    private static final SchemaDocumentation DOCUMENTATION = SchemaDocumentation.create();

    @Test
    void checkedInRegionsAreCurrentAndUpdaterIsIdempotent() throws IOException {
        String checkedIn = Files.readString(README);
        String once = ConfigDocumentationTool.update(checkedIn, DOCUMENTATION);
        String twice = ConfigDocumentationTool.update(once, SchemaDocumentation.create());
        assertEquals(checkedIn, once, "README config tables are stale; run ./gradlew :common:updateConfigDocs");
        assertEquals(once, twice);
    }

    @Test
    void markersAreCompleteUniqueOrderedAndRejectMalformedInput() throws IOException {
        String markdown = Files.readString(README);
        for (String id : ConfigReadmeRenderer.REGION_IDS) {
            assertEquals(1, occurrences(markdown, ConfigReadmeRenderer.beginMarker(id)), id + " begin marker");
            assertEquals(1, occurrences(markdown, ConfigReadmeRenderer.endMarker(id)), id + " end marker");
            assertTrue(
                markdown.indexOf(ConfigReadmeRenderer.beginMarker(id))
                    < markdown.indexOf(ConfigReadmeRenderer.endMarker(id)),
                id + " markers reversed"
            );
        }

        assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(
                markdown.replace(ConfigReadmeRenderer.beginMarker("single-biome"), ""),
                DOCUMENTATION
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(
                markdown + "\n" + ConfigReadmeRenderer.beginMarker("single-biome"),
                DOCUMENTATION
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(
                markdown + "\n<!-- BEGIN GENERATED CONFIG TABLE: unknown -->",
                DOCUMENTATION
            )
        );
        assertMalformedMarker(
            markdown,
            "<!-- BEGIN GENERATED CONFIG TABLE: Single_Biome -->"
        );
        assertMalformedMarker(
            markdown,
            "<!-- BEGIN GENERATED CONFIG TABLE: single--biome -->"
        );
        assertMalformedMarker(
            markdown,
            "<!-- BEGIN GENERATED_CONFIG_TABLE: single-biome -->"
        );

        IllegalArgumentException reversed = assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(markerDocument(true, false), DOCUMENTATION)
        );
        assertTrue(reversed.getMessage().contains("reversed"));

        IllegalArgumentException nested = assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(markerDocument(false, true), DOCUMENTATION)
        );
        assertTrue(nested.getMessage().contains("overlap or nest"));
    }

    @Test
    void partitionsContainEverySchemaLeafExactlyOnce() {
        Map<String, String> regions = ConfigReadmeRenderer.renderRegions(DOCUMENTATION);
        assertEquals(new LinkedHashSet<>(ConfigReadmeRenderer.REGION_IDS), regions.keySet());

        List<String> documentedPaths = new ArrayList<>();
        for (String id : ConfigReadmeRenderer.REGION_IDS) {
            String table = regions.get(id);
            for (String line : table.lines().toList()) {
                if (!line.startsWith("| `")) {
                    continue;
                }
                int end = line.indexOf('`', 3);
                documentedPaths.add(line.substring(3, end));
            }
        }
        List<String> expectedPaths = ConfigReadmeRenderer.REGION_IDS.stream()
            .flatMap(id -> DOCUMENTATION.leaves().stream()
                .filter(node -> ConfigReadmeRenderer.regionFor(node.path()).equals(id))
                .map(SchemaDocumentation.Node::path))
            .toList();
        assertEquals(expectedPaths, documentedPaths, "README partitions must preserve declaration order within each region");
        assertEquals(new LinkedHashSet<>(documentedPaths).size(), documentedPaths.size(), "duplicate README setting row");
    }

    @Test
    void updaterChangesOnlyMarkerInteriorsAndEscapesMarkdown() throws IOException {
        String checkedIn = Files.readString(README);
        String header = "| Setting | Default | Unit / range | Applies | Description |";
        int headerAt = checkedIn.indexOf(header);
        String deliberatelyStale = checkedIn.substring(0, headerAt)
            + "| stale |"
            + checkedIn.substring(headerAt + header.length());
        String repaired = ConfigDocumentationTool.update(deliberatelyStale, DOCUMENTATION);
        assertEquals(outsideGeneratedRegions(deliberatelyStale), outsideGeneratedRegions(repaired));
        assertEquals("left\\|right one line", ConfigReadmeRenderer.escapeCell("left|right\none line"));
        assertEquals("&#96;path&#124;value", ConfigReadmeRenderer.escapeCode("`path|value"));
    }

    private static String outsideGeneratedRegions(String markdown) {
        String reduced = markdown;
        for (String id : ConfigReadmeRenderer.REGION_IDS) {
            String begin = ConfigReadmeRenderer.beginMarker(id);
            String end = ConfigReadmeRenderer.endMarker(id);
            int start = reduced.indexOf(begin) + begin.length();
            int finish = reduced.indexOf(end, start);
            reduced = reduced.substring(0, start) + "\n<generated>\n" + reduced.substring(finish);
        }
        return reduced;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static void assertMalformedMarker(String markdown, String malformed) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ConfigDocumentationTool.update(
                markdown.replace(ConfigReadmeRenderer.beginMarker("single-biome"), malformed),
                DOCUMENTATION
            )
        );
        assertTrue(exception.getMessage().contains("Malformed generated README marker"));
    }

    private static String markerDocument(boolean reversedFirst, boolean nestedFirstTwo) {
        StringBuilder markdown = new StringBuilder();
        List<String> ids = ConfigReadmeRenderer.REGION_IDS;
        int next = 0;
        if (reversedFirst) {
            appendRegion(markdown, ids.getFirst(), true);
            next = 1;
        } else if (nestedFirstTwo) {
            markdown.append(ConfigReadmeRenderer.beginMarker(ids.get(0))).append('\n')
                .append(ConfigReadmeRenderer.beginMarker(ids.get(1))).append('\n')
                .append(ConfigReadmeRenderer.endMarker(ids.get(1))).append('\n')
                .append(ConfigReadmeRenderer.endMarker(ids.get(0))).append('\n');
            next = 2;
        }
        for (int index = next; index < ids.size(); index++) {
            appendRegion(markdown, ids.get(index), false);
        }
        return markdown.toString();
    }

    private static void appendRegion(StringBuilder markdown, String id, boolean reversed) {
        String first = reversed ? ConfigReadmeRenderer.endMarker(id) : ConfigReadmeRenderer.beginMarker(id);
        String second = reversed ? ConfigReadmeRenderer.beginMarker(id) : ConfigReadmeRenderer.endMarker(id);
        markdown.append(first).append('\n').append("placeholder\n").append(second).append('\n');
    }
}
