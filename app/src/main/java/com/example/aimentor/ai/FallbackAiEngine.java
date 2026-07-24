package com.example.aimentor.ai;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Uses the remote engine first and returns deterministic offline guidance when
 * the provider cannot produce a safe, complete answer.
 */
public class FallbackAiEngine implements AiEngine {

    private static final long DEFAULT_REMOTE_DEADLINE_MS = 60_000L;
    private static final ExecutorService REMOTE_EXECUTOR = new ThreadPoolExecutor(
            0, 1, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable, "ai-remote");
                thread.setDaemon(true);
                return thread;
            });

    private final AiEngine remoteEngine;
    private final AiEngine localEngine;
    private final ExecutorService remoteExecutor;
    private final long remoteDeadlineMs;

    public FallbackAiEngine(AiEngine remoteEngine, AiEngine localEngine) {
        this(remoteEngine, localEngine, REMOTE_EXECUTOR, DEFAULT_REMOTE_DEADLINE_MS);
    }

    FallbackAiEngine(AiEngine remoteEngine, AiEngine localEngine,
                     ExecutorService remoteExecutor, long remoteDeadlineMs) {
        if (remoteEngine == null || localEngine == null) {
            throw new IllegalArgumentException("Both AI engines are required.");
        }
        if (remoteExecutor == null || remoteDeadlineMs <= 0L) {
            throw new IllegalArgumentException("A valid remote deadline is required.");
        }
        this.remoteEngine = remoteEngine;
        this.localEngine = localEngine;
        this.remoteExecutor = remoteExecutor;
        this.remoteDeadlineMs = remoteDeadlineMs;
    }

    @Override
    public AiAnswer answer(String question, String educationLevel,
                           String explanationStyle, String subjectHint) {
        return answer(question, educationLevel, explanationStyle,
                subjectHint, "");
    }

    @Override
    public AiAnswer answer(String question, String educationLevel,
                           String explanationStyle, String subjectHint,
                           String subjects) {
        Future<AiAnswer> remoteTask;
        try {
            remoteTask = remoteExecutor.submit(() -> remoteEngine.answer(
                    question, educationLevel, explanationStyle,
                    subjectHint, subjects));
        } catch (RejectedExecutionException ignored) {
            // A previous platform DNS call may still be blocked. Do not queue
            // more remote work behind it; provide offline help immediately.
            return offlineFallback(question, educationLevel,
                    explanationStyle, subjectHint, subjects);
        }

        try {
            return remoteTask.get(remoteDeadlineMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
            remoteTask.cancel(true);
            return offlineFallback(question, educationLevel,
                    explanationStyle, subjectHint, subjects);
        } catch (InterruptedException ignored) {
            remoteTask.cancel(true);
            Thread.currentThread().interrupt();
            return offlineFallback(question, educationLevel,
                    explanationStyle, subjectHint, subjects);
        } catch (ExecutionException ignored) {
            return offlineFallback(question, educationLevel,
                    explanationStyle, subjectHint, subjects);
        }
    }

    @Override
    public List<QuizQuestion> generateQuiz(String subject, int count) {
        return localEngine.generateQuiz(subject, count);
    }

    @Override
    public List<QuizQuestion> generateQuiz(QuizGenerationConfig config) {
        try {
            List<QuizQuestion> remoteQuiz = remoteEngine.generateQuiz(config);
            if (remoteQuiz != null && !remoteQuiz.isEmpty()) {
                return remoteQuiz;
            }
        } catch (RuntimeException ignored) {
            // Preserve a usable quiz when the provider is unavailable or malformed.
        }
        return localEngine.generateQuiz(config);
    }

    @Override
    public String name() {
        return remoteEngine.name() + " + offline fallback";
    }

    private AiAnswer offlineFallback(String question, String educationLevel,
                                     String explanationStyle, String subjectHint,
                                     String subjects) {
        AiAnswer fallback = localEngine.answer(question, educationLevel,
                explanationStyle, subjectHint, subjects);
        fallback.setSource(AnswerSource.LOCAL_FALLBACK);
        fallback.setModelName(localEngine.name());
        return fallback;
    }
}
