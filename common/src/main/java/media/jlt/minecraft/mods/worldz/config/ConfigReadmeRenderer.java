package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation;
import media.jlt.minecraft.mods.worldz.config.schema.SchemaDocumentation.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure renderer for the generated README configuration-table regions. */
public final class ConfigReadmeRenderer {
    public static final List<String> REGION_IDS = List.of(
        "single-biome",
        "chaos-biomes",
        "strip-world",
        "strip-bands",
        "ocean-island",
        "sky-island",
        "floating-islands",
        "sky-chunk",
        "cave",
        "nether-start-capsule",
        "nether-start",
        "end-start",
        "flat",
        "deep-flat",
        "stacked",
        "forever-night",
        "rising-lava",
        "structure-distance",
        "generic-and-world-defaults",
        "shared-kits"
    );

    private static final Set<String> GENERIC_ROOTS = Set.of(
        "allowedBiomes",
        "starter",
        "naturalBiomes",
        "layout",
        "spawn",
        "overworldBorder",
        "netherBorder",
        "endBorder",
        "overworldExterior",
        "netherExterior"
    );

    private ConfigReadmeRenderer() {
    }

    /** Renders all 20 named regions and fails if any schema leaf is omitted or duplicated. */
    public static Map<String, String> renderRegions(SchemaDocumentation documentation) {
        Map<String, List<Node>> partitioned = new LinkedHashMap<>();
        REGION_IDS.forEach(id -> partitioned.put(id, new ArrayList<>()));
        Set<String> seen = new LinkedHashSet<>();
        for (Node leaf : documentation.leaves()) {
            String region = regionFor(leaf.path());
            if (!seen.add(leaf.path())) {
                throw new IllegalStateException("Duplicate README config leaf: " + leaf.path());
            }
            partitioned.get(region).add(leaf);
        }
        if (seen.size() != documentation.leaves().size()) {
            throw new IllegalStateException("README partition did not cover every schema leaf exactly once");
        }

        Map<String, String> rendered = new LinkedHashMap<>();
        for (Map.Entry<String, List<Node>> entry : partitioned.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException("README config region is empty: " + entry.getKey());
            }
            rendered.put(entry.getKey(), renderTable(entry.getValue()));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(rendered));
    }

    public static String beginMarker(String id) {
        return "<!-- BEGIN GENERATED CONFIG TABLE: " + id + " -->";
    }

    public static String endMarker(String id) {
        return "<!-- END GENERATED CONFIG TABLE: " + id + " -->";
    }

    static String regionFor(String path) {
        if (path.startsWith("kits." + SchemaDocumentation.DYNAMIC_NAME + ".")) {
            return "shared-kits";
        }
        if (path.startsWith("singleBiome.")) {
            return "single-biome";
        }
        if (path.startsWith("chaosBiomes.")) {
            return "chaos-biomes";
        }
        if (path.startsWith("stripWorld.bands.")) {
            return "strip-bands";
        }
        if (path.startsWith("stripWorld.")) {
            return "strip-world";
        }
        if (path.startsWith("oceanIsland.")) {
            return "ocean-island";
        }
        if (path.startsWith("skyIsland.floatingIslands.")) {
            return "floating-islands";
        }
        if (path.startsWith("skyIsland.")) {
            return "sky-island";
        }
        if (path.startsWith("chunkIsland.")) {
            return "sky-chunk";
        }
        if (path.startsWith("cave.")) {
            return "cave";
        }
        if (path.startsWith("netherStart.capsule.")) {
            return "nether-start-capsule";
        }
        if (path.startsWith("netherStart.")) {
            return "nether-start";
        }
        if (path.startsWith("endStart.")) {
            return "end-start";
        }
        if (path.startsWith("flat.")) {
            return "flat";
        }
        if (path.startsWith("deepFlat.")) {
            return "deep-flat";
        }
        if (path.startsWith("stacked.")) {
            return "stacked";
        }
        if (path.startsWith("foreverNight.")) {
            return "forever-night";
        }
        if (path.startsWith("risingLava.")) {
            return "rising-lava";
        }
        if (path.startsWith("structureDistance.")) {
            return "structure-distance";
        }
        String root = path.substring(0, path.indexOf('.') < 0 ? path.length() : path.indexOf('.'));
        if (GENERIC_ROOTS.contains(root)) {
            return "generic-and-world-defaults";
        }
        throw new IllegalStateException("No README config partition for " + path);
    }

    private static String renderTable(List<Node> leaves) {
        StringBuilder table = new StringBuilder("""
            | Setting | Default | Unit / range | Applies | Description |
            |---|---|---|---|---|
            """);
        for (Node leaf : leaves) {
            table.append("| `")
                .append(escapeCode(leaf.path()))
                .append("` | `")
                .append(escapeCode(
                    leaf.path().contains(SchemaDocumentation.DYNAMIC_NAME)
                        ? "varies by named entry"
                        : leaf.defaultText()
                ))
                .append("` | ")
                .append(escapeCell(leaf.unitAndRangeText().isBlank() ? "—" : leaf.unitAndRangeText()))
                .append(" | ")
                .append(escapeCell(leaf.appliesText()))
                .append(" | ")
                .append(escapeCell(leaf.docs().doc()))
                .append(" |\n");
        }
        return table.toString();
    }

    static String escapeCell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    static String escapeCode(String value) {
        return value.replace("`", "&#96;").replace("|", "&#124;").replace("\n", " ");
    }
}
