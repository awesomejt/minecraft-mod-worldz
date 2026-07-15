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

    private static WorldLayoutPlan mixedPlan(long seed, double oceanCoverage, int regionScale, int coastBlend, List<WorldLayoutPlan.BiomeWeight> beach) {
        return new WorldLayoutPlan(
            LayoutMode.MIXED, seed, regionScale, oceanCoverage, coastBlend,
            LAND, OCEAN, beach, Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
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
    void sameCoordinatesAlwaysSampleIdenticallyForOneSeed() {
        WorldLayoutPlan plan = mixedPlan(42L, 0.35, 512, 128, List.of());

        for (int i = 0; i < 20; i++) {
            int x = i * 137 - 900;
            int z = i * 211 + 400;
            assertEquals(plan.sampleAt(x, z), plan.sampleAt(x, z));
        }
    }

    @Test
    void differentSeedsCanProduceDifferentLayouts() {
        WorldLayoutPlan planA = mixedPlan(1L, 0.35, 512, 128, List.of());
        WorldLayoutPlan planB = mixedPlan(2L, 0.35, 512, 128, List.of());

        boolean anyDifference = false;
        for (int cell = 0; cell < 200; cell++) {
            int x = cell * 512 + 256;
            if (!planA.sampleAt(x, 256).role().equals(planB.sampleAt(x, 256).role())) {
                anyDifference = true;
                break;
            }
        }
        assertTrue(anyDifference, "Two different seeds sampled an identical role at every tested cell.");
    }

    @Test
    void landOnlyModeOnlyEverReturnsConfiguredLandBiomes() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.LAND_ONLY, 7L, 256, 0.0, 64, LAND, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );
        Set<String> allowed = Set.of("minecraft:plains", "minecraft:desert");

        for (int cell = 0; cell < 100; cell++) {
            WorldLayoutPlan.LayoutSample sample = plan.sampleAt(cell * 256 + 10, -cell * 256 - 10);
            assertEquals(BiomeRole.LAND, sample.role());
            assertEquals(1.0, sample.landFactor());
            assertTrue(allowed.contains(sample.biomeId().orElseThrow()));
        }
    }

    @Test
    void sampleRoleReturnsOnlyThatRolesCandidatesRegardlessOfMode() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.LAND_ONLY, 7L, 256, 0.0, 64, LAND, OCEAN, BEACH,
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        for (int cell = 0; cell < 40; cell++) {
            int x = cell * 256 + 10;
            assertTrue(Set.of("minecraft:ocean").contains(plan.sampleRole(BiomeRole.OCEAN, x, -x).orElseThrow()));
            assertTrue(Set.of("minecraft:beach").contains(plan.sampleRole(BiomeRole.BEACH, x, -x).orElseThrow()));
        }
    }

    @Test
    void sampleRoleIsEmptyWhenThatRoleHasNoCandidates() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.LAND_ONLY, 7L, 256, 0.0, 64, LAND, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        assertTrue(plan.sampleRole(BiomeRole.BEACH, 100, 100).isEmpty());
    }

    @Test
    void mixedModeOnlyReturnsAllowedBiomesForTheSampledRole() {
        WorldLayoutPlan plan = mixedPlan(99L, 0.35, 400, 40, BEACH);
        Set<String> allowedLand = Set.of("minecraft:plains", "minecraft:desert");
        Set<String> allowedOcean = Set.of("minecraft:ocean");
        Set<String> allowedBeach = Set.of("minecraft:beach");

        for (int x = -20_000; x < 20_000; x += 137) {
            WorldLayoutPlan.LayoutSample sample = plan.sampleAt(x, x / 3);
            String biome = sample.biomeId().orElseThrow();
            switch (sample.role()) {
                case LAND -> assertTrue(allowedLand.contains(biome));
                case OCEAN -> assertTrue(allowedOcean.contains(biome));
                case BEACH -> assertTrue(allowedBeach.contains(biome));
            }
        }
    }

    @Test
    void weightedSelectionRepresentsEachPositiveWeightBiomeProportionally() {
        WorldLayoutPlan plan = new WorldLayoutPlan(
            LayoutMode.LAND_ONLY, 5L, 64, 0.0, 16, LAND, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        );

        int plainsCount = 0;
        int desertCount = 0;
        int total = 0;
        for (int cellX = 0; cellX < 80; cellX++) {
            for (int cellZ = 0; cellZ < 80; cellZ++) {
                String biome = plan.sampleAt(cellX * 64 + 1, cellZ * 64 + 1).biomeId().orElseThrow();
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
    void mixedOceanCoverageIsApproximatelyRepresentedOverALargeSample() {
        WorldLayoutPlan plan = mixedPlan(11L, 0.35, 100, 10, List.of());

        int oceanCells = 0;
        int totalCells = 0;
        for (int cellX = 0; cellX < 128; cellX++) {
            for (int cellZ = 0; cellZ < 128; cellZ++) {
                // Sample the cell center, far from any coast-blend boundary influence.
                int x = cellX * 100 + 50;
                int z = cellZ * 100 + 50;
                if (plan.sampleAt(x, z).role() == BiomeRole.OCEAN) {
                    oceanCells++;
                }
                totalCells++;
            }
        }

        double measured = (double) oceanCells / totalCells;
        assertTrue(measured > 0.30 && measured < 0.40, "measured ocean coverage was " + measured + ", target 0.35");
    }

    @Test
    void landFactorTransitionsContinuouslyAcrossARoleBoundary() {
        int scale = 64;
        int blendWidth = 16;
        // Fix z at mid-cell so only the x-axis boundary is ever within the blend width;
        // z=0 would sit exactly on a z-axis boundary for every sampled x.
        int z = scale / 2;
        WorldLayoutPlan plan = mixedPlan(3L, 0.5, scale, blendWidth, List.of());

        double previous = plan.sampleAt(-4000, z).landFactor();
        double maxJump = 0.0;
        for (int x = -3999; x <= 4000; x++) {
            double current = plan.sampleAt(x, z).landFactor();
            maxJump = Math.max(maxJump, Math.abs(current - previous));
            previous = current;
        }

        assertTrue(maxJump < 0.1, "landFactor jumped by " + maxJump + " between adjacent blocks");
    }

    @Test
    void constructorRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> mixedPlan(1L, 0.35, 0, 128, List.of()));
        assertThrows(IllegalArgumentException.class, () -> mixedPlan(1L, 0.35, 512, -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> mixedPlan(1L, -0.1, 512, 128, List.of()));
        assertThrows(IllegalArgumentException.class, () -> mixedPlan(1L, 1.1, 512, 128, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.MIXED, 1L, 512, 0.35, 128, LAND, OCEAN, List.of(),
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
            LayoutMode.LAND_ONLY, 1L, 512, 0.0, 128, duplicated, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
    }

    @Test
    void constructorEnforcesModeSpecificBiomeRequirements() {
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.LAND_ONLY, 1L, 512, 0.0, 128, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.OCEAN, 1L, 512, 0.0, 128, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.MIXED, 1L, 512, 0.35, 128, LAND, List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorldLayoutPlan(
            LayoutMode.SINGLE_BIOME, 1L, 512, 0.0, 128, List.of(), List.of(), List.of(),
            Optional.empty(), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
        ));
    }

    @Test
    void resultCollectionsAreImmutableSnapshots() {
        WorldLayoutPlan plan = mixedPlan(1L, 0.35, 512, 128, List.of());

        assertThrows(UnsupportedOperationException.class, () -> plan.landBiomes().add(new WorldLayoutPlan.BiomeWeight("minecraft:jungle", 1.0)));
        assertThrows(UnsupportedOperationException.class, () -> plan.oceanBiomes().clear());
        assertThrows(UnsupportedOperationException.class, () -> plan.roleOverrides().put("minecraft:plains", BiomeRole.OCEAN));
    }

    @Test
    void fromConfigPartitionsWeightedBiomesByMaintainedAndOverriddenRole() {
        WorldzConfig config = new WorldzConfig();
        LayoutConfig layout = new LayoutConfig();
        layout.mode = LayoutMode.MIXED;
        layout.biomes = List.of("minecraft:plains@3", "minecraft:desert", "minecraft:ocean", "minecraft:swamp");
        layout.roleOverrides = Map.of("minecraft:swamp", "ocean");
        config.layout = layout;

        WorldLayoutPlan plan = WorldLayoutPlan.fromConfig(config, 123L);

        assertEquals(LayoutMode.MIXED, plan.mode());
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
