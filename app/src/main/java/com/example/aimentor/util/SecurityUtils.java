package com.example.aimentor.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * Password hashing helpers. Passwords are never stored in plain text: they are
 * hashed with SHA-256 over a per-user random salt and stored as hex.
 * Pure Java (no android.util.Base64) so it works on every supported API level
 * and can be unit tested on the JVM.
 *
 * NOTE: For a production release this should be upgraded to a slow KDF such as
 * bcrypt / Argon2 / PBKDF2 and combined with EncryptedSharedPreferences /
 * SQLCipher for encryption-at-rest (documented as a future improvement).
 */
public final class SecurityUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() { }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String hashPassword(String password, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(fromHex(saltHex));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String password, String saltHex, String expectedHashHex) {
        String actual = hashPassword(password, saltHex);
        return constantTimeEquals(actual, expectedHashHex);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }
}
