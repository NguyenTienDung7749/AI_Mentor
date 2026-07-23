package com.example.aimentor.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Tracks which user is currently signed in (simple session persistence). */
public class SessionManager {

    private static final String PREFS = "ai_mentor_session";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_DARK_THEME = "dark_theme";

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
        prefs.edit().remove(KEY_USER_ID).apply();
    }

    public boolean isDarkTheme() {
        return prefs.getBoolean(KEY_DARK_THEME, false);
    }

    public void setDarkTheme(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply();
    }
}
