package com.example.aimentor.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts legacy stored answer sections to Markdown without damaging LaTeX. */
public final class AnswerMarkdownFormatter {

    private static final Pattern BLOCK_MATH =
            Pattern.compile("\\$\\$\\s*(.+?)\\s*\\$\\$[.,;:]?", Pattern.DOTALL);
    private static final Pattern X_RIGHT_ARROW =
            Pattern.compile("\\\\xrightarrow\\{([^{}]+)\\}");
    private static final Pattern DOUBLE_BACKSLASH_CMD =
            Pattern.compile("\\\\\\\\((?:frac|sqrt|sum|prod|int|lim|infty|"
                    + "alpha|beta|gamma|delta|theta|lambda|mu|pi|sigma|omega|"
                    + "cdot|times|div|pm|mp|leq|geq|neq|approx|equiv|"
                    + "rightarrow|leftarrow|Rightarrow|Leftarrow|"
                    + "text|mathrm|mathbf|begin|end|left|right|over|under|"
                    + "hat|bar|vec|dot|ddot|tilde|log|ln|sin|cos|tan|"
                    + "partial|nabla|forall|exists|cup|cap|subset|supset|"
                    + "in|notin|to|mapsto|circ|oplus|otimes|binom|"
                    + "stackrel|overset|underset|quad|qquad|"
                    + "displaystyle|textstyle|operatorname)\\b)");
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
        // Replace unsupported \xrightarrow with compatible stackrel
        String compatibleLatex = X_RIGHT_ARROW.matcher(markdown)
                .replaceAll("\\\\stackrel{$1}{\\\\longrightarrow}");
        // Fix double-escaped backslashes that leak from JSON: \\frac -> \frac
        compatibleLatex = DOUBLE_BACKSLASH_CMD.matcher(compatibleLatex)
                .replaceAll("\\\\$1");
        // Ensure $$ block math sits on its own lines for JLatexMathPlugin
        String isolatedBlocks = BLOCK_MATH.matcher(compatibleLatex)
                .replaceAll("\n\n\\$\\$$1\\$\\$\n\n");
        return isolatedBlocks.replaceAll("\\n{3,}", "\n\n").trim();
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
