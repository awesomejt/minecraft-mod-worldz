package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherStartPlanTest {
    @Test
    void disabledPlanHasSafePlaceholderValues() {
        NetherStartPlan disabled = NetherStartPlan.disabled();
        assertFalse(disabled.enabled());
        assertEquals(NetherStartPlan.DEFAULT_SPAWN_Y, disabled.spawnY());
        assertEquals(StarterKitTier.MEDIUM, disabled.chestTier());
    }

    @Test
    void fromConfigResolvesAnEnabledPlan() {
        NetherStartConfig config = new NetherStartConfig();
        config.spawnY = 64;
        config.chestTier = StarterKitTier.HARD;

        NetherStartPlan plan = NetherStartPlan.fromConfig(config);

        assertTrue(plan.enabled());
        assertEquals(64, plan.spawnY());
        assertEquals(StarterKitTier.HARD, plan.chestTier());
    }

    @Test
    void invalidSpawnYIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, NetherStartPlan.MIN_SPAWN_Y - 1, StarterKitTier.MEDIUM
        ));
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(
            true, NetherStartPlan.MAX_SPAWN_Y + 1, StarterKitTier.MEDIUM
        ));
    }

    @Test
    void invalidChestTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetherStartPlan(true, 32, null));
    }
}
