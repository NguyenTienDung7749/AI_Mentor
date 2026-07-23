package com.example.aimentor;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.aimentor.util.SessionManager;

/** Applies the saved theme preference app-wide on startup. */
public class AiMentorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(session.isDarkTheme()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
