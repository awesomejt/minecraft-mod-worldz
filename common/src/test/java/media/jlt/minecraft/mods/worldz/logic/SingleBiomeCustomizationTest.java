package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleBiomeCustomizationTest {
    @Test
    void fromConfigCopiesSanitizedDefaults() {
        WorldzConfig config = new WorldzConfig();

        SingleBiomeCustomization customization = SingleBiomeCustomization.fromConfig(config);

        assertEquals("minecraft:plains", customization.landBiome());
        assertEquals("", customization.starterBiome());
        assertEquals(256, customization.starterRadiusBlocks());
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, customization.spawnStrategy());
    }

    @Test
    void landBiomeMustBeOneIdNotATag() {
        assertThrows(IllegalArgumentException.class, () -> new SingleBiomeCustomization(
            "#minecraft:is_overworld", "", 256, SpawnStrategy.STARTER_AT_ORIGIN
        ));
    }

    @Test
    void starterBiomeIsOptionalButRejectsTags() {
        SingleBiomeCustomization empty = new SingleBiomeCustomization(
            "minecraft:plains", "", 256, SpawnStrategy.STARTER_AT_ORIGIN
        );
        assertEquals("", empty.starterBiome());

        assertThrows(IllegalArgumentException.class, () -> new SingleBiomeCustomization(
            "minecraft:plains", "#minecraft:is_overworld", 256, SpawnStrategy.STARTER_AT_ORIGIN
        ));
    }

    @Test
    void starterRadiusMustBeInRange() {
        assertThrows(IllegalArgumentException.class, () -> new SingleBiomeCustomization(
            "minecraft:plains", "", 1, SpawnStrategy.STARTER_AT_ORIGIN
        ));
        assertThrows(IllegalArgumentException.class, () -> new SingleBiomeCustomization(
            "minecraft:plains", "", 999_999, SpawnStrategy.STARTER_AT_ORIGIN
        ));
    }

    @Test
    void fromTextParsesDecimalRadius() {
        SingleBiomeCustomization customization = SingleBiomeCustomization.fromText(
            "minecraft:desert", "minecraft:plains", "512", SpawnStrategy.PREFERRED_NATURAL_BIOME
        );

        assertEquals("minecraft:desert", customization.landBiome());
        assertEquals("minecraft:plains", customization.starterBiome());
        assertEquals(512, customization.starterRadiusBlocks());
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, customization.spawnStrategy());
    }

    @Test
    void fromTextRejectsNonNumericRadius() {
        assertThrows(IllegalArgumentException.class, () -> SingleBiomeCustomization.fromText(
            "minecraft:plains", "", "not-a-number", SpawnStrategy.STARTER_AT_ORIGIN
        ));
    }

    @Test
    void allowedBiomeIdsIsJustLandBiomeWhenStarterIsUnsetOrTheSame() {
        SingleBiomeCustomization unset = new SingleBiomeCustomization(
            "minecraft:plains", "", 256, SpawnStrategy.STARTER_AT_ORIGIN
        );
        assertEquals(List.of("minecraft:plains"), unset.allowedBiomeIds());

        SingleBiomeCustomization same = new SingleBiomeCustomization(
            "minecraft:plains", "minecraft:plains", 256, SpawnStrategy.STARTER_AT_ORIGIN
        );
        assertEquals(List.of("minecraft:plains"), same.allowedBiomeIds());
    }

    @Test
    void allowedBiomeIdsIncludesADifferentStarterBiome() {
        SingleBiomeCustomization customization = new SingleBiomeCustomization(
            "minecraft:desert", "minecraft:plains", 256, SpawnStrategy.STARTER_AT_ORIGIN
        );

        assertTrue(customization.allowedBiomeIds().containsAll(List.of("minecraft:desert", "minecraft:plains")));
        assertEquals(2, customization.allowedBiomeIds().size());
    }
}
