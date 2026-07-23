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
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue(request != null);
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        assertEquals("/v1/chat/completions", request.getPath());
        JsonObject requestBody = JsonParser.parseString(
                request.getBody().readUtf8()).getAsJsonObject();
        assertEquals("auto", requestBody.get("model").getAsString());
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
    }

    @Test
    public void answer_http429_isMarkedRetryable() {
        server.enqueue(jsonResponse(429, "{\"error\":{\"message\":\"busy\"}}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(429, error.getHttpStatus());
        assertTrue(error.isRetryable());
    }

    @Test
    public void answer_http500_isMarkedRetryable() {
        server.enqueue(jsonResponse(500, "{\"error\":{\"message\":\"server\"}}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(500, error.getHttpStatus());
        assertTrue(error.isRetryable());
    }

    @Test
    public void answer_missingChoices_isRejected() {
        server.enqueue(jsonResponse(200, "{\"model\":\"test\",\"choices\":[]}"));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.INVALID_RESPONSE, error.getKind());
    }

    @Test
    public void answer_reasoningCutOffByTokenLimit_isRejected() {
        String body = "{\"model\":\"test\",\"choices\":[{"
                + "\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":\"\","
                + "\"reasoning_content\":\"unfinished reasoning\"},"
                + "\"finish_reason\":\"length\"}]}";
        server.enqueue(jsonResponse(200, body));
        RemoteAiEngine engine = engine(1_000, 1_000);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.INVALID_RESPONSE, error.getKind());
    }

    @Test
    public void answer_timeout_isNetworkFailure() {
        server.enqueue(jsonResponse(200, completionBody("Late answer"))
                .setBodyDelay(250, TimeUnit.MILLISECONDS));
        RemoteAiEngine engine = engine(1_000, 50);

        AiServiceException error = expectFailure(engine);

        assertEquals(AiServiceException.Kind.NETWORK, error.getKind());
        assertTrue(error.isRetryable());
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
        return "{\"model\":\"test-model\",\"choices\":[{"
                + "\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":"
                + new Gson().toJson(content) + "},"
                + "\"finish_reason\":\"stop\"}]}";
    }
}
