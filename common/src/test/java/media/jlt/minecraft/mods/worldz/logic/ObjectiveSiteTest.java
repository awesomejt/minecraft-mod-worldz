package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectiveSiteTest {
    @Test
    void structureMarginMustFitOnBothAxes() {
        assertTrue(ObjectiveSite.fitsInside(384, -384, 512, 128));
        assertFalse(ObjectiveSite.fitsInside(385, 0, 512, 128));
        assertFalse(ObjectiveSite.fitsInside(0, -385, 512, 128));
    }

    @Test
    void arithmeticHandlesExtremeNegativeCoordinates() {
        assertFalse(ObjectiveSite.fitsInside(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0));
    }

    @Test
    void fallbackStaysPreferredWhenThereIsRoom() {
        assertEquals(32, ObjectiveSite.fallbackX(512));
        assertEquals(32, ObjectiveSite.fallbackX(64));
    }

    @Test
    void fallbackMovesInwardForTinyDefensiveInputs() {
        assertEquals(4, ObjectiveSite.fallbackX(20));
        assertEquals(0, ObjectiveSite.fallbackX(10));
    }
}
