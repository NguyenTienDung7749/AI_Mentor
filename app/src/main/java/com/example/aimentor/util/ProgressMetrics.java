package com.example.aimentor.util;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure progress calculations shared by repository analytics and tests. */
public final class ProgressMetrics {

    public static final String EXPLORING = "Exploring";
    public static final String PRACTISING = "Practising";
    public static final String DEVELOPING = "Developing";
    public static final String MASTERED = "Mastered";

    private ProgressMetrics() { }

    public static String masteryLabel(
            int correct, int total, int savedQuestionCount) {
        if (total <= 0) {
            return savedQuestionCount > 0 ? EXPLORING : PRACTISING;
        }
        int accuracy = Gamification.accuracyPercent(correct, total);
        if (total >= 10 && accuracy >= 85) return MASTERED;
        if (accuracy >= 65) return DEVELOPING;
        return PRACTISING;
    }

    public static int currentStreakDays(List<Long> activityTimes, long now) {
        if (activityTimes == null || activityTimes.isEmpty()) return 0;
        Set<Long> activeDays = new HashSet<>();
        for (Long timestamp : activityTimes) {
            if (timestamp != null && timestamp > 0L) {
                activeDays.add(dayStart(timestamp));
            }
        }
        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(dayStart(now));
        if (!activeDays.contains(cursor.getTimeInMillis())) {
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        int streak = 0;
        while (activeDays.contains(cursor.getTimeInMillis())) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private static long dayStart(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
