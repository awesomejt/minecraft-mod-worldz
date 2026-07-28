package media.jlt.minecraft.mods.worldz.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@link WorldzConfig#load}'s directory-based load path (DESIGN §43.4.2, TODO 25.7b): the
 * merge-before-schema-walk mechanics, per-file error isolation, the {@code all.yaml} bundle
 * winning wholesale, and that presence tracking still crosses a file boundary the way 25.5's
 * {@code stacked} gate depends on. {@link ConfigLayoutTest} already proves {@link
 * ConfigLayout#FILES} itself is a total, non-overlapping partition -- this class proves the loader
 * built on top of it behaves.
 */
class ConfigDirectoryLoadTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Yaml RAW_LOADER = new Yaml(new SafeConstructor(new LoaderOptions()));

    @TempDir
    Path temporaryDirectory;

    private Path modDir() {
        return temporaryDirectory.resolve("jlt_worldz");
    }

    /**
     * The load path's real proof (DESIGN §43.9 row b): split the golden {@code
     * reference-defaults.yaml} into the 15 files {@link ConfigLayout#FILES} describes -- wrapped
     * files as a partial root copy, unwrapped files as their one owned key's body directly, per
     * §43.2's rules -- then run the real {@link WorldzConfig#load} and confirm the merged result's
     * {@link WorldzConfig#toYaml()} is byte-identical to the golden file.
     */
    @Test
    void byteIdenticalRoundTripThroughAllFifteenSplitFiles() throws IOException {
        String golden = Files.readString(Path.of("src/test/resources/config/reference-defaults.yaml"));
        Map<?, ?> root = loadRawMap(golden);
        splitIntoFiles(modDir(), root);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(golden, config.toYaml());
    }

    @Test
    void absentDirectoryUsesDefaultsWithoutCreatingAnything() throws IOException {
        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertEquals(new WorldzConfig().sanitize(LOGGER).toYaml(), config.toYaml());
        assertFalse(Files.exists(modDir()));
        assertTrue(Files.exists(temporaryDirectory.resolve("jlt_worldz.reference.yaml")));
    }

    @Test
    void absentSplitFilesLeaveTheirSectionsAtDefaultsWhileOthersApply() throws IOException {
        Files.createDirectories(modDir());
        Files.writeString(modDir().resolve("runtime.yaml"), """
            foreverNight:
              enabled: true
              lockAfterDays: 5
            """);
        // Every other one of ConfigLayout.FILES' 14 files is left entirely absent.

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertTrue(config.foreverNight.enabled);
        assertEquals(5, config.foreverNight.lockAfterDays);
        assertFalse(config.overworldBorder.enabled);
        assertEquals("minecraft:plains", config.singleBiome.landBiome);
    }

    @Test
    void blankSplitFileIsSkippedSilentlyWithoutWarning() throws IOException {
        CapturingLogger logger = new CapturingLogger();
        Files.createDirectories(modDir());
        Files.writeString(modDir().resolve("runtime.yaml"), "   \n");
        Files.writeString(modDir().resolve("world-defaults.yaml"), """
            overworldBorder:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", logger);

        assertFalse(config.foreverNight.enabled);
        assertTrue(config.overworldBorder.enabled);
        assertTrue(logger.warnings().isEmpty(), "a blank split file must not warn: " + logger.warnings());
    }

    @Test
    void brokenYamlSyntaxInOneSplitFileWarnsNamesItAndIsIsolated() throws IOException {
        CapturingLogger logger = new CapturingLogger();
        Files.createDirectories(modDir());
        Path broken = modDir().resolve("runtime.yaml");
        Files.writeString(broken, "foreverNight: [tru");
        Files.writeString(modDir().resolve("world-defaults.yaml"), """
            overworldBorder:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", logger);

        assertFalse(config.foreverNight.enabled);
        assertTrue(config.overworldBorder.enabled);
        assertEquals(1, logger.warnings().size(), () -> "unexpected warnings: " + logger.warnings());
        assertTrue(logger.warnings().get(0).contains(broken.toString()));
    }

    @Test
    void nonMappingSplitFileRootWarnsNamesItAndIsIsolated() throws IOException {
        CapturingLogger logger = new CapturingLogger();
        Files.createDirectories(modDir().resolve("world-types"));
        Path notAMapping = modDir().resolve("world-types/cave.yaml");
        Files.writeString(notAMapping, "- a\n- b\n");
        Files.writeString(modDir().resolve("world-defaults.yaml"), """
            overworldBorder:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", logger);

        assertEquals(-32, config.cave.spawnDepthY);
        assertTrue(config.overworldBorder.enabled);
        assertEquals(1, logger.warnings().size(), () -> "unexpected warnings: " + logger.warnings());
        assertTrue(logger.warnings().get(0).contains(notAMapping.toString()));
    }

    @Test
    void bundlePresentAlongsideSplitFilesWinsWholesaleAndWarnsNamingBoth() throws IOException {
        CapturingLogger logger = new CapturingLogger();
        Files.createDirectories(modDir());
        Files.writeString(modDir().resolve("all.yaml"), """
            starter:
              radius: 999
            """);
        Files.writeString(modDir().resolve("runtime.yaml"), """
            foreverNight:
              enabled: true
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", logger);

        assertEquals(999, config.starterRadiusBlocks);
        assertFalse(config.foreverNight.enabled, "the split file must be entirely ignored once all.yaml exists");
        assertEquals(1, logger.warnings().size(), () -> "unexpected warnings: " + logger.warnings());
        String warning = logger.warnings().get(0);
        assertTrue(warning.contains("all.yaml"));
        assertTrue(warning.contains("runtime.yaml"));
    }

    /**
     * Defect fix (code review of TODO 25.7b): {@code readBundle} used to record {@code all.yaml}
     * into {@code loadedFiles} only after a successful parse, so a malformed bundle threw before
     * it was ever named -- the outer catch's fallback WARN read {@code "[]"} instead of naming the
     * file (DESIGN §43.4.4 requires the fallback WARN to list the files that were loaded).
     */
    @Test
    void malformedBundleWarnsNamingTheBundleFile() throws IOException {
        CapturingLogger logger = new CapturingLogger();
        Files.createDirectories(modDir());
        Files.writeString(modDir().resolve("all.yaml"), "starter: [tru");

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", logger);

        assertEquals(256, config.starterRadiusBlocks);
        assertEquals(1, logger.warnings().size(), () -> "unexpected warnings: " + logger.warnings());
        assertTrue(logger.warnings().get(0).contains("all.yaml"));
    }

    /**
     * 25.5's {@code stacked} gate ({@code LimitedBiomeSource.java:336}, {@code
     * EnvelopedChunkGenerator.java:997}) reads {@code config.present("overworldBorder")}, which now
     * has to survive {@code overworldBorder} living in {@code world-defaults.yaml} while {@code
     * stacked} lives in {@code world-types/stacked.yaml} -- a different file entirely.
     */
    @Test
    void overworldBorderPresenceSurvivesTheWorldDefaultsStackedFileBoundary() throws IOException {
        Files.createDirectories(modDir().resolve("world-types"));
        Files.writeString(modDir().resolve("world-defaults.yaml"), """
            overworldBorder:
              enabled: true
              initialRadius: 128
              finalRadius: 4096
            """);
        Files.writeString(modDir().resolve("world-types/stacked.yaml"), """
            worldSizeChunks: 8
            """);

        WorldzConfig config = WorldzConfig.load(temporaryDirectory, "jlt_worldz", LOGGER);

        assertTrue(config.present("overworldBorder"));
        assertEquals(8, config.stacked.worldSizeChunks);
    }

    /** Writes every {@link ConfigLayout#FILES} entry under {@code directory}, wrapped or unwrapped
     * per {@link ConfigFile#unwrapped()}, extracting each file's owned keys from {@code root}. */
    private static void splitIntoFiles(Path directory, Map<?, ?> root) throws IOException {
        for (ConfigFile file : ConfigLayout.FILES) {
            Path target = directory.resolve(file.relativePath());
            Files.createDirectories(target.getParent());
            Object content = file.unwrapped() ? root.get(file.rootKeys().get(0)) : subsetOf(root, file.rootKeys());
            Files.writeString(target, WorldzConfig.createYaml().dump(content));
        }
    }

    private static Map<Object, Object> subsetOf(Map<?, ?> root, List<String> keys) {
        Map<Object, Object> subset = new LinkedHashMap<>();
        for (String key : keys) {
            if (root.containsKey(key)) {
                subset.put(key, root.get(key));
            }
        }
        return subset;
    }

    private static Map<?, ?> loadRawMap(String yaml) {
        Object loaded = RAW_LOADER.load(yaml);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("root value must be a YAML mapping");
        }
        return map;
    }

    /**
     * Minimal {@link Logger} capturing every formatted {@code warn(...)} call, since this test
     * package has no mocking library available. Every non-warn level is a silent no-op; {@link
     * MarkerIgnoringBase} supplies the marker-taking overloads by delegating to these.
     */
    private static final class CapturingLogger extends MarkerIgnoringBase {
        private final List<String> warnings = new ArrayList<>();

        List<String> warnings() {
            return warnings;
        }

        private void capture(String format, Object... args) {
            warnings.add(MessageFormatter.arrayFormat(format, args).getMessage());
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public void trace(String msg) {
        }

        @Override
        public void trace(String format, Object arg) {
        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {
        }

        @Override
        public void trace(String format, Object... arguments) {
        }

        @Override
        public void trace(String msg, Throwable t) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String msg) {
        }

        @Override
        public void debug(String format, Object arg) {
        }

        @Override
        public void debug(String format, Object arg1, Object arg2) {
        }

        @Override
        public void debug(String format, Object... arguments) {
        }

        @Override
        public void debug(String msg, Throwable t) {
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(String msg) {
        }

        @Override
        public void info(String format, Object arg) {
        }

        @Override
        public void info(String format, Object arg1, Object arg2) {
        }

        @Override
        public void info(String format, Object... arguments) {
        }

        @Override
        public void info(String msg, Throwable t) {
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(String msg) {
            capture(msg);
        }

        @Override
        public void warn(String format, Object arg) {
            capture(format, arg);
        }

        @Override
        public void warn(String format, Object... arguments) {
            capture(format, arguments);
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
            capture(format, arg1, arg2);
        }

        @Override
        public void warn(String msg, Throwable t) {
            capture(msg);
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(String msg) {
        }

        @Override
        public void error(String format, Object arg) {
        }

        @Override
        public void error(String format, Object arg1, Object arg2) {
        }

        @Override
        public void error(String format, Object... arguments) {
        }

        @Override
        public void error(String msg, Throwable t) {
        }
    }
}
