package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.aimentor.R;
import com.example.aimentor.activities.LoginActivity;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsFragment extends Fragment {

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
        MaterialButton btnSavePrefs = view.findViewById(R.id.btnSavePrefs);
        MaterialButton btnRemind = view.findViewById(R.id.btnRemind);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        spLevel.setAdapter(makeAdapter(Arrays.asList("Middle School", "High School", "University")));
        spStyle.setAdapter(makeAdapter(Arrays.asList("Short", "Detailed", "Step-by-step")));

        switchTheme.setChecked(session.isDarkTheme());
        switchTheme.setOnCheckedChangeListener((b, checked) -> {
            session.setDarkTheme(checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnSavePrefs.setOnClickListener(v -> savePrefs());
        btnRemind.setOnClickListener(v -> sendReminder());
        btnLogout.setOnClickListener(v -> logout());
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
        User user = userRepository.getUser(session.getCurrentUserId());
        if (user == null) return;
        tvName.setText(user.name);
        tvEmail.setText(user.email);

        StudyRepository.Progress p = studyRepository.getProgress(user.id);
        tvLevelInfo.setText("Level " + p.level + " - " + p.levelTitle + " (" + p.xp + " XP)");
        progressLevel.setMax(com.example.aimentor.util.Gamification.XP_PER_LEVEL);
        progressLevel.setProgress(p.xpIntoLevel);
        tvXpProgress.setText(getString(R.string.xp_progress,
                p.xpIntoLevel, com.example.aimentor.util.Gamification.XP_PER_LEVEL,
                p.xpToNext));
        tvBadges.setText(p.badges.isEmpty()
                ? "Badges: none yet - keep learning!"
                : "Badges: " + android.text.TextUtils.join(", ", p.badges));
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

        selectSpinner(spLevel, user.educationLevel);
        selectSpinner(spStyle, user.explanationStyle);

        String subjects = user.subjects == null ? "" : user.subjects;
        cbMath.setChecked(subjects.contains("Mathematics"));
        cbScience.setChecked(subjects.contains("Science"));
        cbProgramming.setChecked(subjects.contains("Programming"));
        cbHistory.setChecked(subjects.contains("History"));
        cbLanguages.setChecked(subjects.contains("Languages"));
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

    private void selectSpinner(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i))) {
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

        userRepository.updatePreferences(session.getCurrentUserId(),
                (String) spLevel.getSelectedItem(),
                android.text.TextUtils.join(",", subjects),
                (String) spStyle.getSelectedItem());
        Toast.makeText(requireContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }

    private void sendReminder() {
        boolean posted = NotificationHelper.notify(requireContext(),
                "Time to review!",
                "Revisit your bookmarked answers and try a quick practice quiz today.");
        Toast.makeText(requireContext(),
                posted ? "Reminder sent to your notifications."
                        : "Enable notifications to receive reminders.",
                Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        session.logout();
        Intent login = new Intent(requireContext(), LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        requireActivity().finish();
    }
}
