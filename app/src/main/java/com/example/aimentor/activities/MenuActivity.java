package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aimentor.R;
import com.example.aimentor.adapters.ViewPagerAdapter;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.ReminderScheduler;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuActivity extends AppCompatActivity {

    private static final int[] TITLE_RES = {
            R.string.tab_home, R.string.tab_library,
            R.string.tab_quiz, R.string.tab_progress, R.string.tab_settings
    };

    private BottomNavigationView bottomNavigationView;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        WindowUiHelper.apply(this);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setupViewPager();
        clickTabNavigation();

        NotificationHelper.ensureChannel(this);
        ReminderScheduler.ensureScheduled(this);
    }

    private void clickTabNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            viewPager.setCurrentItem(indexForMenu(item.getItemId()),
                    WindowUiHelper.animationsEnabled(this));
            return true;
        });
    }

    private int indexForMenu(int itemId) {
        if (itemId == R.id.Category_menu) return 1;
        if (itemId == R.id.Quiz_menu) return 2;
        if (itemId == R.id.Progress_menu) return 3;
        if (itemId == R.id.Settings_menu) return 4;
        return 0;
    }

    /** Lets dashboard actions open an existing top-level destination. */
    public void selectTab(int position) {
        if (viewPager == null) return;
        int safePosition = Math.max(0, Math.min(position, TITLE_RES.length - 1));
        viewPager.setCurrentItem(
                safePosition, WindowUiHelper.animationsEnabled(this));
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int menuId;
                if (position == 1) menuId = R.id.Category_menu;
                else if (position == 2) menuId = R.id.Quiz_menu;
                else if (position == 3) menuId = R.id.Progress_menu;
                else if (position == 4) menuId = R.id.Settings_menu;
                else menuId = R.id.home_menu;
                bottomNavigationView.getMenu().findItem(menuId).setChecked(true);
            }
        });
    }
}
