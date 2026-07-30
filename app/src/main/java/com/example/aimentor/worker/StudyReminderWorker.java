package com.example.aimentor.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.aimentor.R;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;

import java.util.Calendar;

/** Posts one reminder only while a user is signed in and reminders are enabled. */
public final class StudyReminderWorker extends Worker {

    public StudyReminderWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || !session.isReminderEnabled()
                || !NotificationHelper.canPostNotifications(context)) {
            return Result.success();
        }
        String title = context.getString(R.string.reminder_title);
        String body = context.getString(R.string.reminder_body);
        try {
            StudyRepository.Progress progress =
                    new StudyRepository(context)
                            .getProgress(session.getCurrentUserId());
            boolean weeklySummaryDay =
                    Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                            == Calendar.SUNDAY;
            if (weeklySummaryDay && session.isWeeklySummaryEnabled()
                    && progress.activityCountLast7Days > 0) {
                title = context.getString(
                        R.string.weekly_summary_reminder_title);
                String activities = context.getResources()
                        .getQuantityString(
                                R.plurals.learning_activity_count,
                                progress.activityCountLast7Days,
                                progress.activityCountLast7Days);
                String activeDays = context.getResources()
                        .getQuantityString(
                                R.plurals.active_day_count,
                                progress.activeDaysLast7Days,
                                progress.activeDaysLast7Days);
                body = context.getString(
                        R.string.weekly_summary_reminder_body,
                        activities, activeDays);
            } else if (session.isReviewReminderEnabled()
                    && progress.dueReviewCount > 0) {
                title = context.getString(R.string.review_reminder_title);
                body = context.getResources().getQuantityString(
                        R.plurals.review_reminder_body,
                        progress.dueReviewCount, progress.dueReviewCount);
            } else if (session.isReviewReminderEnabled()
                    && !progress.repeatedTopics.isEmpty()) {
                title = context.getString(
                        R.string.recurring_topic_reminder_title);
                body = context.getString(
                        R.string.recurring_topic_reminder_body,
                        progress.repeatedTopics.get(0).topic);
            }
        } catch (RuntimeException ignored) {
            // A reminder should still be posted if analytics cannot be loaded.
        }
        NotificationHelper.notifyDailyReminder(context, title, body);
        return Result.success();
    }
}
