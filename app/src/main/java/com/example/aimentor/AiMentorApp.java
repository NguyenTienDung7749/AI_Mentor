package com.example.aimentor;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.PendingQuestionScheduler;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

/** Applies the saved theme preference app-wide on startup. */
public class AiMentorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(toNightMode(session.getThemeMode()));
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) ->
                        "Material You".equals(
                                new SessionManager(activity).getSelectedAccentTheme()))
                .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);
        PendingQuestionScheduler.enqueue(this);
    }

    private int toNightMode(int mode) {
        if (mode == SessionManager.THEME_MODE_LIGHT) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (mode == SessionManager.THEME_MODE_DARK) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
