package com.example.aimentor.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Very small abuse / spam detector for submitted questions. Pure Java.
 * Implements the "Abuse detection to prevent spam or inappropriate content"
 * requirement at a basic level (client side).
 */
public final class ContentModerator {

    private static final String[] BANNED = {
            "fuck", "shit", "bitch", "asshole", "bastard", "dick", "porn", "nsfw"};
    private static final Pattern[] BANNED_WORD_PATTERNS = buildBannedWordPatterns();

    public static final class Result {
        public final boolean allowed;
        public final String reason;
        Result(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
    }

    private ContentModerator() { }

    public static Result check(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new Result(false, "Please type a question first.");
        }
        String t = text.toLowerCase(Locale.ROOT);

        for (Pattern pattern : BANNED_WORD_PATTERNS) {
            if (pattern.matcher(t).find()) {
                return new Result(false, "Your message contains inappropriate language. "
                        + "Please rephrase it as an academic question.");
            }
        }
        if (isSpam(text)) {
            return new Result(false, "That looks like spam. Please ask a real study question.");
        }
        return new Result(true, "");
    }

    private static boolean isSpam(String text) {
        String t = text.trim();
        if (t.length() < 3) return true;
        // one character repeated many times, e.g. "aaaaaaaaaa"
        int maxRun = 1, run = 1;
        for (int i = 1; i < t.length(); i++) {
            if (t.charAt(i) == t.charAt(i - 1)) {
                run++;
                if (run > maxRun) maxRun = run;
            } else {
                run = 1;
            }
        }
        if (maxRun >= 8) return true;
        // no letters or digits at all (only symbols)
        boolean hasAlnum = false;
        for (int i = 0; i < t.length(); i++) {
            if (Character.isLetterOrDigit(t.charAt(i))) { hasAlnum = true; break; }
        }
        return !hasAlnum;
    }

    private static Pattern[] buildBannedWordPatterns() {
        Pattern[] patterns = new Pattern[BANNED.length];
        for (int i = 0; i < BANNED.length; i++) {
            patterns[i] = Pattern.compile(
                    "(?<![\\p{L}\\p{N}_])" + Pattern.quote(BANNED[i])
                            + "(?![\\p{L}\\p{N}_])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        return patterns;
    }
}
