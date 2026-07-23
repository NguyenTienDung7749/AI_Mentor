package com.example.aimentor.util;

/**
 * Deterministic password-strength validation. Pure Java (JVM unit testable).
 * Implements the "Password strength validation" security requirement.
 */
public final class PasswordValidator {

    public enum Strength { WEAK, MEDIUM, STRONG }

    private PasswordValidator() { }

    public static Strength evaluate(String password) {
        if (password == null) return Strength.WEAK;
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (hasLower(password) && hasUpper(password)) score++;
        if (hasDigit(password)) score++;
        if (hasSymbol(password)) score++;

        if (password.length() < 6 || score <= 1) return Strength.WEAK;
        if (score <= 3) return Strength.MEDIUM;
        return Strength.STRONG;
    }

    /** Minimum bar to allow account creation: at least medium strength. */
    public static boolean isAcceptable(String password) {
        return evaluate(password) != Strength.WEAK;
    }

    public static String requirementMessage() {
        return "Use at least 8 characters with letters and numbers (a symbol makes it stronger).";
    }

    private static boolean hasLower(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isLowerCase(s.charAt(i))) return true;
        return false;
    }
    private static boolean hasUpper(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isUpperCase(s.charAt(i))) return true;
        return false;
    }
    private static boolean hasDigit(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) return true;
        return false;
    }
    private static boolean hasSymbol(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) return true;
        }
        return false;
    }
}
