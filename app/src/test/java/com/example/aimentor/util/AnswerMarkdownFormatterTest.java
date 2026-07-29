package com.example.aimentor.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AnswerMarkdownFormatterTest {

    @Test
    public void format_preservesLatexAndStylesKnownSections() {
        String stored = "Subject: Mathematics | Level: Advanced\n\n"
                + "Answer\nThe roots are $x = \\\\frac{-b \\\\pm "
                + "\\\\sqrt{b^2-4ac}}{2a}$.\n\n"
                + "Key concepts\n- **Discriminant** matters.";

        String formatted = AnswerMarkdownFormatter.format(stored);

        assertFalse(formatted.contains("Subject:"));
        assertTrue(formatted.contains("## Answer"));
        // Double backslashes should be normalized to single backslashes
        assertTrue(formatted.contains(
                "$x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$"));
        assertTrue(formatted.contains("**Discriminant**"));
    }

    @Test
    public void format_doesNotDoubleWrapExistingMath() {
        String formatted = AnswerMarkdownFormatter.format(
                "Answer\nUse $$A \\xrightarrow{heat} B$$.");

        assertTrue(formatted.contains(
                "\n\n$$A \\stackrel{heat}{\\longrightarrow} B$$"));
        assertFalse(formatted.contains("$$$$"));
    }

    @Test
    public void format_stylesShortSectionsAndNormalizesUnsupportedArrow() {
        String formatted = AnswerMarkdownFormatter.format(
                "Trả lời ngắn\nCây tạo $O_2$.\n\n"
                        + "Ý chính\n$$6CO_2 \\xrightarrow{light} C_6H_12O_6$$.");

        assertTrue(formatted.contains("**Trả lời ngắn**"));
        assertFalse(formatted.contains("Ý chính"));
        assertTrue(formatted.contains("$O_2$"));
        assertFalse(formatted.contains("xrightarrow"));
    }

    @Test
    public void format_fixesDoubleBackslashSpacing() {
        // \, (thin space) is common in integrals; should be preserved
        String stored = "Trả lời ngắn\n"
                + "$$\\\\int f(x)\\\\,dx = F(x) + C$$";

        String formatted = AnswerMarkdownFormatter.format(stored);

        assertTrue(formatted.contains("\\int f(x)\\,dx = F(x) + C"));
        assertFalse(formatted.contains("\\\\int"));
        assertFalse(formatted.contains("\\\\,"));
    }

    @Test
    public void format_preservesInlineMathDollars() {
        String stored = "Trả lời ngắn\n"
                + "trong đó $F(x)$ là hàm bất kỳ và $C$ là hằng số.";

        String formatted = AnswerMarkdownFormatter.format(stored);

        assertTrue(formatted.contains("$F(x)$"));
        assertTrue(formatted.contains("$C$"));
    }
}
