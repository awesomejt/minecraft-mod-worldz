package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleBiomeCustomizationTest {
    private static SingleBiomeCustomization create(
        String landBiome, String starterBiome, int starterRadiusBlocks, SpawnStrategy spawnStrategy,
        boolean allowRivers, boolean allowOceans
    ) {
        return new SingleBiomeCustomization(
            landBiome, starterBiome, starterRadiusBlocks, spawnStrategy, allowRivers, allowOceans,
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

        SingleBiomeCustomization customization = SingleBiomeCustomization.fromConfig(config);

        assertEquals("minecraft:plains", customization.landBiome());
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

        SingleBiomeCustomization customization = SingleBiomeCustomization.fromConfig(config);

        assertTrue(customization.overworldBorder().enabled());
        assertEquals(2048, customization.overworldBorder().finalRadiusBlocks());
        assertTrue(customization.endBorder().carryFromOverworld());
        assertEquals(320, customization.endBorder().minimumRadiusBlocks());
        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertTrue(customization.worldLimitPlan().end().carryFromOverworld());
    }

    @Test
    void fromConfigCopiesAllowRiversAndOceans() {
        WorldzConfig config = new WorldzConfig();
        config.singleBiome.allowRivers = true;
        config.singleBiome.allowOceans = true;

        SingleBiomeCustomization customization = SingleBiomeCustomization.fromConfig(config);

        assertTrue(customization.allowRivers());
        assertTrue(customization.allowOceans());
    }

    @Test
    void landBiomeMustBeOneIdNotATag() {
        assertThrows(IllegalArgumentException.class, () -> create(
            "#minecraft:is_overworld", "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterBiomeIsOptionalButRejectsTags() {
        SingleBiomeCustomization empty = create(
            "minecraft:plains", "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );
        assertEquals("", empty.starterBiome());

        assertThrows(IllegalArgumentException.class, () -> create(
            "minecraft:plains", "#minecraft:is_overworld", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void starterRadiusMustBeInRange() {
        assertThrows(IllegalArgumentException.class, () -> create(
            "minecraft:plains", "", 1, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> create(
            "minecraft:plains", "", 999_999, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        ));
    }

    @Test
    void fromTextParsesDecimalRadius() {
        SingleBiomeCustomization customization = SingleBiomeCustomization.fromText(
            "minecraft:desert", "minecraft:plains", "512", SpawnStrategy.PREFERRED_NATURAL_BIOME, true, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        );

        assertEquals("minecraft:desert", customization.landBiome());
        assertEquals("minecraft:plains", customization.starterBiome());
        assertEquals(512, customization.starterRadiusBlocks());
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, customization.spawnStrategy());
        assertTrue(customization.allowRivers());
        assertFalse(customization.allowOceans());
    }

    @Test
    void fromTextRejectsNonNumericRadius() {
        assertThrows(IllegalArgumentException.class, () -> SingleBiomeCustomization.fromText(
            "minecraft:plains", "", "not-a-number", SpawnStrategy.STARTER_AT_ORIGIN, false, false,
            defaultBorder(), defaultBorder(), WorldzCustomization.EndBorderSettings.disabled(),
            WorldzCustomization.ExteriorSettings.normal(), WorldzCustomization.ExteriorSettings.normal()
        ));
    }

    @Test
    void allowedBiomeIdsIsJustLandBiomeWhenStarterIsUnsetOrTheSame() {
        SingleBiomeCustomization unset = create(
            "minecraft:plains", "", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );
        assertEquals(List.of("minecraft:plains"), unset.allowedBiomeIds());

        SingleBiomeCustomization same = create(
            "minecraft:plains", "minecraft:plains", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );
        assertEquals(List.of("minecraft:plains"), same.allowedBiomeIds());
    }

    @Test
    void allowedBiomeIdsIncludesADifferentStarterBiome() {
        SingleBiomeCustomization customization = create(
            "minecraft:desert", "minecraft:plains", 256, SpawnStrategy.STARTER_AT_ORIGIN, false, false
        );

        assertTrue(customization.allowedBiomeIds().containsAll(List.of("minecraft:desert", "minecraft:plains")));
        assertEquals(2, customization.allowedBiomeIds().size());
    }
}
