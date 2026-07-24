package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.aimentor.R;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.PasswordValidator;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private TextView tvStrength;
    private MaterialButton btnSignup;
    private UserRepository userRepository;
    private SessionManager session;
    private int submitGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        WindowUiHelper.apply(this);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvSignupHeading), true);

        userRepository = new UserRepository(this);
        session = new SessionManager(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        tvStrength = findViewById(R.id.tvStrength);
        ViewCompat.setAccessibilityLiveRegion(tvStrength,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        btnSignup = findViewById(R.id.btnSignup);

        etPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { updateStrength(s.toString()); }
            public void afterTextChanged(Editable s) { }
        });

        btnSignup.setOnClickListener(v -> attemptSignup());
        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());
    }

    private void updateStrength(String password) {
        if (password.isEmpty()) {
            tvStrength.setText(R.string.password_requirement);
            tvStrength.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            return;
        }
        PasswordValidator.Strength strength = PasswordValidator.evaluate(password);
        int colorRes;
        String label;
        switch (strength) {
            case STRONG:
                label = getString(R.string.password_strength_strong);
                colorRes = R.color.success;
                break;
            case MEDIUM:
                label = getString(R.string.password_strength_medium);
                colorRes = R.color.primary;
                break;
            default:
                label = getString(R.string.password_strength_weak,
                        PasswordValidator.requirementMessage());
                colorRes = R.color.error;
                break;
        }
        tvStrength.setText(label);
        tvStrength.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private void attemptSignup() {
        String name = valueOf(etName);
        String email = valueOf(etEmail);
        String password = valueOf(etPassword);
        String confirm = valueOf(etConfirm);

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.passwords_do_not_match,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int generation = ++submitGeneration;
        btnSignup.setEnabled(false);
        userRepository.registerAsync(name, email, password, result -> {
            if (!canHandle(generation)) return;
            if (!result.success) {
                btnSignup.setEnabled(true);
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                return;
            }
            session.setCurrentUserId(result.userId);
            Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private boolean canHandle(int generation) {
        return generation == submitGeneration
                && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        submitGeneration++;
        super.onDestroy();
    }

    private String valueOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
