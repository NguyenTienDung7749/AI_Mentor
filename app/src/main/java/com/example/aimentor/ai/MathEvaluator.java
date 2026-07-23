package com.example.aimentor.ai;

/**
 * A tiny recursive-descent evaluator for arithmetic expressions
 * ( + - * / ^ , parentheses, decimals ). Pure Java, JVM unit testable.
 * Lets the offline engine return a genuinely computed result for maths questions.
 */
public final class MathEvaluator {

    private final String s;
    private int pos = -1;
    private int ch;

    private MathEvaluator(String expression) {
        this.s = expression;
    }

    /** Returns true when the text is (mostly) a bare arithmetic expression. */
    public static boolean looksLikeExpression(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        int digits = 0;
        int operators = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isDigit(c)) {
                digits++;
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^') {
                operators++;
            } else if (c == '.' || c == '(' || c == ')' || c == ' ' || c == '=' || c == '?') {
                // allowed filler
            } else {
                return false; // a letter or other symbol -> not a bare expression
            }
        }
        return digits >= 1 && operators >= 1;
    }

    /** Evaluates the expression; throws IllegalArgumentException when invalid. */
    public static double evaluate(String expression) {
        if (expression == null) throw new IllegalArgumentException("null expression");
        String clean = expression.replace("=", "").replace("?", "").trim();
        return new MathEvaluator(clean).parse();
    }

    private void nextChar() {
        ch = (++pos < s.length()) ? s.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parse() {
        nextChar();
        double x = parseExpression();
        if (pos < s.length()) {
            throw new IllegalArgumentException("Unexpected: " + (char) ch);
        }
        return x;
    }

    // expression = term | expression '+' term | expression '-' term
    private double parseExpression() {
        double x = parseTerm();
        for (; ; ) {
            if (eat('+')) x += parseTerm();
            else if (eat('-')) x -= parseTerm();
            else return x;
        }
    }

    // term = factor | term '*' factor | term '/' factor
    private double parseTerm() {
        double x = parseFactor();
        for (; ; ) {
            if (eat('*')) x *= parseFactor();
            else if (eat('/')) {
                double d = parseFactor();
                if (d == 0) throw new IllegalArgumentException("Division by zero");
                x /= d;
            } else return x;
        }
    }

    // factor = '+' factor | '-' factor | number | '(' expression ')' | factor '^' factor
    private double parseFactor() {
        if (eat('+')) return parseFactor();
        if (eat('-')) return -parseFactor();

        double x;
        int startPos = this.pos;
        if (eat('(')) {
            x = parseExpression();
            if (!eat(')')) throw new IllegalArgumentException("Missing ')'");
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(s.substring(startPos, this.pos));
        } else {
            throw new IllegalArgumentException("Unexpected: " + (char) ch);
        }
        if (eat('^')) x = Math.pow(x, parseFactor());
        return x;
    }
}
