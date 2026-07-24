package com.example.aimentor.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure-Java helpers for topic insights derived from genuine saved questions. */
public final class LearningAnalytics {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "and", "for", "with", "that", "this", "what", "when", "where",
            "which", "why", "how", "are", "was", "were", "does", "can", "could",
            "please", "explain", "about", "into", "from", "your", "you",
            "la", "va", "cua", "cho", "voi", "mot", "nhung", "nhu", "the", "nao",
            "tai", "sao", "gi", "hay", "toi", "ban", "duoc", "trong", "ve"));

    private LearningAnalytics() { }

    /**
     * Returns recurring terms and the number of distinct saved questions in
     * which each term appeared. A term repeated inside one question counts once.
     */
    public static List<TopicFrequency> repeatedTopics(
            List<String> questionTexts, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        if (questionTexts != null) {
            for (String text : questionTexts) {
                Set<String> termsInQuestion = extractTerms(text);
                for (String term : termsInQuestion) {
                    counts.put(term, counts.getOrDefault(term, 0) + 1);
                }
            }
        }
        List<TopicFrequency> topics = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                topics.add(new TopicFrequency(entry.getKey(), entry.getValue()));
            }
        }
        topics.sort(Comparator
                .comparingInt((TopicFrequency topic) -> topic.questionCount)
                .reversed()
                .thenComparing(topic -> topic.topic));
        if (limit > 0 && topics.size() > limit) {
            return new ArrayList<>(topics.subList(0, limit));
        }
        return topics;
    }

    private static Set<String> extractTerms(String text) {
        Set<String> terms = new HashSet<>();
        if (text == null) return terms;
        String normalized = Normalizer.normalize(
                        text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        for (String token : normalized.split("[^\\p{L}\\p{N}+#]+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)
                    && !token.matches("\\d+")) {
                terms.add(token);
            }
        }
        return terms;
    }

    public static final class TopicFrequency {
        public final String topic;
        public final int questionCount;

        public TopicFrequency(String topic, int questionCount) {
            this.topic = topic;
            this.questionCount = questionCount;
        }
    }
}
