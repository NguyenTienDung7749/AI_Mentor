package com.example.aimentor.ai;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Converts structured or plain model output into the app's answer model. */
public final class AiResponseParser {

    private static final Gson GSON = new Gson();

    private AiResponseParser() { }

    public static AiAnswer parse(String rawText, String fallbackSubject,
                                 String fallbackDifficulty) {
        String clean = stripMarkdownFence(rawText);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Empty AI response");
        }

        if (looksLikeJsonCandidate(clean)) {
            try {
                StructuredAnswer data = GSON.fromJson(clean, StructuredAnswer.class);
                if (data == null || isBlank(data.directAnswer)) {
                    throw new IllegalArgumentException("Missing direct answer");
                }
                return toAnswer(data, fallbackSubject, fallbackDifficulty);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("Malformed structured answer", e);
            }
        }

        AiAnswer plain = new AiAnswer();
        plain.setSubject(defaultIfBlank(fallbackSubject, SubjectClassifier.GENERAL));
        plain.setDifficulty(defaultIfBlank(fallbackDifficulty, "Intermediate"));
        plain.setDirectAnswer(clean);
        return plain;
    }

    private static AiAnswer toAnswer(StructuredAnswer data, String fallbackSubject,
                                     String fallbackDifficulty) {
        AiAnswer answer = new AiAnswer();
        answer.setSubject(defaultIfBlank(data.subject,
                defaultIfBlank(fallbackSubject, SubjectClassifier.GENERAL)));
        answer.setDifficulty(defaultIfBlank(data.difficulty,
                defaultIfBlank(fallbackDifficulty, "Intermediate")));
        answer.setDirectAnswer(data.directAnswer.trim());
        answer.setSimplified(defaultIfBlank(data.simplified, ""));
        addNonBlank(answer.getSteps(), data.steps);
        addNonBlank(answer.getKeyConcepts(), data.keyConcepts);
        addNonBlank(answer.getCommonMistakes(), data.commonMistakes);
        addNonBlank(answer.getFollowUps(), data.followUps);
        return answer;
    }

    private static void addNonBlank(List<String> target, List<String> source) {
        if (source == null) return;
        for (String item : source) {
            if (!isBlank(item)) target.add(item.trim());
        }
    }

    private static boolean looksLikeJsonCandidate(String text) {
        // A response beginning or ending like JSON must never silently become
        // a plain-text answer when the object is malformed or truncated.
        return text.startsWith("{") || text.endsWith("}");
    }

    private static String stripMarkdownFence(String rawText) {
        if (rawText == null) return "";
        String text = rawText.trim();
        if (!text.startsWith("```")) return text;

        int firstLineEnd = text.indexOf('\n');
        int closingFence = text.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) return text;
        return text.substring(firstLineEnd + 1, closingFence).trim();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class StructuredAnswer {
        String subject;
        String difficulty;

        @SerializedName(value = "directAnswer",
                alternate = {"direct_answer", "answer"})
        String directAnswer;

        String simplified;
        List<String> steps = new ArrayList<>();
        List<String> keyConcepts = new ArrayList<>();
        List<String> commonMistakes = new ArrayList<>();
        List<String> followUps = new ArrayList<>();
    }
}
