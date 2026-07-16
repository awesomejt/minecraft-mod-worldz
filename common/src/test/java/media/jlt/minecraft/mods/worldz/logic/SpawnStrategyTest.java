package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpawnStrategyTest {
    @Test
    void parsesStableLowercaseNames() {
        assertEquals(SpawnStrategy.STARTER_AT_ORIGIN, SpawnStrategy.parse("starter_at_origin"));
        assertEquals(SpawnStrategy.PREFERRED_NATURAL_BIOME, SpawnStrategy.parse(" Preferred_Natural_Biome "));
        assertEquals(SpawnStrategy.VANILLA_SPAWN, SpawnStrategy.parse("vanilla_spawn"));
    }

    @Test
    void serializedNameRoundTripsThroughParse() {
        for (SpawnStrategy strategy : SpawnStrategy.values()) {
            assertEquals(strategy, SpawnStrategy.parse(strategy.serializedName()));
        }
    }

    @Test
    void rejectsUnknownOrMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> SpawnStrategy.parse("teleport"));
        assertThrows(IllegalArgumentException.class, () -> SpawnStrategy.parse(null));
    }
}
