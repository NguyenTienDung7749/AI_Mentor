package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TotpUtilsTest {

    private static final String RFC_SECRET = TotpUtils.encodeBase32(
            "12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    public void generateCode_matchesRfc6238Vectors() {
        assertEquals("94287082", TotpUtils.generateCode(RFC_SECRET, 59_000L, 8));
        assertEquals("07081804", TotpUtils.generateCode(RFC_SECRET, 1_111_111_109_000L, 8));
        assertEquals("14050471", TotpUtils.generateCode(RFC_SECRET, 1_111_111_111_000L, 8));
        assertEquals("89005924", TotpUtils.generateCode(RFC_SECRET, 1_234_567_890_000L, 8));
        assertEquals("69279037", TotpUtils.generateCode(RFC_SECRET, 2_000_000_000_000L, 8));
    }

    @Test
    public void verify_acceptsCurrentAndAdjacentTimeWindows() {
        long now = 1_700_000_000_000L;
        String current = TotpUtils.generateCode(RFC_SECRET, now);
        assertTrue(TotpUtils.verify(RFC_SECRET, current, now));
        assertTrue(TotpUtils.verify(RFC_SECRET, current, now + 30_000L));
        assertFalse(TotpUtils.verify(RFC_SECRET, current, now + 90_000L));
    }

    @Test
    public void verify_rejectsMalformedCodes() {
        assertFalse(TotpUtils.verify(RFC_SECRET, "", 59_000L));
        assertFalse(TotpUtils.verify(RFC_SECRET, "12345", 59_000L));
        assertFalse(TotpUtils.verify(RFC_SECRET, "abcdef", 59_000L));
    }

    @Test
    public void base32_roundTripAndDisplayFormatting_areStable() {
        byte[] value = "assignment-security".getBytes(StandardCharsets.UTF_8);
        String encoded = TotpUtils.encodeBase32(value);
        assertEquals("assignment-security",
                new String(TotpUtils.decodeBase32(encoded), StandardCharsets.UTF_8));
        assertTrue(TotpUtils.formatSecret(encoded).contains(" "));
    }
}
