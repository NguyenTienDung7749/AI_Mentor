package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.example.aimentor.R;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** First-use setup: education level, subjects of interest and explanation style. */
public class OnboardingActivity extends AppCompatActivity {

    private RadioGroup rgLevel, rgStyle;
    private CheckBox cbMath, cbScience, cbProgramming, cbHistory, cbLanguages;
    private SessionManager session;
    private UserRepository userRepository;
    private MaterialButton btnFinish;
    private int saveGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        WindowUiHelper.apply(this);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvOnboardingHeading), true);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvOnboardingLevelHeading), true);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvOnboardingSubjectHeading), true);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvOnboardingStyleHeading), true);

        session = new SessionManager(this);
        userRepository = new UserRepository(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        rgLevel = findViewById(R.id.rgLevel);
        rgStyle = findViewById(R.id.rgStyle);
        cbMath = findViewById(R.id.cbMath);
        cbScience = findViewById(R.id.cbScience);
        cbProgramming = findViewById(R.id.cbProgramming);
        cbHistory = findViewById(R.id.cbHistory);
        cbLanguages = findViewById(R.id.cbLanguages);

        btnFinish = findViewById(R.id.btnFinish);
        btnFinish.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {
        String level = "High School";
        int levelId = rgLevel.getCheckedRadioButtonId();
        if (levelId == R.id.rbMiddle) level = "Middle School";
        else if (levelId == R.id.rbUniversity) level = "University";

        List<String> subjects = new ArrayList<>();
        if (cbMath.isChecked()) subjects.add("Mathematics");
        if (cbScience.isChecked()) subjects.add("Science");
        if (cbProgramming.isChecked()) subjects.add("Programming");
        if (cbHistory.isChecked()) subjects.add("History");
        if (cbLanguages.isChecked()) subjects.add("Languages");

        String style = "Step-by-step";
        int styleId = rgStyle.getCheckedRadioButtonId();
        if (styleId == R.id.rbShort) style = "Short";
        else if (styleId == R.id.rbDetailed) style = "Detailed";

        int generation = ++saveGeneration;
        btnFinish.setEnabled(false);
        userRepository.saveOnboardingAsync(
                session.getCurrentUserId(), level,
                android.text.TextUtils.join(",", subjects), style,
                result -> {
                    if (!canHandle(generation)) return;
                    if (!result.success) {
                        btnFinish.setEnabled(true);
                        Toast.makeText(this, result.message,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(this, MenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
    }

    private boolean canHandle(int generation) {
        return generation == saveGeneration
                && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        saveGeneration++;
        super.onDestroy();
    }
}
