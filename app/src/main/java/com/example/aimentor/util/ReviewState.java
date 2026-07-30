package com.example.aimentor.util;

import androidx.annotation.IntDef;

import com.example.aimentor.data.Question;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Derives a simple review workflow from the saved-answer fields without
 * duplicating state in the database.
 */
public final class ReviewState {

    public static final int ALL = 0;
    public static final int DUE = 1;
    public static final int LEARNING = 2;
    public static final int MASTERED = 3;

    public static final long REVIEW_INTERVAL_MS =
            7L * 24L * 60L * 60L * 1000L;

    @IntDef({ALL, DUE, LEARNING, MASTERED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Value { }

    private ReviewState() { }

    @Value
    public static int classify(Question question, long now) {
        if (question == null) return LEARNING;
        if (question.reviewed
                && question.reviewedAt > now - REVIEW_INTERVAL_MS) {
            return MASTERED;
        }
        if (question.bookmarked || question.reviewed) {
            return DUE;
        }
        return LEARNING;
    }

    public static boolean matches(
            Question question, @Value int filter, long now) {
        return filter == ALL || classify(question, now) == filter;
    }
}
