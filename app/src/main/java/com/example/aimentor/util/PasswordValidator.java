package com.example.aimentor.util;

/**
 * Deterministic password-strength validation. Pure Java (JVM unit testable).
 * Implements the "Password strength validation" security requirement.
 */
public final class PasswordValidator {

    public enum Strength { WEAK, MEDIUM, STRONG }

    private PasswordValidator() { }

    public static Strength evaluate(String password) {
        if (!meetsMinimum(password)) return Strength.WEAK;
        boolean longPassword = password.length() >= 12;
        boolean mixedCase = hasLower(password) && hasUpper(password);
        return longPassword && mixedCase && hasSymbol(password)
                ? Strength.STRONG : Strength.MEDIUM;
    }

    /** Minimum bar: eight characters containing at least one letter and digit. */
    public static boolean isAcceptable(String password) {
        return meetsMinimum(password);
    }

    public static String requirementMessage() {
        return "Use at least 8 characters with letters and numbers (a symbol makes it stronger).";
    }

    private static boolean meetsMinimum(String password) {
        return password != null
                && password.length() >= 8
                && hasLetter(password)
                && hasDigit(password);
    }

    private static boolean hasLetter(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) return true;
        }
        return false;
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
