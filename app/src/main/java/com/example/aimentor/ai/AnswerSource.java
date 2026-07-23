package com.example.aimentor.ai;

/**
 * Identifies how an answer was produced without exposing credentials or
 * provider error details.
 */
public enum AnswerSource {
    REMOTE,
    LOCAL,
    LOCAL_FALLBACK,
    LEGACY;

    public static AnswerSource fromStorage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LEGACY;
        }
        try {
            return AnswerSource.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return LEGACY;
        }
    }
}
