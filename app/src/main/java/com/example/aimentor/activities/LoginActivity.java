package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.example.aimentor.R;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private UserRepository userRepository;
    private SessionManager session;
    private int operationGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_layout_login);
        WindowUiHelper.apply(this);
        ViewCompat.setAccessibilityHeading(
                findViewById(R.id.tvLoginHeading), true);

        userRepository = new UserRepository(this);
        session = new SessionManager(this);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvGoSignup).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        // Keep the user signed in across app launches.
        if (session.isLoggedIn()) {
            int generation = ++operationGeneration;
            btnLogin.setEnabled(false);
            userRepository.getUserAsync(
                    session.getCurrentUserId(), existing -> {
                if (!canHandle(generation)) return;
                if (existing != null) {
                    goToNextScreen(existing);
                } else {
                    session.logout();
                    btnLogin.setEnabled(true);
                }
            });
        }
    }

    private void attemptLogin() {
        String email = valueOf(etEmail);
        String password = valueOf(etPassword);

        int generation = ++operationGeneration;
        btnLogin.setEnabled(false);
        userRepository.loginAsync(email, password, result -> {
            if (!canHandle(generation)) return;
            if (!result.success) {
                btnLogin.setEnabled(true);
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                return;
            }
            session.setCurrentUserId(result.userId);
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            userRepository.getUserAsync(result.userId, user -> {
                if (!canHandle(generation)) return;
                if (user == null) {
                    session.logout();
                    btnLogin.setEnabled(true);
                    Toast.makeText(this,
                            R.string.login_account_load_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                goToNextScreen(user);
            });
        });
    }

    private boolean canHandle(int generation) {
        return generation == operationGeneration
                && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        operationGeneration++;
        super.onDestroy();
    }

    private void goToNextScreen(User user) {
        Intent intent = (user != null && user.onboardingCompleted)
                ? new Intent(this, MenuActivity.class)
                : new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }

    private String valueOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
