package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.RisingLavaConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RisingLavaScheduleTest {
    @Test
    void staysAtStartYDuringTheDelay() {
        RisingLavaSchedule schedule = new RisingLavaSchedule(-64, 64, 3, 1, 1);

        assertEquals(-64, schedule.levelAtTick(0));
        assertEquals(-64, schedule.levelAtTick(schedule.delayTicks() - 1));
    }

    @Test
    void risesByWholeIntervalsAfterTheDelay() {
        RisingLavaSchedule schedule = new RisingLavaSchedule(-64, 64, 0, 1, 1);

        assertEquals(-64, schedule.levelAtTick(0));
        assertEquals(-63, schedule.levelAtTick(BorderSchedule.TICKS_PER_DAY));
        assertEquals(-62, schedule.levelAtTick(2 * BorderSchedule.TICKS_PER_DAY));
        // Partial progress toward the next whole block doesn't round up early.
        assertEquals(-63, schedule.levelAtTick(2 * BorderSchedule.TICKS_PER_DAY - 1));
    }

    @Test
    void clampsAtMaxYAndNeverExceedsIt() {
        RisingLavaSchedule schedule = new RisingLavaSchedule(-64, -60, 0, 1, 1);

        assertEquals(-60, schedule.levelAtTick(4 * BorderSchedule.TICKS_PER_DAY));
        assertEquals(-60, schedule.levelAtTick(Long.MAX_VALUE / 2));
    }

    @Test
    void multiBlockRatesRiseFasterThanOneBlockPerInterval() {
        RisingLavaSchedule schedule = new RisingLavaSchedule(-64, 64, 0, 5, 1);

        assertEquals(-59, schedule.levelAtTick(BorderSchedule.TICKS_PER_DAY));
        assertEquals(-54, schedule.levelAtTick(2 * BorderSchedule.TICKS_PER_DAY));
    }

    @Test
    void identicalStartAndMaxNeverRises() {
        RisingLavaSchedule schedule = new RisingLavaSchedule(0, 0, 0, 1, 1);

        assertEquals(0, schedule.levelAtTick(0));
        assertEquals(0, schedule.levelAtTick(1_000_000L));
    }

    @Test
    void invalidSchedulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RisingLavaSchedule(64, -64, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RisingLavaSchedule(-64, 64, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RisingLavaSchedule(-64, 64, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RisingLavaSchedule(-64, 64, 0, 1, 0));
    }

    @Test
    void fromConfigResolvesAllFields() {
        RisingLavaConfig config = new RisingLavaConfig();
        config.startY = -64;
        config.maxY = 64;
        config.delayDays = 3;
        config.rateBlocks = 2;
        config.rateDays = 1;

        RisingLavaSchedule schedule = RisingLavaSchedule.fromConfig(config);

        assertEquals(-64, schedule.startY());
        assertEquals(64, schedule.maxY());
        assertEquals(3, schedule.delayDays());
        assertEquals(2, schedule.rateBlocks());
        assertEquals(1, schedule.rateDays());
    }
}
