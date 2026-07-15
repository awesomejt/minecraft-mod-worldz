package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldzCustomizationTest {
    @Test
    void configDefaultsAreCopiedIntoAnImmutableSnapshot() {
        WorldzConfig config = new WorldzConfig();
        config.allowedBiomes = new ArrayList<>(List.of("plains", "#is_overworld"));
        config.starterBiome = "desert";
        config.overworldBorder.enabled = true;
        config.overworldBorder.finalRadiusBlocks = 1024;

        WorldzCustomization customization = WorldzCustomization.fromConfig(config);
        config.allowedBiomes.clear();

        assertEquals(List.of("minecraft:plains", "#minecraft:is_overworld"), customization.allowedBiomes());
        assertEquals("minecraft:desert", customization.starterBiome());
        assertTrue(customization.overworldBorder().enabled());
        assertEquals(1024, customization.overworldBorder().finalRadiusBlocks());
        assertThrows(UnsupportedOperationException.class, () -> customization.allowedBiomes().clear());
    }

    @Test
    void editableBiomeTextAcceptsCommasNewlinesShortIdsAndTags() {
        WorldzCustomization customization = WorldzCustomization.fromText(
            " plains, minecraft:desert\n#is_overworld ",
            " cherry_grove ",
            "768",
            border(false),
            border(true)
        );

        assertEquals(
            List.of("minecraft:plains", "minecraft:desert", "#minecraft:is_overworld"),
            customization.allowedBiomes()
        );
        assertEquals("minecraft:cherry_grove", customization.starterBiome());
        assertEquals(768, customization.starterRadiusBlocks());
        assertEquals("minecraft:plains\nminecraft:desert\n#minecraft:is_overworld", customization.allowedBiomesText());
    }

    @Test
    void acceptsEmptyButRejectsMalformedAllowedBiomeLists() {
        assertTrue(WorldzCustomization.fromText(
            "  \n ", "", "512", border(false), border(false)
        ).allowedBiomes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "minecraft:has space", "", "512", border(false), border(false)
        ));
    }

    @Test
    void starterMustBeADirectBiomeId() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "#is_overworld", "512", border(false), border(false)
        ));
    }

    @Test
    void numericFieldsMustBeWholeNumbersWithinDocumentedRanges() {
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "", "63", border(false), border(false)
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.fromText(
            "plains", "", "five", border(false), border(false)
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.BorderSettings.fromText(
            true, "512", "15000000", "0", true
        ));
        assertThrows(IllegalArgumentException.class, () -> WorldzCustomization.BorderSettings.fromText(
            true, "512", "512", "-1", true
        ));
    }

    @Test
    void borderSettingsBecomeAnIndependentPersistedPlan() {
        WorldzCustomization customization = new WorldzCustomization(
            List.of("plains"),
            "",
            512,
            new WorldzCustomization.BorderSettings(true, 256, 2048, 100, true),
            new WorldzCustomization.BorderSettings(false, 512, 512, 0, false)
        );

        assertTrue(customization.worldLimitPlan().overworld().enabled());
        assertEquals(2048, customization.worldLimitPlan().overworld().finalRadiusBlocks());
        assertFalse(customization.worldLimitPlan().nether().enabled());
    }

    private static WorldzCustomization.BorderSettings border(boolean enabled) {
        return new WorldzCustomization.BorderSettings(enabled, 512, 512, 0, true);
    }
}
