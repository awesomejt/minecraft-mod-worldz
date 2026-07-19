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
    void rateDerivesContinuousDurationAndOverridesTotalDays() {
        BorderSchedule schedule = new BorderSchedule(512, 1512, 999, 100, 2);

        assertEquals(480_000L, schedule.durationTicks());
        assertEquals(1012.0, schedule.radiusAtTick(240_000));
    }

    @Test
    void rateUsesAProportionalFinalPartialInterval() {
        BorderSchedule schedule = new BorderSchedule(512, 762, 0, 100, 2);

        assertEquals(120_000L, schedule.durationTicks());
        assertEquals(762.0, schedule.radiusAtTick(120_000));
    }

    @Test
    void staticRateScheduleNeedsNoTransition() {
        BorderSchedule schedule = new BorderSchedule(512, 512, 100, 50, 2);

        assertEquals(0L, schedule.durationTicks());
        assertEquals(512.0, schedule.radiusAtTick(0));
    }

    @Test
    void invalidSchedulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(0, 512, 1));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, 512, -1));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, 1024, 1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new BorderSchedule(512, 1024, 1, -1, 1));
    }

    @Test
    void delayHoldsInitialRadiusBeforeGrowthBegins() {
        BorderSchedule schedule = new BorderSchedule(512, 1512, 10, 5, 0, 0);

        assertEquals(120_000L, schedule.delayTicks());
        assertEquals(360_000L, schedule.totalDurationTicks());
        assertEquals(512.0, schedule.radiusAtTick(119_999L));
        assertEquals(512.0, schedule.radiusAtTick(120_000L));
        assertEquals(1012.0, schedule.radiusAtTick(240_000L));
        assertEquals(1512.0, schedule.radiusAtTick(360_000L));
    }

    @Test
    void delayAlsoAppliesToCollapseAndDeferredImmediateResize() {
        BorderSchedule collapse = new BorderSchedule(1024, 512, 4, 2, 0, 0);
        BorderSchedule jump = new BorderSchedule(1024, 512, 0, 2, 0, 0);

        assertEquals(1024.0, collapse.radiusAtTick(47_999L));
        assertEquals(768.0, collapse.radiusAtTick(96_000L));
        assertEquals(1024.0, jump.radiusAtTick(47_999L));
        assertEquals(512.0, jump.radiusAtTick(48_000L));
    }

    @Test
    void delayDoesNotChangeRateDerivedTransitionDuration() {
        BorderSchedule schedule = new BorderSchedule(512, 762, 999, 7, 100, 2);

        assertEquals(120_000L, schedule.durationTicks());
        assertEquals(168_000L, schedule.delayTicks());
        assertEquals(288_000L, schedule.totalDurationTicks());
    }

    @Test
    void steppedScheduleHoldsUntilEachIntervalFullyElapses() {
        BorderSchedule schedule = new BorderSchedule(8, 1024, 0, 0, 1, 1, ResizeStyle.STEPPED);

        assertEquals(8.0, schedule.radiusAtTick(0));
        assertEquals(8.0, schedule.radiusAtTick(23_999L));
        assertEquals(9.0, schedule.radiusAtTick(24_000L));
        assertEquals(9.0, schedule.radiusAtTick(47_999L));
        assertEquals(10.0, schedule.radiusAtTick(48_000L));
    }

    @Test
    void steppedScheduleAppliesMultipleStepsAtOnceAndClampsAtFinal() {
        BorderSchedule schedule = new BorderSchedule(8, 1024, 0, 0, 8, 1, ResizeStyle.STEPPED);

        assertEquals(88.0, schedule.radiusAtTick(240_000L));
        assertEquals(1024.0, schedule.radiusAtTick(schedule.totalDurationTicks()));
        assertEquals(1024.0, schedule.radiusAtTick(Long.MAX_VALUE));
    }

    @Test
    void steppedScheduleCollapsesInTheOtherDirection() {
        BorderSchedule schedule = new BorderSchedule(1024, 32, 0, 0, 2, 1, ResizeStyle.STEPPED);

        assertEquals(1024.0, schedule.radiusAtTick(0));
        assertEquals(1022.0, schedule.radiusAtTick(24_000L));
        assertEquals(32.0, schedule.radiusAtTick(schedule.totalDurationTicks()));
    }

    @Test
    void steppedScheduleRespectsAnInitialDelay() {
        BorderSchedule schedule = new BorderSchedule(8, 1024, 0, 5, 1, 1, ResizeStyle.STEPPED);

        assertEquals(120_000L, schedule.delayTicks());
        assertEquals(8.0, schedule.radiusAtTick(119_999L));
        assertEquals(8.0, schedule.radiusAtTick(120_000L));
        assertEquals(9.0, schedule.radiusAtTick(144_000L));
    }

    @Test
    void steppedScheduleWithoutARateIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BorderSchedule(8, 1024, 0, 0, 0, 0, ResizeStyle.STEPPED)
        );
    }
}
