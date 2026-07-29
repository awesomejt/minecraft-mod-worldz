package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explicit README updater. It lives in test source so production startup can never mutate
 * repository documentation.
 */
public final class ConfigDocumentationTool {
    private static final Pattern VALID_MARKER = Pattern.compile(
        "<!-- (BEGIN|END) GENERATED CONFIG TABLE: ([a-z0-9]+(?:-[a-z0-9]+)*) -->"
    );
    private static final Pattern MARKER_CANDIDATE = Pattern.compile(
        "<!--.*(?i:GENERATED[\\s_-]+CONFIG[\\s_-]+TABLE).*"
    );

    private ConfigDocumentationTool() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ConfigDocumentationTool <README.md>");
        }
        Path readme = Path.of(args[0]);
        String before = Files.readString(readme);
        String after = update(before, SchemaDocumentation.create());
        Files.writeString(readme, after);
    }

    static String update(String markdown, SchemaDocumentation documentation) {
        validateKnownMarkers(markdown);
        Map<String, String> regions = ConfigReadmeRenderer.renderRegions(documentation);
        List<Region> positions = new ArrayList<>();
        for (String id : ConfigReadmeRenderer.REGION_IDS) {
            String begin = ConfigReadmeRenderer.beginMarker(id);
            String end = ConfigReadmeRenderer.endMarker(id);
            int beginIndex = uniqueIndex(markdown, begin);
            int endIndex = uniqueIndex(markdown, end);
            if (beginIndex >= endIndex) {
                throw new IllegalArgumentException("Generated README markers are reversed for " + id);
            }
            positions.add(new Region(id, beginIndex, beginIndex + begin.length(), endIndex, endIndex + end.length()));
        }
        positions.sort(Comparator.comparingInt(Region::begin));
        for (int index = 1; index < positions.size(); index++) {
            if (positions.get(index).begin() < positions.get(index - 1).endAfter()) {
                throw new IllegalArgumentException(
                    "Generated README marker regions overlap or nest: "
                        + positions.get(index - 1).id() + " and " + positions.get(index).id()
                );
            }
        }

        String updated = markdown;
        positions.sort(Comparator.comparingInt(Region::begin).reversed());
        for (Region region : positions) {
            String body = "\n" + regions.get(region.id()) + ConfigReadmeRenderer.endMarker(region.id());
            updated = updated.substring(0, region.beginAfter()) + body + updated.substring(region.endAfter());
        }
        return updated;
    }

    private static void validateKnownMarkers(String markdown) {
        for (String line : markdown.lines().toList()) {
            if (!MARKER_CANDIDATE.matcher(line).find()) {
                continue;
            }
            Matcher matcher = VALID_MARKER.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Malformed generated README marker: " + line);
            }
            if (!ConfigReadmeRenderer.REGION_IDS.contains(matcher.group(2))) {
                throw new IllegalArgumentException("Unknown generated README marker id: " + matcher.group(2));
            }
        }
    }

    private static int uniqueIndex(String text, String marker) {
        int first = text.indexOf(marker);
        if (first < 0) {
            throw new IllegalArgumentException("Missing generated README marker: " + marker);
        }
        if (text.indexOf(marker, first + marker.length()) >= 0) {
            throw new IllegalArgumentException("Duplicate generated README marker: " + marker);
        }
        return first;
    }

    private record Region(String id, int begin, int beginAfter, int end, int endAfter) {
    }
}
