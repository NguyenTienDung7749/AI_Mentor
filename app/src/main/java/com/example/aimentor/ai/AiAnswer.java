package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private AnswerSource source = AnswerSource.LOCAL;
    private String modelName = "";

    public String getSubject() { return subject; }
    public void setSubject(String subject) {
        this.subject = SubjectClassifier.normalize(subject);
    }

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
     * shown to the student. The repository decides whether the answer is eligible
     * to be stored in the local database.
     */
    public String toDisplayString() {
        return toDisplayString("");
    }

    public String toDisplayString(String questionLanguageHint) {
        boolean vietnamese = isVietnamese(questionLanguageHint);
        String displayedSubject = vietnamese ? vietnameseSubject(subject) : subject;
        String displayedDifficulty = vietnamese
                ? vietnameseDifficulty(difficulty) : difficulty;
        StringBuilder sb = new StringBuilder();
        sb.append(vietnamese ? "Môn học: " : "Subject: ").append(displayedSubject)
          .append(vietnamese ? "   |   Trình độ: " : "   |   Level: ")
          .append(displayedDifficulty).append("\n\n");
        sb.append(vietnamese ? "Câu trả lời\n" : "Answer\n")
                .append(cleanMarkdown(directAnswer)).append("\n");
        if (!steps.isEmpty()) {
            sb.append(vietnamese ? "\nGiải thích từng bước\n" : "\nStep-by-step\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ")
                        .append(cleanMarkdown(steps.get(i))).append("\n");
            }
        }
        if (simplified != null && !simplified.isEmpty()) {
            sb.append(vietnamese ? "\nNói một cách đơn giản\n" : "\nIn simple terms\n")
                    .append(cleanMarkdown(simplified)).append("\n");
        }
        if (!keyConcepts.isEmpty()) {
            sb.append(vietnamese ? "\nKhái niệm chính\n" : "\nKey concepts\n");
            for (String c : keyConcepts) {
                sb.append("- ").append(cleanMarkdown(c)).append("\n");
            }
        }
        if (!commonMistakes.isEmpty()) {
            sb.append(vietnamese
                    ? "\nNhững lỗi thường gặp cần tránh\n"
                    : "\nCommon mistakes to avoid\n");
            for (String c : commonMistakes) {
                sb.append("- ").append(cleanMarkdown(c)).append("\n");
            }
        }
        if (!followUps.isEmpty()) {
            sb.append(vietnamese
                    ? "\nCâu hỏi luyện tập thêm\n"
                    : "\nFollow-up questions to practise\n");
            for (String c : followUps) {
                sb.append("- ").append(cleanMarkdown(c)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String cleanMarkdown(String value) {
        if (value == null) return "";
        return value.replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .trim();
    }

    private boolean isVietnamese(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches("(?s).*[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệ"
                + "íìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ].*")) {
            return true;
        }
        String padded = " " + lower.replaceAll("[^a-z]+", " ").trim() + " ";
        String[] commonWords = {
                " toi ", " ban ", " cua ", " nguoi ", " khong ",
                " tai sao ", " nhu the nao ", " la gi ", " mot "
        };
        int matches = 0;
        for (String word : commonWords) {
            if (padded.contains(word) && ++matches >= 2) return true;
        }
        return false;
    }

    private String vietnameseSubject(String value) {
        if (value == null) return "Tổng quát";
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mathematics": return "Toán học";
            case "science": return "Khoa học";
            case "programming": return "Lập trình";
            case "history": return "Lịch sử";
            case "languages": return "Ngôn ngữ";
            default: return "Tổng quát";
        }
    }

    private String vietnameseDifficulty(String value) {
        if (value == null) return "Trung cấp";
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "beginner": return "Cơ bản";
            case "advanced": return "Nâng cao";
            default: return "Trung cấp";
        }
    }
}
