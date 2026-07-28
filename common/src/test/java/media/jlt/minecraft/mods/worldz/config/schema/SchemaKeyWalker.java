package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ConfigFile;
import media.jlt.minecraft.mods.worldz.config.ConfigLayout;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared dotted-key walker over a {@link SchemaSection} tree (DESIGN §42.5; TODO 25.6a), recursing
 * into nested sections via {@link Rule.Nested} exactly as the schema itself does at parse/sanitize
 * time. Promoted out of {@code ConfigSchemaMetadataTest}, whose {@code theSchemasDeclaredKeysExactly
 * MatchWhatToMapEmits} test was the original (and, until now, only) caller, so {@link
 * ConfigFixturesTest} can reuse the same walk against real {@code config/tests/*.yaml} fixtures
 * instead of a fresh implementation.
 */
final class SchemaKeyWalker {
    private SchemaKeyWalker() {
    }

    /**
     * Walks the schema tree in lockstep with the real {@link SchemaSection#toMap} output for a
     * fully-populated value, collecting every leaf setting's full dotted path into {@code keys} and
     * recording any structural mismatch (a declared setting {@code toMap()} doesn't emit, or a
     * {@code toMap()} key with no declaring setting) into {@code problems}.
     *
     * @param section the schema section to walk
     * @param value a real instance of {@code S}, for {@code toMap()} to render
     * @param keys collects every leaf setting's full dotted path
     * @param problems collects any declare()/toMap() mismatch, dotted-path-prefixed
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void collectKeys(SchemaSection section, Object value, List<String> keys, List<String> problems) {
        Map<String, Object> map = section.toMap(value);
        List<Setting> settings = section.settings();
        for (Object settingObject : settings) {
            Setting setting = (Setting) settingObject;
            String key = (String) setting.key();
            String fullPath = childPath(section, key);
            if (!map.containsKey(key)) {
                problems.add(fullPath + ": declared in the schema but toMap() does not emit it");
                continue;
            }
            Object rule = setting.rule();
            if (rule instanceof Rule.Nested nested && nested.section() instanceof SchemaSection childSchema) {
                Object childValue = setting.accessor().get(value);
                collectKeys(childSchema, childValue, keys, problems);
            } else {
                keys.add(fullPath);
            }
        }
        for (Object mapKey : map.keySet()) {
            String key = (String) mapKey;
            boolean declared = settings.stream().anyMatch(candidate -> ((Setting) candidate).key().equals(key));
            if (!declared) {
                problems.add(childPath(section, key) + ": toMap() emits it but no setting declares it");
            }
        }
    }

    /**
     * Walks a raw YAML-loaded map in lockstep with the schema tree, recursing into a nested section
     * only where the schema itself declares a {@link Rule.Nested} setting for that key <em>and</em>
     * the file's own value at that key is a mapping -- never based on the raw value's shape alone.
     * That distinction matters: {@code layout.roleOverrides} is a leaf {@code stringMap} setting
     * whose YAML value is itself a mapping with arbitrary user-defined keys, and must not be mistaken
     * for a nested section whose sub-keys need their own declaring settings.
     *
     * @param section the schema section to walk
     * @param fileMap the raw map loaded from a fixture at this same nesting level
     * @param unknownKeys collects the full dotted path of every file key the schema does not declare
     *     at its exact nesting level
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void findUnknownKeys(SchemaSection section, Map<?, ?> fileMap, List<String> unknownKeys) {
        List<Setting> settings = section.settings();
        for (Map.Entry<?, ?> entry : fileMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Setting matching = settings.stream()
                .filter(candidate -> ((Setting) candidate).key().equals(key))
                .findFirst()
                .orElse(null);
            if (matching == null) {
                unknownKeys.add(childPath(section, key));
                continue;
            }
            Object rule = matching.rule();
            if (rule instanceof Rule.Nested nested && nested.section() instanceof SchemaSection childSchema
                && entry.getValue() instanceof Map<?, ?> childMap) {
                findUnknownKeys(childSchema, childMap, unknownKeys);
            }
        }
    }

    /**
     * Per-file variant of {@link #findUnknownKeys} (DESIGN §43.5, TODO 25.7c): the split introduces
     * a new failure mode the whole-root walk can't diagnose -- "right key, wrong file" -- so this
     * separates that case ({@code misfiled}) out from a genuinely unknown key ({@code unknown})
     * rather than lumping both together.
     *
     * <ul>
     *   <li><strong>unwrapped</strong> ({@code file.unwrapped()}): the file's one owned root key
     *       resolves to a {@code Setting} whose {@link Rule.Nested} child section is the file's
     *       entire shape -- {@code fileMap} <em>is</em> that section's body directly, so it's handed
     *       straight to {@link #findUnknownKeys} against just that child section, never the whole
     *       root (e.g. {@code cave.yaml} is checked against {@code CaveSchema} only).
     *   <li><strong>wrapped</strong>: each of {@code fileMap}'s own root-level keys is looked up
     *       against the keys {@code file} owns. Owned -&gt; recurse exactly as {@link
     *       #findUnknownKeys} does today, including its {@link Rule.Nested} + value-is-a-map guard
     *       (the guard that keeps {@code layout.roleOverrides}'s arbitrary user-defined sub-keys from
     *       being mistaken for a nested section). Not owned by this file but owned by another
     *       ({@link ConfigLayout#owning}) -&gt; {@code misfiled}. Owned by no file at all -&gt;
     *       {@code unknown}.
     * </ul>
     *
     * @param root the whole-config root schema, for resolving a root key to its {@code Setting}
     * @param file the {@link ConfigFile} {@code fileMap} was loaded from
     * @param fileMap the raw map loaded from that file's root
     * @param unknown collects the dotted path of every key neither {@code file} nor any other
     *     {@link ConfigLayout} file declares
     * @param misfiled collects a human-readable "key belongs in otherFile" entry for every key a
     *     <em>different</em> {@link ConfigLayout} file owns
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void findUnknownKeysInFile(
        WorldzRootSchema root, ConfigFile file, Map<?, ?> fileMap, List<String> unknown, List<String> misfiled
    ) {
        List<Setting> rootSettings = (List<Setting>) (List<?>) root.settings();
        if (file.unwrapped()) {
            Setting matching = rootSettingFor(rootSettings, file.rootKeys().get(0));
            recurseIfNested(matching, fileMap, unknown);
            return;
        }
        for (Map.Entry<?, ?> entry : fileMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (!file.rootKeys().contains(key)) {
                Optional<ConfigFile> owner = ConfigLayout.owning(key);
                if (owner.isPresent()) {
                    misfiled.add(key + " belongs in " + owner.get().relativePath());
                } else {
                    unknown.add(key);
                }
                continue;
            }
            Setting matching = rootSettingFor(rootSettings, key);
            if (matching != null && entry.getValue() instanceof Map<?, ?> childMap) {
                recurseIfNested(matching, childMap, unknown);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static Setting rootSettingFor(List<Setting> rootSettings, String key) {
        return rootSettings.stream()
            .filter(candidate -> ((Setting) candidate).key().equals(key))
            .findFirst()
            .orElse(null);
    }

    @SuppressWarnings("rawtypes")
    private static void recurseIfNested(Setting matching, Map<?, ?> childMap, List<String> unknownKeys) {
        if (matching == null) {
            return;
        }
        Object rule = matching.rule();
        if (rule instanceof Rule.Nested nested && nested.section() instanceof SchemaSection childSchema) {
            findUnknownKeys(childSchema, childMap, unknownKeys);
        }
    }

    static String childPath(SchemaSection<?> section, String key) {
        return section.path().isEmpty() ? key : section.path() + "." + key;
    }
}
