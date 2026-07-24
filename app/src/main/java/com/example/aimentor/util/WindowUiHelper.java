package com.example.aimentor.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Keeps system-bar insets and short entrance motion consistent across activities.
 * Motion is skipped when the system animator scale is disabled.
 */
public final class WindowUiHelper {

    private static final long ENTRANCE_DURATION_MS = 220L;

    private WindowUiHelper() { }

    @SuppressWarnings("deprecation")
    public static void apply(Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        boolean lightBars = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightBars);
        controller.setAppearanceLightNavigationBars(lightBars);

        View content = activity.findViewById(android.R.id.content);
        int left = content.getPaddingLeft();
        int top = content.getPaddingTop();
        int right = content.getPaddingRight();
        int bottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + insets.left, top + insets.top,
                    right + insets.right, bottom + insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
        reveal(content);
    }

    public static boolean animationsEnabled(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    public static void reveal(View view) {
        view.animate().cancel();
        if (!animationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        float offset = 10f * view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(offset);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTRANCE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
