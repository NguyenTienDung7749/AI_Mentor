package com.example.aimentor.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.aimentor.R;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;

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
        NotificationHelper.notifyDailyReminder(
                context,
                context.getString(R.string.reminder_title),
                context.getString(R.string.reminder_body));
        return Result.success();
    }
}
