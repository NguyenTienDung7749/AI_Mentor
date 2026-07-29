package com.example.aimentor.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts legacy stored answer sections to Markdown without damaging LaTeX. */
public final class AnswerMarkdownFormatter {

    // Matches $$...$$ block math (lazy) with optional trailing punctuation.
    private static final Pattern BLOCK_MATH =
            Pattern.compile("\\$\\$\\s*(.+?)\\s*\\$\\$[.,;:]?", Pattern.DOTALL);
    // Matches $...$ inline math (lazy, no newlines).
    private static final Pattern INLINE_MATH =
            Pattern.compile("(?<!\\$)\\$(?!\\$)(.+?)(?<!\\$)\\$(?!\\$)");
    // Matches \[...\] display math (LaTeX standard).
    private static final Pattern BACKSLASH_BRACKET_MATH =
            Pattern.compile("\\\\\\[\\s*(.+?)\\s*\\\\]", Pattern.DOTALL);
    // Matches [ ... ] that contain LaTeX commands (heuristic: content has backslash commands).
    private static final Pattern BRACKET_BLOCK_MATH =
            Pattern.compile("^\\[\\s*(.+?)\\s*]\\s*$", Pattern.MULTILINE);
    // Matches a stray single $ at the start of what looks like a LaTeX expression
    // (e.g. "$r^2" without a closing $). We'll wrap it as inline math.
    private static final Pattern STRAY_DOLLAR =
            Pattern.compile("(?<!\\$)\\$(?!\\$)([A-Za-z0-9_\\\\{^]+(?:\\{[^}]*\\}|[A-Za-z0-9_^])*)(?!\\$)(?=[\\s.,;:!?)]|$)");
    private static final Pattern X_RIGHT_ARROW =
            Pattern.compile("\\\\xrightarrow\\{([^{}]+)\\}");
    // Matches any double-escaped backslash before a LaTeX command or short
    // spacing token (\, \; \! \> \  etc.).  Covers every named command
    // (alphabetic) plus the common punctuation-style commands.
    private static final Pattern DOUBLE_BACKSLASH_CMD =
            Pattern.compile("\\\\\\\\((?:[a-zA-Z]+|[,;!> ]))");
    private static final Set<String> PRIMARY_HEADINGS = primaryHeadings();
    private static final Set<String> SECONDARY_HEADINGS = secondaryHeadings();

    private AnswerMarkdownFormatter() { }

