package com.example.aimentor.ai;

import java.util.List;

/**
 * Strategy interface for answer / quiz generation.
 *
 * {@link AiEngineFactory} selects a remote engine with offline fallback when a
 * local demo key is configured and otherwise selects {@link LocalAiEngine}.
 * Callers depend only on this interface.
 */
public interface AiEngine {

    /** Generate a structured answer for a student question. */
    AiAnswer answer(String question, String educationLevel, String explanationStyle, String subjectHint);

    /**
     * Generate a structured answer with optional interests from the student's
     * local learning profile. Existing engines may ignore the extra context.
     */
    default AiAnswer answer(String question, String educationLevel,
                            String explanationStyle, String subjectHint,
                            String subjects) {
        return answer(question, educationLevel, explanationStyle, subjectHint);
    }

    /** Generate a set of practice questions for a subject. */
    List<QuizQuestion> generateQuiz(String subject, int count);

    /** Generate a quiz using optional difficulty and study-history context. */
    default List<QuizQuestion> generateQuiz(QuizGenerationConfig config) {
        return generateQuiz(config.getSubject(), config.getCount());
    }

    /** Human readable engine name (shown in the UI / reports). */
    String name();
}
