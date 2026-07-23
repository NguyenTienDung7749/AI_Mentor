package com.example.aimentor.ai;

import java.io.InterruptedIOException;

/** Sanitized failure raised by a remote AI provider. */
public class AiServiceException extends RuntimeException {

    public enum Kind {
        CONFIGURATION,
        NETWORK,
        TIMEOUT,
        HTTP,
        INCOMPLETE_RESPONSE,
        INVALID_RESPONSE
    }

    private final Kind kind;
    private final int httpStatus;

    private AiServiceException(Kind kind, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public static AiServiceException configuration(String message) {
        return new AiServiceException(Kind.CONFIGURATION, 0, message, null);
    }

    public static AiServiceException network(Throwable cause) {
        if (cause instanceof InterruptedIOException) {
            return new AiServiceException(Kind.TIMEOUT, 0,
                    "The AI service took too long to respond.", cause);
        }
        return new AiServiceException(Kind.NETWORK, 0,
                "The AI service could not be reached.", cause);
    }

    public static AiServiceException http(int status) {
        return new AiServiceException(Kind.HTTP, status,
                "The AI service returned HTTP " + status + ".", null);
    }

    public static AiServiceException invalidResponse() {
        return new AiServiceException(Kind.INVALID_RESPONSE, 0,
                "The AI service returned an invalid response.", null);
    }

    public static AiServiceException incompleteResponse() {
        return new AiServiceException(Kind.INCOMPLETE_RESPONSE, 0,
                "The AI service could not complete the answer within the token limit.", null);
    }

    public Kind getKind() {
        return kind;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        // A full-call timeout is not retried because doing so doubles the
        // visible wait. The hybrid engine immediately provides offline help.
        return kind == Kind.NETWORK
                || (kind == Kind.HTTP
                && (httpStatus == 408 || httpStatus == 429 || httpStatus >= 500));
    }
}
