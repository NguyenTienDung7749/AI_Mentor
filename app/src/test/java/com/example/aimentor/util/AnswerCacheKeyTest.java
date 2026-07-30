package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class AnswerCacheKeyTest {

    @Test
    public void equivalentWhitespaceAndCase_createSameKey() {
        String first = AnswerCacheKey.create(
                "  What   is Gravity? ", "Science",
                "Step-by-step", "High School");
        String second = AnswerCacheKey.create(
                "what is gravity?", " science ",
                "STEP-BY-STEP", "high school");

        assertEquals(first, second);
    }

    @Test
    public void subjectStyleAndLevel_arePartOfExactMatch() {
        String baseline = AnswerCacheKey.create(
                "Explain arrays", "Programming",
                "Detailed", "University");

        assertNotEquals(baseline, AnswerCacheKey.create(
                "Explain arrays", "General",
                "Detailed", "University"));
        assertNotEquals(baseline, AnswerCacheKey.create(
                "Explain arrays", "Programming",
                "Short", "University"));
        assertNotEquals(baseline, AnswerCacheKey.create(
                "Explain arrays", "Programming",
                "Detailed", "Middle School"));
    }
}
