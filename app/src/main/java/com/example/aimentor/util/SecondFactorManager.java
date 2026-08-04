package com.example.aimentor.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Optional authenticator-app two-factor sign-in. TOTP seeds receive an extra
 * application layer of AES-GCM encryption whose non-exportable key lives in
 * Android Keystore; the rest of the app continues to use Android's private,
 * credential-encrypted application storage.
 */
public final class SecondFactorManager {

    private static final String PREFS = "ai_mentor_second_factor";
    private static final String KEY_ALIAS = "ai_mentor_totp_aes_v1";
    private static final String ENABLED_PREFIX = "enabled_";
    private static final String SECRET_PREFIX = "secret_";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private final SharedPreferences preferences;

    public SecondFactorManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled(long userId) {
        return userId > 0L && preferences.getBoolean(enabledKey(userId), false);
    }

    public Setup createSetup() {
        String secret = TotpUtils.newSecret();
        return new Setup(secret, TotpUtils.formatSecret(secret));
    }

    @SuppressLint("ApplySharedPref") // 2FA must be durable before success is reported.
    public Result enable(long userId, Setup setup, String code) {
        if (userId <= 0L || setup == null
                || !TotpUtils.verify(setup.secret, code, System.currentTimeMillis())) {
            return Result.failure("Enter the current 6-digit code from your authenticator app.");
        }
        try {
            String encrypted = encrypt(userId, setup.secret);
            // Prove that the stored payload can be read before declaring 2FA enabled.
            if (!setup.secret.equals(decrypt(userId, encrypted))) {
                return Result.failure("Two-factor verification could not be secured on this device.");
            }
            boolean saved = preferences.edit()
                    .putString(secretKey(userId), encrypted)
                    .putBoolean(enabledKey(userId), true)
                    .commit();
            return saved && isEnabled(userId)
                    ? Result.success("Authenticator verification is now on.")
                    : Result.failure("Two-factor verification could not be saved.");
        } catch (GeneralSecurityException | RuntimeException e) {
            return Result.failure("Android Keystore could not protect the authenticator key.");
        }
    }

    public boolean verify(long userId, String code) {
        if (!isEnabled(userId)) return false;
        String encrypted = preferences.getString(secretKey(userId), "");
        if (encrypted == null || encrypted.isEmpty()) return false;
        try {
            String secret = decrypt(userId, encrypted);
            return TotpUtils.verify(secret, code, System.currentTimeMillis());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Fail closed if the encrypted seed or Keystore entry is unavailable.
            return false;
        }
    }

    public Result disable(long userId, String code) {
        if (!isEnabled(userId)) {
            return Result.success("Authenticator verification is already off.");
        }
        if (!verify(userId, code)) {
            return Result.failure("Enter the current 6-digit authenticator code.");
        }
        clear(userId);
        return Result.success("Authenticator verification is now off.");
    }

    public void clear(long userId) {
        if (userId <= 0L) return;
        preferences.edit()
                .remove(secretKey(userId))
                .remove(enabledKey(userId))
                .apply();
    }

    private String encrypt(long userId, String plainText)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        cipher.updateAAD(aad(userId));
        byte[] encrypted = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8));
        try {
            return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                    + "." + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } finally {
            Arrays.fill(encrypted, (byte) 0);
        }
    }

    private String decrypt(long userId, String payload)
            throws GeneralSecurityException {
        String[] pieces = payload == null ? new String[0] : payload.split("\\.", -1);
        if (pieces.length != 2) throw new GeneralSecurityException("Invalid payload");
        byte[] iv = Base64.decode(pieces[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(pieces[1], Base64.NO_WRAP);
        byte[] plain = null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(userId));
            plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(encrypted, (byte) 0);
            if (plain != null) Arrays.fill(plain, (byte) 0);
        }
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException("Android Keystore could not be loaded", e);
        }
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private byte[] aad(long userId) {
        return (PREFS + ":" + userId).getBytes(StandardCharsets.UTF_8);
    }

    private String enabledKey(long userId) {
        return ENABLED_PREFIX + userId;
    }

    private String secretKey(long userId) {
        return SECRET_PREFIX + userId;
    }

    public static final class Setup {
        public final String secret;
        public final String displaySecret;

        Setup(String secret, String displaySecret) {
            this.secret = secret;
            this.displaySecret = displaySecret;
        }
    }

    public static final class Result {
        public final boolean success;
        public final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static Result success(String message) {
            return new Result(true, message);
        }

        static Result failure(String message) {
            return new Result(false, message);
        }
    }
}
