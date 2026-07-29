package media.jlt.minecraft.mods.worldz.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Exercises {@link KitLibrary#shipped()} in isolation, before anything in the real schema depends
 * on it (DESIGN §44.4.1/§44.5, §44.8 row b; TODO 25.8b).
 */
class KitLibraryTest {
    /** DESIGN §44.5's table, in declaration order. */
    private static final List<String> SHIPPED_NAMES = List.of(
        "cave-easy", "cave-medium", "cave-hard",
        "sky-island-easy", "sky-island-medium", "sky-island-hard",
        "nether-start-easy", "nether-start-medium", "nether-start-hard",
        "end-start-easy", "end-start-medium", "end-start-hard",
        "ocean-island-default", "floating-islands-loot"
    );

    @Test
    void shippedHasExactlyTheFourteenNamesInOrder() {
        LinkedHashMap<String, StarterKitConfig> kits = KitLibrary.shipped();

        assertEquals(SHIPPED_NAMES, List.copyOf(kits.keySet()));
        assertEquals(Set.copyOf(SHIPPED_NAMES), kits.keySet());
    }

    /**
     * {@code ocean-island-default} is deliberately not a moved factory method at all (DESIGN
     * §44.3.2/§44.4.1) -- it is the existing no-arg {@link StarterKitConfig} constructor's own
     * defaults, load-bearing for partial-inline kits.
     */
    @Test
    void oceanIslandDefaultIsTheBareConstructorDefaultNotAMovedFactory() {
        StarterKitConfig constructed = new StarterKitConfig();

        assertKitEquals(constructed, KitLibrary.shipped().get("ocean-island-default"));
    }

    /**
     * DESIGN §44.4.1's "fresh {@code LinkedHashMap} copy each call": two calls must never alias
     * either the map itself or any entry's own list fields, so sanitizing one config load's {@code
     * kits} map (which mutates entries in place, e.g. {@code StarterKitSchema.postValidate}'s
     * clamp) can never leak into another load.
     */
    @Test
    void eachCallReturnsFreshInstancesNeverAliasedWithAnotherCall() {
        LinkedHashMap<String, StarterKitConfig> first = KitLibrary.shipped();
        LinkedHashMap<String, StarterKitConfig> second = KitLibrary.shipped();

        assertNotSame(first, second);
        for (String name : SHIPPED_NAMES) {
            StarterKitConfig a = first.get(name);
            StarterKitConfig b = second.get(name);
            assertNotSame(a, b, name + ": two calls must not share the same StarterKitConfig instance");
            assertNotSame(a.essentials, b.essentials, name + ": two calls must not share the same essentials list");
            assertNotSame(a.extras, b.extras, name + ": two calls must not share the same extras list");
        }

        first.get("cave-easy").essentials.clear();
        assertEquals(6, second.get("cave-easy").essentials.size(), "mutating one call's entry must not affect the other's");
    }

    private static void assertKitEquals(StarterKitConfig expected, StarterKitConfig actual) {
        assertEquals(expected.essentials, actual.essentials);
        assertEquals(expected.extras, actual.extras);
        assertEquals(expected.extrasCount, actual.extrasCount);
    }
}
