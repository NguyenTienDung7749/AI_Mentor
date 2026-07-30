package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;

import com.example.aimentor.data.Question;

import org.junit.Test;

public class ReviewStateTest {

    private static final long NOW = 2_000_000_000_000L;

    @Test
    public void classify_unreviewedAnswerAsLearning() {
        assertEquals(ReviewState.LEARNING,
                ReviewState.classify(new Question(), NOW));
    }

    @Test
    public void classify_bookmarkAndExpiredReviewAsDue() {
        Question bookmarked = new Question();
        bookmarked.bookmarked = true;
        assertEquals(ReviewState.DUE,
                ReviewState.classify(bookmarked, NOW));

        Question expired = new Question();
        expired.reviewed = true;
        expired.reviewedAt = NOW - ReviewState.REVIEW_INTERVAL_MS - 1L;
        assertEquals(ReviewState.DUE,
                ReviewState.classify(expired, NOW));
    }

    @Test
    public void classify_recentReviewAsMastered() {
        Question reviewed = new Question();
        reviewed.reviewed = true;
        reviewed.reviewedAt = NOW - 60_000L;
        assertEquals(ReviewState.MASTERED,
                ReviewState.classify(reviewed, NOW));
    }
}
