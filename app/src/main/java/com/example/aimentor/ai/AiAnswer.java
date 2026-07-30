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
    private boolean visionUncertain;
    private boolean outOfScope;

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

    public boolean isVisionUncertain() {
        return visionUncertain;
    }

    public void setVisionUncertain(boolean visionUncertain) {
        this.visionUncertain = visionUncertain;
    }

    public boolean isOutOfScope() { return outOfScope; }
    public void setOutOfScope(boolean outOfScope) {
        this.outOfScope = outOfScope;
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
        return toDisplayString(questionLanguageHint, "Step-by-step");
    }

    public String toDisplayString(
            String questionLanguageHint, String explanationStyle) {
        boolean vietnamese = isVietnamese(questionLanguageHint);
        if (outOfScope) return preserveFormatting(directAnswer);
        String displayedSubject = vietnamese ? vietnameseSubject(subject) : subject;
        String displayedDifficulty = vietnamese
                ? vietnameseDifficulty(difficulty) : difficulty;
        StringBuilder sb = new StringBuilder();
        sb.append(vietnamese ? "Môn học: " : "Subject: ").append(displayedSubject)
          .append(vietnamese ? "   |   Trình độ: " : "   |   Level: ")
          .append(displayedDifficulty).append("\n\n");
        String style = explanationStyle == null
                ? "" : explanationStyle.toLowerCase(Locale.ROOT);
        if (style.contains("short")) {
            sb.append(vietnamese ? "Trả lời ngắn\n" : "Quick answer\n")
                    .append(preserveFormatting(directAnswer));
            return sb.toString().trim();
        }
        if (style.contains("detail")) {
            sb.append(vietnamese ? "Câu trả lời\n" : "Answer\n")
                    .append(preserveFormatting(directAnswer)).append("\n");
            if (simplified != null && !simplified.isEmpty()) {
                sb.append(vietnamese ? "\nTổng quan dễ hiểu\n" : "\nClear overview\n")
                        .append(preserveFormatting(simplified)).append("\n");
            }
            appendBullets(sb, steps,
                    vietnamese ? "Phân tích chi tiết" : "Detailed explanation");
            appendBullets(sb, keyConcepts,
                    vietnamese ? "Khái niệm quan trọng" : "Key concepts");
            appendBullets(sb, commonMistakes,
                    vietnamese ? "Điểm cần lưu ý" : "Important cautions");
            appendBullets(sb, followUps,
                    vietnamese ? "Kiểm tra mức độ hiểu" : "Check your understanding");
            return sb.toString().trim();
        }

        sb.append(vietnamese ? "Câu trả lời\n" : "Answer\n")
                .append(preserveFormatting(directAnswer)).append("\n");
        if (!steps.isEmpty()) {
            sb.append(vietnamese ? "\nGiải thích từng bước\n" : "\nStep-by-step\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ")
                        .append(stripListPrefix(steps.get(i))).append("\n");
            }
        }
        if (simplified != null && !simplified.isEmpty()) {
            sb.append(vietnamese ? "\nNói một cách đơn giản\n" : "\nIn simple terms\n")
                    .append(preserveFormatting(simplified)).append("\n");
        }
        if (!keyConcepts.isEmpty()) {
            sb.append(vietnamese ? "\nKhái niệm chính\n" : "\nKey concepts\n");
            for (String c : keyConcepts) {
                sb.append("- ").append(stripListPrefix(c)).append("\n");
            }
        }
        if (!commonMistakes.isEmpty()) {
            sb.append(vietnamese
                    ? "\nNhững lỗi thường gặp cần tránh\n"
                    : "\nCommon mistakes to avoid\n");
            for (String c : commonMistakes) {
                sb.append("- ").append(stripListPrefix(c)).append("\n");
            }
        }
        if (!followUps.isEmpty()) {
            sb.append(vietnamese
                    ? "\nCâu hỏi luyện tập thêm\n"
                    : "\nFollow-up questions to practise\n");
            for (String c : followUps) {
                sb.append("- ").append(stripListPrefix(c)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void appendBullets(
            StringBuilder output, List<String> values, String heading) {
        if (values.isEmpty()) return;
        output.append("\n").append(heading).append("\n");
        for (String value : values) {
            output.append("- ").append(stripListPrefix(value)).append("\n");
        }
    }

    private String stripListPrefix(String value) {
        return preserveFormatting(value).replaceFirst(
                "^(?:\\d+[.)]|[-*])\\s+", "");
    }

    private String preserveFormatting(String value) {
        if (value == null) return "";
        return value.trim();
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
