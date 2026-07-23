package com.example.aimentor.ai;

import java.util.List;

/**
 * Strategy interface for answer / quiz generation.
 *
 * The project ships with {@link LocalAiEngine}, a fully offline rule based
 * implementation, so the application is functional without any paid API key
 * or backend. A production build can supply a remote implementation
 * (e.g. Google Gemini or OpenAI) that reads its key from local.properties /
 * BuildConfig without changing any caller — no secret is committed to the repo.
 */
public interface AiEngine {

    /** Generate a structured answer for a student question. */
    AiAnswer answer(String question, String educationLevel, String explanationStyle, String subjectHint);

    /** Generate a set of practice questions for a subject. */
    List<QuizQuestion> generateQuiz(String subject, int count);

    /** Human readable engine name (shown in the UI / reports). */
    String name();
}
