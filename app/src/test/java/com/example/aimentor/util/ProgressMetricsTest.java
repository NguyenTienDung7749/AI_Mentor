package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ProgressMetricsTest {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    public void mastery_requiresEvidenceAndAccuracy() {
        assertEquals(ProgressMetrics.EXPLORING,
                ProgressMetrics.masteryLabel(0, 0, 2));
        assertEquals(ProgressMetrics.PRACTISING,
                ProgressMetrics.masteryLabel(4, 10, 4));
        assertEquals(ProgressMetrics.DEVELOPING,
                ProgressMetrics.masteryLabel(7, 10, 4));
        assertEquals(ProgressMetrics.MASTERED,
                ProgressMetrics.masteryLabel(9, 10, 4));
    }

    @Test
    public void streak_acceptsTodayOrYesterdayAsLatestActivity() {
        long now = System.currentTimeMillis();
        assertEquals(3, ProgressMetrics.currentStreakDays(
                Arrays.asList(now, now - DAY, now - 2L * DAY), now));
        assertEquals(2, ProgressMetrics.currentStreakDays(
                Arrays.asList(now - DAY, now - 2L * DAY), now));
        assertEquals(0, ProgressMetrics.currentStreakDays(
                Collections.singletonList(now - 3L * DAY), now));
    }
}
