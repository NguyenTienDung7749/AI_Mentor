package com.example.aimentor.util;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Small RFC 6238 TOTP implementation used by optional authenticator-based
 * two-factor sign-in. The class has no Android dependency so the algorithm can
 * be verified with the published RFC test vectors on the JVM.
 */
public final class TotpUtils {

    private static final char[] BASE32 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final long STEP_MILLIS = 30_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtils() { }

    public static String newSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        return encodeBase32(secret);
    }

    public static String formatSecret(String secret) {
        String clean = normalizeSecret(secret);
        StringBuilder displayed = new StringBuilder(clean.length() + 7);
        for (int index = 0; index < clean.length(); index++) {
            if (index > 0 && index % 4 == 0) displayed.append(' ');
            displayed.append(clean.charAt(index));
        }
        return displayed.toString();
    }

    public static String generateCode(String secret, long timestampMillis) {
        return generateCode(secret, timestampMillis, CODE_DIGITS);
    }

    static String generateCode(
            String secret, long timestampMillis, int digits) {
        if (digits < 6 || digits > 8) {
            throw new IllegalArgumentException("TOTP digits must be between 6 and 8");
        }
        byte[] key = decodeBase32(secret);
        long counter = Math.max(0L, timestampMillis / STEP_MILLIS);
        byte[] message = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(message);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int modulus = 1;
            for (int index = 0; index < digits; index++) modulus *= 10;
            return String.format(Locale.ROOT, "%0" + digits + "d", binary % modulus);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TOTP is unavailable", e);
        } finally {
            Arrays.fill(key, (byte) 0);
            Arrays.fill(message, (byte) 0);
        }
    }

    public static boolean verify(
            String secret, String candidate, long timestampMillis) {
        String cleanCode = candidate == null
                ? "" : candidate.replaceAll("\\s+", "");
        if (!cleanCode.matches("\\d{6}")) return false;
        for (int window = -1; window <= 1; window++) {
            long adjusted = timestampMillis + window * STEP_MILLIS;
            if (constantTimeEquals(generateCode(secret, adjusted), cleanCode)) {
                return true;
            }
        }
        return false;
    }

    static String encodeBase32(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder output = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                output.append(BASE32[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            output.append(BASE32[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return output.toString();
    }

    static byte[] decodeBase32(String value) {
        String clean = normalizeSecret(value);
        if (clean.isEmpty()) throw new IllegalArgumentException("TOTP secret is empty");
        byte[] output = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int outputIndex = 0;
        for (int index = 0; index < clean.length(); index++) {
            int decoded = decodeBase32Character(clean.charAt(index));
            if (decoded < 0) throw new IllegalArgumentException("Invalid TOTP secret");
            buffer = (buffer << 5) | decoded;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[outputIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return outputIndex == output.length
                ? output : Arrays.copyOf(output, outputIndex);
    }

    private static int decodeBase32Character(char value) {
        if (value >= 'A' && value <= 'Z') return value - 'A';
        if (value >= '2' && value <= '7') return value - '2' + 26;
        return -1;
    }

    private static String normalizeSecret(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT)
                .replace("=", "")
                .replaceAll("\\s+", "");
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int difference = 0;
        for (int index = 0; index < left.length(); index++) {
            difference |= left.charAt(index) ^ right.charAt(index);
        }
        return difference == 0;
    }
}
