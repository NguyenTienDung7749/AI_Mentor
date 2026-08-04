package com.example.aimentor.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecondFactorManagerInstrumentedTest {

    @Test
    public void authenticatorSecret_isKeystoreProtectedAndControlsVerification() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        SecondFactorManager manager = new SecondFactorManager(context);
        long testUserId = 8_000_000L + Math.abs(System.nanoTime() % 100_000L);
        manager.clear(testUserId);
        try {
            SecondFactorManager.Setup setup = manager.createSetup();
            String currentCode = TotpUtils.generateCode(
                    setup.secret, System.currentTimeMillis());

            SecondFactorManager.Result enabled = manager.enable(
                    testUserId, setup, currentCode);
            assertTrue(enabled.message, enabled.success);
            assertTrue(manager.isEnabled(testUserId));
            assertTrue(manager.verify(testUserId, currentCode));
            assertFalse(manager.verify(testUserId, "000000"));

            SecondFactorManager.Result disabled = manager.disable(
                    testUserId, currentCode);
            assertTrue(disabled.message, disabled.success);
            assertFalse(manager.isEnabled(testUserId));
        } finally {
            manager.clear(testUserId);
        }
    }
}
