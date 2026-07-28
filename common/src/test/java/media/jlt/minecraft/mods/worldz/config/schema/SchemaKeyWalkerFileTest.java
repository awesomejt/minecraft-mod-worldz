package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ConfigFile;
import media.jlt.minecraft.mods.worldz.config.ConfigLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@link SchemaKeyWalker#findUnknownKeysInFile} (DESIGN §43.5/§43.9 row c, TODO 25.7c): the
 * per-file "right key, wrong file" diagnostic {@link SchemaKeyWalker#findUnknownKeys}'s whole-root
 * walk can't express, since run against an unwrapped file's body it would report every one of that
 * file's real keys as unknown. {@link ConfigFixturesTest} is deliberately untouched by this task --
 * it exercises the original whole-root method against the bundle-shaped {@code config/tests/*.yaml}
 * fixtures and stays that way (25.7c's own constraint).
 */
class SchemaKeyWalkerFileTest {
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();

    @Test
    void unwrappedFileStrayKeyIsCheckedAgainstItsOwnSectionOnly() {
        ConfigFile caveFile = ConfigLayout.owning("cave").orElseThrow();
        Map<String, Object> caveMap = Map.of("notARealCaveSetting", 1);
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, caveFile, caveMap, unknown, misfiled);

        // "cave.": CaveSchema's own path prefix -- proof the stray key was measured against
        // CaveSchema alone, never the whole 26-key root (DESIGN §43.5's unwrapped case).
        assertEquals(List.of("cave.notARealCaveSetting"), unknown);
        assertTrue(misfiled.isEmpty(), () -> "unexpected misfiled: " + misfiled);
    }

    @Test
    void wrappedFileKeyOwnedByAnotherFileIsReportedAsMisfiledNamingTheRightFile() {
        ConfigFile runtimeFile = fileNamed("runtime.yaml");
        // A user writes "cave:" straight into runtime.yaml instead of world-types/cave.yaml.
        Map<String, Object> runtimeMap = Map.of("cave", Map.of("spawnDepthY", -40));
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, runtimeFile, runtimeMap, unknown, misfiled);

        assertTrue(unknown.isEmpty(), () -> "unexpected unknown: " + unknown);
        assertEquals(List.of("cave belongs in world-types/cave.yaml"), misfiled);
    }

    @Test
    void wrappedFileKeyOwnedByNoFileAtAllIsReportedAsUnknownNotMisfiled() {
        ConfigFile runtimeFile = fileNamed("runtime.yaml");
        Map<String, Object> runtimeMap = Map.of("notARealRootKeyAtAll", 1);
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, runtimeFile, runtimeMap, unknown, misfiled);

        assertEquals(List.of("notARealRootKeyAtAll"), unknown);
        assertTrue(misfiled.isEmpty(), () -> "unexpected misfiled: " + misfiled);
    }

    @Test
    void layoutRoleOverridesArbitrarySubKeysStillDoNotTripTheCheck() {
        ConfigFile worldzFile = fileNamed("world-types/worldz.yaml");
        Map<String, Object> worldzMap = Map.of(
            "layout", Map.of("roleOverrides", Map.of("minecraft:some_custom_id", "land", "another:weird_one", "ocean"))
        );
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, worldzFile, worldzMap, unknown, misfiled);

        assertTrue(unknown.isEmpty(), () -> "roleOverrides' arbitrary sub-keys must not be flagged: " + unknown);
        assertTrue(misfiled.isEmpty(), () -> "unexpected misfiled: " + misfiled);
    }

    private static ConfigFile fileNamed(String relativePath) {
        return ConfigLayout.FILES.stream()
            .filter(file -> file.relativePath().equals(relativePath))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no ConfigLayout.FILES entry named " + relativePath));
    }
}
