package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * A single practice / quiz question. Pure Java model (JVM unit testable).
 */
public class QuizQuestion {
    private final String prompt;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;
    private final String subject;

    public QuizQuestion(String prompt, List<String> options, int correctIndex,
                        String explanation, String subject) {
        this.prompt = prompt;
        this.options = options != null ? options : new ArrayList<String>();
        this.correctIndex = correctIndex;
        this.explanation = explanation != null ? explanation : "";
        this.subject = subject != null ? subject : "General";
    }

    public String getPrompt() { return prompt; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
    public String getSubject() { return subject; }

    public boolean isCorrect(int selectedIndex) { return selectedIndex == correctIndex; }
}
