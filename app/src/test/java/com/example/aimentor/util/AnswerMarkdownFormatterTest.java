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
                "$$x = \\\\frac{-b \\\\pm \\\\sqrt{b^2-4ac}}{2a}$$"));
        assertTrue(formatted.contains("**Discriminant**"));
    }

    @Test
    public void format_doesNotDoubleWrapExistingMath() {
        String formatted = AnswerMarkdownFormatter.format(
                "Answer\nUse $$\\\\sqrt{2}$$.");

        assertTrue(formatted.contains("$$\\\\sqrt{2}$$"));
        assertFalse(formatted.contains("$$$$"));
    }
}
