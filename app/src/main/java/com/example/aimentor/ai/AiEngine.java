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

    /** Generate a set of practice questions for a subject. */
    List<QuizQuestion> generateQuiz(String subject, int count);

    /** Human readable engine name (shown in the UI / reports). */
    String name();
}
