package com.example.aimentor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.aimentor.ai.AiAnswer;
import com.example.aimentor.ai.LocalAiEngine;
import com.example.aimentor.ai.MathEvaluator;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.util.ContentModerator;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.PasswordValidator;
import com.example.aimentor.util.SecurityUtils;
import com.example.aimentor.util.Validators;

import org.junit.Test;

import java.util.List;

/** JVM unit tests for the pure-Java study logic (run with ./gradlew test). */
public class StudyLogicTest {

    @Test
    public void mathEvaluator_respectsOrderOfOperations() {
        assertEquals(14.0, MathEvaluator.evaluate("2 + 3 * 4"), 0.0001);
        assertEquals(20.0, MathEvaluator.evaluate("(2 + 3) * 4"), 0.0001);
        assertEquals(8.0, MathEvaluator.evaluate("2 ^ 3"), 0.0001);
        assertTrue(MathEvaluator.looksLikeExpression("2 + 2 = ?"));
        assertFalse(MathEvaluator.looksLikeExpression("what is photosynthesis"));
    }

    @Test
    public void subjectClassifier_detectsSubjects() {
        assertEquals(SubjectClassifier.MATH, SubjectClassifier.classify("solve 2x + 6 = 14"));
        assertEquals(SubjectClassifier.SCIENCE, SubjectClassifier.classify("explain Newton's law of motion"));
        assertEquals(SubjectClassifier.PROGRAMMING, SubjectClassifier.classify("java for loop over an array"));
        assertEquals(SubjectClassifier.HISTORY, SubjectClassifier.classify("causes of the world war"));
        assertEquals(SubjectClassifier.GENERAL, SubjectClassifier.classify("hello mentor"));
    }

    @Test
    public void passwordValidator_gradesStrength() {
        assertEquals(PasswordValidator.Strength.WEAK, PasswordValidator.evaluate("abc"));
        assertTrue(PasswordValidator.isAcceptable("Study2026"));
        assertFalse(PasswordValidator.isAcceptable("123"));
    }

    @Test
    public void validators_checkEmail() {
        assertTrue(Validators.isValidEmail("student@example.com"));
        assertFalse(Validators.isValidEmail("not-an-email"));
        assertFalse(Validators.isValidEmail(""));
    }

    @Test
    public void gamification_levelsAndBadges() {
        assertEquals(1, Gamification.levelForXp(0));
        assertEquals(3, Gamification.levelForXp(250));
        List<String> badges = Gamification.earnedBadges(10, 5, 26, 5);
        assertTrue(badges.contains("Curious Mind (10 questions)"));
        assertTrue(badges.contains("Level 5 Reached"));
    }

    @Test
    public void security_hashRoundTrip() {
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword("Study2026", salt);
        assertTrue(SecurityUtils.verify("Study2026", salt, hash));
        assertFalse(SecurityUtils.verify("wrong", salt, hash));
    }

    @Test
    public void moderation_blocksSpam() {
        assertFalse(ContentModerator.check("aaaaaaaaaa").allowed);
        assertFalse(ContentModerator.check("").allowed);
        assertTrue(ContentModerator.check("What is gravity?").allowed);
    }

    @Test
    public void engine_producesStructuredAnswerAndQuiz() {
        LocalAiEngine engine = new LocalAiEngine();
        AiAnswer a = engine.answer("2 + 3 * 4 = ?", "high school", "step-by-step", "Auto");
        assertEquals(SubjectClassifier.MATH, a.getSubject());
        assertTrue(a.getDirectAnswer().contains("14"));
        assertFalse(a.getSteps().isEmpty());

        List<QuizQuestion> quiz = engine.generateQuiz(SubjectClassifier.SCIENCE, 5);
        assertEquals(5, quiz.size());
        boolean hasMultipleChoice = false;
        boolean hasTrueFalse = false;
        for (QuizQuestion q : quiz) {
            assertTrue(q.getCorrectIndex() >= 0);
            assertTrue(q.getCorrectIndex() < q.getOptions().size());
            assertFalse(q.getPrompt().isEmpty());
            assertFalse(q.getExplanation().isEmpty());
            hasMultipleChoice |= q.getType() == QuizQuestion.Type.MULTIPLE_CHOICE;
            hasTrueFalse |= q.getType() == QuizQuestion.Type.TRUE_FALSE;
        }
        assertTrue(hasMultipleChoice);
        assertTrue(hasTrueFalse);
    }
}
