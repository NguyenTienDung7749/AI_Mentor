package com.example.aimentor.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FallbackAiEngineTest {

    @Test
    public void answer_remoteSuccess_doesNotUseLocalEngine() {
        TestEngine remote = new TestEngine("remote", false);
        TestEngine local = new TestEngine("local", false);
        FallbackAiEngine engine = new FallbackAiEngine(remote, local);

        AiAnswer answer = engine.answer("Question", "High School", "Detailed", "Auto");

        assertEquals("remote answer", answer.getDirectAnswer());
        assertEquals(1, remote.answerCalls);
        assertEquals(0, local.answerCalls);
    }

    @Test
    public void answer_remoteFailure_returnsClearlyMarkedOfflineFallback() {
        TestEngine remote = new TestEngine("remote", true);
        TestEngine local = new TestEngine("local", false);
        FallbackAiEngine engine = new FallbackAiEngine(remote, local);

        AiAnswer answer = engine.answer("Question", "High School", "Detailed", "Auto");

        assertEquals("local answer", answer.getDirectAnswer());
        assertEquals(AnswerSource.LOCAL_FALLBACK, answer.getSource());
        assertEquals("local", answer.getModelName());
        assertEquals(1, remote.answerCalls);
        assertEquals(1, local.answerCalls);
    }

    @Test
    public void generateQuiz_remainsOfflineDuringThisBatch() {
        TestEngine remote = new TestEngine("remote", false);
        TestEngine local = new TestEngine("local", false);
        FallbackAiEngine engine = new FallbackAiEngine(remote, local);

        engine.generateQuiz("Science", 1);

        assertEquals(0, remote.quizCalls);
        assertEquals(1, local.quizCalls);
    }

    @Test
    public void answerSource_unknownStoredValue_becomesLegacy() {
        assertEquals(AnswerSource.LEGACY, AnswerSource.fromStorage("unknown"));
        assertEquals(AnswerSource.LEGACY, AnswerSource.fromStorage(null));
        assertEquals(AnswerSource.REMOTE, AnswerSource.fromStorage("REMOTE"));
    }

    @Test
    public void answer_remoteWorkerHangs_returnsFallbackAtIndependentDeadline() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TestEngine remote = new TestEngine("remote", false, 2_000L);
        TestEngine local = new TestEngine("local", false);
        FallbackAiEngine engine = new FallbackAiEngine(
                remote, local, executor, 40L);
        long startedAt = System.nanoTime();

        try {
            AiAnswer answer = engine.answer(
                    "Question", "High School", "Detailed", "Auto");
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

            assertEquals(AnswerSource.LOCAL_FALLBACK, answer.getSource());
            assertEquals(1, local.answerCalls);
            assertTrueDeadline(elapsedMs);
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertTrueDeadline(long elapsedMs) {
        if (elapsedMs >= 500L) {
            throw new AssertionError("Fallback exceeded deadline: " + elapsedMs + " ms");
        }
    }

    private static class TestEngine implements AiEngine {
        private final String engineName;
        private final boolean fail;
        private final long delayMs;
        int answerCalls;
        int quizCalls;

        TestEngine(String engineName, boolean fail) {
            this(engineName, fail, 0L);
        }

        TestEngine(String engineName, boolean fail, long delayMs) {
            this.engineName = engineName;
            this.fail = fail;
            this.delayMs = delayMs;
        }

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint) {
            answerCalls++;
            if (delayMs > 0L) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw AiServiceException.network(e);
                }
            }
            if (fail) {
                throw AiServiceException.http(500);
            }
            AiAnswer answer = new AiAnswer();
            answer.setDirectAnswer(engineName + " answer");
            answer.setSource(engineName.equals("remote")
                    ? AnswerSource.REMOTE : AnswerSource.LOCAL);
            answer.setModelName(engineName);
            return answer;
        }

        @Override
        public List<QuizQuestion> generateQuiz(String subject, int count) {
            quizCalls++;
            return Collections.emptyList();
        }

        @Override
        public String name() {
            return engineName;
        }
    }
}
