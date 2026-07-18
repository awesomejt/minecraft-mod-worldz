package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaosBiomesCustomizationTest {
    @Test
    void fromConfigCopiesSanitizedDefaults() {
        WorldzConfig config = new WorldzConfig();

        ChaosBiomesCustomization customization = ChaosBiomesCustomization.fromConfig(config);

        assertEquals(config.chaosBiomes.biomes, customization.biomes());
        assertEquals(512, customization.regionScaleBlocks());
        assertEquals("", customization.starterBiome());
        assertEquals(256, customization.starterRadiusBlocks());
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, customization.spawnStrategy());
        assertFalse(customization.allowRivers());
        assertFalse(customization.allowOceans());
    }

    @Test
    void constructorRequiresAtLeastOneBiome() {
        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of(), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void constructorRejectsInvalidBiomeEntries() {
        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of("#minecraft:is_overworld"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void constructorCanonicalizesWeightedEntries() {
        ChaosBiomesCustomization customization = new ChaosBiomesCustomization(
            List.of("desert@2", "jungle"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert@2.0", "minecraft:jungle"), customization.biomes());
    }

    @Test
    void constructorRejectsRegionScaleOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of("minecraft:desert"), 1, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of("minecraft:desert"), 999_999, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterBiomeIsOptionalButRejectsTags() {
        ChaosBiomesCustomization empty = new ChaosBiomesCustomization(
            List.of("minecraft:desert"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );
        assertEquals("", empty.starterBiome());

        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of("minecraft:desert"), 512, "#minecraft:is_overworld", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterRadiusMustBeInRange() {
        assertThrows(IllegalArgumentException.class, () -> new ChaosBiomesCustomization(
            List.of("minecraft:desert"), 512, "", 1, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void fromTextParsesNewlineSeparatedBiomesAndDecimalFields() {
        ChaosBiomesCustomization customization = ChaosBiomesCustomization.fromText(
            "minecraft:desert\nminecraft:jungle@2", "256", "minecraft:plains", "128",
            SpawnStrategy.PREFERRED_NATURAL_BIOME, true, false
        );

        assertEquals(List.of("minecraft:desert", "minecraft:jungle@2.0"), customization.biomes());
        assertEquals(256, customization.regionScaleBlocks());
        assertEquals("minecraft:plains", customization.starterBiome());
        assertEquals(128, customization.starterRadiusBlocks());
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, customization.spawnStrategy());
        assertTrue(customization.allowRivers());
        assertFalse(customization.allowOceans());
    }

    @Test
    void fromTextRejectsNonNumericFields() {
        assertThrows(IllegalArgumentException.class, () -> ChaosBiomesCustomization.fromText(
            "minecraft:desert", "not-a-number", "", "256", SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void biomesTextRoundTripsNewlineSeparatedEntries() {
        ChaosBiomesCustomization customization = new ChaosBiomesCustomization(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals("minecraft:desert\nminecraft:jungle", customization.biomesText());
    }

    @Test
    void allowedBiomeIdsIsBiomesPlusDistinctStarter() {
        ChaosBiomesCustomization customization = new ChaosBiomesCustomization(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "minecraft:plains", 256,
            SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert", "minecraft:jungle", "minecraft:plains"), customization.allowedBiomeIds());
    }

    @Test
    void allowedBiomeIdsDoesNotDuplicateAStarterAlreadyInBiomes() {
        ChaosBiomesCustomization customization = new ChaosBiomesCustomization(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "minecraft:desert", 256,
            SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert", "minecraft:jungle"), customization.allowedBiomeIds());
    }
}
