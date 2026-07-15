package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile.OceanLayer.AIR;
import static media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile.OceanLayer.BEDROCK;
import static media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile.OceanLayer.STONE;
import static media.jlt.minecraft.mods.worldz.logic.ExteriorTerrainProfile.OceanLayer.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExteriorTerrainProfileTest {
    @Test
    void overworldOceanHasBedrockSolidFloorSixteenWaterBlocksAndAir() {
        assertEquals(46, ExteriorTerrainProfile.oceanFloorY(-64, 319, 63));
        assertEquals(BEDROCK, ExteriorTerrainProfile.oceanLayerAt(-64, -64, 319, 63));
        assertEquals(STONE, ExteriorTerrainProfile.oceanLayerAt(46, -64, 319, 63));
        assertEquals(WATER, ExteriorTerrainProfile.oceanLayerAt(47, -64, 319, 63));
        assertEquals(WATER, ExteriorTerrainProfile.oceanLayerAt(62, -64, 319, 63));
        assertEquals(AIR, ExteriorTerrainProfile.oceanLayerAt(63, -64, 319, 63));
    }

    @Test
    void heightQueriesMatchGeneratedVoidAndOceanColumns() {
        assertEquals(-64, ExteriorTerrainProfile.baseHeight(ExteriorMode.VOID, false, -64, 319, 63));
        assertEquals(47, ExteriorTerrainProfile.baseHeight(ExteriorMode.OCEAN, true, -64, 319, 63));
        assertEquals(63, ExteriorTerrainProfile.baseHeight(ExteriorMode.OCEAN, false, -64, 319, 63));
    }

    @Test
    void shallowBuildHeightClampsOceanProfile() {
        assertEquals(4, ExteriorTerrainProfile.oceanFloorY(4, 10, 8));
        assertEquals(5, ExteriorTerrainProfile.baseHeight(ExteriorMode.OCEAN, true, 4, 10, 8));
        assertEquals(8, ExteriorTerrainProfile.baseHeight(ExteriorMode.OCEAN, false, 4, 10, 8));
    }

    @Test
    void normalTerrainCannotUseAnExteriorColumnProfile() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ExteriorTerrainProfile.baseHeight(ExteriorMode.NORMAL, false, -64, 319, 63)
        );
    }
}
