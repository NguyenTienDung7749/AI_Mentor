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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        assertEquals(1, Gamification.levelForXp(-10));
        assertEquals(1, Gamification.levelForXp(99));
        assertEquals(2, Gamification.levelForXp(100));
        assertEquals(3, Gamification.levelForXp(250));
        assertEquals(50, Gamification.xpIntoLevel(250));
        assertEquals(50, Gamification.xpToNextLevel(250));
        List<String> badges = Gamification.earnedBadges(10, 5, 26, 5);
        assertTrue(badges.contains("Curious Mind (10 questions)"));
        assertTrue(badges.contains("Quiz Enthusiast (5 quizzes)"));
        assertTrue(badges.contains("Sharp Shooter (25 correct)"));
        assertTrue(badges.contains("Level 5 Reached"));
    }

    @Test
    public void progressAccuracy_handlesEmptyAndInvalidTotals() {
        assertEquals(0, Gamification.accuracyPercent(0, 0));
        assertEquals(0, Gamification.accuracyPercent(5, -1));
        assertEquals(0, Gamification.accuracyPercent(-2, 10));
        assertEquals(70, Gamification.accuracyPercent(7, 10));
        assertEquals(100, Gamification.accuracyPercent(12, 10));
    }

    @Test
    public void security_hashRoundTrip() {
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword("Study2026", salt);
        assertTrue(hash.startsWith("pbkdf2-"));
        assertFalse(SecurityUtils.needsUpgrade(hash));
        assertTrue(SecurityUtils.verify("Study2026", salt, hash));
        assertFalse(SecurityUtils.verify("wrong", salt, hash));
    }

    @Test
    public void security_legacyHashStillVerifiesAndRequestsUpgrade() throws Exception {
        String salt = "00112233445566778899aabbccddeeff";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(hexToBytes(salt));
        String legacyHash = toHex(digest.digest(
                "Study2026".getBytes(StandardCharsets.UTF_8)));

        assertTrue(SecurityUtils.verify("Study2026", salt, legacyHash));
        assertFalse(SecurityUtils.verify("wrong", salt, legacyHash));
        assertTrue(SecurityUtils.needsUpgrade(legacyHash));
    }

    @Test
    public void security_corruptCredentialFailsClosed() {
        assertFalse(SecurityUtils.verify("Study2026", "not-hex", "broken"));
        assertFalse(SecurityUtils.verify("Study2026", "00",
                "pbkdf2-sha256$999999999$00"));
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

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
