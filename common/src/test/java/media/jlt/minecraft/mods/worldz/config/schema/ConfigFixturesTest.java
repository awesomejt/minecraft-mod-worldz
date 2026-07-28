package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The fixture gate DESIGN §42.5 says does not exist yet (TODO 25.6a). {@code
 * ConfigSchemaDifferentialTest} was the only thing that ever loaded {@code config/tests/*.yaml},
 * and it was retired at TODO 25.2h once there was no more "old" implementation left to compare
 * against -- since then, a stale or mis-migrated fixture has parsed cleanly, built green, and only
 * failed silently in Jason's manual test round. This closes that gap for every one of the 104
 * fixtures:
 *
 * <ul>
 *   <li>it parses and sanitizes without throwing;
 *   <li>every dotted key it sets is declared by {@link WorldzRootSchema} at its exact nesting level
 *       ({@link SchemaKeyWalker#findUnknownKeys}, a parallel walk of the file's own map against the
 *       schema tree -- never based on a raw value's shape alone, since {@code layout.roleOverrides}
 *       is a leaf {@code stringMap} setting with arbitrary user-defined sub-keys);
 *   <li>the directory's file count matches {@link #EXPECTED_FIXTURE_COUNT}, so a fixture can't be
 *       silently dropped later without this test noticing.
 * </ul>
 *
 * <p>Configs 57, 97 and 98 were the three known, pre-existing gaps this gate exists to prove is
 * working (DESIGN §42.1/§42.5/§42.7) -- each set a key the parse layer had never actually declared,
 * despite it being documented in {@code README.md}. All three are now resolved: TODO 25.6d wired
 * {@code skyIsland.exclusionZone} into the schema (config 57), and TODO 25.6g wired {@code flat}/
 * {@code skyIsland}'s shared {@code underground: {biome, belowSurface}} group into the schema
 * (configs 97/98, migrated to the new nested key in that same commit). {@link #KNOWN_UNKNOWN_KEYS}
 * is therefore empty today -- kept as a live map (not deleted outright) so a future genuine gap of
 * this same shape has an obvious place to be recorded and asserted precisely, the same discipline
 * that made all three of these provably fixed rather than silently reconciled.
 */
class ConfigFixturesTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Yaml LOADER = new Yaml(new SafeConstructor(new LoaderOptions()));
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();
    private static final Path FIXTURE_DIRECTORY = Path.of("../config/tests");

    // Hardcoded rather than derived from testConfigFileNames()'s own size, so a fixture silently
    // added to or removed from config/tests/ fails this assertion instead of passing unnoticed.
    // NOTE: TODO.md/DESIGN.md/CONFIG-RESTRUCTURE.md/GOALS.md all say "104" fixtures -- verified
    // empirically here (`ls config/tests/*.yaml`, 2026-07-28) that the real, current count is 103
    // (101 numbered files, two of which -- 27, 29 -- also have an "a"-suffixed sibling). No commit
    // has ever deleted a config/tests/*.yaml file, so "104" looks like a stale count in the prose
    // rather than a fixture that used to exist; flagged for Jason rather than silently reconciled
    // (TODO Deviation log, 2026-07-28).
    private static final int EXPECTED_FIXTURE_COUNT = 103;

    private static final Map<String, Set<String>> KNOWN_UNKNOWN_KEYS = Map.of();

    @Test
    void fixtureDirectoryHasTheExpectedFileCount() throws IOException {
        assertEquals(
            EXPECTED_FIXTURE_COUNT, testConfigFileNames().count(), "config/tests/*.yaml file count changed unexpectedly"
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testConfigFileNames")
    void everyFixtureParsesSanitizesAndDeclaresOnlyKnownKeys(String fileName) throws IOException {
        Path file = FIXTURE_DIRECTORY.resolve(fileName);
        Map<?, ?> raw = loadRawMap(file);

        List<String> unknownKeys = new ArrayList<>();
        SchemaKeyWalker.findUnknownKeys(ROOT, raw, unknownKeys);
        assertEquals(
            KNOWN_UNKNOWN_KEYS.getOrDefault(fileName, Set.of()),
            new LinkedHashSet<>(unknownKeys),
            () -> fileName + ": unknown-key set changed (see DESIGN §42.5/§42.7 -- 57/97/98 resolved at 25.6d/25.6g)"
        );

        WorldzConfig parsed = assertDoesNotThrow(() -> ROOT.read(raw, new ParseContext(LOGGER)), fileName + " failed to parse");
        assertDoesNotThrow(
            () -> ROOT.sanitize(parsed, new SanitizeContext(LOGGER, parsed)), fileName + " failed to sanitize"
        );
    }

    static Stream<String> testConfigFileNames() throws IOException {
        try (var stream = Files.list(FIXTURE_DIRECTORY)) {
            return stream.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".yaml")).sorted().toList().stream();
        }
    }

    private static Map<?, ?> loadRawMap(Path file) throws IOException {
        Object loaded = LOADER.load(Files.readString(file));
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(file + ": root value must be a YAML mapping");
        }
        return map;
    }
}
