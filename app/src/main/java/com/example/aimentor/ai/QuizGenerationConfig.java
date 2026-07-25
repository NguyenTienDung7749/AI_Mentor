package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Context used to generate a personalized quiz without exposing Room models. */
public class QuizGenerationConfig {
    private final String subject;
    private final String difficulty;
    private final int count;
    private final List<String> studyTopics;

    public QuizGenerationConfig(String subject, String difficulty, int count,
                                List<String> studyTopics) {
        this.subject = SubjectClassifier.normalize(subject);
        this.difficulty = normalizeDifficulty(difficulty);
        this.count = Math.max(1, Math.min(count, 20));
        this.studyTopics = studyTopics == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(studyTopics));
    }

    public String getSubject() { return subject; }
    public String getDifficulty() { return difficulty; }
    public int getCount() { return count; }
    public List<String> getStudyTopics() { return studyTopics; }

    private String normalizeDifficulty(String value) {
        if ("Beginner".equalsIgnoreCase(value)) return "Beginner";
        if ("Advanced".equalsIgnoreCase(value)) return "Advanced";
        return "Intermediate";
    }
}
