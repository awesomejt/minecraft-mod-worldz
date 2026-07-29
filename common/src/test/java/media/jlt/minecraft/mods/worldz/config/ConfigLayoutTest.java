package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.Applicability;
import media.jlt.minecraft.mods.worldz.config.schema.Setting;
import media.jlt.minecraft.mods.worldz.config.schema.WorldzRootSchema;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@link ConfigLayout#FILES} totally and correctly partitions {@link
 * WorldzRootSchema#declare()}'s 27 root keys (DESIGN §43.9 row a, TODO 25.7a; {@code kits} added
 * at TODO 25.8b). No load-path change is exercised here -- that is TODO 25.7b's {@code
 * ConfigDirectoryLoadTest}.
 */
class ConfigLayoutTest {
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();
    private static final Path WORLD_PRESET_DIRECTORY = Path.of("src/main/resources/data/jlt_worldz/worldgen/world_preset");

    // The one deliberate filename-vs-owned-key mismatch (DESIGN §43.2 row 17): world-types/
    // sky-chunk.yaml owns chunkIsland, not "chunkIsland" converted to its own filename/preset id
    // (which would be chunk_island, a preset that doesn't exist). Allow-listed by exact root key,
    // mirroring ConfigSchemaMetadataTest.BLOCKS_SUFFIX_ALLOW_LIST's "allow-list the one deliberate
    // exception" style rather than weakening the check for every unwrapped file.
    private static final Set<String> FILENAME_KEY_MISMATCH_ALLOW_LIST = Set.of("chunkIsland");

    @Test
    void filesExactlyPartitionTheRootSchemasKeys() {
        List<String> rootKeys = ROOT.settings().stream().map(Setting::key).toList();
        List<String> owned = ConfigLayout.FILES.stream().flatMap(file -> file.rootKeys().stream()).toList();

        assertEquals(
            new LinkedHashSet<>(rootKeys).size(), rootKeys.size(), "WorldzRootSchema.declare() itself has a duplicate key"
        );
        assertEquals(
            new LinkedHashSet<>(owned).size(), owned.size(), "ConfigLayout.FILES owns a root key more than once"
        );
        assertEquals(
            new LinkedHashSet<>(rootKeys), new LinkedHashSet<>(owned), "ConfigLayout.FILES does not exactly partition the 27 root keys"
        );
    }

    @Test
    void everyUnwrappedFileOwnsExactlyOneKey() {
        for (ConfigFile file : ConfigLayout.FILES) {
            if (file.unwrapped()) {
                assertEquals(
                    1, file.rootKeys().size(), file.relativePath() + " is unwrapped but does not own exactly one key"
                );
            }
        }
    }

    @Test
    void owningFindsTheRightFileForEveryRootKeyAndNothingElse() {
        for (ConfigFile file : ConfigLayout.FILES) {
            for (String key : file.rootKeys()) {
                assertEquals(Optional.of(file), ConfigLayout.owning(key), "owning(\"" + key + "\") did not resolve to " + file.relativePath());
            }
        }
        assertEquals(Optional.empty(), ConfigLayout.owning("notARealRootKey"));
    }

    @Test
    void everyWorldTypeFilenameNamesARealWorldPresetJsonId() {
        for (ConfigFile file : ConfigLayout.FILES) {
            if (!file.relativePath().startsWith("world-types/")) {
                continue;
            }
            String presetId = presetIdFromFilename(file);
            assertTrue(
                Files.exists(WORLD_PRESET_DIRECTORY.resolve(presetId + ".json")),
                file.relativePath() + " names preset id '" + presetId + "', which has no world_preset/*.json"
            );
        }
    }

    @Test
    void everyUnwrappedWorldTypeFilenameMatchesItsOwnedKeyExceptTheAllowListedException() {
        for (ConfigFile file : ConfigLayout.FILES) {
            if (!file.unwrapped() || !file.relativePath().startsWith("world-types/")) {
                continue;
            }
            String ownedKey = file.rootKeys().get(0);
            String expectedPresetId = camelToSnake(ownedKey);
            String actualPresetId = presetIdFromFilename(file);
            if (FILENAME_KEY_MISMATCH_ALLOW_LIST.contains(ownedKey)) {
                continue;
            }
            assertEquals(
                expectedPresetId, actualPresetId, file.relativePath() + " does not match its owned key '" + ownedKey + "'"
            );
        }

        // The allow-listed exception itself, pinned by exact name so any *other* mismatch still
        // fails the loop above.
        ConfigFile skyChunk = ConfigLayout.owning("chunkIsland").orElseThrow();
        assertEquals("world-types/sky-chunk.yaml", skyChunk.relativePath());
    }

    @Test
    void applicabilityAgreesWithConfigLayoutsFileAssignment() {
        for (ConfigFile file : ConfigLayout.FILES) {
            for (String key : file.rootKeys()) {
                Applicability applies = settingFor(key).applies();
                if (file.relativePath().equals("runtime.yaml")) {
                    assertEquals(Applicability.Scope.LIVE, applies.scope(), key + " should be live (runtime.yaml)");
                } else if (file.relativePath().equals("world-defaults.yaml")) {
                    assertEquals(
                        Applicability.Scope.WORLD_DEFAULT, applies.scope(), key + " should be a world default (world-defaults.yaml)"
                    );
                } else if (file.relativePath().equals("kits.yaml")) {
                    // Kit contents are read once, at first spawn, by StarterKitDeployment, for
                    // whichever preset is active -- not re-read live like runtime.yaml's three
                    // sections, and not scoped to one preset (DESIGN §44.4.4).
                    assertEquals(
                        Applicability.Scope.WORLD_DEFAULT, applies.scope(), key + " should be a world default (kits.yaml)"
                    );
                } else if (file.relativePath().startsWith("world-types/")) {
                    assertEquals(Applicability.Scope.PRESET, applies.scope(), key + " should be preset-scoped (" + file.relativePath() + ")");
                    assertEquals(
                        expectedPresets(file), applies.presets(), key + "'s Applicability presets don't match " + file.relativePath()
                    );
                } else {
                    throw new AssertionError("unrecognized file: " + file.relativePath());
                }
            }
        }
    }

    /** Every {@code world-types/*.yaml} key's expected {@link Applicability#presets()}: empty for
     * the generic {@code worldz.yaml} (the "generic preset only" convention), the file's own preset
     * id(s) otherwise -- {@code strip-world.yaml} covers both {@code strip_world} presets' keys. */
    private static Set<String> expectedPresets(ConfigFile file) {
        if (file.relativePath().equals("world-types/worldz.yaml")) {
            return Set.of();
        }
        return Set.of(presetIdFromFilename(file));
    }

    private static Setting<?, ?> settingFor(String key) {
        return ROOT.settings().stream()
            .filter(setting -> setting.key().equals(key))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no root setting declared for key '" + key + "'"));
    }

    private static String presetIdFromFilename(ConfigFile file) {
        String basename = file.relativePath().substring(file.relativePath().lastIndexOf('/') + 1);
        String withoutExtension = basename.substring(0, basename.length() - ".yaml".length());
        return withoutExtension.replace('-', '_');
    }

    /** Converts a camelCase root key (e.g. {@code "singleBiome"}) to its snake_case preset id
     * (e.g. {@code "single_biome"}). */
    private static String camelToSnake(String camelCase) {
        StringBuilder builder = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                builder.append('_').append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
