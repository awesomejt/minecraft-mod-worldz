package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatingIslandsPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        FloatingIslandsPlan disabled = FloatingIslandsPlan.disabled();
        assertFalse(disabled.enabled());
        assertFalse(disabled.exclusionZone().enabled());
        assertFalse(disabled.oreDepositsEnabled());
        assertFalse(disabled.lootChestEnabled());
    }

    @Test
    void disabledPlanNeverHitsRegardlessOfSpawnChance() {
        FloatingIslandsPlan plan = plan(true, 1.0, false, 256, new IslandPlan.ExclusionZone(false, 0));
        FloatingIslandsPlan disabled = new FloatingIslandsPlan(
            false, plan.minRadiusBlocks(), plan.maxRadiusBlocks(), plan.shapeAmplitude(), plan.cellSizeBlocks(),
            plan.spawnChance(), plan.biomeVariety(), plan.islandBiomes(), plan.exclusionZone(), false, false
        );
        for (int x = -2000; x <= 2000; x += 400) {
            assertFalse(disabled.at(x, 0, 42L, "minecraft:plains").present());
        }
    }

    @Test
    void zeroSpawnChanceNeverHits() {
        FloatingIslandsPlan plan = plan(true, 0.0, false, 256, new IslandPlan.ExclusionZone(false, 0));
        for (int x = -2000; x <= 2000; x += 400) {
            assertFalse(plan.at(x, 0, 42L, "minecraft:plains").present());
        }
    }

    @Test
    void fullSpawnChanceAlwaysHitsSomewhereNearEveryCellCenter() {
        // Radius large enough to guarantee the unjittered cell center is covered regardless of
        // where jitter (bounded to 0.3 * cellSizeBlocks per axis) actually placed the island.
        FloatingIslandsPlan plan = new FloatingIslandsPlan(
            true, 150, 150, 0.0, 256, 1.0, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        );
        for (int cellIndex = -5; cellIndex <= 5; cellIndex++) {
            int x = cellIndex * 256 + 128;
            assertTrue(plan.at(x, 128, 42L, "minecraft:plains").present(), "expected a hit near cell " + cellIndex);
        }
    }

    @Test
    void exclusionZoneSuppressesIslandsInsideItsRadius() {
        FloatingIslandsPlan withZone = plan(true, 1.0, false, 64, new IslandPlan.ExclusionZone(true, 100_000));
        FloatingIslandsPlan.Hit hit = withZone.at(128, 128, 42L, "minecraft:plains");
        assertFalse(hit.present());
    }

    @Test
    void biomeVarietyPicksFromThePoolInsteadOfTheFallback() {
        FloatingIslandsPlan plan = new FloatingIslandsPlan(
            true, 100, 100, 0.0, 256, 1.0, true, List.of("minecraft:desert"), new IslandPlan.ExclusionZone(false, 0), false, false
        );
        FloatingIslandsPlan.Hit hit = plan.at(128, 128, 42L, "minecraft:plains");
        assertTrue(hit.present());
        assertEquals("minecraft:desert", hit.biome());
    }

    @Test
    void noBiomeVarietyUsesTheFallbackBiome() {
        FloatingIslandsPlan plan = new FloatingIslandsPlan(
            true, 100, 100, 0.0, 256, 1.0, false, List.of("minecraft:desert"), new IslandPlan.ExclusionZone(false, 0), false, false
        );
        FloatingIslandsPlan.Hit hit = plan.at(128, 128, 42L, "minecraft:plains");
        assertTrue(hit.present());
        assertEquals("minecraft:plains", hit.biome());
    }

    @Test
    void invalidRadiusRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FloatingIslandsPlan(
            true, 7, 32, 0.3, 256, 0.5, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> new FloatingIslandsPlan(
            true, 64, 32, 0.3, 256, 0.5, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        ));
    }

    @Test
    void invalidSpawnChanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FloatingIslandsPlan(
            true, 16, 32, 0.3, 256, -0.1, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> new FloatingIslandsPlan(
            true, 16, 32, 0.3, 256, 1.1, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        ));
    }

    @Test
    void biomeVarietyWithNoBiomesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FloatingIslandsPlan(
            true, 16, 32, 0.3, 256, 0.5, true, List.of(), new IslandPlan.ExclusionZone(false, 0), false, false
        ));
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        FloatingIslandsConfig config = new FloatingIslandsConfig();
        config.enabled = true;
        config.minRadiusBlocks = 20;
        config.maxRadiusBlocks = 40;
        config.oreDepositsEnabled = true;
        config.lootChestEnabled = true;

        FloatingIslandsPlan plan = FloatingIslandsPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(20, plan.minRadiusBlocks());
        assertEquals(40, plan.maxRadiusBlocks());
        assertTrue(plan.oreDepositsEnabled());
        assertTrue(plan.lootChestEnabled());
    }

    @Test
    void fromTextParsesDecimalFieldsAndSplitsBiomes() {
        FloatingIslandsPlan plan = FloatingIslandsPlan.fromText(
            true, "20", "40", "0.4", "300", "0.7", true, "minecraft:plains, minecraft:desert\nminecraft:taiga",
            true, "500", true, true
        );

        assertEquals(20, plan.minRadiusBlocks());
        assertEquals(40, plan.maxRadiusBlocks());
        assertEquals(0.4, plan.shapeAmplitude());
        assertEquals(300, plan.cellSizeBlocks());
        assertEquals(0.7, plan.spawnChance());
        assertEquals(List.of("minecraft:plains", "minecraft:desert", "minecraft:taiga"), plan.islandBiomes());
        assertTrue(plan.exclusionZone().enabled());
        assertEquals(500, plan.exclusionZone().radiusBlocks());
        assertTrue(plan.oreDepositsEnabled());
        assertTrue(plan.lootChestEnabled());
    }

    @Test
    void fromTextRejectsNonNumericRadius() {
        assertThrows(IllegalArgumentException.class, () -> FloatingIslandsPlan.fromText(
            true, "not-a-number", "40", "0.3", "256", "0.5", false, "minecraft:plains", false, "256", false, false
        ));
    }

    @Test
    void islandBiomesTextJoinsWithNewlines() {
        FloatingIslandsPlan plan = new FloatingIslandsPlan(
            true, 16, 32, 0.3, 256, 0.5, true, List.of("minecraft:plains", "minecraft:desert"),
            new IslandPlan.ExclusionZone(false, 0), false, false
        );
        assertEquals("minecraft:plains\nminecraft:desert", plan.islandBiomesText());
    }

    @Test
    void nearbyIslandsFindsThePresentIslandRegardlessOfJitter() {
        FloatingIslandsPlan plan = new FloatingIslandsPlan(
            true, 16, 32, 0.1, 256, 1.0, false, List.of("minecraft:plains"), new IslandPlan.ExclusionZone(false, 0), false, false
        );
        List<FloatingIslandsPlan.ResolvedIsland> nearby = plan.nearbyIslands(128, 128, 42L, "minecraft:plains");
        assertEquals(9, nearby.size());
        assertTrue(nearby.stream().anyMatch(island -> island.radius() >= 16 && island.radius() <= 32));
    }

    @Test
    void nearbyIslandsIsEmptyWhenDisabled() {
        FloatingIslandsPlan plan = FloatingIslandsPlan.disabled();
        assertTrue(plan.nearbyIslands(128, 128, 42L, "minecraft:plains").isEmpty());
    }

    @Test
    void resolvedIslandPickIsDeterministicAndWithinBounds() {
        FloatingIslandsPlan.ResolvedIsland island = new FloatingIslandsPlan.ResolvedIsland(100.0, -50.0, 20.0, "minecraft:plains");
        List<String> candidates = List.of("minecraft:ore_coal", "minecraft:ore_iron_small", "minecraft:ore_gold_buried");

        String first = island.pick(candidates, 42L, "ore_feature");
        String second = island.pick(candidates, 42L, "ore_feature");

        assertTrue(candidates.contains(first));
        assertEquals(first, second);
    }

    @Test
    void resolvedIslandPickRejectsEmptyCandidates() {
        FloatingIslandsPlan.ResolvedIsland island = new FloatingIslandsPlan.ResolvedIsland(0.0, 0.0, 20.0, "minecraft:plains");
        assertThrows(IllegalArgumentException.class, () -> island.pick(List.of(), 42L, "ore_feature"));
    }

    @Test
    void resolvedIslandPickYStaysWithinTheInclusiveRange() {
        FloatingIslandsPlan.ResolvedIsland island = new FloatingIslandsPlan.ResolvedIsland(12.0, 34.0, 20.0, "minecraft:plains");
        for (long seed = 0; seed < 20; seed++) {
            int y = island.pickY(58, 63, seed, "ore_y");
            assertTrue(y >= 58 && y <= 63, "expected y in [58,63], got " + y);
        }
    }

    @Test
    void resolvedIslandPickYRejectsInvertedRange() {
        FloatingIslandsPlan.ResolvedIsland island = new FloatingIslandsPlan.ResolvedIsland(0.0, 0.0, 20.0, "minecraft:plains");
        assertThrows(IllegalArgumentException.class, () -> island.pickY(63, 58, 42L, "ore_y"));
    }

    @Test
    void guaranteedVillageSiteStructureIdMatchesTheKnownVillageVariants() {
        List<String> knownStructureIds = List.of(
            "minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
            "minecraft:village_snowy", "minecraft:village_taiga"
        );
        FloatingIslandsPlan plan = plan(true, 0.6, false, 256, new IslandPlan.ExclusionZone(true, 256));
        for (long seed = 0; seed < 10; seed++) {
            FloatingIslandsPlan.VillageSite site = plan.guaranteedVillageSite(seed);
            assertTrue(knownStructureIds.contains(site.structureId()), "unexpected structure id " + site.structureId());
        }
    }

    @Test
    void guaranteedVillageSiteIsDeterministic() {
        FloatingIslandsPlan plan = plan(true, 0.6, false, 256, new IslandPlan.ExclusionZone(true, 256));
        FloatingIslandsPlan.VillageSite first = plan.guaranteedVillageSite(42L);
        FloatingIslandsPlan.VillageSite second = plan.guaranteedVillageSite(42L);
        assertEquals(first, second);
    }

    @Test
    void guaranteedVillageSiteIsAlwaysBeyondTheExclusionZone() {
        FloatingIslandsPlan plan = plan(true, 0.6, false, 256, new IslandPlan.ExclusionZone(true, 1000));
        for (long seed = 0; seed < 30; seed++) {
            FloatingIslandsPlan.VillageSite site = plan.guaranteedVillageSite(seed);
            double distance = Math.hypot(site.centerX(), site.centerZ());
            assertTrue(distance >= 1000, "expected village beyond exclusion zone, got distance " + distance);
        }
    }

    @Test
    void guaranteedVillageIslandAlwaysAppearsInTheGrid() {
        FloatingIslandsPlan plan = plan(true, 0.6, false, 256, new IslandPlan.ExclusionZone(true, 256));
        for (long seed = 0; seed < 10; seed++) {
            FloatingIslandsPlan.VillageSite site = plan.guaranteedVillageSite(seed);
            int x = (int) Math.round(site.centerX());
            int z = (int) Math.round(site.centerZ());
            FloatingIslandsPlan.Hit hit = plan.at(x, z, seed, "minecraft:plains");
            assertTrue(hit.present(), "expected the guaranteed village's own island to appear in the grid, seed " + seed);
            assertTrue(hit.distanceFromShore() < 0.0, "expected the village's own center to be well inside its island");
        }
    }

    private static FloatingIslandsPlan plan(
        boolean enabled, double spawnChance, boolean biomeVariety, int cellSizeBlocks, IslandPlan.ExclusionZone exclusionZone
    ) {
        return new FloatingIslandsPlan(
            enabled, 16, 32, 0.1, cellSizeBlocks, spawnChance, biomeVariety, List.of("minecraft:plains"), exclusionZone, false, false
        );
    }
}
