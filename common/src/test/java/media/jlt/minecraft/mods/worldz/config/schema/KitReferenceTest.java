package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link Codecs#namedOrSection}, {@link Rule.KitReference}, {@link Codecs#sectionMap}
 * and {@link Rule.NestedMap} in isolation (DESIGN §44.3, §44.4.2; TODO 25.8a), since no real
 * {@code *Schema.java} binds either mechanism yet -- {@link StarterKitSchema#reference} isn't
 * called from any real site until TODO 25.8c/d, and {@code kits}' own root {@code Setting} isn't
 * declared until TODO 25.8b. Same synthetic-POJO style {@link SettingGroupTest} used for {@code
 * Setting.group} before any real section nested one.
 *
 * <p>{@link Rule.KitReference}'s {@code library} field is a {@code Function<SanitizeContext,
 * Map<String, StarterKitConfig>>} rather than the design's literal {@code ctx.root().kits} (DESIGN
 * §44.3.4) -- {@link SanitizeContext#root()} is concretely typed to {@code WorldzConfig}, which has
 * no {@code kits} field until TODO 25.8b, so that exact expression cannot compile here. The tests
 * below supply a synthetic library map directly, standing in for the eventual root coupling, while
 * still exercising {@link Rule.KitReference}'s real resolve/fallback/materialize logic.
 */
class KitReferenceTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final StarterKitSchema SITE_SCHEMA = new StarterKitSchema("site");
    private static final ValueCodec<StarterKitConfig> SITE_CODEC =
        Codecs.namedOrSection(SITE_SCHEMA, StarterKitConfig::reference, config -> config.ref);

    @Test
    void inlineMappingRoundTripsByteIdenticallyThroughNamedOrSection() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("essentials", List.of("minecraft:stick:1"));
        raw.put("extras", List.of("minecraft:coal:2"));
        raw.put("extrasCount", 1);

        StarterKitConfig parsed = SITE_CODEC.read(raw, new ParseContext(LOGGER));

        assertNull(parsed.ref, "an inline mapping must not become a reference");
        assertEquals(List.of("minecraft:stick:1"), parsed.essentials);
        assertEquals(List.of("minecraft:coal:2"), parsed.extras);
        assertEquals(1, parsed.extrasCount);

        Object written = SITE_CODEC.write(parsed);

        assertEquals(raw, written, "an inline mapping must round-trip byte-identically -- nothing downstream has moved");
    }

    @Test
    void stringValueReadsAsAReferenceWithReferencesEmptyStubState() {
        StarterKitConfig parsed = SITE_CODEC.read("cave-easy", new ParseContext(LOGGER));

        assertEquals("cave-easy", parsed.ref);
        assertTrue(parsed.essentials.isEmpty(), "reference(name) leaves essentials empty");
        assertTrue(parsed.extras.isEmpty(), "reference(name) leaves extras empty");
        assertEquals(0, parsed.extrasCount, "reference(name) leaves extrasCount at 0");
    }

    @Test
    void stringValueIsTrimmedBeforeBecomingAReference() {
        StarterKitConfig parsed = SITE_CODEC.read("  cave-easy  ", new ParseContext(LOGGER));

        assertEquals("cave-easy", parsed.ref);
    }

    @Test
    void referencedValueWritesBackAsTheBareName() {
        StarterKitConfig referenced = StarterKitConfig.reference("cave-easy");

        assertEquals("cave-easy", SITE_CODEC.write(referenced));
    }

    @Test
    void nonStringNonMapValueStillThrowsSchemaSectionsExactMessage() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> SITE_CODEC.read(42, new ParseContext(LOGGER))
        );

        assertEquals("site must be a mapping", exception.getMessage(), "SchemaSection.read's own message, unchanged");
    }

    @Test
    void knownReferenceMaterializesFreshCopiesFromTheLibraryOntoTheReferringInstance() {
        Map<String, StarterKitConfig> library = Map.of("cave-easy", libraryEntry(List.of("minecraft:torch:4"), List.of("minecraft:apple:1"), 1));
        Rule.KitReference<Object> rule = new Rule.KitReference<>(SITE_SCHEMA, ctx -> library, "cave-easy");
        StarterKitConfig referenced = StarterKitConfig.reference("cave-easy");

        StarterKitConfig result = rule.apply(new Object(), referenced, "site", new SanitizeContext(LOGGER, null));

        assertEquals("cave-easy", result.ref, "ref survives on the instance so write can re-emit the name");
        assertEquals(List.of("minecraft:torch:4"), result.essentials);
        assertEquals(List.of("minecraft:apple:1"), result.extras);
        assertEquals(1, result.extrasCount);
        assertNotSame(library.get("cave-easy").essentials, result.essentials, "materialized contents are fresh copies (DESIGN R11), never aliased");
    }

    @Test
    void unknownReferenceWarnsAndFallsBackToTheSitesOwnDefaultName() {
        Map<String, StarterKitConfig> library = Map.of("cave-easy", libraryEntry(List.of("minecraft:bread:1"), List.of(), 0));
        Rule.KitReference<Object> rule = new Rule.KitReference<>(SITE_SCHEMA, ctx -> library, "cave-easy");
        StarterKitConfig referenced = StarterKitConfig.reference("no-such-kit");

        StarterKitConfig result = rule.apply(new Object(), referenced, "site", new SanitizeContext(LOGGER, null));

        assertEquals("cave-easy", result.ref, "an unknown name is rewritten to the site's own default name");
        assertEquals(List.of("minecraft:bread:1"), result.essentials);
    }

    @Test
    void nullValueDefaultsToAReferenceToTheSitesOwnDefaultName() {
        Map<String, StarterKitConfig> library = Map.of("cave-easy", libraryEntry(List.of("minecraft:bread:1"), List.of(), 0));
        Rule.KitReference<Object> rule = new Rule.KitReference<>(SITE_SCHEMA, ctx -> library, "cave-easy");

        StarterKitConfig result = rule.apply(new Object(), null, "site", new SanitizeContext(LOGGER, null));

        assertEquals("cave-easy", result.ref);
        assertEquals(List.of("minecraft:bread:1"), result.essentials);
    }

    @Test
    void whenBothTheReferenceAndTheDefaultNameAreMissingFallsBackToInlineSanitize() {
        Rule.KitReference<Object> rule = new Rule.KitReference<>(SITE_SCHEMA, ctx -> Map.of(), "cave-easy");
        StarterKitConfig referenced = StarterKitConfig.reference("no-such-kit");

        StarterKitConfig result = rule.apply(new Object(), referenced, "site", new SanitizeContext(LOGGER, null));

        assertEquals("cave-easy", result.ref, "still rewritten to defaultName even though it is also undefined");
        assertTrue(result.essentials.isEmpty());
        assertEquals(0, result.extrasCount);
    }

    @Test
    void inlineValuesSanitizeThroughTheInlineSectionExactlyAsToday() {
        StarterKitConfig inline = new StarterKitConfig();
        inline.essentials = new ArrayList<>();
        inline.extras = new ArrayList<>();
        inline.extrasCount = 5;
        Rule.KitReference<Object> rule = new Rule.KitReference<>(SITE_SCHEMA, ctx -> Map.of(), "cave-easy");

        StarterKitConfig result = rule.apply(new Object(), inline, "site", new SanitizeContext(LOGGER, null));

        assertNull(result.ref);
        assertEquals(0, result.extrasCount, "StarterKitSchema.postValidate's extrasCount-vs-empty-extras clamp still fires");
    }

    @Test
    void sectionMapAndNestedMapRoundTripNamedEntriesWithIndividualSanitize() {
        StarterKitSchema entrySchema = new StarterKitSchema("kits");
        ValueCodec<Map<String, StarterKitConfig>> codec = Codecs.sectionMap(entrySchema);
        Rule<Object, Map<String, StarterKitConfig>> rule = new Rule.NestedMap<>(entrySchema);

        Map<String, Object> rawA = new LinkedHashMap<>();
        rawA.put("essentials", List.of("minecraft:stick:1"));
        rawA.put("extras", List.of());
        rawA.put("extrasCount", 3);
        Map<String, Object> rawB = new LinkedHashMap<>();
        rawB.put("essentials", List.of("minecraft:torch:2"));
        rawB.put("extras", List.of("minecraft:coal:1"));
        rawB.put("extrasCount", 1);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("kit-a", rawA);
        raw.put("kit-b", rawB);

        Map<String, StarterKitConfig> parsed = codec.read(raw, new ParseContext(LOGGER));

        assertEquals(List.of("kit-a", "kit-b"), List.copyOf(parsed.keySet()), "names preserved, in order");
        assertEquals(List.of("minecraft:stick:1"), parsed.get("kit-a").essentials);
        assertEquals(3, parsed.get("kit-a").extrasCount, "read is parse-only -- the clamp hasn't run yet");

        Map<String, StarterKitConfig> sanitized = rule.apply(new Object(), parsed, "kits", new SanitizeContext(LOGGER, null));

        assertEquals(0, sanitized.get("kit-a").extrasCount, "each entry sanitized individually -- the empty-extras clamp fired for kit-a");
        assertEquals(1, sanitized.get("kit-b").extrasCount, "kit-b's non-empty extras pool is left alone");

        Object written = codec.write(sanitized);

        assertTrue(written instanceof Map<?, ?>);
        @SuppressWarnings("unchecked")
        Map<String, Object> writtenMap = (Map<String, Object>) written;
        assertEquals(List.of("kit-a", "kit-b"), List.copyOf(writtenMap.keySet()), "names preserved on emit, in order");
        @SuppressWarnings("unchecked")
        Map<String, Object> writtenA = (Map<String, Object>) writtenMap.get("kit-a");
        assertEquals(0, writtenA.get("extrasCount"));
    }

    @Test
    void sectionMapRejectsANonMappingRawValue() {
        ValueCodec<Map<String, StarterKitConfig>> codec = Codecs.sectionMap(new StarterKitSchema("kits"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> codec.read(List.of("not", "a", "map"), new ParseContext(LOGGER).child("kits"))
        );

        assertEquals("kits must be a mapping", exception.getMessage());
    }

    @Test
    void nestedMapTreatsANullValueAsAnEmptyMap() {
        Rule<Object, Map<String, StarterKitConfig>> rule = new Rule.NestedMap<>(new StarterKitSchema("kits"));

        Map<String, StarterKitConfig> sanitized = rule.apply(new Object(), null, "kits", new SanitizeContext(LOGGER, null));

        assertFalse(sanitized == null);
        assertTrue(sanitized.isEmpty());
    }

    private static StarterKitConfig libraryEntry(List<String> essentials, List<String> extras, int extrasCount) {
        StarterKitConfig entry = new StarterKitConfig();
        entry.essentials = new ArrayList<>(essentials);
        entry.extras = new ArrayList<>(extras);
        entry.extrasCount = extrasCount;
        return entry;
    }
}
