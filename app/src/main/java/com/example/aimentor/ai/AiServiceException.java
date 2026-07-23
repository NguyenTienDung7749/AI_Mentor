package com.example.aimentor.ai;

/** Sanitized failure raised by a remote AI provider. */
public class AiServiceException extends RuntimeException {

    public enum Kind {
        CONFIGURATION,
        NETWORK,
        HTTP,
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

    public Kind getKind() {
        return kind;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return kind == Kind.NETWORK
                || (kind == Kind.HTTP
                && (httpStatus == 408 || httpStatus == 429 || httpStatus >= 500));
    }
}
