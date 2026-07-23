package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured result produced by an {@link AiEngine} for a single question.
 * Pure Java model (no Android dependency) so it can be unit tested on the JVM.
 */
public class AiAnswer {
    private String subject = "General";
    private String difficulty = "Intermediate";
    private String directAnswer = "";
    private String simplified = "";
    private final List<String> steps = new ArrayList<>();
    private final List<String> keyConcepts = new ArrayList<>();
    private final List<String> commonMistakes = new ArrayList<>();
    private final List<String> followUps = new ArrayList<>();
    private boolean reusedFromCache = false;
    private AnswerSource source = AnswerSource.LOCAL;
    private String modelName = "";

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDirectAnswer() { return directAnswer; }
    public void setDirectAnswer(String directAnswer) { this.directAnswer = directAnswer; }

    public String getSimplified() { return simplified; }
    public void setSimplified(String simplified) { this.simplified = simplified; }

    public List<String> getSteps() { return steps; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public List<String> getCommonMistakes() { return commonMistakes; }
    public List<String> getFollowUps() { return followUps; }

    public boolean isReusedFromCache() { return reusedFromCache; }
    public void setReusedFromCache(boolean reusedFromCache) { this.reusedFromCache = reusedFromCache; }

    public AnswerSource getSource() { return source; }
    public void setSource(AnswerSource source) {
        this.source = source == null ? AnswerSource.LEGACY : source;
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) {
        this.modelName = modelName == null ? "" : modelName.trim();
    }

    /**
     * Renders the structured answer into a human readable block of text that is
     * stored in the local database and shown to the student (works fully offline).
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject: ").append(subject)
          .append("   |   Level: ").append(difficulty).append("\n\n");
        sb.append("Answer\n").append(directAnswer).append("\n");
        if (!steps.isEmpty()) {
            sb.append("\nStep-by-step\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        }
        if (simplified != null && !simplified.isEmpty()) {
            sb.append("\nIn simple terms\n").append(simplified).append("\n");
        }
        if (!keyConcepts.isEmpty()) {
            sb.append("\nKey concepts\n");
            for (String c : keyConcepts) sb.append("- ").append(c).append("\n");
        }
        if (!commonMistakes.isEmpty()) {
            sb.append("\nCommon mistakes to avoid\n");
            for (String c : commonMistakes) sb.append("- ").append(c).append("\n");
        }
        if (!followUps.isEmpty()) {
            sb.append("\nFollow-up questions to practise\n");
            for (String c : followUps) sb.append("- ").append(c).append("\n");
        }
        if (reusedFromCache) {
            sb.append("\n(Answer reused from a very similar earlier question to save AI cost.)");
        }
        return sb.toString().trim();
    }
}
