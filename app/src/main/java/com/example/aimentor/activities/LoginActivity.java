package com.example.aimentor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.example.aimentor.R;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.SecondFactorManager;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private UserRepository userRepository;
    private SessionManager session;
    private SecondFactorManager secondFactorManager;
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
        secondFactorManager = new SecondFactorManager(this);
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
            if (secondFactorManager.isEnabled(result.userId)) {
                showTwoFactorDialog(result.userId, generation);
            } else {
                completeLogin(result.userId, generation, result.message);
            }
        });
    }

    private void showTwoFactorDialog(long userId, int generation) {
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_totp_code, null, false);
        TextInputLayout codeLayout = dialogView.findViewById(
                R.id.tilTwoFactorCode);
        TextInputEditText codeInput = dialogView.findViewById(
                R.id.etTwoFactorCode);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.two_factor_login_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (ignored, which) -> {
                    if (canHandle(generation)) btnLogin.setEnabled(true);
                })
                .setPositiveButton(R.string.verify, null)
                .create();
        dialog.setOnCancelListener(ignored -> {
            if (canHandle(generation)) btnLogin.setEnabled(true);
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String code = valueOf(codeInput);
                if (!secondFactorManager.verify(userId, code)) {
                    codeLayout.setError(getString(R.string.authenticator_code_invalid));
                    codeInput.requestFocus();
                    return;
                }
                codeLayout.setError(null);
                dialog.dismiss();
                completeLogin(userId, generation,
                        getString(R.string.two_factor_verified));
            });
        });
        dialog.show();
    }

    private void completeLogin(long userId, int generation, String message) {
        if (!canHandle(generation)) return;
        session.setCurrentUserId(userId);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        userRepository.getUserAsync(userId, user -> {
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
        if (user != null && user.onboardingCompleted) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
        finish();
    }

    private String valueOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
