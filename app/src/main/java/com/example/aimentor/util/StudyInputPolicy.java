package com.example.aimentor.util;

/** Shared limits for text that can be sent to the study mentor. */
public final class StudyInputPolicy {

    public static final int MAX_QUESTION_CHARS = 6_000;

    public static final class LimitedText {
        public final String text;
        public final boolean truncated;

        LimitedText(String text, boolean truncated) {
            this.text = text;
            this.truncated = truncated;
        }
    }

    private StudyInputPolicy() { }

    /**
     * Removes unsupported null characters and caps OCR output before it reaches
     * the editable question field or a remote request.
     */
    public static LimitedText limitOcrText(String rawText) {
        String clean = rawText == null
                ? "" : rawText.replace("\u0000", "").trim();
        if (clean.length() <= MAX_QUESTION_CHARS) {
            return new LimitedText(clean, false);
        }
        return new LimitedText(
                clean.substring(0, MAX_QUESTION_CHARS).trim(), true);
    }
}
