package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandOceanProfileTest {
    private static final Set<String> SHALLOW_IDS = Set.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:ocean");

    @Test
    void depthStaysShallowWithinTheShallowBand() {
        assertEquals(8, IslandOceanProfile.floorDepthBelowSeaLevel(0.0, 64, 128, 8, 32));
        assertEquals(8, IslandOceanProfile.floorDepthBelowSeaLevel(64.0, 64, 128, 8, 32));
    }

    @Test
    void depthRampsSmoothlyAcrossTheDeepeningBand() {
        assertEquals(8, IslandOceanProfile.floorDepthBelowSeaLevel(65.0, 64, 128, 8, 32));
        assertEquals(20, IslandOceanProfile.floorDepthBelowSeaLevel(128.0, 64, 128, 8, 32));
    }

    @Test
    void depthHoldsAtTheDeepValueBeyondTheDeepeningBand() {
        assertEquals(32, IslandOceanProfile.floorDepthBelowSeaLevel(192.0, 64, 128, 8, 32));
        assertEquals(32, IslandOceanProfile.floorDepthBelowSeaLevel(300.0, 64, 128, 8, 32));
    }

    @Test
    void zeroDeepenWidthJumpsStraightToDeep() {
        assertEquals(32, IslandOceanProfile.floorDepthBelowSeaLevel(65.0, 64, 0, 8, 32));
    }

    @Test
    void shallowBandOnlyDrawsWarmAndLukewarmVariety() {
        for (int x = 0; x < 2000; x += 137) {
            String biome = IslandOceanProfile.biomeAt(x, -x, 0.0, 64, 128, 42L);
            assertTrue(SHALLOW_IDS.contains(biome), biome + " is not a shallow ocean biome");
        }
    }

    @Test
    void beyondShallowBandDrawsFromTheCompleteOceanSet() {
        for (int x = 0; x < 2000; x += 137) {
            String biome = IslandOceanProfile.biomeAt(x, -x, 100.0, 64, 128, 42L);
            assertTrue(BiomeRoles.oceanIds().contains(biome), biome + " is not a known ocean biome");
        }
    }

    @Test
    void oceanBiomePickIsDeterministic() {
        String first = IslandOceanProfile.biomeAt(500, -300, 200.0, 64, 128, 42L);
        String second = IslandOceanProfile.biomeAt(500, -300, 200.0, 64, 128, 42L);
        assertEquals(first, second);
    }

    @Test
    void regionBoundariesAreJitteredNotPinnedToTheRawGridLine() {
        // With a plain axis-aligned grid, every seed would transition biome at exactly the raw
        // grid line (x = 0, a multiple of the 128-block region scale). Jittered feature points
        // should scatter that transition point across seeds instead -- proof the boundary reads
        // as an organic patch edge rather than a checkerboard line.
        Set<Integer> transitionPoints = new java.util.HashSet<>();
        for (long seed = 1; seed <= 30; seed++) {
            String previous = IslandOceanProfile.biomeAt(-10, 0, 300.0, 64, 128, seed);
            for (int x = -9; x <= 10; x++) {
                String current = IslandOceanProfile.biomeAt(x, 0, 300.0, 64, 128, seed);
                if (!current.equals(previous)) {
                    transitionPoints.add(x);
                }
                previous = current;
            }
        }
        assertTrue(
            transitionPoints.size() > 1,
            "every seed transitioned biome at the same x, meaning region boundaries are still grid-pinned"
        );
    }

    @Test
    void shoreBiomeIsBeachOrStonyShoreAndDeterministic() {
        List<String> shoreIds = List.of("minecraft:beach", "minecraft:stony_shore");
        for (int x = 0; x < 200; x += 17) {
            String biome = IslandOceanProfile.shoreBiomeAt(x, -x, 42L);
            assertTrue(shoreIds.contains(biome), biome + " is not a shore biome");
        }
        assertEquals(IslandOceanProfile.shoreBiomeAt(5, 5, 42L), IslandOceanProfile.shoreBiomeAt(5, 5, 42L));
    }
}
