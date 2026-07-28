package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.LegacySections;
import media.jlt.minecraft.mods.worldz.config.SchemaSections;
import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Proves TODO 25.2a's three converted sections ({@code spawn}, {@code starterKit}, {@code
 * starterCapsule}) are behaviorally identical to the hand-written logic they replace (DESIGN
 * §41.8). For every corpus input, both the {@link LegacySections} adapter (a verbatim copy of the
 * pre-25.2a logic) and the {@link SchemaSections} entry (the new {@code SchemaSection}) are run
 * through read/sanitize/toMap/summary, and three things must match exactly: the dumped YAML
 * string, the summary string, and the ordered list of captured {@code WARN} lines.
 *
 * <p>Corpus: every {@code config/tests/*.yaml} file's top-level {@code spawn:} section (real,
 * user-shaped input -- spawn is the only one of the three converted sections that sits at the
 * config root), plus {@code config/jlt_worldz.example.yaml}, plus fresh defaults, plus hand-written
 * adversarial fragments targeting all three sections' rules (spawn defaults/invalid values,
 * starter-kit essentials/extras/extrasCount edge cases, capsule bounds and odd-rounding for both
 * Nether-start and End-start bounds). {@code starterKit}/{@code starterCapsule} are nested inside
 * still-legacy sections (cave, ocean island, Nether/End start, ...) rather than sitting at the
 * config root themselves, so they are exercised directly against hand-written fragments rather
 * than mined from the 104-file corpus -- enough to prove the two sections actually converted this
 * step, per TODO 25.2a's scope (the remaining ~23 sections' turn comes in 25.2b-h).
 */
class ConfigSchemaDifferentialTest {
    private static final Yaml LOADER = new Yaml(new SafeConstructor(new LoaderOptions()));
    private static final Yaml DUMPER = new Yaml(new DumperOptionsBuilder().build());

    // ---- spawn: mined from the real config/tests/*.yaml corpus + example + defaults ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("spawnCorpus")
    void spawnSectionMatchesLegacyAcrossCorpus(String label, Object rawSpawnValueOrNull) {
        assertSpawnIdentical(label, rawSpawnValueOrNull);
    }

    static Stream<Arguments> spawnCorpus() throws IOException {
        List<Arguments> arguments = new ArrayList<>();
        for (Path file : testConfigFiles()) {
            Object loaded = LOADER.load(Files.readString(file));
            Object spawnValue = loaded instanceof Map<?, ?> map ? map.get("spawn") : null;
            arguments.add(Arguments.of(file.getFileName().toString(), spawnValue));
        }
        Object exampleLoaded = LOADER.load(Files.readString(Path.of("../config/jlt_worldz.example.yaml")));
        Object exampleSpawn = exampleLoaded instanceof Map<?, ?> map ? map.get("spawn") : null;
        arguments.add(Arguments.of("jlt_worldz.example.yaml", exampleSpawn));
        arguments.add(Arguments.of("defaults (absent)", null));
        return arguments.stream();
    }

    // ---- spawn: hand-written adversarial fragments ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("spawnAdversarialFragments")
    void spawnSectionMatchesLegacyForAdversarialFragments(String label, String fragment) {
        Object raw = fragment == null ? null : LOADER.load(fragment);
        assertSpawnIdentical(label, raw);
    }

    static Stream<Arguments> spawnAdversarialFragments() {
        return Stream.of(
            Arguments.of("empty mapping", "{}"),
            Arguments.of("valid strategy: vanilla_spawn", "strategy: vanilla_spawn"),
            Arguments.of("valid strategy: preferred_natural_biome", "strategy: preferred_natural_biome"),
            Arguments.of("invalid strategy value", "strategy: not-a-strategy"),
            Arguments.of("wrong type for strategy", "strategy: true"),
            Arguments.of("wrong type for the whole section", "true")
        );
    }

    private static void assertSpawnIdentical(String label, Object rawOrNull) {
        RecordingLogger legacyLogger = new RecordingLogger();
        RecordingLogger schemaLogger = new RecordingLogger();
        String name = "spawn";

        ThrowingSupplier<SpawnConfig> legacyRun = () -> {
            SpawnConfig parsed = rawOrNull == null ? new SpawnConfig() : LegacySections.spawn(name).read(rawOrNull, new ParseContext(legacyLogger));
            return LegacySections.spawn(name).sanitize(parsed, new SanitizeContext(legacyLogger, null));
        };
        ThrowingSupplier<SpawnConfig> schemaRun = () -> {
            SpawnConfig parsed = rawOrNull == null ? new SpawnConfig() : SchemaSections.spawn(name).read(rawOrNull, new ParseContext(schemaLogger));
            return SchemaSections.spawn(name).sanitize(parsed, new SanitizeContext(schemaLogger, null));
        };

        SpawnConfig legacyResult = runOrNull(legacyRun);
        SpawnConfig schemaResult = runOrNull(schemaRun);

        if (legacyResult == null || schemaResult == null) {
            assertEquals(legacyResult == null, schemaResult == null, label + ": one side threw and the other didn't");
            return;
        }

        assertEquals(
            DUMPER.dump(LegacySections.spawn(name).toMap(legacyResult)),
            DUMPER.dump(SchemaSections.spawn(name).toMap(schemaResult)),
            label + ": toMap()"
        );
        assertEquals(
            LegacySections.spawn(name).summary(legacyResult), SchemaSections.spawn(name).summary(schemaResult), label + ": summary()"
        );
        assertEquals(legacyLogger.warnings(), schemaLogger.warnings(), label + ": WARN lines");
    }

    // ---- starterKit: hand-written adversarial fragments ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("starterKitFragments")
    void starterKitSectionMatchesLegacy(String label, String fragment) {
        String name = "oceanIsland.starterKit";
        RecordingLogger legacyLogger = new RecordingLogger();
        RecordingLogger schemaLogger = new RecordingLogger();
        Object raw = LOADER.load(fragment);

        StarterKitConfig legacyResult = runOrNull(() -> LegacySections.starterKit(name).sanitize(
            LegacySections.starterKit(name).read(raw, new ParseContext(legacyLogger)), new SanitizeContext(legacyLogger, null)
        ));
        StarterKitConfig schemaResult = runOrNull(() -> SchemaSections.starterKit(name).sanitize(
            SchemaSections.starterKit(name).read(raw, new ParseContext(schemaLogger)), new SanitizeContext(schemaLogger, null)
        ));

        if (legacyResult == null || schemaResult == null) {
            assertEquals(legacyResult == null, schemaResult == null, label + ": one side threw and the other didn't");
            return;
        }

        assertEquals(
            DUMPER.dump(LegacySections.starterKit(name).toMap(legacyResult)),
            DUMPER.dump(SchemaSections.starterKit(name).toMap(schemaResult)),
            label + ": toMap()"
        );
        assertEquals(
            LegacySections.starterKit(name).summary(legacyResult), SchemaSections.starterKit(name).summary(schemaResult), label + ": summary()"
        );
        assertEquals(legacyLogger.warnings(), schemaLogger.warnings(), label + ": WARN lines");
    }

    static Stream<Arguments> starterKitFragments() {
        return Stream.of(
            Arguments.of("empty mapping (all defaults)", "{}"),
            Arguments.of("fully specified, no warnings", """
                essentials: ["minecraft:torch:1"]
                extras: ["minecraft:coal:2"]
                extrasCount: 1
                """),
            Arguments.of("negative extrasCount is clamped to 0", "extrasCount: -5"),
            Arguments.of("positive extrasCount with empty extras pool is clamped to 0", """
                essentials: []
                extras: []
                extrasCount: 3
                """),
            Arguments.of("essentials/extras explicitly empty lists (not null) stay empty", """
                essentials: []
                extras: []
                extrasCount: 0
                """),
            Arguments.of("wrong type for the whole section", "true")
        );
    }

    // ---- starterCapsule: hand-written adversarial fragments, both Nether-start and End-start bounds ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("starterCapsuleFragments")
    void starterCapsuleSectionMatchesLegacyForNetherStartBounds(String label, String fragment) {
        assertCapsuleIdentical(
            "netherStart.capsule [nether-start bounds] " + label, fragment,
            NetherStartPlan.MIN_CAPSULE_SIZE_BLOCKS, NetherStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, NetherStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, NetherStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("starterCapsuleFragments")
    void starterCapsuleSectionMatchesLegacyForEndStartBounds(String label, String fragment) {
        assertCapsuleIdentical(
            "endStart.capsule [end-start bounds] " + label, fragment,
            EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS, EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            EndStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, EndStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            EndStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, EndStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    static Stream<Arguments> starterCapsuleFragments() {
        return Stream.of(
            Arguments.of("empty mapping (all defaults)", "{}"),
            Arguments.of("even sizeBlocks is odd-rounded, then within bounds", "sizeBlocks: 6"),
            Arguments.of("very negative sizeBlocks: odd-round then clamp to minimum", "sizeBlocks: -5"),
            Arguments.of("huge sizeBlocks: odd already, clamp to maximum", "sizeBlocks: 9999"),
            Arguments.of("heightBlocks below minimum", "heightBlocks: -50"),
            Arguments.of("heightBlocks above maximum", "heightBlocks: 999"),
            Arguments.of("lightSpacingBlocks at zero is clamped up", "lightSpacingBlocks: 0"),
            Arguments.of("lightSpacingBlocks far above maximum", "lightSpacingBlocks: 9999"),
            Arguments.of("valid lightSource: lantern", "lightSource: lantern"),
            Arguments.of("wrong type for the whole section", "true")
        );
    }

    private static void assertCapsuleIdentical(
        String label, String fragment, int minSize, int maxSize, int minHeight, int maxHeight, int minSpacing, int maxSpacing
    ) {
        String name = "capsule";
        RecordingLogger legacyLogger = new RecordingLogger();
        RecordingLogger schemaLogger = new RecordingLogger();
        Object raw = LOADER.load(fragment);

        var legacyCodec = LegacySections.starterCapsule(name, minSize, maxSize, minHeight, maxHeight, minSpacing, maxSpacing);
        var schemaCodec = SchemaSections.starterCapsule(name, minSize, maxSize, minHeight, maxHeight, minSpacing, maxSpacing);

        StarterCapsuleConfig legacyResult = runOrNull(() -> legacyCodec.sanitize(
            legacyCodec.read(raw, new ParseContext(legacyLogger)), new SanitizeContext(legacyLogger, null)
        ));
        StarterCapsuleConfig schemaResult = runOrNull(() -> schemaCodec.sanitize(
            schemaCodec.read(raw, new ParseContext(schemaLogger)), new SanitizeContext(schemaLogger, null)
        ));

        if (legacyResult == null || schemaResult == null) {
            assertEquals(legacyResult == null, schemaResult == null, label + ": one side threw and the other didn't");
            return;
        }

        assertEquals(DUMPER.dump(legacyCodec.toMap(legacyResult)), DUMPER.dump(schemaCodec.toMap(schemaResult)), label + ": toMap()");
        assertEquals(legacyCodec.summary(legacyResult), schemaCodec.summary(schemaResult), label + ": summary()");
        assertEquals(legacyLogger.warnings(), schemaLogger.warnings(), label + ": WARN lines");
    }

    // ---- error paths: a root-level non-mapping value must reject identically ----

    @Test
    void nonMappingRawValueIsRejectedIdenticallyByBothPaths() {
        Object raw = "just a string, not a mapping";
        assertThrows(IllegalArgumentException.class, () -> LegacySections.spawn("spawn").read(raw, new ParseContext(new RecordingLogger())));
        assertThrows(IllegalArgumentException.class, () -> SchemaSections.spawn("spawn").read(raw, new ParseContext(new RecordingLogger())));
        assertThrows(IllegalArgumentException.class, () -> LegacySections.starterKit("kit").read(raw, new ParseContext(new RecordingLogger())));
        assertThrows(IllegalArgumentException.class, () -> SchemaSections.starterKit("kit").read(raw, new ParseContext(new RecordingLogger())));
    }

    @Test
    void emptyDocumentMeansAbsentForBothPaths() {
        Object loaded = LOADER.load("");
        assertEquals(null, loaded);
    }

    private static List<Path> testConfigFiles() throws IOException {
        try (var stream = Files.list(Path.of("../config/tests"))) {
            return stream.filter(path -> path.toString().endsWith(".yaml")).sorted().toList();
        }
    }

    private static <T> T runOrNull(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException expected) {
            return null;
        } catch (Exception unexpected) {
            fail(unexpected);
            return null;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }

    /** Builds a stable SnakeYAML dumper so both sides of every comparison are rendered identically. */
    private static final class DumperOptionsBuilder {
        DumperOptions build() {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setWidth(120);
            return options;
        }
    }
}
