package com.example.aimentor.util;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.aimentor.worker.StudyReminderWorker;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Owns the app's single, inexact daily study reminder. */
public final class ReminderScheduler {

    public static final String UNIQUE_WORK_NAME = "daily_study_reminder";
    private static final String WORK_TAG = "study_reminder";

    private ReminderScheduler() { }

    public static Operation enable(Context context, int hour, int minute) {
        SessionManager session = new SessionManager(context);
        session.setReminderTime(hour, minute);
        session.setReminderEnabled(true);
        return enqueue(context, hour, minute,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE);
    }

    public static Operation ensureScheduled(Context context) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || !session.isReminderEnabled()) {
            return cancel(context);
        }
        return enqueue(context, session.getReminderHour(),
                session.getReminderMinute(), ExistingPeriodicWorkPolicy.KEEP);
    }

    public static Operation reschedule(Context context, int hour, int minute) {
        SessionManager session = new SessionManager(context);
        session.setReminderTime(hour, minute);
        if (!session.isReminderEnabled()) {
            return cancel(context);
        }
        return enqueue(context, hour, minute,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE);
    }

    public static Operation disable(Context context) {
        new SessionManager(context).setReminderEnabled(false);
        return cancel(context);
    }

    private static Operation enqueue(
            Context context, int hour, int minute,
            ExistingPeriodicWorkPolicy policy) {
        long delay = ReminderTimeCalculator.millisUntilNext(
                System.currentTimeMillis(), hour, minute, TimeZone.getDefault());
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        StudyReminderWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(WORK_TAG)
                        .build();
        return WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request);
    }

    private static Operation cancel(Context context) {
        return WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
