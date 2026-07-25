package com.example.aimentor.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiAnswerDisplayTest {

    @Test
    public void shortStyle_usesCompactEnglishForm() {
        String display = sampleAnswer().toDisplayString(
                "What is gravity?", "Short");

        assertTrue(display.contains("Quick answer"));
        assertTrue(display.contains("Key takeaway"));
        assertFalse(display.contains("Step-by-step"));
        assertFalse(display.contains("Detailed explanation"));
    }

    @Test
    public void detailedStyle_usesThematicEnglishForm() {
        String display = sampleAnswer().toDisplayString(
                "Explain gravity in detail.", "Detailed");

        assertTrue(display.contains("Detailed explanation"));
        assertTrue(display.contains("Important cautions"));
        assertTrue(display.contains("Check your understanding"));
        assertFalse(display.contains("Step-by-step"));
    }

    @Test
    public void stepStyle_keepsSequentialEnglishForm() {
        String display = sampleAnswer().toDisplayString(
                "Explain gravity step by step.", "Step-by-step");

        assertTrue(display.contains("Step-by-step"));
        assertTrue(display.contains("1. Objects have mass."));
        assertFalse(display.contains("Detailed explanation"));
    }

    @Test
    public void vietnameseQuestion_usesVietnameseHeadingsForEveryStyle() {
        AiAnswer answer = sampleAnswer();

        assertTrue(answer.toDisplayString(
                "Trọng lực là gì?", "Short").contains("Trả lời ngắn"));
        assertTrue(answer.toDisplayString(
                "Giải thích chi tiết về trọng lực.", "Detailed")
                .contains("Phân tích chi tiết"));
        assertTrue(answer.toDisplayString(
                "Giải thích trọng lực từng bước.", "Step-by-step")
                .contains("Giải thích từng bước"));
    }

    @Test
    public void quizCount_isClampedToTwenty() {
        QuizGenerationConfig maximum = new QuizGenerationConfig(
                "Science", "Intermediate", 99, null);
        QuizGenerationConfig minimum = new QuizGenerationConfig(
                "Science", "Intermediate", 0, null);

        assertTrue(maximum.getCount() == 20);
        assertTrue(minimum.getCount() == 1);
    }

    private AiAnswer sampleAnswer() {
        AiAnswer answer = new AiAnswer();
        answer.setSubject("Science");
        answer.setDifficulty("Intermediate");
        answer.setDirectAnswer("Gravity attracts objects with mass.");
        answer.setSimplified("Masses pull one another.");
        answer.getSteps().add("Objects have mass.");
        answer.getSteps().add("Their masses create attraction.");
        answer.getKeyConcepts().add("Mass");
        answer.getCommonMistakes().add("Gravity is not limited to Earth.");
        answer.getFollowUps().add("How does distance affect gravity?");
        return answer;
    }
}
