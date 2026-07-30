package com.example.aimentor.Fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.LocalLeaderboardRow;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.LearningAnalytics;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Displays learning analytics calculated from the signed-in user's Room data. */
public class ProgressFragment extends Fragment {

    private StudyRepository studyRepository;
    private SessionManager session;
    private TextView tvLevelInfo, tvXpProgress, tvBadges, tvProgressEmpty;
    private TextView tvProgressQuestions, tvProgressQuizzes;
    private TextView tvProgressAccuracy, tvProgressCorrect;
    private TextView tvReviewSummary, tvAccuracyTrends, tvRepeatedTopics;
    private TextView tvSubjectBreakdown, tvWeeklySummary, tvWeeklyEmpty;
    private TextView tvLocalLeaderboard;
    private ProgressBar progressLevel;
    private LinearLayout weeklyActivityContainer;
    private ScrollView progressScroll;
    private boolean initialScrollApplied;
    private int refreshGeneration;

    public ProgressFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studyRepository = new StudyRepository(requireContext());
        session = new SessionManager(requireContext());

        tvLevelInfo = view.findViewById(R.id.tvLevelInfo);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        tvBadges = view.findViewById(R.id.tvBadges);
        tvProgressEmpty = view.findViewById(R.id.tvProgressEmpty);
        tvProgressQuestions = view.findViewById(R.id.tvProgressQuestions);
        tvProgressQuizzes = view.findViewById(R.id.tvProgressQuizzes);
        tvProgressAccuracy = view.findViewById(R.id.tvProgressAccuracy);
        tvProgressCorrect = view.findViewById(R.id.tvProgressCorrect);
        tvReviewSummary = view.findViewById(R.id.tvReviewSummary);
        tvAccuracyTrends = view.findViewById(R.id.tvAccuracyTrends);
        tvRepeatedTopics = view.findViewById(R.id.tvRepeatedTopics);
        tvSubjectBreakdown = view.findViewById(R.id.tvSubjectBreakdown);
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary);
        tvWeeklyEmpty = view.findViewById(R.id.tvWeeklyEmpty);
        tvLocalLeaderboard = view.findViewById(R.id.tvLocalLeaderboard);
        progressLevel = view.findViewById(R.id.progressLevel);
        weeklyActivityContainer = view.findViewById(R.id.weeklyActivityContainer);
        progressScroll = view.findViewById(R.id.progressScroll);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvProgressHeading), true);
    }

    @Override
    public void onResume() {
        super.onResume();
        int generation = ++refreshGeneration;
        studyRepository.getProgressAsync(session.getCurrentUserId(), progress -> {
            if (!canRender(generation)) return;
            renderProgress(progress);
        });
    }

    @Override
    public void onDestroyView() {
        refreshGeneration++;
        super.onDestroyView();
    }

    private boolean canRender(int generation) {
        return generation == refreshGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void renderProgress(StudyRepository.Progress progress) {
        tvProgressEmpty.setVisibility(
                progress.totalQuestions == 0 && progress.quizzesCompleted == 0
                        ? View.VISIBLE : View.GONE);
        tvLevelInfo.setText(getString(R.string.level_summary_with_xp,
                progress.level, progress.levelTitle, progress.xp));
        progressLevel.setMax(Gamification.XP_PER_LEVEL);
        progressLevel.setProgress(progress.xpIntoLevel);
        progressLevel.setContentDescription(getString(
                R.string.level_progress_description, progress.level,
                progress.xpIntoLevel, Gamification.XP_PER_LEVEL));
        tvXpProgress.setText(getString(R.string.xp_progress,
                progress.xpIntoLevel, Gamification.XP_PER_LEVEL,
                progress.xpToNext));
        tvBadges.setText(progress.badges.isEmpty()
                ? getString(R.string.badges_empty)
                : getString(R.string.badges_summary,
                android.text.TextUtils.join(", ", progress.badges)));

        tvProgressQuestions.setText(getString(
                R.string.stat_number, progress.totalQuestions));
        tvProgressQuizzes.setText(getString(
                R.string.stat_number, progress.quizzesCompleted));
        tvProgressAccuracy.setText(getString(
                R.string.stat_percent, progress.accuracyPercent));
        tvProgressCorrect.setText(getString(R.string.quiz_correct_ratio,
                progress.totalCorrect, progress.totalAnswered));

        int reviewMinutes = (int) Math.min(Integer.MAX_VALUE,
                Math.round(progress.totalReviewDurationMs / 60000.0));
        String bookmarks = getResources().getQuantityString(
                R.plurals.bookmark_count, progress.bookmarkedCount,
                progress.bookmarkedCount);
        String reviewed = getResources().getQuantityString(
                R.plurals.reviewed_answer_count, progress.reviewedAnswers,
                progress.reviewedAnswers);
        String readingTime = getResources().getQuantityString(
                R.plurals.minute_count, reviewMinutes, reviewMinutes);
        String reviewDetail = getString(R.string.progress_review_detail,
                bookmarks, reviewed, readingTime);
        String reviewStates = getString(R.string.progress_review_states,
                progress.dueReviewCount, progress.learningCount,
                progress.masteredCount);
        tvReviewSummary.setText(getString(
                R.string.two_line_summary, reviewDetail, reviewStates));

        tvAccuracyTrends.setText(buildAccuracyTrends(progress));
        bindRepeatedTopics(progress);
        bindSubjectBreakdown(progress);
        bindWeeklyActivity(progress);
        bindLocalLeaderboard(progress);
        if (!initialScrollApplied) {
            initialScrollApplied = true;
            progressScroll.post(() -> progressScroll.scrollTo(0, 0));
        }
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
            String answerCount = getResources().getQuantityString(
                    R.plurals.quiz_answer_count, answered, answered);
            return getString(R.string.accuracy_trend_first,
                    period, accuracy, answerCount);
        }
        int change = accuracy - previousAccuracy;
        return getString(R.string.accuracy_trend_comparison,
                period, accuracy,
                change >= 0 ? "+" + change : String.valueOf(change));
    }

    private void bindRepeatedTopics(StudyRepository.Progress progress) {
        if (progress.repeatedTopics.isEmpty()) {
            tvRepeatedTopics.setText(R.string.repeated_topics_empty);
            return;
        }
        List<String> topicLines = new ArrayList<>();
        for (LearningAnalytics.TopicFrequency topic : progress.repeatedTopics) {
            String questionCount = getResources().getQuantityString(
                    R.plurals.question_count,
                    topic.questionCount, topic.questionCount);
            topicLines.add(getString(R.string.repeated_topic_item,
                    topic.topic, questionCount));
        }
        tvRepeatedTopics.setText(
                android.text.TextUtils.join("  •  ", topicLines));
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
                    subject, questionCount, quizCount)
                    + "\n" + masteryLine(progress, subject));
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
        String weeklyDetail = getString(
                R.string.weekly_activity_summary, activityCount, activeDayCount);
        String streak = getResources().getQuantityString(
                R.plurals.current_streak_days,
                progress.currentStreakDays, progress.currentStreakDays);
        tvWeeklySummary.setText(getString(
                R.string.two_line_summary, weeklyDetail, streak));
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

    private String masteryLine(
            StudyRepository.Progress progress, String subject) {
        StudyRepository.SubjectMastery mastery =
                progress.subjectMastery.get(subject);
        if (mastery == null || mastery.answered == 0) {
            return getString(R.string.subject_mastery_no_quiz,
                    mastery == null ? "Exploring" : mastery.label);
        }
        String answers = getResources().getQuantityString(
                R.plurals.quiz_answer_count,
                mastery.answered, mastery.answered);
        return getString(R.string.subject_mastery_with_accuracy,
                mastery.label, mastery.accuracyPercent, answers);
    }

    private void bindLocalLeaderboard(StudyRepository.Progress progress) {
        if (!progress.localLeaderboardEnabled) {
            tvLocalLeaderboard.setText(
                    R.string.local_leaderboard_opt_in_needed);
            return;
        }
        if (progress.localLeaderboard.isEmpty()) {
            tvLocalLeaderboard.setText(R.string.local_leaderboard_empty);
            return;
        }
        List<String> rows = new ArrayList<>();
        long currentUserId = session.getCurrentUserId();
        for (int index = 0; index < progress.localLeaderboard.size(); index++) {
            LocalLeaderboardRow entry = progress.localLeaderboard.get(index);
            String name = entry.name == null || entry.name.trim().isEmpty()
                    ? getString(R.string.default_student_name)
                    : entry.name.trim();
            if (entry.id == currentUserId) {
                name = getString(
                        R.string.local_leaderboard_current_name, name);
            }
            rows.add(getString(R.string.local_leaderboard_row,
                    index + 1, name, entry.xp));
        }
        tvLocalLeaderboard.setText(
                android.text.TextUtils.join("\n", rows));
    }

    private void addActivityRow(String label, int count, int max) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        int onSurfaceVariant = MaterialColors.getColor(
                row, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int onSurface = MaterialColors.getColor(
                row, com.google.android.material.R.attr.colorOnSurface);
        int primary = MaterialColors.getColor(
                row, com.google.android.material.R.attr.colorPrimary);
        int track = MaterialColors.getColor(
                row, com.google.android.material.R.attr.colorSurfaceVariant);

        TextView dayLabel = new TextView(requireContext());
        dayLabel.setText(label);
        dayLabel.setTextColor(onSurfaceVariant);
        row.addView(dayLabel, new LinearLayout.LayoutParams(
                dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));

        ProgressBar bar = new ProgressBar(requireContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(max);
        bar.setProgress(count);
        bar.setProgressTintList(ColorStateList.valueOf(primary));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(track));
        String activityDescription = getResources().getQuantityString(
                R.plurals.learning_activity_count, count, count);
        bar.setContentDescription(getString(
                R.string.daily_activity_description, label, activityDescription));
        LinearLayout.LayoutParams barParams =
                new LinearLayout.LayoutParams(0, dp(8), 1f);
        barParams.setMarginStart(dp(8));
        barParams.setMarginEnd(dp(8));
        row.addView(bar, barParams);

        TextView countView = new TextView(requireContext());
        countView.setText(getString(R.string.activity_count, count));
        countView.setGravity(Gravity.END);
        countView.setTextColor(onSurface);
        row.addView(countView, new LinearLayout.LayoutParams(
                dp(32), LinearLayout.LayoutParams.WRAP_CONTENT));
        weeklyActivityContainer.addView(row);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
