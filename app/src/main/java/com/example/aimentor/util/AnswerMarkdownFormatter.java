package com.example.aimentor.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Converts legacy stored answer sections to Markdown without damaging LaTeX. */
public final class AnswerMarkdownFormatter {

    private static final Pattern BLOCK_MATH =
            Pattern.compile("\\$\\$\\s*(.+?)\\s*\\$\\$[.,;:]?", Pattern.DOTALL);
    private static final Pattern X_RIGHT_ARROW =
            Pattern.compile("\\\\xrightarrow\\{([^{}]+)\\}");
    private static final Set<String> PRIMARY_HEADINGS = primaryHeadings();
    private static final Set<String> SECONDARY_HEADINGS = secondaryHeadings();

    private AnswerMarkdownFormatter() { }

    public static String format(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        StringBuilder markdown = new StringBuilder();
        boolean firstContentLine = true;
        for (String rawLine : value.trim().split("\\R", -1)) {
            String line = rawLine.trim();
            if (firstContentLine && isRepeatedMetadata(line)) {
                firstContentLine = false;
                continue;
            }
            if (!line.isEmpty()) firstContentLine = false;
            String heading = line.endsWith(":")
                    ? line.substring(0, line.length() - 1).trim() : line;
            if (PRIMARY_HEADINGS.contains(heading)) {
                line = "## " + heading;
            } else if (SECONDARY_HEADINGS.contains(heading)) {
                line = "### " + heading;
            }
            if (markdown.length() > 0) markdown.append('\n');
            markdown.append(line);
        }
        return normalizeMath(markdown.toString().trim());
    }

    private static String normalizeMath(String markdown) {
        String compatibleLatex = X_RIGHT_ARROW.matcher(markdown)
                .replaceAll("\\\\stackrel{$1}{\\\\longrightarrow}");
        String isolatedBlocks = BLOCK_MATH.matcher(compatibleLatex)
                .replaceAll("\n\n\\$\\$$1\\$\\$\n\n");
        return isolatedBlocks.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static boolean isRepeatedMetadata(String line) {
        return line.startsWith("Subject:") || line.startsWith("Môn học:");
    }

    private static Set<String> primaryHeadings() {
        Set<String> result = new HashSet<>();
        result.add("Answer");
        result.add("Câu trả lời");
        result.add("Quick answer");
        result.add("Trả lời ngắn");
        return result;
    }

    private static Set<String> secondaryHeadings() {
        Set<String> result = new HashSet<>();
        result.add("Key takeaway");
        result.add("Ý chính");
        result.add("Clear overview");
        result.add("Tổng quan dễ hiểu");
        result.add("Detailed explanation");
        result.add("Phân tích chi tiết");
        result.add("Important cautions");
        result.add("Điểm cần lưu ý");
        result.add("Check your understanding");
        result.add("Kiểm tra mức độ hiểu");
        result.add("Step-by-step");
        result.add("Giải thích từng bước");
        result.add("In simple terms");
        result.add("Nói một cách đơn giản");
        result.add("Key concepts");
        result.add("Khái niệm chính");
        result.add("Common mistakes to avoid");
        result.add("Những lỗi thường gặp cần tránh");
        result.add("Follow-up questions to practise");
        result.add("Câu hỏi luyện tập thêm");
        return result;
    }
}