    public static String format(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String shortAnswer = formatShortAnswer(value);
        if (shortAnswer != null) return normalizeMath(shortAnswer);
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

    private static String formatShortAnswer(String value) {
        StringBuilder body = new StringBuilder();
        String label = null;
        boolean collecting = false;
        for (String rawLine : value.trim().split("\\R", -1)) {
            String line = rawLine.trim();
            if (isRepeatedMetadata(line)) continue;
            if ("Quick answer".equals(line) || "Quick answer:".equals(line)) {
                label = "Quick answer";
                collecting = true;
                continue;
            }
            if ("Tr\u1ea3 l\u1eddi ng\u1eafn".equals(line)
                    || "Tr\u1ea3 l\u1eddi ng\u1eafn:".equals(line)) {
                label = "Tr\u1ea3 l\u1eddi ng\u1eafn";
                collecting = true;
                continue;
            }
            if (collecting && ("Key takeaway".equals(line)
                    || "Key takeaway:".equals(line)
                    || "\u00dd ch\u00ednh".equals(line)
                    || "\u00dd ch\u00ednh:".equals(line))) {
                break;
            }
            if (!collecting) continue;
            if (body.length() > 0) body.append('\n');
            body.append(line);
        }
        if (label == null || body.toString().trim().isEmpty()) return null;
        return "**" + label + "**\n\n" + body.toString().trim();
    }

    private static String normalizeMath(String markdown) {
        // Fix double-escaped backslashes that leak from JSON inside math zones.
        String result = fixDoubleBackslashesInMath(markdown);

        // Replace unsupported \xrightarrow with compatible stackrel
        result = X_RIGHT_ARROW.matcher(result)
                .replaceAll("\\\\stackrel{$1}{\\\\longrightarrow}");

        // Convert \[...\] display math to $$...$$
        result = BACKSLASH_BRACKET_MATH.matcher(result)
                .replaceAll("\n\n\\$\\$$1\\$\\$\n\n");

        // Convert [ ... ] that contain LaTeX commands to $$...$$
        // Only matches lines that start with [ and contain backslash commands
        StringBuffer sb = new StringBuffer();
        Matcher bm = BRACKET_BLOCK_MATH.matcher(result);
        while (bm.find()) {
            String content = bm.group(1);
            // Only convert if the content looks like LaTeX (has backslash commands)
            if (content != null && content.contains("\\")) {
                bm.appendReplacement(sb, "\n\\$\\$" + Matcher.quoteReplacement(content) + "\\$\\$\n");
            }
        }
        bm.appendTail(sb);
        result = sb.toString();

        // Fix stray single $ followed by LaTeX-like content without a closing $
        // by wrapping them as inline math $...$
        result = fixStrayDollars(result);

        // Ensure $$ block math sits on its own lines for JLatexMathPlugin.
        result = BLOCK_MATH.matcher(result)
                .replaceAll("\n\n\\$\\$$1\\$\\$\n\n");
        return result.replaceAll("\\n{3,}", "\n\n").trim();
    }

    /**
     * Finds stray single-$ sequences (e.g. "$r^2" without closing $) and wraps
     * them as proper inline math "$r^2$". Only acts on sequences that are NOT
     * already properly delimited.
     */
    private static String fixStrayDollars(String text) {
        // First check: if all $ signs are properly paired, do nothing
        Matcher inlineMatcher = INLINE_MATH.matcher(text);
        // Build a set of positions covered by valid inline math
        boolean[] covered = new boolean[text.length()];
        while (inlineMatcher.find()) {
            for (int i = inlineMatcher.start(); i < inlineMatcher.end(); i++) {
                covered[i] = true;
            }
        }
        // Also mark positions covered by block math $$...$$
        Matcher blockMatcher = BLOCK_MATH.matcher(text);
        while (blockMatcher.find()) {
            for (int i = blockMatcher.start(); i < blockMatcher.end(); i++) {
                covered[i] = true;
            }
        }
        // Find lone $ that are not covered
        StringBuilder sb2 = new StringBuilder(text);
        int offset = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '$' && !covered[i]) {
                // Check this isn't part of $$
                if (i + 1 < text.length() && text.charAt(i + 1) == '$') continue;
                if (i > 0 && text.charAt(i - 1) == '$') continue;
                // Find the extent of the LaTeX-like content after this $
                int j = i + 1;
                while (j < text.length() && text.charAt(j) != ' '
                        && text.charAt(j) != '\n' && text.charAt(j) != '$'
                        && text.charAt(j) != ',' && text.charAt(j) != '.') {
                    j++;
                }
                // Also grab content after space if it looks like it continues
                // (e.g. "$r^2" -> just grab until non-math)
                if (j > i + 1) {
                    String mathContent = text.substring(i + 1, j);
                    // Only fix if it looks like math (has ^, _, \, digits mixed with letters)
                    if (mathContent.matches(".*[\\\\^_{}].*") || mathContent.matches("[a-zA-Z][0-9].*")) {
                        // Insert closing $ after the content
                        sb2.insert(j + offset, '$');
                        offset++;
                    }
                }
            }
        }
        return sb2.toString();
    }

    /**
     * Walks through the text, identifies math zones ($...$ and $$...$$),
     * and fixes double-escaped backslashes (\\cmd -> \cmd) only inside those
     * zones.  Non-math text is left untouched.
     */
    private static String fixDoubleBackslashesInMath(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        int len = text.length();
        while (i < len) {
            if (text.charAt(i) == '$') {
                boolean block = i + 1 < len && text.charAt(i + 1) == '$';
                String delimiter = block ? "$$" : "$";
                int start = i;
                int contentStart = i + delimiter.length();
                // Find the closing delimiter
                int closingIdx = findClosingDelimiter(text, contentStart, delimiter);
                if (closingIdx < 0) {
                    // No closing delimiter found; output the rest as-is
                    out.append(text, i, len);
                    break;
                }
                int end = closingIdx + delimiter.length();
                // Extract the math content and fix double backslashes
                String mathContent = text.substring(contentStart, closingIdx);
                String fixedMath = DOUBLE_BACKSLASH_CMD.matcher(mathContent)
                        .replaceAll("\\\\$1");
                out.append(delimiter).append(fixedMath).append(delimiter);
                // Skip optional trailing punctuation after block math
                if (block && end < len) {
                    char after = text.charAt(end);
                    if (after == '.' || after == ',' || after == ';' || after == ':') {
                        end++;
                    }
                }
                i = end;
            } else {
                // Also fix double backslashes in non-math context because some
                // answers have double-escaped LaTeX commands even outside $ delimiters
                // (e.g. in plain text that later gets wrapped by $...$).
                out.append(text.charAt(i));
                i++;
            }
        }

        // Also do a global pass for double backslashes that appear outside
        // math delimiters (some answers have inconsistent escaping)
        return DOUBLE_BACKSLASH_CMD.matcher(out.toString()).replaceAll("\\\\$1");
    }

    /**
     * Finds the index of the closing math delimiter starting from {@code from}.
     * Returns -1 if not found.
     */
    private static int findClosingDelimiter(String text, int from, String delimiter) {
        int idx = text.indexOf(delimiter, from);
        if (delimiter.equals("$")) {
            // For single $, make sure we don't match $$ (block delimiter)
            while (idx >= 0 && idx + 1 < text.length() && text.charAt(idx + 1) == '$') {
                idx = text.indexOf(delimiter, idx + 2);
            }
            // Also skip if preceded by another $ (we're inside $$)
            while (idx > 0 && text.charAt(idx - 1) == '$') {
                idx = text.indexOf(delimiter, idx + 1);
                if (idx >= 0 && idx + 1 < text.length() && text.charAt(idx + 1) == '$') {
                    idx = text.indexOf(delimiter, idx + 2);
                }
            }
        }
        return idx;
    }

    private static boolean isRepeatedMetadata(String line) {
        return line.startsWith("Subject:") || line.startsWith("M\u00f4n h\u1ecdc:");
    }

    private static Set<String> primaryHeadings() {
        Set<String> result = new HashSet<>();
        result.add("Answer");
        result.add("C\u00e2u tr\u1ea3 l\u1eddi");
        result.add("Quick answer");
        result.add("Tr\u1ea3 l\u1eddi ng\u1eafn");
        return result;
    }

    private static Set<String> secondaryHeadings() {
        Set<String> result = new HashSet<>();
        result.add("Key takeaway");
        result.add("\u00dd ch\u00ednh");
        result.add("Clear overview");
        result.add("T\u1ed5ng quan d\u1ec5 hi\u1ec3u");
        result.add("Detailed explanation");
        result.add("Ph\u00e2n t\u00edch chi ti\u1ebft");
        result.add("Important cautions");
        result.add("\u0110i\u1ec3m c\u1ea7n l\u01b0u \u00fd");
        result.add("Check your understanding");
        result.add("Ki\u1ec3m tra m\u1ee9c \u0111\u1ed9 hi\u1ec3u");
        result.add("Step-by-step");
        result.add("Gi\u1ea3i th\u00edch t\u1eebng b\u01b0\u1edbc");
        result.add("In simple terms");
        result.add("N\u00f3i m\u1ed9t c\u00e1ch \u0111\u01a1n gi\u1ea3n");
        result.add("Key concepts");
        result.add("Kh\u00e1i ni\u1ec7m ch\u00ednh");
        result.add("Common mistakes to avoid");
        result.add("Nh\u1eefng l\u1ed7i th\u01b0\u1eddng g\u1eb7p c\u1ea7n tr\u00e1nh");
        result.add("Follow-up questions to practise");
        result.add("C\u00e2u h\u1ecfi luy\u1ec7n t\u1eadp th\u00eam");
        return result;
    }
}
