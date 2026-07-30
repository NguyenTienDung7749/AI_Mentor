package com.example.aimentor.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Builds a deterministic key only for exact reusable text-question inputs. */
public final class AnswerCacheKey {

    private AnswerCacheKey() { }

    public static String create(
            String question, String subjectHint,
            String explanationStyle, String educationLevel) {
        String canonical = normalize(question) + "\n"
                + normalize(subjectHint) + "\n"
                + normalize(explanationStyle) + "\n"
                + normalize(educationLevel);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                output.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException impossibleOnAndroid) {
            throw new IllegalStateException(
                    "SHA-256 is not available", impossibleOnAndroid);
        }
    }

    static String normalize(String value) {
        if (value == null) return "";
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
