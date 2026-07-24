package com.example.aimentor.Fragments;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.aimentor.util.ReminderScheduler;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.Gamification;
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

    private TextView tvName, tvEmail, tvLevelInfo, tvBadges, tvXpProgress, tvAvatar;
    private TextView tvProgressSummary, tvSubjectBreakdown;
    private TextView tvWeeklySummary, tvWeeklyEmpty, tvReviewSummary;
    private TextView tvAccuracyTrends, tvRepeatedTopics;
    private ProgressBar progressLevel;
    private LinearLayout weeklyActivityContainer;
    private Spinner spLevel, spStyle, spAvatar, spAccentTheme;
    private CheckBox cbMath, cbScience, cbProgramming, cbHistory, cbLanguages;
    private SwitchMaterial switchTheme, switchReminder;
    private MaterialButton btnSavePrefs, btnDeleteAccount, btnReminderTime;
    private MaterialButton btnSaveAppearance;
    private TextView tvReminderStatus, tvUnlockStatus;
    private int currentLevel = 1;
    private int loadGeneration;
    private int mutationGeneration;
    private boolean bindingReminderState;

    private final ActivityResultLauncher<String> enableReminderPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::handleEnableReminderPermission);
    private final ActivityResultLauncher<String> testReminderPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!canRenderReminderAction()) return;
                        if (granted) {
                            sendReminderNow();
                        } else {
                            showReminderPermissionNeeded();
                        }
                    });

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
        tvAvatar = view.findViewById(R.id.tvAvatar);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvLevelInfo = view.findViewById(R.id.tvLevelInfo);
        tvBadges = view.findViewById(R.id.tvBadges);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        tvProgressSummary = view.findViewById(R.id.tvProgressSummary);
        tvReviewSummary = view.findViewById(R.id.tvReviewSummary);
        tvAccuracyTrends = view.findViewById(R.id.tvAccuracyTrends);
        tvRepeatedTopics = view.findViewById(R.id.tvRepeatedTopics);
        tvSubjectBreakdown = view.findViewById(R.id.tvSubjectBreakdown);
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary);
        tvWeeklyEmpty = view.findViewById(R.id.tvWeeklyEmpty);
        progressLevel = view.findViewById(R.id.progressLevel);
        weeklyActivityContainer = view.findViewById(R.id.weeklyActivityContainer);
        spLevel = view.findViewById(R.id.spLevel);
        spStyle = view.findViewById(R.id.spStyle);
        spAvatar = view.findViewById(R.id.spAvatar);
        spAccentTheme = view.findViewById(R.id.spAccentTheme);
        tvUnlockStatus = view.findViewById(R.id.tvUnlockStatus);
        btnSaveAppearance = view.findViewById(R.id.btnSaveAppearance);
        cbMath = view.findViewById(R.id.cbMath);
        cbScience = view.findViewById(R.id.cbScience);
        cbProgramming = view.findViewById(R.id.cbProgramming);
        cbHistory = view.findViewById(R.id.cbHistory);
        cbLanguages = view.findViewById(R.id.cbLanguages);
        switchTheme = view.findViewById(R.id.switchTheme);
        switchReminder = view.findViewById(R.id.switchReminder);
        btnReminderTime = view.findViewById(R.id.btnReminderTime);
        tvReminderStatus = view.findViewById(R.id.tvReminderStatus);
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
        btnSaveAppearance.setOnClickListener(v -> saveAppearance());
        btnRemind.setOnClickListener(v -> sendReminder());
        switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (bindingReminderState) return;
            if (checked) {
                requestReminderPermissionOrEnable();
            } else {
                ReminderScheduler.disable(requireContext());
                bindReminderState();
                Toast.makeText(requireContext(),
                        R.string.reminder_disabled, Toast.LENGTH_SHORT).show();
            }
        });
        btnReminderTime.setOnClickListener(v -> chooseReminderTime());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        bindReminderState();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindReminderState();
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
        currentLevel = p.level;
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
        long reviewMinutes = Math.round(p.totalReviewDurationMs / 60000.0);
        tvReviewSummary.setText(getString(R.string.review_time_summary,
                p.reviewedAnswers, reviewMinutes));
        tvAccuracyTrends.setText(buildAccuracyTrends(p));
        bindRepeatedTopics(p);
        bindAppearanceRewards(p.level);
        bindSubjectBreakdown(p);
        bindWeeklyActivity(p);
    }

    private void bindAppearanceRewards(int level) {
        List<String> avatars = Gamification.unlockedAvatars(level);
        List<String> themes = Gamification.unlockedAccentThemes(level);
        spAvatar.setAdapter(makeAdapter(avatars));
        spAccentTheme.setAdapter(makeAdapter(themes));
        selectSpinner(spAvatar, avatars.toArray(new String[0]),
                session.getSelectedAvatar());
        selectSpinner(spAccentTheme, themes.toArray(new String[0]),
                session.getSelectedAccentTheme());
        tvAvatar.setText(Gamification.avatarSymbol(
                String.valueOf(spAvatar.getSelectedItem())));
        tvAvatar.setContentDescription(getString(R.string.selected_avatar_description,
                String.valueOf(spAvatar.getSelectedItem())));
        tvUnlockStatus.setText(getString(
                R.string.next_appearance_unlock, Gamification.nextUnlock(level)));
    }

    private void saveAppearance() {
        String avatar = String.valueOf(spAvatar.getSelectedItem());
        String accent = String.valueOf(spAccentTheme.getSelectedItem());
        if (!Gamification.unlockedAvatars(currentLevel).contains(avatar)
                || !Gamification.unlockedAccentThemes(currentLevel).contains(accent)) {
            Toast.makeText(requireContext(),
                    R.string.appearance_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        session.setSelectedAvatar(avatar);
        session.setSelectedAccentTheme(accent);
        Toast.makeText(requireContext(),
                R.string.appearance_saved, Toast.LENGTH_SHORT).show();
        requireActivity().recreate();
    }

    private String buildAccuracyTrends(StudyRepository.Progress progress) {
        return trendLine(getString(R.string.last_7_days),
                progress.current7DayAccuracy, progress.current7DayAnswered,
                progress.previous7DayAccuracy, progress.previous7DayAnswered)
                + "\n" + trendLine(getString(R.string.last_30_days),
                progress.current30DayAccuracy, progress.current30DayAnswered,
                progress.previous30DayAccuracy, progress.previous30DayAnswered);
    }

    private String trendLine(String period, int accuracy, int answered,
                             int previousAccuracy, int previousAnswered) {
        if (answered == 0) {
            return getString(R.string.accuracy_trend_empty, period);
        }
        if (previousAnswered == 0) {
            return getString(R.string.accuracy_trend_first,
                    period, accuracy, answered);
        }
        int change = accuracy - previousAccuracy;
        return getString(R.string.accuracy_trend_comparison,
                period, accuracy, change >= 0 ? "+" + change : String.valueOf(change));
    }

    private void bindRepeatedTopics(StudyRepository.Progress progress) {
        if (progress.repeatedTopics.isEmpty()) {
            tvRepeatedTopics.setText(R.string.repeated_topics_empty);
            return;
        }
        List<String> topicLines = new ArrayList<>();
        for (com.example.aimentor.util.LearningAnalytics.TopicFrequency topic
                : progress.repeatedTopics) {
            topicLines.add(getString(R.string.repeated_topic_item,
                    topic.topic, topic.questionCount));
        }
        tvRepeatedTopics.setText(android.text.TextUtils.join("  •  ", topicLines));
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasRuntimePermission(requireContext())) {
            testReminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        sendReminderNow();
    }

    private void sendReminderNow() {
        boolean posted = NotificationHelper.notifyDailyReminder(
                requireContext(), getString(R.string.reminder_title),
                getString(R.string.reminder_body));
        Toast.makeText(requireContext(),
                posted ? R.string.reminder_sent
                        : R.string.reminder_permission_needed,
                Toast.LENGTH_SHORT).show();
    }

    private void requestReminderPermissionOrEnable() {
        NotificationHelper.ensureChannel(requireContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasRuntimePermission(requireContext())) {
            enableReminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableReminder();
    }

    private void handleEnableReminderPermission(boolean granted) {
        if (!canRenderReminderAction()) return;
        if (granted) {
            enableReminder();
        } else {
            setReminderSwitchChecked(false);
            ReminderScheduler.disable(requireContext());
            showReminderPermissionNeeded();
        }
    }

    private void enableReminder() {
        if (!NotificationHelper.canPostNotifications(requireContext())) {
            setReminderSwitchChecked(false);
            ReminderScheduler.disable(requireContext());
            showReminderPermissionNeeded();
            return;
        }
        ReminderScheduler.enable(requireContext(),
                session.getReminderHour(), session.getReminderMinute());
        bindReminderState();
        Toast.makeText(requireContext(),
                R.string.reminder_enabled, Toast.LENGTH_SHORT).show();
    }

    private void chooseReminderTime() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hour, minute) -> {
                    session.setReminderTime(hour, minute);
                    if (session.isReminderEnabled()) {
                        ReminderScheduler.reschedule(
                                requireContext(), hour, minute);
                    }
                    bindReminderState();
                },
                session.getReminderHour(),
                session.getReminderMinute(),
                android.text.format.DateFormat.is24HourFormat(requireContext()));
        dialog.setTitle(R.string.reminder_choose_time);
        dialog.show();
    }

    private void bindReminderState() {
        if (switchReminder == null || btnReminderTime == null
                || tvReminderStatus == null || session == null) {
            return;
        }
        boolean enabled = session.isReminderEnabled();
        setReminderSwitchChecked(enabled);
        String displayTime = formattedReminderTime();
        btnReminderTime.setText(
                getString(R.string.reminder_time_button, displayTime));
        if (enabled && !NotificationHelper.canPostNotifications(requireContext())) {
            tvReminderStatus.setText(R.string.reminder_permission_needed);
        } else {
            tvReminderStatus.setText(enabled
                    ? getString(R.string.reminder_schedule_status, displayTime)
                    : getString(R.string.reminder_schedule_off));
        }
    }

    private String formattedReminderTime() {
        java.util.Calendar time = java.util.Calendar.getInstance();
        time.set(java.util.Calendar.HOUR_OF_DAY, session.getReminderHour());
        time.set(java.util.Calendar.MINUTE, session.getReminderMinute());
        return android.text.format.DateFormat
                .getTimeFormat(requireContext()).format(time.getTime());
    }

    private void setReminderSwitchChecked(boolean checked) {
        bindingReminderState = true;
        switchReminder.setChecked(checked);
        bindingReminderState = false;
    }

    private boolean canRenderReminderAction() {
        return isAdded() && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void showReminderPermissionNeeded() {
        Toast.makeText(requireContext(),
                R.string.reminder_permission_needed, Toast.LENGTH_LONG).show();
    }

    private void logout() {
        ReminderScheduler.disable(requireContext());
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
                                if (result.success) {
                                    ReminderScheduler.disable(requireContext());
                                    session.logout();
                                }
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
