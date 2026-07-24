package com.example.aimentor.ai;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A single practice question. Choice questions use options/correctIndex while
 * open-response questions use one or more accepted textual answers.
 */
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        SHORT_ANSWER,
        FILL_IN_THE_BLANK
    }

    private final String prompt;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;
    private final String subject;
    private final String difficulty;
    private final Type type;
    private final List<String> acceptedAnswers;

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject) {
        this(prompt, options, correctIndex, explanation, subject,
                "Intermediate", Type.MULTIPLE_CHOICE, null);
    }

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject, String difficulty,
                        Type type) {
        this(prompt, options, correctIndex, explanation, subject, difficulty,
                type, null);
    }

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject, String difficulty,
                        Type type, List<String> acceptedAnswers) {
        this.prompt = prompt == null ? "" : prompt;
        this.options = options == null
                ? new ArrayList<>() : new ArrayList<>(options);
        this.correctIndex = correctIndex;
        this.explanation = explanation == null ? "" : explanation;
        this.subject = SubjectClassifier.normalize(subject);
        this.difficulty = normalizeDifficulty(difficulty);
        this.type = type == null ? Type.MULTIPLE_CHOICE : type;
        this.acceptedAnswers = cleanAnswers(acceptedAnswers);
    }

    public String getPrompt() { return prompt; }
    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
    public String getSubject() { return subject; }
    public String getDifficulty() { return difficulty; }
    public Type getType() { return type; }
    public List<String> getAcceptedAnswers() {
        return Collections.unmodifiableList(acceptedAnswers);
    }

    public boolean isCorrect(int selectedIndex) {
        return !requiresTextAnswer() && selectedIndex == correctIndex;
    }

    public boolean requiresTextAnswer() {
        return type == Type.SHORT_ANSWER || type == Type.FILL_IN_THE_BLANK;
    }

    public boolean isCorrect(String answer) {
        if (!requiresTextAnswer()) return false;
        String normalized = normalizeAnswer(answer);
        if (normalized.isEmpty()) return false;
        for (String accepted : acceptedAnswers) {
            if (normalized.equals(normalizeAnswer(accepted))) return true;
        }
        return false;
    }

    public String getDisplayAnswer() {
        if (!acceptedAnswers.isEmpty()) return acceptedAnswers.get(0);
        if (correctIndex >= 0 && correctIndex < options.size()) {
            return options.get(correctIndex);
        }
        return "";
    }

    private List<String> cleanAnswers(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        if (values == null) return cleaned;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                cleaned.add(value.trim());
            }
        }
        return cleaned;
    }

    private String normalizeAnswer(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeDifficulty(String value) {
        if ("Beginner".equalsIgnoreCase(value)) return "Beginner";
        if ("Advanced".equalsIgnoreCase(value)) return "Advanced";
        return "Intermediate";
    }
}
