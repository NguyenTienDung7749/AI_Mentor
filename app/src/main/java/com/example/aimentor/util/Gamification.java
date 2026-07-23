package com.example.aimentor.util;

import java.util.ArrayList;
import java.util.List;

/**
 * XP / level / badge rules for the gamification feature. Pure Java, JVM testable.
 * Level curve: every 100 XP is one level (level 1 at 0 XP).
 */
public final class Gamification {

    public static final int XP_PER_LEVEL = 100;
    public static final int XP_ASK = 10;       // asking a question
    public static final int XP_REVIEW = 2;     // reviewing a saved answer
    public static final int XP_QUIZ_CORRECT = 5; // per correct quiz answer

    private Gamification() { }

    public static int levelForXp(int xp) {
        if (xp < 0) xp = 0;
        return (xp / XP_PER_LEVEL) + 1;
    }

    /** XP already earned inside the current level (0..XP_PER_LEVEL-1). */
    public static int xpIntoLevel(int xp) {
        if (xp < 0) xp = 0;
        return xp % XP_PER_LEVEL;
    }

    /** XP still required to reach the next level. */
    public static int xpToNextLevel(int xp) {
        return XP_PER_LEVEL - xpIntoLevel(xp);
    }

    /** Stable percentage for progress UI, including empty or malformed totals. */
    public static int accuracyPercent(int correct, int total) {
        if (total <= 0) return 0;
        int safeCorrect = Math.max(0, Math.min(correct, total));
        return Math.round((safeCorrect * 100f) / total);
    }

    public static String titleForLevel(int level) {
        if (level >= 15) return "Scholar";
        if (level >= 10) return "Expert Learner";
        if (level >= 5) return "Rising Star";
        if (level >= 2) return "Explorer";
        return "Beginner";
    }

    /**
     * Returns the list of achievement badges earned for the given activity totals.
     */
    public static List<String> earnedBadges(int totalQuestions, int quizzesCompleted,
                                            int correctQuizAnswers, int level) {
        List<String> badges = new ArrayList<>();
        if (totalQuestions >= 1) badges.add("First Question");
        if (totalQuestions >= 10) badges.add("Curious Mind (10 questions)");
        if (totalQuestions >= 50) badges.add("Knowledge Seeker (50 questions)");
        if (quizzesCompleted >= 1) badges.add("Quiz Starter");
        if (quizzesCompleted >= 5) badges.add("Quiz Enthusiast (5 quizzes)");
        if (correctQuizAnswers >= 25) badges.add("Sharp Shooter (25 correct)");
        if (level >= 5) badges.add("Level 5 Reached");
        if (level >= 10) badges.add("Level 10 Reached");
        return badges;
    }
}
