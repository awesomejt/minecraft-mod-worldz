package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterKitPlanTest {
    private static final StarterKitPlan.ItemAmount LILY_PAD = StarterKitPlan.ItemAmount.parse("minecraft:lily_pad:1");
    private static final StarterKitPlan.ItemAmount DIRT = StarterKitPlan.ItemAmount.parse("minecraft:dirt:4");
    private static final StarterKitPlan.ItemAmount BREAD = StarterKitPlan.ItemAmount.parse("minecraft:bread:3");
    private static final StarterKitPlan.ItemAmount TORCH = StarterKitPlan.ItemAmount.parse("minecraft:torch:8");

    @Test
    void itemAmountParsesIdAndCount() {
        StarterKitPlan.ItemAmount amount = StarterKitPlan.ItemAmount.parse("minecraft:dirt:4");
        assertEquals("minecraft:dirt", amount.itemId());
        assertEquals(4, amount.count());
    }

    @Test
    void itemAmountWithoutCountDefaultsToOne() {
        StarterKitPlan.ItemAmount amount = StarterKitPlan.ItemAmount.parse("minecraft:lily_pad");
        assertEquals("minecraft:lily_pad", amount.itemId());
        assertEquals(1, amount.count());
    }

    @Test
    void itemAmountRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> new StarterKitPlan.ItemAmount("", 1));
    }

    @Test
    void itemAmountRejectsNonPositiveCount() {
        assertThrows(IllegalArgumentException.class, () -> new StarterKitPlan.ItemAmount("minecraft:dirt", 0));
        assertThrows(IllegalArgumentException.class, () -> new StarterKitPlan.ItemAmount("minecraft:dirt", -1));
    }

    @Test
    void resolveAlwaysIncludesEssentials() {
        StarterKitPlan plan = new StarterKitPlan(List.of(LILY_PAD, DIRT), List.of(BREAD), 0);
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(42L);
        assertEquals(List.of(LILY_PAD, DIRT), resolved);
    }

    @Test
    void resolvePicksExtrasCountItemsFromThePool() {
        StarterKitPlan plan = new StarterKitPlan(List.of(LILY_PAD), List.of(BREAD, TORCH), 2);
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(42L);
        assertEquals(3, resolved.size());
        assertEquals(LILY_PAD, resolved.get(0));
        assertTrue(List.of(BREAD, TORCH).contains(resolved.get(1)));
        assertTrue(List.of(BREAD, TORCH).contains(resolved.get(2)));
    }

    @Test
    void resolveIsDeterministic() {
        StarterKitPlan plan = new StarterKitPlan(List.of(LILY_PAD), List.of(BREAD, TORCH), 2);
        assertEquals(plan.resolve(42L), plan.resolve(42L));
    }

    @Test
    void resolveCanDifferAcrossSeeds() {
        StarterKitPlan plan = new StarterKitPlan(List.of(), List.of(BREAD, TORCH), 20);
        boolean anyDifference = false;
        for (long seed = 1; seed <= 20; seed++) {
            if (!plan.resolve(1L).equals(plan.resolve(seed))) {
                anyDifference = true;
                break;
            }
        }
        assertTrue(anyDifference, "expected at least one seed to pick a different sequence of extras");
    }

    @Test
    void negativeExtrasCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StarterKitPlan(List.of(LILY_PAD), List.of(BREAD), -1));
    }

    @Test
    void positiveExtrasCountWithEmptyPoolIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StarterKitPlan(List.of(LILY_PAD), List.of(), 1));
    }

    @Test
    void zeroExtrasCountWithEmptyPoolIsAllowed() {
        StarterKitPlan plan = new StarterKitPlan(List.of(LILY_PAD), List.of(), 0);
        assertEquals(List.of(LILY_PAD), plan.resolve(42L));
    }
}
