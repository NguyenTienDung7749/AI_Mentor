package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * A single practice / quiz question. Pure Java model (JVM unit testable).
 */
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        MULTIPLE_CHOICE,
        TRUE_FALSE
    }

    private final String prompt;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;
    private final String subject;
    private final String difficulty;
    private final Type type;

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject) {
        this(prompt, options, correctIndex, explanation, subject,
                "Intermediate", Type.MULTIPLE_CHOICE);
    }

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject, String difficulty,
                        Type type) {
        this.prompt = prompt;
        this.options = options != null ? options : new ArrayList<String>();
        this.correctIndex = correctIndex;
        this.explanation = explanation != null ? explanation : "";
        this.subject = SubjectClassifier.normalize(subject);
        this.difficulty = normalizeDifficulty(difficulty);
        this.type = type == null ? Type.MULTIPLE_CHOICE : type;
    }

    public String getPrompt() { return prompt; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
    public String getSubject() { return subject; }
    public String getDifficulty() { return difficulty; }
    public Type getType() { return type; }

    public boolean isCorrect(int selectedIndex) { return selectedIndex == correctIndex; }

    private String normalizeDifficulty(String value) {
        if ("Beginner".equalsIgnoreCase(value)) return "Beginner";
        if ("Advanced".equalsIgnoreCase(value)) return "Advanced";
        return "Intermediate";
    }
}
