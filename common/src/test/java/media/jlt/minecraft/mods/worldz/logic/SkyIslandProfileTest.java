package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyIslandProfileTest {
    @Test
    void aboveSurfaceIsVoid() {
        assertEquals(SkyIslandProfile.Layer.VOID, SkyIslandProfile.layerAt(64, 64, 6));
        assertEquals(SkyIslandProfile.Layer.VOID, SkyIslandProfile.layerAt(100, 64, 6));
    }

    @Test
    void belowTheSlabIsVoid() {
        assertEquals(SkyIslandProfile.Layer.VOID, SkyIslandProfile.layerAt(57, 64, 6));
        assertEquals(SkyIslandProfile.Layer.VOID, SkyIslandProfile.layerAt(-64, 64, 6));
    }

    @Test
    void topBlockIsTheSingleBlockJustBelowSurface() {
        assertEquals(SkyIslandProfile.Layer.TOP, SkyIslandProfile.layerAt(63, 64, 6));
    }

    @Test
    void subsoilIsTheNextFewBlocksDown() {
        assertEquals(SkyIslandProfile.Layer.SUBSOIL, SkyIslandProfile.layerAt(62, 64, 6));
        assertEquals(SkyIslandProfile.Layer.SUBSOIL, SkyIslandProfile.layerAt(61, 64, 6));
    }

    @Test
    void coreIsEverythingElseDownToTheBottom() {
        assertEquals(SkyIslandProfile.Layer.CORE, SkyIslandProfile.layerAt(60, 64, 6));
        assertEquals(SkyIslandProfile.Layer.CORE, SkyIslandProfile.layerAt(58, 64, 6));
    }

    @Test
    void thinSlabHasNoRoomForACore() {
        // thickness 1: bottom == surfaceY - 1, the single block is both bottom and top.
        assertEquals(SkyIslandProfile.Layer.TOP, SkyIslandProfile.layerAt(63, 64, 1));
        assertEquals(SkyIslandProfile.Layer.VOID, SkyIslandProfile.layerAt(62, 64, 1));
    }

    @Test
    void familyMatchesDesertFamilyBiomes() {
        assertEquals(SkyIslandProfile.BiomeFamily.DESERT, SkyIslandProfile.familyFor("minecraft:desert"));
        assertEquals(SkyIslandProfile.BiomeFamily.DESERT, SkyIslandProfile.familyFor("minecraft:badlands"));
        assertEquals(SkyIslandProfile.BiomeFamily.DESERT, SkyIslandProfile.familyFor("minecraft:beach"));
    }

    @Test
    void familyMatchesSnowyFamilyBiomes() {
        assertEquals(SkyIslandProfile.BiomeFamily.SNOWY, SkyIslandProfile.familyFor("minecraft:snowy_plains"));
        assertEquals(SkyIslandProfile.BiomeFamily.SNOWY, SkyIslandProfile.familyFor("minecraft:ice_spikes"));
        assertEquals(SkyIslandProfile.BiomeFamily.SNOWY, SkyIslandProfile.familyFor("minecraft:frozen_peaks"));
        assertEquals(SkyIslandProfile.BiomeFamily.SNOWY, SkyIslandProfile.familyFor("minecraft:grove"));
    }

    @Test
    void familyMatchesMushroomFields() {
        assertEquals(SkyIslandProfile.BiomeFamily.MUSHROOM, SkyIslandProfile.familyFor("minecraft:mushroom_fields"));
    }

    @Test
    void familyMatchesWaterFamilyBiomes() {
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:ocean"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:deep_cold_ocean"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:river"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:swamp"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:mangrove_swamp"));
    }

    @Test
    void frozenOceanAndRiverAreWaterNotSnowy() {
        // Regression coverage (2026-07-25): "frozen" alone routes to SNOWY below, but
        // frozen_ocean/frozen_river are real water biomes and must be checked first.
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:frozen_ocean"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:deep_frozen_ocean"));
        assertEquals(SkyIslandProfile.BiomeFamily.WATER, SkyIslandProfile.familyFor("minecraft:frozen_river"));
        // frozen_peaks has no "ocean"/"river"/"swamp" substring, so it still reaches snowy.
        assertEquals(SkyIslandProfile.BiomeFamily.SNOWY, SkyIslandProfile.familyFor("minecraft:frozen_peaks"));
    }

    @Test
    void familyDefaultsForEverythingElse() {
        assertEquals(SkyIslandProfile.BiomeFamily.DEFAULT, SkyIslandProfile.familyFor("minecraft:plains"));
        assertEquals(SkyIslandProfile.BiomeFamily.DEFAULT, SkyIslandProfile.familyFor("minecraft:jungle"));
        assertEquals(SkyIslandProfile.BiomeFamily.DEFAULT, SkyIslandProfile.familyFor(null));
    }
}
