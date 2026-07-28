package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Permanent completeness gate (DESIGN §41.8's closing paragraph; TODO 25.2h). With the legacy path
 * and its differential harness retired, this -- plus the golden {@code reference-defaults.yaml}
 * test -- is what keeps the schema honest from here on:
 *
 * <ul>
 *   <li>every {@link Setting} reachable from {@link WorldzRootSchema}, recursing into nested
 *       sections via {@link Rule.Nested}, must carry a non-blank doc, a non-null
 *       {@link Applicability}, and (for numeric settings) a deliberate {@link Unit};
 *   <li>the schema's own declared keys must exactly match what {@link SchemaSection#toMap} really
 *       emits at every nesting level, for a fully-populated default config -- not merely what
 *       {@link SchemaSection#declare()} claims.
 * </ul>
 *
 * This is the shape of bug F6 measured (CONFIG-RESTRUCTURE.md: "12 of 25 sections undocumented")
 * -- caught mechanically from here on instead of by inspection.
 */
class ConfigSchemaMetadataTest {
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();

    // TODO 25.6's own naming sweep (§2/§42.3) drops `Blocks` from every leaf key it touches --
    // `Chunks`/`Days`/`Y`/`Chance` stay, because they're units/axis-names, not the banned suffix.
    // `strip.widthRadiusBlocks` is the sole deliberate holdout (DESIGN §42.1: renaming it now, then
    // again to `width` with new absolute semantics at TODO 25.9, would cost two fixture migrations
    // for a key whose meaning is about to change anyway) -- allow-listed here rather than exempting
    // `strip` wholesale, so any other stray `Blocks` leaf still fails this test.
    private static final Set<String> BLOCKS_SUFFIX_ALLOW_LIST = Set.of("strip.widthRadiusBlocks");

    @Test
    void everySettingHasADocAUnitAndAnApplicability() {
        List<String> problems = new ArrayList<>();
        collectMetadataProblems(ROOT, problems);
        assertTrue(
            problems.isEmpty(),
            () -> problems.size() + " setting(s) with missing/incomplete metadata:\n" + String.join("\n", problems)
        );
    }

    @Test
    void theSchemasDeclaredKeysExactlyMatchWhatToMapEmits() {
        List<String> keys = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        WorldzConfig defaults = ROOT.defaults();
        SchemaKeyWalker.collectKeys(ROOT, defaults, keys, problems);
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));

        Set<String> distinctKeys = new LinkedHashSet<>(keys);
        assertEquals(distinctKeys.size(), keys.size(), "duplicate dotted setting key(s) in the schema: " + keys);
    }

    /**
     * TODO 25.6h close-out gate: no leaf key introduced by the restructure may still carry the
     * dropped {@code Blocks} suffix (DESIGN §42.3's naming sweep), except {@link
     * #BLOCKS_SUFFIX_ALLOW_LIST}'s one deliberate holdout. This walks the same full schema tree
     * {@link #theSchemasDeclaredKeysExactlyMatchWhatToMapEmits} does, so it catches a miss anywhere
     * in the tree, not just in the sections an individual 25.6 sub-step happened to touch.
     */
    @Test
    void noLeafKeyEndsInTheDroppedBlocksSuffix() {
        List<String> keys = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        WorldzConfig defaults = ROOT.defaults();
        SchemaKeyWalker.collectKeys(ROOT, defaults, keys, problems);
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));

        List<String> offenders = keys.stream()
            .filter(key -> key.endsWith("Blocks"))
            .filter(key -> !BLOCKS_SUFFIX_ALLOW_LIST.contains(key))
            .toList();
        assertTrue(
            offenders.isEmpty(),
            () -> offenders.size() + " leaf key(s) still carry the dropped `Blocks` suffix (DESIGN §42.3): " + offenders
        );
    }

    /** Walks every setting, recursing into nested sections, checking doc/unit/applicability. */
    private static void collectMetadataProblems(SchemaSection<?> section, List<String> problems) {
        for (Setting<?, ?> setting : section.settings()) {
            String fullPath = childPath(section, setting.key());
            checkMetadata(setting, fullPath, problems);
            if (setting.rule() instanceof Rule.Nested<?, ?> nested && nested.section() instanceof SchemaSection<?> child) {
                collectMetadataProblems(child, problems);
            }
        }
    }

    private static void checkMetadata(Setting<?, ?> setting, String fullPath, List<String> problems) {
        Docs docs = setting.docs();
        if (docs.doc() == null || docs.doc().isBlank()) {
            problems.add(fullPath + ": missing doc");
        }
        if (setting.applies() == null) {
            problems.add(fullPath + ": missing applicability");
        }
        // Every numeric setting must declare a real Unit; Unit.NONE is only ever intentional for
        // shapes that genuinely have none (flags, enums, free text, ids, lists) -- DESIGN §41.2.
        if ((setting.codec() == Codecs.INT || setting.codec() == Codecs.DOUBLE) && docs.unit() == Unit.NONE) {
            problems.add(fullPath + ": numeric setting has no Unit");
        }
    }

    private static String childPath(SchemaSection<?> section, String key) {
        return section.path().isEmpty() ? key : section.path() + "." + key;
    }
}
