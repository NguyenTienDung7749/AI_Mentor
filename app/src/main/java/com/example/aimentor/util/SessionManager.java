package com.example.aimentor.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks the local session and non-sensitive UI preferences, including
 * theme and reminder settings. Passwords and API credentials are never written
 * to SharedPreferences.
 */
public class SessionManager {

    private static final String PREFS = "ai_mentor_session";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_AVATAR_PREFIX = "avatar_";
    private static final String KEY_ACCENT_PREFIX = "accent_";

    public static final int DEFAULT_REMINDER_HOUR = 19;
    public static final int DEFAULT_REMINDER_MINUTE = 0;
    public static final int THEME_MODE_SYSTEM = 0;
    public static final int THEME_MODE_LIGHT = 1;
    public static final int THEME_MODE_DARK = 2;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setCurrentUserId(long id) {
        prefs.edit().putLong(KEY_USER_ID, id).apply();
    }

    public long getCurrentUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public boolean isLoggedIn() {
        return getCurrentUserId() > 0;
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .putBoolean(KEY_REMINDER_ENABLED, false)
                .apply();
    }

    public boolean isDarkTheme() {
        return getThemeMode() == THEME_MODE_DARK;
    }

    public void setDarkTheme(boolean dark) {
        setThemeMode(dark ? THEME_MODE_DARK : THEME_MODE_LIGHT);
    }

    /**
     * Returns System/Light/Dark while preserving the legacy boolean preference
     * for users upgrading from an earlier build.
     */
    public int getThemeMode() {
        if (prefs.contains(KEY_THEME_MODE)) {
            return clampThemeMode(prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM));
        }
        if (prefs.contains(KEY_DARK_THEME)) {
            return prefs.getBoolean(KEY_DARK_THEME, false)
                    ? THEME_MODE_DARK : THEME_MODE_LIGHT;
        }
        return THEME_MODE_SYSTEM;
    }

    public void setThemeMode(int mode) {
        prefs.edit()
                .putInt(KEY_THEME_MODE, clampThemeMode(mode))
                .remove(KEY_DARK_THEME)
                .apply();
    }

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public void setReminderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR);
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE);
    }

    public void setReminderTime(int hour, int minute) {
        prefs.edit()
                .putInt(KEY_REMINDER_HOUR, clamp(hour, 0, 23))
                .putInt(KEY_REMINDER_MINUTE, clamp(minute, 0, 59))
                .apply();
    }

    public String getSelectedAvatar() {
        return prefs.getString(userKey(KEY_AVATAR_PREFIX), "Learner");
    }

    public void setSelectedAvatar(String avatar) {
        prefs.edit().putString(userKey(KEY_AVATAR_PREFIX),
                avatar == null ? "Learner" : avatar).apply();
    }

    public String getSelectedAccentTheme() {
        return prefs.getString(userKey(KEY_ACCENT_PREFIX), "Indigo");
    }

    public void setSelectedAccentTheme(String accent) {
        prefs.edit().putString(userKey(KEY_ACCENT_PREFIX),
                accent == null ? "Indigo" : accent).apply();
    }

    private String userKey(String prefix) {
        return prefix + Math.max(0L, getCurrentUserId());
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int clampThemeMode(int mode) {
        if (mode == THEME_MODE_LIGHT || mode == THEME_MODE_DARK) return mode;
        return THEME_MODE_SYSTEM;
    }
}
