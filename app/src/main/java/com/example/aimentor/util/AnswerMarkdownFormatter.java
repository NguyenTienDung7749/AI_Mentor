package com.example.aimentor.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts legacy stored answer sections to Markdown without damaging LaTeX. */
public final class AnswerMarkdownFormatter {

    private static final Pattern SINGLE_DOLLAR_MATH =
            Pattern.compile("(?<!\\$)\\$([^$\\r\\n]+)\\$(?!\\$)");
    private static final Set<String> HEADINGS = headings();

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
            if (HEADINGS.contains(heading)) {
                line = "## " + heading;
            }
            if (markdown.length() > 0) markdown.append('\n');
            markdown.append(line);
        }
        return normalizeInlineMath(markdown.toString().trim());
    }

    private static String normalizeInlineMath(String markdown) {
        Matcher matcher = SINGLE_DOLLAR_MATH.matcher(markdown);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement("$$" + matcher.group(1) + "$$"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean isRepeatedMetadata(String line) {
        return line.startsWith("Subject:") || line.startsWith("Môn học:");
    }

    private static Set<String> headings() {
        Set<String> result = new HashSet<>();
        result.add("Answer");
        result.add("Câu trả lời");
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
