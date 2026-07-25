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
        assertTrue(formatted.contains(
                "$x = \\\\frac{-b \\\\pm \\\\sqrt{b^2-4ac}}{2a}$"));
        assertTrue(formatted.contains("**Discriminant**"));
    }

    @Test
    public void format_doesNotDoubleWrapExistingMath() {
        String formatted = AnswerMarkdownFormatter.format(
                "Answer\nUse $$\\\\sqrt{2}$$.");

        assertTrue(formatted.contains("\n\n$$\\\\sqrt{2}$$"));
        assertFalse(formatted.contains("$$$$"));
    }

    @Test
    public void format_stylesShortSectionsAndNormalizesUnsupportedArrow() {
        String formatted = AnswerMarkdownFormatter.format(
                "Trả lời ngắn\nCây tạo $O_2$.\n\n"
                        + "Ý chính\n$$6CO_2 \\xrightarrow{light} C_6H_12O_6$$.");

        assertTrue(formatted.contains("## Trả lời ngắn"));
        assertTrue(formatted.contains("### Ý chính"));
        assertTrue(formatted.contains("$O_2$"));
        assertTrue(formatted.contains(
                "\\stackrel{light}{\\longrightarrow}"));
    }
}
