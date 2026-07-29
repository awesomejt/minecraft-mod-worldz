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
 *
 * <p>Also the home for TODO 25.8e's (DESIGN §44.8 row e) two new recursion shapes: {@code kits.yaml}
 * (unwrapped, {@link Rule.NestedMap}) tolerates its own arbitrary kit names while still checking each
 * kit body, and an inline kit at a site ({@link Rule.KitReference}) has its sub-keys checked against
 * {@link StarterKitSchema} -- both exercised here rather than in a fresh file, matching this file's
 * own existing per-file-walk precedent.
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
    void flatStripWorldFileIsCheckedAgainstTheMergedStripWorldSchema() {
        ConfigFile stripFile = ConfigLayout.owning("stripWorld").orElseThrow();
        Map<String, Object> stripMap = Map.of(
            "width", 4,
            "spawn", Map.of("strategy", "starter_at_origin"),
            "notARealStripSetting", true
        );
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, stripFile, stripMap, unknown, misfiled);

        assertEquals(List.of("stripWorld.notARealStripSetting"), unknown);
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
    void typoedKeyInsideAnInlineKitAtASiteIsCaughtAgainstStarterKitSchema() {
        ConfigFile oceanIslandFile = fileNamed("world-types/ocean-island.yaml");
        Map<String, Object> oceanIslandMap = Map.of(
            "starterKit", Map.of("essentails", List.of("minecraft:bread:1"))
        );
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, oceanIslandFile, oceanIslandMap, unknown, misfiled);

        // "oceanIsland.starterKit.": proof the typo was checked against StarterKitSchema specifically
        // (Rule.KitReference's inline schema, TODO 25.8e), not skipped now that the leaf is polymorphic.
        assertEquals(List.of("oceanIsland.starterKit.essentails"), unknown);
        assertTrue(misfiled.isEmpty(), () -> "unexpected misfiled: " + misfiled);
    }

    @Test
    void kitsYamlArbitraryKitNameIsNotFlaggedButItsOwnBodyStillIs() {
        ConfigFile kitsFile = fileNamed("kits.yaml");
        Map<String, Object> kitsMap = Map.of(
            "my-brutal-kit", Map.of(
                "essentials", List.of("minecraft:stick:1"),
                "extras", List.of(),
                "extrasCount", 0
            )
        );
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, kitsFile, kitsMap, unknown, misfiled);

        // "my-brutal-kit" itself (a user-chosen name, Rule.NestedMap) must never appear here.
        assertTrue(unknown.isEmpty(), () -> "an arbitrary kit name must not be flagged: " + unknown);
        assertTrue(misfiled.isEmpty(), () -> "unexpected misfiled: " + misfiled);
    }

    @Test
    void kitsYamlTypoedKeyInsideANamedKitsBodyIsStillCaught() {
        ConfigFile kitsFile = fileNamed("kits.yaml");
        Map<String, Object> kitsMap = Map.of("cave-easy", Map.of("essentails", List.of("minecraft:bread:1")));
        List<String> unknown = new ArrayList<>();
        List<String> misfiled = new ArrayList<>();

        SchemaKeyWalker.findUnknownKeysInFile(ROOT, kitsFile, kitsMap, unknown, misfiled);

        // The name ("cave-easy") is never part of the reported path -- only the shared kits entry
        // schema's own path ("kits"), since every named kit body shares the one StarterKitSchema.
        assertEquals(List.of("kits.essentails"), unknown);
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
