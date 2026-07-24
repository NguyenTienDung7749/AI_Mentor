package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.activities.LoginActivity;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsFragment extends Fragment {

    private static final String[] EDUCATION_VALUES = {
            "Middle School", "High School", "University"
    };
    private static final String[] STYLE_VALUES = {
            "Short", "Detailed", "Step-by-step"
    };

    private UserRepository userRepository;
    private StudyRepository studyRepository;
    private SessionManager session;

    private TextView tvName, tvEmail, tvLevelInfo, tvBadges, tvXpProgress;
    private TextView tvProgressSummary, tvSubjectBreakdown;
    private TextView tvWeeklySummary, tvWeeklyEmpty;
    private ProgressBar progressLevel;
    private LinearLayout weeklyActivityContainer;
    private Spinner spLevel, spStyle;
    private CheckBox cbMath, cbScience, cbProgramming, cbHistory, cbLanguages;
    private SwitchMaterial switchTheme;
    private MaterialButton btnSavePrefs, btnDeleteAccount;
    private int loadGeneration;
    private int mutationGeneration;

    public SettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository(requireContext());
        studyRepository = new StudyRepository(requireContext());
        session = new SessionManager(requireContext());

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvLevelInfo = view.findViewById(R.id.tvLevelInfo);
        tvBadges = view.findViewById(R.id.tvBadges);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        tvProgressSummary = view.findViewById(R.id.tvProgressSummary);
        tvSubjectBreakdown = view.findViewById(R.id.tvSubjectBreakdown);
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary);
        tvWeeklyEmpty = view.findViewById(R.id.tvWeeklyEmpty);
        progressLevel = view.findViewById(R.id.progressLevel);
        weeklyActivityContainer = view.findViewById(R.id.weeklyActivityContainer);
        spLevel = view.findViewById(R.id.spLevel);
        spStyle = view.findViewById(R.id.spStyle);
        cbMath = view.findViewById(R.id.cbMath);
        cbScience = view.findViewById(R.id.cbScience);
        cbProgramming = view.findViewById(R.id.cbProgramming);
        cbHistory = view.findViewById(R.id.cbHistory);
        cbLanguages = view.findViewById(R.id.cbLanguages);
        switchTheme = view.findViewById(R.id.switchTheme);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvPreferencesHeading), true);
        btnSavePrefs = view.findViewById(R.id.btnSavePrefs);
        MaterialButton btnRemind = view.findViewById(R.id.btnRemind);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        spLevel.setAdapter(makeAdapter(Arrays.asList(
                getResources().getStringArray(R.array.education_level_choices))));
        spStyle.setAdapter(makeAdapter(Arrays.asList(
                getResources().getStringArray(R.array.explanation_style_choices))));

        switchTheme.setChecked(session.isDarkTheme());
        switchTheme.setOnCheckedChangeListener((b, checked) -> {
            session.setDarkTheme(checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnSavePrefs.setOnClickListener(v -> savePrefs());
        btnRemind.setOnClickListener(v -> sendReminder());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUser();
    }

    private ArrayAdapter<String> makeAdapter(List<String> items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void loadUser() {
        if (!session.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        long userId = session.getCurrentUserId();
        int generation = ++loadGeneration;
        userRepository.getUserAsync(userId, user -> {
            if (!canRenderLoad(generation) || user == null) return;
            renderUser(user);
            studyRepository.getProgressAsync(user.id, progress -> {
                if (!canRenderLoad(generation)) return;
                renderProgress(progress);
            });
        });
    }

    private boolean canRenderLoad(int generation) {
        return generation == loadGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void renderUser(User user) {
        if (user == null) return;
        tvName.setText(user.name);
        tvEmail.setText(user.email);

        selectSpinner(spLevel, EDUCATION_VALUES, user.educationLevel);
        selectSpinner(spStyle, STYLE_VALUES, user.explanationStyle);

        String subjects = user.subjects == null ? "" : user.subjects;
        cbMath.setChecked(subjects.contains("Mathematics"));
        cbScience.setChecked(subjects.contains("Science"));
        cbProgramming.setChecked(subjects.contains("Programming"));
        cbHistory.setChecked(subjects.contains("History"));
        cbLanguages.setChecked(subjects.contains("Languages"));
    }

    private void renderProgress(StudyRepository.Progress p) {
        tvLevelInfo.setText(getString(R.string.level_summary_with_xp,
                p.level, p.levelTitle, p.xp));
        progressLevel.setMax(com.example.aimentor.util.Gamification.XP_PER_LEVEL);
        progressLevel.setProgress(p.xpIntoLevel);
        progressLevel.setContentDescription(getString(
                R.string.level_progress_description, p.level, p.xpIntoLevel,
                com.example.aimentor.util.Gamification.XP_PER_LEVEL));
        tvXpProgress.setText(getString(R.string.xp_progress,
                p.xpIntoLevel, com.example.aimentor.util.Gamification.XP_PER_LEVEL,
                p.xpToNext));
        tvBadges.setText(p.badges.isEmpty()
                ? getString(R.string.badges_empty)
                : getString(R.string.badges_summary,
                        android.text.TextUtils.join(", ", p.badges)));
        String savedQuestions = getResources().getQuantityString(
                R.plurals.saved_question_count, p.totalQuestions, p.totalQuestions);
        String quizAttempts = getResources().getQuantityString(
                R.plurals.quiz_attempt_count, p.quizzesCompleted, p.quizzesCompleted);
        String bookmarks = getResources().getQuantityString(
                R.plurals.bookmark_count, p.bookmarkedCount, p.bookmarkedCount);
        tvProgressSummary.setText(getString(R.string.progress_summary,
                savedQuestions, quizAttempts, p.totalCorrect,
                p.totalAnswered, p.accuracyPercent, bookmarks));
        bindSubjectBreakdown(p);
        bindWeeklyActivity(p);
    }

    @Override
    public void onDestroyView() {
        loadGeneration++;
        mutationGeneration++;
        super.onDestroyView();
    }

    private void bindSubjectBreakdown(StudyRepository.Progress progress) {
        Set<String> subjects = new LinkedHashSet<>();
        subjects.addAll(progress.subjectCounts.keySet());
        subjects.addAll(progress.subjectQuizCounts.keySet());
        List<String> lines = new ArrayList<>();
        for (String subject : SubjectClassifier.SUBJECTS) {
            if (!subjects.contains(subject)) continue;
            int questions = progress.subjectCounts.containsKey(subject)
                    ? progress.subjectCounts.get(subject) : 0;
            int quizzes = progress.subjectQuizCounts.containsKey(subject)
                    ? progress.subjectQuizCounts.get(subject) : 0;
            String questionCount = getResources().getQuantityString(
                    R.plurals.question_count, questions, questions);
            String quizCount = getResources().getQuantityString(
                    R.plurals.quiz_attempt_count, quizzes, quizzes);
            lines.add(getString(R.string.subject_question_quiz_count,
                    subject, questionCount, quizCount));
        }
        tvSubjectBreakdown.setText(lines.isEmpty()
                ? getString(R.string.subject_activity_empty)
                : android.text.TextUtils.join("\n", lines));
    }

    private void bindWeeklyActivity(StudyRepository.Progress progress) {
        weeklyActivityContainer.removeAllViews();
        String activityCount = getResources().getQuantityString(
                R.plurals.learning_activity_count, progress.activityCountLast7Days,
                progress.activityCountLast7Days);
        String activeDayCount = getResources().getQuantityString(
                R.plurals.active_day_count, progress.activeDaysLast7Days,
                progress.activeDaysLast7Days);
        tvWeeklySummary.setText(getString(
                R.string.weekly_activity_summary, activityCount, activeDayCount));
        boolean empty = progress.activityCountLast7Days == 0;
        tvWeeklyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        weeklyActivityContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;

        int max = 1;
        for (StudyRepository.DailyActivity day : progress.last7Days) {
            max = Math.max(max, day.count);
        }
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE d", Locale.getDefault());
        for (StudyRepository.DailyActivity day : progress.last7Days) {
            addActivityRow(dateFormat.format(day.dayStart), day.count, max);
        }
    }

    private void addActivityRow(String label, int count, int max) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView dayLabel = new TextView(requireContext());
        dayLabel.setText(label);
        dayLabel.setTextColor(requireContext().getColor(R.color.text_secondary));
        row.addView(dayLabel, new LinearLayout.LayoutParams(dp(64),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ProgressBar bar = new ProgressBar(requireContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(max);
        bar.setProgress(count);
        bar.setContentDescription(getString(
                R.string.daily_activity_description, label, count));
        LinearLayout.LayoutParams barParams =
                new LinearLayout.LayoutParams(0, dp(8), 1f);
        barParams.setMarginStart(dp(8));
        barParams.setMarginEnd(dp(8));
        row.addView(bar, barParams);

        TextView countView = new TextView(requireContext());
        countView.setText(getString(R.string.activity_count, count));
        countView.setGravity(android.view.Gravity.END);
        countView.setTextColor(requireContext().getColor(R.color.text_primary));
        row.addView(countView, new LinearLayout.LayoutParams(dp(32),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        weeklyActivityContainer.addView(row);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void selectSpinner(Spinner spinner, String[] values, String value) {
        if (value == null) return;
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void savePrefs() {
        List<String> subjects = new ArrayList<>();
        if (cbMath.isChecked()) subjects.add("Mathematics");
        if (cbScience.isChecked()) subjects.add("Science");
        if (cbProgramming.isChecked()) subjects.add("Programming");
        if (cbHistory.isChecked()) subjects.add("History");
        if (cbLanguages.isChecked()) subjects.add("Languages");

        int generation = ++mutationGeneration;
        setMutationActionsEnabled(false);
        userRepository.updatePreferencesAsync(
                session.getCurrentUserId(),
                EDUCATION_VALUES[spLevel.getSelectedItemPosition()],
                android.text.TextUtils.join(",", subjects),
                STYLE_VALUES[spStyle.getSelectedItemPosition()],
                result -> {
                    if (!canRenderMutation(generation)) return;
                    setMutationActionsEnabled(true);
                    Toast.makeText(requireContext(),
                            result.success
                                    ? getString(R.string.preferences_saved)
                                    : result.message,
                            result.success
                                    ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                            .show();
                    if (result.success) loadUser();
                });
    }

    private boolean canRenderMutation(int generation) {
        return generation == mutationGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void setMutationActionsEnabled(boolean enabled) {
        btnSavePrefs.setEnabled(enabled);
        btnDeleteAccount.setEnabled(enabled);
    }

    private void sendReminder() {
        boolean posted = NotificationHelper.notify(requireContext(),
                getString(R.string.reminder_title),
                getString(R.string.reminder_body));
        Toast.makeText(requireContext(),
                posted ? R.string.reminder_sent
                        : R.string.reminder_permission_needed,
                Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        session.logout();
        navigateToLogin();
    }

    private void showDeleteAccountDialog() {
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setHint(R.string.delete_account_password_hint);
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int horizontalPadding = dp(24);
        passwordInput.setPadding(horizontalPadding, dp(8), horizontalPadding, dp(8));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setView(passwordInput)
                .setNegativeButton(R.string.delete_account_cancel, null)
                .setPositiveButton(R.string.delete_account_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = passwordInput.getText() == null
                            ? "" : passwordInput.getText().toString();
                    int generation = ++mutationGeneration;
                    passwordInput.setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(false);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    setMutationActionsEnabled(false);
                    userRepository.deleteAccountAsync(
                            session.getCurrentUserId(), password, result -> {
                                if (result.success) session.logout();
                                if (!canRenderMutation(generation)) return;
                                if (!result.success) {
                                    passwordInput.setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            .setEnabled(true);
                                    dialog.setCancelable(true);
                                    dialog.setCanceledOnTouchOutside(true);
                                    setMutationActionsEnabled(true);
                                    passwordInput.setError(result.message);
                                    return;
                                }
                                dialog.dismiss();
                                Toast.makeText(requireContext(),
                                        R.string.delete_account_success,
                                        Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            });
                }));
        dialog.show();
    }

    private void navigateToLogin() {
        Intent login = new Intent(requireContext(), LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        requireActivity().finish();
    }
}
