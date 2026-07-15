package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
