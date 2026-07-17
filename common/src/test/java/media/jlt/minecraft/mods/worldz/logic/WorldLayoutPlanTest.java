package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.LayoutConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLayoutPlanTest {
    private static final List<WorldLayoutPlan.BiomeWeight> LAND = List.of(
        new WorldLayoutPlan.BiomeWeight("minecraft:plains", 3.0),
        new WorldLayoutPlan.BiomeWeight("minecraft:desert", 1.0)
    );
    private static final List<WorldLayoutPlan.BiomeWeight> OCEAN = List.of(
        new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0)
    );
    private static final List<WorldLayoutPlan.BiomeWeight> BEACH = List.of(
        new WorldLayoutPlan.BiomeWeight("minecraft:beach", 1.0)
    );

    private static WorldLayoutPlan oceanPlan(long seed, int regionScale, List<WorldLayoutPlan.BiomeWeight> beach) {
        return new WorldLayoutPlan(
            LayoutMode.OCEAN, seed, regionScale, List.of(), OCEAN, beach, Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
    }

    @Test
    void legacyPlanSamplesLandEverywhereAndDecodesSafely() {
        WorldLayoutPlan legacy = WorldLayoutPlan.legacy();

        assertEquals(LayoutMode.LEGACY, legacy.mode());
        assertEquals(WorldLayoutPlan.LEGACY_MODE_REVISION, legacy.algorithmRevision());
        WorldLayoutPlan.LayoutSample sample = legacy.sampleAt(12345, -6789);
        assertEquals(BiomeRole.LAND, sample.role());
        assertTrue(sample.biomeId().isEmpty());
        assertEquals(1.0, sample.landFactor());
    }

    @Test
    void withSeedReturnsTheSameInstanceWhenTheSeedAlreadyMatches() {
        WorldLayoutPlan plan = oceanPlan(42L, 512, List.of());

        assertEquals(plan, plan.withSeed(42L));
    }

    @Test
    void withSeedChangesOnlySamplingNotOtherFields() {
        WorldLayoutPlan plan = oceanPlan(1L, 512, List.of());
        WorldLayoutPlan reseeded = plan.withSeed(99L);

        assertEquals(99L, reseeded.seed());
        assertEquals(plan.mode(), reseeded.mode());
        assertEquals(plan.regionScaleBlocks(), reseeded.regionScaleBlocks());
        assertEquals(plan.oceanBiomes(), reseeded.oceanBiomes());
        // A different seed can (not must) sample a different biome at the same column;
        // what matters is that sampling now follows the new seed deterministically.
        assertEquals(reseeded.sampleAt(500, 500), reseeded.sampleAt(500, 500));
    }

    @Test
    void voidPlanSamplesLandEverywhereWithNoConfiguredBiome() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.VOID, 1L, 512, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        WorldLayoutPlan.LayoutSample sample = plan.sampleAt(999, -999);
        assertEquals(BiomeRole.LAND, sample.role());
        assertTrue(sample.biomeId().isEmpty());
        assertEquals(1.0, sample.landFactor());
    }

    @Test
    void oceanModeAlwaysSamplesOceanRoleAndOnlyConfiguredOceanBiomes() {
        WorldLayoutPlan plan = oceanPlan(7L, 256, List.of());
        Set<String> allowed = Set.of("minecraft:ocean");

        for (int cell = 0; cell < 100; cell++) {
            WorldLayoutPlan.LayoutSample sample = plan.sampleAt(cell * 256 + 10, -cell * 256 - 10);
            assertEquals(BiomeRole.OCEAN, sample.role());
            assertEquals(0.0, sample.landFactor());
            assertTrue(allowed.contains(sample.biomeId().orElseThrow()));
        }
    }

    @Test
    void singleBiomeModeAlwaysSamplesTheConfiguredBiomeAndItsDefaultRole() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 1L, 512, List.of(), List.of(), List.of(),
            Optional.of("minecraft:plains"), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        WorldLayoutPlan.LayoutSample sample = plan.sampleAt(4096, -4096);
        assertEquals(BiomeRole.LAND, sample.role());
        assertEquals("minecraft:plains", sample.biomeId().orElseThrow());
        assertEquals(1.0, sample.landFactor());
    }

    @Test
    void singleBiomeModeSamplingIsIndependentOfSeed() {
        // GOALS 10's "randomness is based on seed" comes entirely from vanilla's own
        // real-seed-driven terrain/structures/caves (DESIGN §20.4's ChunkMapMixin fix) --
        // single_biome's own layout sampling has only one possible answer per
        // position, so re-seeding must never change what sampleAt returns.
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 1L, 512, List.of(), List.of(), List.of(),
            Optional.of("minecraft:desert"), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
        WorldLayoutPlan reseeded = plan.withSeed(987654321L);

        WorldLayoutPlan.LayoutSample original = plan.sampleAt(1234, -5678);
        WorldLayoutPlan.LayoutSample afterReseed = reseeded.sampleAt(1234, -5678);
        assertEquals(original.role(), afterReseed.role());
        assertEquals(original.biomeId(), afterReseed.biomeId());
        assertEquals(original.landFactor(), afterReseed.landFactor());
    }

    @Test
    void singleBiomeModeHonorsARoleOverride() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 1L, 512, List.of(), List.of(), List.of(),
            Optional.of("minecraft:swamp"), Map.of("minecraft:swamp", BiomeRole.OCEAN), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        WorldLayoutPlan.LayoutSample sample = plan.sampleAt(0, 0);
        assertEquals(BiomeRole.OCEAN, sample.role());
        assertEquals(0.0, sample.landFactor());
    }

    @Test
    void sameCoordinatesAlwaysSampleIdenticallyForOneSeed() {
        WorldLayoutPlan plan = oceanPlan(42L, 512, List.of());

        for (int i = 0; i < 20; i++) {
            int x = i * 137 - 900;
            int z = i * 211 + 400;
            assertEquals(plan.sampleAt(x, z), plan.sampleAt(x, z));
        }
    }

    @Test
    void differentSeedsCanProduceDifferentBiomeSelections() {
        List<WorldLayoutPlan.BiomeWeight> multipleOcean = List.of(
            new WorldLayoutPlan.BiomeWeight("minecraft:ocean", 1.0),
            new WorldLayoutPlan.BiomeWeight("minecraft:cold_ocean", 1.0)
        );
        WorldLayoutPlan planA = new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, List.of(), multipleOcean, List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
        WorldLayoutPlan planB = new WorldLayoutPlan(
            LayoutMode.OCEAN, 2L, 512, List.of(), multipleOcean, List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        boolean anyDifference = false;
        for (int cell = 0; cell < 200; cell++) {
            int x = cell * 512 + 256;
            if (!planA.sampleAt(x, 256).biomeId().equals(planB.sampleAt(x, 256).biomeId())) {
                anyDifference = true;
                break;
            }
        }
        assertTrue(anyDifference, "Two different seeds sampled an identical biome at every tested cell.");
    }

    @Test
    void sampleRoleReturnsOnlyThatRolesCandidatesRegardlessOfMode() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.OCEAN, 7L, 256, LAND, OCEAN, BEACH,
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        for (int cell = 0; cell < 40; cell++) {
            int x = cell * 256 + 10;
            assertTrue(Set.of("minecraft:plains", "minecraft:desert").contains(plan.sampleRole(BiomeRole.LAND, x, -x).orElseThrow()));
            assertTrue(Set.of("minecraft:beach").contains(plan.sampleRole(BiomeRole.BEACH, x, -x).orElseThrow()));
        }
    }

    @Test
    void sampleRoleIsEmptyWhenThatRoleHasNoCandidates() {
        WorldLayoutPlan plan = oceanPlan(7L, 256, List.of());

        assertTrue(plan.sampleRole(BiomeRole.BEACH, 100, 100).isEmpty());
    }

    @Test
    void weightedSelectionRepresentsEachPositiveWeightBiomeProportionally() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 5L, 64, LAND, List.of(), List.of(),
            Optional.of("minecraft:plains"), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        int plainsCount = 0;
        int desertCount = 0;
        int total = 0;
        for (int cellX = 0; cellX < 80; cellX++) {
            for (int cellZ = 0; cellZ < 80; cellZ++) {
                String biome = plan.sampleRole(BiomeRole.LAND, cellX * 64 + 1, cellZ * 64 + 1).orElseThrow();
                total++;
                if (biome.equals("minecraft:plains")) {
                    plainsCount++;
                } else if (biome.equals("minecraft:desert")) {
                    desertCount++;
                }
            }
        }

        assertEquals(total, plainsCount + desertCount);
        double plainsShare = (double) plainsCount / total;
        // Weights are 3:1 (expected share 0.75); allow generous slack against sampling noise.
        assertTrue(plainsShare > 0.65 && plainsShare < 0.85, "measured plains share was " + plainsShare);
    }

    @Test
    void constructorRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> oceanPlan(1L, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, List.of(), OCEAN, List.of(),
            Optional.empty(), Map.of(), 0, 0, -1
        ));
    }

    @Test
    void constructorRejectsDuplicateBiomeIdsWithinARole() {
        List<WorldLayoutPlan.BiomeWeight> duplicated = List.of(
            new WorldLayoutPlan.BiomeWeight("minecraft:plains", 1.0),
            new WorldLayoutPlan.BiomeWeight("minecraft:plains", 2.0)
        );
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, duplicated, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
    }

    @Test
    void constructorEnforcesModeSpecificBiomeRequirements() {
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 1L, 512, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
    }

    @Test
    void resultCollectionsAreImmutableSnapshots() {
        WorldLayoutPlan plan = oceanPlan(1L, 512, List.of());

        assertThrows(UnsupportedOperationException.class, () -> plan.landBiomes().add(new WorldLayoutPlan.BiomeWeight("minecraft:jungle", 1.0)));
        assertThrows(UnsupportedOperationException.class, () -> plan.oceanBiomes().clear());
        assertThrows(UnsupportedOperationException.class, () -> plan.roleOverrides().put("minecraft:plains", BiomeRole.OCEAN));
    }

    @Test
    void fromConfigPartitionsWeightedBiomesByMaintainedAndOverriddenRole() {
        WorldzConfig config = new WorldzConfig();
        LayoutConfig layout = new LayoutConfig();
        layout.mode = LayoutMode.OCEAN;
        layout.biomes = List.of("minecraft:plains@3", "minecraft:desert", "minecraft:ocean", "minecraft:swamp");
        layout.roleOverrides = Map.of("minecraft:swamp", "ocean");
        config.layout = layout;

        WorldLayoutPlan plan = WorldLayoutPlan.fromConfig(config, 123L);

        assertEquals(LayoutMode.OCEAN, plan.mode());
        assertEquals(123L, plan.seed());
        assertEquals(
            Set.of("minecraft:plains", "minecraft:desert"),
            Set.copyOf(plan.landBiomes().stream().map(WorldLayoutPlan.BiomeWeight::biomeId).toList())
        );
        assertEquals(
            Set.of("minecraft:ocean", "minecraft:swamp"),
            Set.copyOf(plan.oceanBiomes().stream().map(WorldLayoutPlan.BiomeWeight::biomeId).toList())
        );
        assertEquals(3.0, plan.landBiomes().stream()
            .filter(weight -> weight.biomeId().equals("minecraft:plains"))
            .findFirst().orElseThrow().weight());
    }
}
