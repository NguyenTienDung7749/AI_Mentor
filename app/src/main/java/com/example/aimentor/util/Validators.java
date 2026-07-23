package com.example.aimentor.util;

import java.util.regex.Pattern;

/**
 * Simple input validators. Uses a regex (not android.util.Patterns) so it is
 * pure Java and can be unit tested on the JVM.
 */
public final class Validators {

    // Pragmatic email pattern (local-part @ domain . tld).
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validators() { }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        return !e.isEmpty() && EMAIL.matcher(e).matches();
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
