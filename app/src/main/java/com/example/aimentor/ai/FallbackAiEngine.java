package com.example.aimentor.ai;

import java.util.List;

/**
 * Uses the remote engine first and returns deterministic offline guidance when
 * the provider cannot produce a safe, complete answer.
 */
public class FallbackAiEngine implements AiEngine {

    private final AiEngine remoteEngine;
    private final AiEngine localEngine;

    public FallbackAiEngine(AiEngine remoteEngine, AiEngine localEngine) {
        if (remoteEngine == null || localEngine == null) {
            throw new IllegalArgumentException("Both AI engines are required.");
        }
        this.remoteEngine = remoteEngine;
        this.localEngine = localEngine;
    }

    @Override
    public AiAnswer answer(String question, String educationLevel,
                           String explanationStyle, String subjectHint) {
        try {
            return remoteEngine.answer(question, educationLevel,
                    explanationStyle, subjectHint);
        } catch (AiServiceException ignored) {
            AiAnswer fallback = localEngine.answer(question, educationLevel,
                    explanationStyle, subjectHint);
            fallback.setSource(AnswerSource.LOCAL_FALLBACK);
            fallback.setModelName(localEngine.name());
            return fallback;
        }
    }

    @Override
    public List<QuizQuestion> generateQuiz(String subject, int count) {
        return localEngine.generateQuiz(subject, count);
    }

    @Override
    public String name() {
        return remoteEngine.name() + " + offline fallback";
    }
}
