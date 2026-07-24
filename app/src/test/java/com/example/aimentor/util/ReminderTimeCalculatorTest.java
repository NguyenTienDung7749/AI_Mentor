package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ReminderTimeCalculatorTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test
    public void futureTime_usesToday() {
        long now = localMillis(UTC, 2026, Calendar.JULY, 24, 18, 30, 0);

        long delay = ReminderTimeCalculator.millisUntilNext(
                now, 19, 0, UTC);

        assertEquals(30L * 60L * 1000L, delay);
    }

    @Test
    public void passedOrExactTime_usesNextDay() {
        long passed = localMillis(UTC, 2026, Calendar.JULY, 24, 19, 30, 0);
        long exact = localMillis(UTC, 2026, Calendar.JULY, 24, 19, 0, 0);

        assertEquals(23L * 60L * 60L * 1000L + 30L * 60L * 1000L,
                ReminderTimeCalculator.millisUntilNext(
                        passed, 19, 0, UTC));
        assertEquals(24L * 60L * 60L * 1000L,
                ReminderTimeCalculator.millisUntilNext(
                        exact, 19, 0, UTC));
    }

    @Test
    public void nextLocalTime_survivesDaylightSavingChange() {
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        long now = localMillis(
                newYork, 2026, Calendar.MARCH, 7, 20, 0, 0);

        long delay = ReminderTimeCalculator.millisUntilNext(
                now, 19, 0, newYork);
        Calendar next = Calendar.getInstance(newYork);
        next.setTimeInMillis(now + delay);

        assertTrue(delay > 0L);
        assertEquals(8, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, next.get(Calendar.MINUTE));
    }

    private long localMillis(
            TimeZone zone, int year, int month, int day,
            int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }
}
