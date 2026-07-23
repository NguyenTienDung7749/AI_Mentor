package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aimentor.R;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private UserRepository userRepository;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_layout_login);

        userRepository = new UserRepository(this);
        session = new SessionManager(this);

        // Keep the user signed in across app launches.
        if (session.isLoggedIn()) {
            User existing = userRepository.getUser(session.getCurrentUserId());
            if (existing != null) {
                goToNextScreen(existing);
                return;
            }
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tvGoSignup).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String email = valueOf(etEmail);
        String password = valueOf(etPassword);

        UserRepository.Result result = userRepository.login(email, password);
        if (!result.success) {
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            return;
        }
        session.setCurrentUserId(result.userId);
        User user = userRepository.getUser(result.userId);
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
        goToNextScreen(user);
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
