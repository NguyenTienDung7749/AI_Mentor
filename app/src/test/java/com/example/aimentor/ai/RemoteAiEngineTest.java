package com.example.aimentor.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class RemoteAiEngineTest {

    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void answer_success_parsesStructuredResponseAndSendsAuth() throws Exception {
        String content = "{\"subject\":\"Mathematics\","
                + "\"difficulty\":\"Intermediate\","
                + "\"directAnswer\":\"4\","
                + "\"steps\":[\"Add 2 and 2\"]}";
        server.enqueue(jsonResponse(200, completionBody(content)));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiAnswer answer = engine.answer(
                "What is 2 + 2?", "High School", "Step-by-step", "Auto");

        assertEquals("4", answer.getDirectAnswer());
        assertEquals("Mathematics", answer.getSubject());
        assertEquals(AnswerSource.REMOTE, answer.getSource());
        assertEquals("test-model", answer.getModelName());
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue(request != null);
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        assertEquals("/v1/chat/completions", request.getPath());
        JsonObject requestBody = JsonParser.parseString(
                request.getBody().readUtf8()).getAsJsonObject();
        assertEquals("DeepSeek-V4-Flash", requestBody.get("model").getAsString());
        assertEquals(1200, requestBody.get("max_tokens").getAsInt());
        assertTrue(!requestBody.get("stream").getAsBoolean());
    }

    @Test
    public void answer_http401_isSanitizedAndNotRetryable() {
        server.enqueue(jsonResponse(401, "{\"error\":{\"message\":\"bad key\"}}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.HTTP, error.getKind());
        assertEquals(401, error.getHttpStatus());
        assertTrue(!error.isRetryable());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    public void answer_http429_retriesOnceAndRecovers() {
        server.enqueue(jsonResponse(429, "{\"error\":{\"message\":\"busy\"}}"));
        server.enqueue(jsonResponse(200,
                completionBody("{\"directAnswer\":\"Recovered\"}")));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiAnswer answer = engine.answer(
                "Question", "High School", "Detailed", "Auto");

        assertEquals("Recovered", answer.getDirectAnswer());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void answer_http500_stopsAfterOneRetry() {
        server.enqueue(jsonResponse(500, "{\"error\":{\"message\":\"server\"}}"));
        server.enqueue(jsonResponse(500, "{\"error\":{\"message\":\"server\"}}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(500, error.getHttpStatus());
        assertTrue(error.isRetryable());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void answer_missingChoices_isRejected() {
        server.enqueue(jsonResponse(200, "{\"model\":\"test\",\"choices\":[]}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.INVALID_RESPONSE, error.getKind());
    }

    @Test
    public void answer_tokenLimit_retriesWithExtendedBudgetAndRecovers() throws Exception {
        server.enqueue(jsonResponse(200,
                completionBodyWithFinish("{\"directAnswer\":\"partial", "length")));
        server.enqueue(jsonResponse(200,
                completionBody("{\"directAnswer\":\"Complete answer\"}")));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiAnswer answer = engine.answer(
                "Hard question", "University", "Detailed", "Auto");

        assertEquals("Complete answer", answer.getDirectAnswer());
        RecordedRequest first = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest second = server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue(first != null);
        assertTrue(second != null);
        assertEquals(1200, maxTokens(first));
        assertEquals(2400, maxTokens(second));
    }

    @Test
    public void answer_tokenLimitTwice_isRejectedWithoutSavingPartialContent() {
        server.enqueue(jsonResponse(200,
                completionBodyWithFinish("{\"directAnswer\":\"partial one", "length")));
        server.enqueue(jsonResponse(200,
                completionBodyWithFinish("{\"directAnswer\":\"partial two", "length")));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.INCOMPLETE_RESPONSE, error.getKind());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void answer_reasoningWithoutFinalContent_isRejected() {
        String body = "{\"model\":\"test\",\"choices\":[{"
                + "\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":\"\","
                + "\"reasoning_content\":\"private reasoning only\"},"
                + "\"finish_reason\":\"stop\"}]}";
        server.enqueue(jsonResponse(200, body));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.INVALID_RESPONSE, error.getKind());
    }

    @Test
    public void answer_fullCallTimeout_isNotRetried() {
        server.enqueue(jsonResponse(200, completionBody("Late answer"))
                .setBodyDelay(250, TimeUnit.MILLISECONDS));
        RemoteAiEngine engine = engine(1_000, 50);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.TIMEOUT, error.getKind());
        assertTrue(!error.isRetryable());
        assertEquals(1, server.getRequestCount());
    }

    private RemoteAiEngine engine(long connectTimeoutMs, long readTimeoutMs) {
        return new RemoteAiEngine(server.url("/v1/").toString(),
                "test-key", connectTimeoutMs, readTimeoutMs);
    }

    private AiServiceException expectFailure(RemoteAiEngine engine) {
        try {
            engine.answer("Question", "High School", "Detailed", "Auto");
            fail("Expected AiServiceException");
            return null;
        } catch (AiServiceException e) {
            return e;
        }
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private String completionBody(String content) {
        return completionBodyWithFinish(content, "stop");
    }

    private String completionBodyWithFinish(String content, String finishReason) {
        return "{\"model\":\"test-model\",\"choices\":[{"
                + "\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":"
                + new Gson().toJson(content) + "},"
                + "\"finish_reason\":" + new Gson().toJson(finishReason) + "}]}";
    }

    private int maxTokens(RecordedRequest request) {
        JsonObject body = JsonParser.parseString(
                request.getBody().readUtf8()).getAsJsonObject();
        return body.get("max_tokens").getAsInt();
    }
}
