package com.example.aimentor.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight, deterministic subject detector based on keyword matching.
 * Kept free of Android APIs so it can be unit tested on the JVM.
 */
public final class SubjectClassifier {

    public static final String MATH = "Mathematics";
    public static final String SCIENCE = "Science";
    public static final String PROGRAMMING = "Programming";
    public static final String HISTORY = "History";
    public static final String LANGUAGES = "Languages";
    public static final String GENERAL = "General";

    public static final List<String> SUBJECTS = Arrays.asList(
            MATH, SCIENCE, PROGRAMMING, HISTORY, LANGUAGES, GENERAL);

    private static final String[] MATH_KW = {
            "solve", "equation", "algebra", "geometry", "calculus", "derivative",
            "integral", "fraction", "percentage", "percent", "triangle", "angle",
            "quadratic", "sum", "product", "divide", "multiply", "square root",
            "math", "arithmetic", "matrix", "probability", "ratio"};
    private static final String[] SCIENCE_KW = {
            "physics", "chemistry", "biology", "force", "energy", "velocity",
            "acceleration", "atom", "molecule", "cell", "reaction", "photosynthesis",
            "gravity", "electron", "mass", "voltage", "current", "ecosystem",
            "science", "chemical", "organism", "motion", "newton", "momentum",
            "friction", "heat", "light", "wave", "temperature", "pressure",
            "density", "circuit", "genetics", "evolution", "planet", "speed"};
    private static final String[] PROGRAMMING_KW = {
            "code", "java", "python", "kotlin", "javascript", "function", "variable",
            "array", "loop", "algorithm", "compile", "class", "object", "bug",
            "api", "string", "recursion", "database", "sql", "program", "syntax",
            "boolean", "pointer"};
    private static final String[] HISTORY_KW = {
            "history", "war", "revolution", "century", "empire", "king", "queen",
            "ancient", "treaty", "dynasty", "civilization", "battle", "independence",
            "colonial", "medieval", "president"};
    private static final String[] LANGUAGE_KW = {
            "grammar", "translate", "translation", "sentence", "verb", "noun",
            "tense", "essay", "vocabulary", "pronunciation", "adjective", "paragraph",
            "synonym", "idiom", "spelling", "language"};

    private SubjectClassifier() { }

    /** Restricts provider output to the subject categories supported by the app. */
    public static String normalize(String subject) {
        if (subject == null) return GENERAL;
        for (String allowed : SUBJECTS) {
            if (allowed.equalsIgnoreCase(subject.trim())) {
                return allowed;
            }
        }
        return GENERAL;
    }

    public static String classify(String text) {
        if (text == null || text.trim().isEmpty()) return GENERAL;
        String t = text.toLowerCase(Locale.ROOT);

        int math = score(t, MATH_KW);
        // treat a bare arithmetic expression as maths as well
        if (MathEvaluator.looksLikeExpression(text)) math += 3;

        int science = score(t, SCIENCE_KW);
        int programming = score(t, PROGRAMMING_KW);
        int history = score(t, HISTORY_KW);
        int language = score(t, LANGUAGE_KW);

        int best = math;
        String bestSubject = MATH;
        if (science > best) { best = science; bestSubject = SCIENCE; }
        if (programming > best) { best = programming; bestSubject = PROGRAMMING; }
        if (history > best) { best = history; bestSubject = HISTORY; }
        if (language > best) { best = language; bestSubject = LANGUAGES; }

        return best == 0 ? GENERAL : bestSubject;
    }

    private static int score(String haystack, String[] keywords) {
        int s = 0;
        for (String kw : keywords) {
            if (haystack.contains(kw)) s++;
        }
        return s;
    }
}
