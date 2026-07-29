package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ConfigLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaDocumentationTest {
    private static final SchemaDocumentation DOCUMENTATION = SchemaDocumentation.create();

    @Test
    void everyDocumentedNodeHasMetadataAndEveryLeafAppearsOnceInDeclarationOrder() {
        List<SchemaDocumentation.Node> nodes = allNodes();
        for (SchemaDocumentation.Node node : nodes) {
            assertFalse(node.docs().doc().isBlank(), node.path() + " has no description");
            assertNotNull(node.applicability(), node.path() + " has no effective applicability");
            assertFalse(
                node.applicability().scope() == Applicability.Scope.INHERIT,
                node.path() + " retained unresolved INHERIT scope"
            );
            assertNotNull(node.file(), node.path() + " has no split-file owner");
            assertTrue(
                node.file().rootKeys().contains(rootKey(node.path())),
                node.path() + " is assigned to the wrong split file"
            );
        }

        List<String> paths = DOCUMENTATION.leaves().stream().map(SchemaDocumentation.Node::path).toList();
        assertEquals(new LinkedHashSet<>(paths).size(), paths.size(), "duplicate documented leaf path");
        assertEquals(paths, allNodes().stream().filter(SchemaDocumentation.Node::leaf).map(SchemaDocumentation.Node::path).toList());
    }

    @Test
    void everyRootDeclaresScopeExplicitlyAndRootOrderMatchesTheSchema() {
        WorldzRootSchema root = new WorldzRootSchema();
        assertTrue(
            root.settings().stream().noneMatch(setting -> setting.applies().scope() == Applicability.Scope.INHERIT),
            "root settings must not rely on inherited applicability"
        );
        assertEquals(
            root.settings().stream().map(Setting::key).toList(),
            DOCUMENTATION.root().children().stream().map(SchemaDocumentation.Node::key).toList()
        );
        assertEquals(
            ConfigLayout.FILES.stream().flatMap(file -> file.rootKeys().stream()).collect(java.util.stream.Collectors.toSet()),
            DOCUMENTATION.root().children().stream().map(SchemaDocumentation.Node::key).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void dynamicKitsAreOneWildcardShapeAndEveryKitReferenceMatchesIt() {
        List<SchemaDocumentation.Node> wildcardLeaves = DOCUMENTATION.leaves().stream()
            .filter(node -> node.path().startsWith("kits." + SchemaDocumentation.DYNAMIC_NAME + "."))
            .toList();
        assertEquals(
            List.of(
                "kits.<name>.essentials",
                "kits.<name>.extras",
                "kits.<name>.extrasCount"
            ),
            wildcardLeaves.stream().map(SchemaDocumentation.Node::path).toList()
        );

        List<String> wildcardShape = wildcardLeaves.stream()
            .map(node -> node.path().substring("kits.<name>.".length()))
            .toList();
        List<SchemaDocumentation.Node> references = DOCUMENTATION.leaves().stream()
            .filter(node -> !node.inlineShape().isEmpty())
            .toList();
        assertEquals(14, references.size(), "the 14 starter-kit sites should remain polymorphic leaves");
        references.forEach(reference ->
            assertEquals(wildcardShape, reference.inlineShape(), reference.path() + " inline shape drifted from kits.<name>")
        );
    }

    @Test
    void customizeExposureMatchesTheActualEditors() {
        for (SchemaDocumentation.Node leaf : DOCUMENTATION.leaves()) {
            assertEquals(
                expectedCustomizeExposure(leaf.path()),
                leaf.applicability().customizeExposed(),
                leaf.path() + " Customize metadata disagrees with the editor/customization record"
            );
        }
    }

    private static boolean expectedCustomizeExposure(String path) {
        if (path.equals("allowedBiomes")
            || startsWithAny(
                path,
                "starter.",
                "layout.",
                "spawn.",
                "overworldBorder.",
                "netherBorder.",
                "endBorder.",
                "overworldExterior.",
                "netherExterior.",
                "singleBiome.",
                "chaosBiomes."
            )) {
            return true;
        }
        if (path.startsWith("stripWorld.")) {
            return !path.equals("stripWorld.enabled");
        }
        if (path.startsWith("oceanIsland.")) {
            return !path.equals("oceanIsland.starterKit");
        }
        if (startsWithAny(
            path,
            "skyIsland.biome",
            "skyIsland.radius",
            "skyIsland.shapeAmplitude",
            "skyIsland.surfaceY",
            "skyIsland.thickness",
            "skyIsland.chest.tier",
            "skyIsland.applyToNether",
            "skyIsland.exclusionZone."
        )) {
            return true;
        }
        if (path.startsWith("skyIsland.floatingIslands.")) {
            return Set.of(
                "skyIsland.floatingIslands.enabled",
                "skyIsland.floatingIslands.radius.min",
                "skyIsland.floatingIslands.radius.max",
                "skyIsland.floatingIslands.shapeAmplitude",
                "skyIsland.floatingIslands.cellSize",
                "skyIsland.floatingIslands.spawnChance",
                "skyIsland.floatingIslands.biomeVariety",
                "skyIsland.floatingIslands.biomes",
                "skyIsland.floatingIslands.exclusionZone.enabled",
                "skyIsland.floatingIslands.exclusionZone.radius",
                "skyIsland.floatingIslands.oreDeposits.enabled",
                "skyIsland.floatingIslands.lootChest.enabled",
                "skyIsland.floatingIslands.naturalBiome"
            ).contains(path);
        }
        if (path.startsWith("chunkIsland.")) {
            return !Set.of("chunkIsland.enabled", "chunkIsland.geodeFeatureIds").contains(path);
        }
        if (path.startsWith("cave.")) {
            return Set.of(
                "cave.spawnY",
                "cave.sealedSurface.enabled",
                "cave.sealedSurface.y",
                "cave.cavern.enabled",
                "cave.cavern.radius",
                "cave.cavern.height",
                "cave.chest.enabled",
                "cave.chest.tier"
            ).contains(path);
        }
        if (path.startsWith("netherStart.")) {
            return Set.of("netherStart.spawnY", "netherStart.chest.tier").contains(path);
        }
        if (path.startsWith("endStart.")) {
            return path.equals("endStart.chest.tier");
        }
        if (path.startsWith("flat.")) {
            return !path.startsWith("flat.underground.");
        }
        if (path.startsWith("deepFlat.")) {
            return true;
        }
        if (path.startsWith("stacked.")) {
            return !path.equals("stacked.worldSizeChunks");
        }
        return false;
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static List<SchemaDocumentation.Node> allNodes() {
        List<SchemaDocumentation.Node> nodes = new ArrayList<>();
        DOCUMENTATION.root().children().forEach(child -> collect(child, nodes));
        return nodes;
    }

    private static void collect(SchemaDocumentation.Node node, List<SchemaDocumentation.Node> nodes) {
        nodes.add(node);
        node.children().forEach(child -> collect(child, nodes));
    }

    private static String rootKey(String path) {
        int dot = path.indexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }
}
