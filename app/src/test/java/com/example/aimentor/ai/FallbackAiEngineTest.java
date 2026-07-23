package com.example.aimentor.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

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

    private static class TestEngine implements AiEngine {
        private final String engineName;
        private final boolean fail;
        int answerCalls;
        int quizCalls;

        TestEngine(String engineName, boolean fail) {
            this.engineName = engineName;
            this.fail = fail;
        }

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint) {
            answerCalls++;
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
