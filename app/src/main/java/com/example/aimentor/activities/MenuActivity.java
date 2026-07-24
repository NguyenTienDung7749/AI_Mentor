package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aimentor.R;
import com.example.aimentor.adapters.ViewPagerAdapter;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.ReminderScheduler;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int[] TITLE_RES = {
            R.string.tab_home, R.string.tab_library,
            R.string.tab_quiz, R.string.tab_settings
    };

    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        NavigationView navigationView = findViewById(R.id.drawerNavigation);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(TITLE_RES[0]);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        setupViewPager();
        clickTabNavigation();

        NotificationHelper.ensureChannel(this);
        ReminderScheduler.ensureScheduled(this);

        // Modern back handling: close the drawer first, otherwise let the system handle back.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void clickTabNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            viewPager.setCurrentItem(indexForMenu(item.getItemId()), true);
            return true;
        });
    }

    private int indexForMenu(int itemId) {
        if (itemId == R.id.Category_menu) return 1;
        if (itemId == R.id.Quiz_menu) return 2;
        if (itemId == R.id.Settings_menu || itemId == R.id.account_menu) return 3;
        return 0;
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int menuId;
                if (position == 1) menuId = R.id.Category_menu;
                else if (position == 2) menuId = R.id.Quiz_menu;
                else if (position == 3) menuId = R.id.Settings_menu;
                else menuId = R.id.home_menu;
                bottomNavigationView.getMenu().findItem(menuId).setChecked(true);
                if (getSupportActionBar() != null
                        && position >= 0 && position < TITLE_RES.length) {
                    getSupportActionBar().setTitle(TITLE_RES[position]);
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.logout_menu) {
            logout();
        } else {
            viewPager.setCurrentItem(indexForMenu(menuItem.getItemId()), true);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        ReminderScheduler.disable(this);
        new SessionManager(this).logout();
        Intent login = new Intent(this, LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        finish();
    }
}
