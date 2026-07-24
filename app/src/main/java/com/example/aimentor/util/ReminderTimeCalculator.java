package com.example.aimentor.util;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure-Java calculation for the next local daily reminder. */
public final class ReminderTimeCalculator {

    private ReminderTimeCalculator() { }

    public static long millisUntilNext(
            long nowMillis, int hour, int minute, TimeZone timeZone) {
        TimeZone zone = timeZone == null ? TimeZone.getDefault() : timeZone;
        Calendar now = Calendar.getInstance(zone);
        now.setTimeInMillis(nowMillis);

        Calendar next = Calendar.getInstance(zone);
        next.setTimeInMillis(nowMillis);
        next.set(Calendar.HOUR_OF_DAY, clamp(hour, 0, 23));
        next.set(Calendar.MINUTE, clamp(minute, 0, 59));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return Math.max(1L, next.getTimeInMillis() - nowMillis);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
