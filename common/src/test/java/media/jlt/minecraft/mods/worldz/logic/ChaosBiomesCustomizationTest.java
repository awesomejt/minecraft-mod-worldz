package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaosBiomesCustomizationTest {
    private static ChaosBiomesCustomization create(
        List<String> biomes, int regionScaleBlocks, String starterBiome, int starterRadiusBlocks,
        SpawnStrategy spawnStrategy, boolean allowRivers, boolean allowOceans
    ) {
        return new ChaosBiomesCustomization(
            biomes, regionScaleBlocks, starterBiome, starterRadiusBlocks, spawnStrategy, allowRivers, allowOceans,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );
    }

    private static WorldzCustomization.BorderSettings defaultBorder() {
        return new WorldzCustomization.BorderSettings(false, 512, 512, 0, true);
    }

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
    void fromConfigCopiesBorderExteriorAndEndBorderSettings() {
        WorldzConfig config = new WorldzConfig();
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 2048;
        config.endBorder.carryFromOverworld = true;
        config.endBorder.minimumRadiusBlocks = 320;

        ChaosBiomesCustomization customization = ChaosBiomesCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertTrue(customization.worldLimitPlan().end().carryFromOverworld());
    }

    @Test
    void constructorRequiresAtLeastOneBiome() {
        assertThrows(IllegalArgumentException.class, () -> create(
            List.of(), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void constructorRejectsInvalidBiomeEntries() {
        assertThrows(IllegalArgumentException.class, () -> create(
            List.of("#minecraft:is_overworld"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void constructorCanonicalizesWeightedEntries() {
        ChaosBiomesCustomization customization = create(
            List.of("desert@2", "jungle"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert@2.0", "minecraft:jungle"), customization.biomes());
    }

    @Test
    void constructorRejectsRegionScaleOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> create(
            List.of("minecraft:desert"), 1, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> create(
            List.of("minecraft:desert"), 999_999, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterBiomeIsOptionalButRejectsTags() {
        ChaosBiomesCustomization empty = create(
            List.of("minecraft:desert"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );
        assertEquals("", empty.starterBiome());

        assertThrows(IllegalArgumentException.class, () -> create(
            List.of("minecraft:desert"), 512, "#minecraft:is_overworld", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterRadiusMustBeInRange() {
        assertThrows(IllegalArgumentException.class, () -> create(
            List.of("minecraft:desert"), 512, "", 1, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void fromTextParsesNewlineSeparatedBiomesAndDecimalFields() {
        ChaosBiomesCustomization customization = ChaosBiomesCustomization.fromText(
            "minecraft:desert\nminecraft:jungle@2", "256", "minecraft:plains", "128",
            SpawnStrategy.PREFERRED_NATURAL_BIOME, true, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
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
            "minecraft:desert", "not-a-number", "", "256", SpawnStrategy.STARTER_AT_ORIGIN, false, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void biomesTextRoundTripsNewlineSeparatedEntries() {
        ChaosBiomesCustomization customization = create(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals("minecraft:desert\nminecraft:jungle", customization.biomesText());
    }

    @Test
    void allowedBiomeIdsIsBiomesPlusDistinctStarter() {
        ChaosBiomesCustomization customization = create(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "minecraft:plains", 256,
            SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert", "minecraft:jungle", "minecraft:plains"), customization.allowedBiomeIds());
    }

    @Test
    void allowedBiomeIdsDoesNotDuplicateAStarterAlreadyInBiomes() {
        ChaosBiomesCustomization customization = create(
            List.of("minecraft:desert", "minecraft:jungle"), 512, "minecraft:desert", 256,
            SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertEquals(List.of("minecraft:desert", "minecraft:jungle"), customization.allowedBiomeIds());
    }
}
