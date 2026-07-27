package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeRolesTest {
    @Test
    void classifiesMaintainedVanillaOceanAndBeachBiomes() {
        assertEquals(BiomeRole.OCEAN, BiomeRoles.defaultRole("minecraft:ocean"));
        assertEquals(BiomeRole.OCEAN, BiomeRoles.defaultRole("minecraft:deep_frozen_ocean"));
        assertEquals(BiomeRole.BEACH, BiomeRoles.defaultRole("minecraft:beach"));
        assertEquals(BiomeRole.BEACH, BiomeRoles.defaultRole("minecraft:stony_shore"));
    }

    @Test
    void unknownAndOrdinaryBiomesDefaultToLand() {
        assertEquals(BiomeRole.LAND, BiomeRoles.defaultRole("minecraft:plains"));
        assertEquals(BiomeRole.LAND, BiomeRoles.defaultRole("example:custom_modded_biome"));
    }

    @Test
    void explicitOverrideTakesPrecedenceOverMaintainedDefault() {
        Map<String, BiomeRole> overrides = Map.of("minecraft:plains", BiomeRole.OCEAN);

        assertEquals(BiomeRole.OCEAN, BiomeRoles.resolve("minecraft:plains", overrides));
        assertEquals(BiomeRole.LAND, BiomeRoles.resolve("minecraft:desert", overrides));
    }

    @Test
    void classifiesMaintainedVanillaUndergroundBiomes() {
        assertTrue(BiomeRoles.isUnderground("minecraft:dripstone_caves"));
        assertTrue(BiomeRoles.isUnderground("minecraft:lush_caves"));
        assertTrue(BiomeRoles.isUnderground("minecraft:sulfur_caves"));
        assertTrue(BiomeRoles.isUnderground("minecraft:deep_dark"));
    }

    @Test
    void unknownAndOrdinaryBiomesAreNotUnderground() {
        assertFalse(BiomeRoles.isUnderground("minecraft:plains"));
        assertFalse(BiomeRoles.isUnderground("minecraft:ocean"));
        assertFalse(BiomeRoles.isUnderground("example:custom_modded_biome"));
    }

    @Test
    void undergroundIdsReturnsExactlyTheMaintainedSet() {
        assertEquals(
            Set.of("minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:sulfur_caves", "minecraft:deep_dark"),
            Set.copyOf(BiomeRoles.undergroundIds())
        );
        for (String id : BiomeRoles.undergroundIds()) {
            assertTrue(BiomeRoles.isUnderground(id));
        }
    }
}
