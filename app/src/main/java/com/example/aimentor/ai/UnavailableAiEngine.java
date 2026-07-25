package com.example.aimentor.ai;

import java.util.List;

/** Keeps local quiz practice available while reporting missing online AI setup. */
final class UnavailableAiEngine implements AiEngine {

    private final LocalAiEngine localQuizEngine;

    UnavailableAiEngine(LocalAiEngine localQuizEngine) {
        this.localQuizEngine = localQuizEngine;
    }

    @Override
    public AiAnswer answer(String question, String educationLevel,
                           String explanationStyle, String subjectHint) {
        throw AiServiceException.configuration("Online AI is not configured.");
    }

    @Override
    public List<QuizQuestion> generateQuiz(String subject, int count) {
        return localQuizEngine.generateQuiz(subject, count);
    }

    @Override
    public List<QuizQuestion> generateQuiz(QuizGenerationConfig config) {
        return localQuizEngine.generateQuiz(config);
    }

    @Override
    public String name() {
        return "Online AI";
    }
}
