package com.example.aimentor.util;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing helpers. Passwords are never stored in plain text. New
 * passwords use a deliberately slow PBKDF2 hash with a per-user random salt.
 * Legacy salted SHA-256 hashes remain verifiable so existing demo accounts can
 * be upgraded after their next successful login.
 * Pure Java (no android.util.Base64) so it works on every supported API level
 * and can be unit tested on the JVM.
 *
 * NOTE: A production app should authenticate through a trusted backend rather
 * than treating a local Room record as the account authority.
 */
public final class SecurityUtils {

    private static final String PBKDF2_SHA256 = "PBKDF2WithHmacSHA256";
    private static final String PBKDF2_SHA1 = "PBKDF2WithHmacSHA1";
    private static final String PREFIX_SHA256 = "pbkdf2-sha256";
    private static final String PREFIX_SHA1 = "pbkdf2-sha1";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final int MAX_ACCEPTED_ITERATIONS = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() { }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String hashPassword(String password, String saltHex) {
        String safePassword = password == null ? "" : password;
        byte[] salt = requireSalt(saltHex);
        try {
            return encodePbkdf2(PBKDF2_SHA256, PREFIX_SHA256,
                    safePassword, salt, PBKDF2_ITERATIONS);
        } catch (NoSuchAlgorithmException unavailableOnOlderAndroid) {
            try {
                return encodePbkdf2(PBKDF2_SHA1, PREFIX_SHA1,
                        safePassword, salt, PBKDF2_ITERATIONS);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("PBKDF2 not available", e);
            }
        }
    }

    public static boolean verify(String password, String saltHex, String expectedHashHex) {
        if (expectedHashHex == null || expectedHashHex.isEmpty()) return false;
        String safePassword = password == null ? "" : password;
        try {
            if (expectedHashHex.startsWith(PREFIX_SHA256 + "$")
                    || expectedHashHex.startsWith(PREFIX_SHA1 + "$")) {
                return verifyPbkdf2(safePassword, saltHex, expectedHashHex);
            }
            return constantTimeEquals(
                    legacySha256(safePassword, saltHex), expectedHashHex);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Corrupt local credential data must fail closed, never crash login.
            return false;
        }
    }

    /** True for legacy/weak records that should be re-hashed after login. */
    public static boolean needsUpgrade(String storedHash) {
        if (storedHash == null) return true;
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 3
                || (!PREFIX_SHA256.equals(parts[0]) && !PREFIX_SHA1.equals(parts[0]))) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < PBKDF2_ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean verifyPbkdf2(
            String password, String saltHex, String encodedHash) {
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 3) return false;
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (iterations < 1 || iterations > MAX_ACCEPTED_ITERATIONS) return false;

        String algorithm;
        if (PREFIX_SHA256.equals(parts[0])) {
            algorithm = PBKDF2_SHA256;
        } else if (PREFIX_SHA1.equals(parts[0])) {
            algorithm = PBKDF2_SHA1;
        } else {
            return false;
        }
        try {
            String actual = encodePbkdf2(algorithm, parts[0],
                    password, requireSalt(saltHex), iterations);
            return constantTimeEquals(actual, encodedHash);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static String encodePbkdf2(String algorithm, String prefix,
                                       String password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, iterations, PBKDF2_KEY_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance(algorithm)
                    .generateSecret(spec).getEncoded();
            return prefix + "$" + iterations + "$" + toHex(hash);
        } catch (java.security.spec.InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to derive password hash", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String legacySha256(String password, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(requireSalt(saltHex));
            return toHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
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

    private static byte[] requireSalt(String hex) {
        if (hex == null || hex.length() < 2 || (hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Invalid salt");
        }
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid salt");
            out[i / 2] = (byte) ((high << 4) + low);
        }
        return out;
    }
}
