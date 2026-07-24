package com.example.aimentor.util;

import android.app.Activity;

import com.example.aimentor.R;

/** Applies the current student's unlocked accent before Activity inflation. */
public final class AppearanceManager {

    private AppearanceManager() { }

    public static void apply(Activity activity) {
        String accent = new SessionManager(activity).getSelectedAccentTheme();
        if ("Ocean".equals(accent)) {
            activity.setTheme(R.style.Theme_AIMentor_Ocean);
        } else if ("Forest".equals(accent)) {
            activity.setTheme(R.style.Theme_AIMentor_Forest);
        } else if ("Sunset".equals(accent)) {
            activity.setTheme(R.style.Theme_AIMentor_Sunset);
        } else {
            activity.setTheme(R.style.Theme_AIMentor);
        }
    }
}
