package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BorderScheduleTest {
    @Test
    void staticScheduleUsesFinalRadiusImmediately() {
        BorderSchedule schedule = new BorderSchedule(512, 256, 0);

        assertEquals(1024.0, schedule.initialDiameterBlocks());
        assertEquals(512.0, schedule.finalDiameterBlocks());
        assertEquals(0L, schedule.durationTicks());
        assertEquals(256.0, schedule.radiusAtTick(0));
    }

    @Test
    void growingScheduleInterpolatesAndClampsAtBothEnds() {
        BorderSchedule schedule = new BorderSchedule(512, 1512, 100);

        assertEquals(2_400_000L, schedule.durationTicks());
        assertEquals(512.0, schedule.radiusAtTick(-1));
        assertEquals(1012.0, schedule.radiusAtTick(1_200_000));
        assertEquals(1512.0, schedule.radiusAtTick(2_400_000));
        assertEquals(1512.0, schedule.radiusAtTick(Long.MAX_VALUE));
    }

    @Test
    void shrinkingScheduleInterpolatesInTheOtherDirection() {
        BorderSchedule schedule = new BorderSchedule(1024, 512, 2);

        assertEquals(768.0, schedule.radiusAtTick(24_000));
    }

    @Test
    void invalidSchedulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(0, 512, 1));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, 512, -1));
    }
}
