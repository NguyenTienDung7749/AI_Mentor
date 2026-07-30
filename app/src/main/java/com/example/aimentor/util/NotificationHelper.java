package com.example.aimentor.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.activities.MenuActivity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Posts local notifications for scheduled reminders and level-up alerts.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "study_reminders";
    public static final int DAILY_REMINDER_NOTIFICATION_ID = 1001;
    public static final int ANSWER_READY_NOTIFICATION_ID = 1002;
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(2000);

    private NotificationHelper() { }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(
                    context.getString(R.string.notification_channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static boolean hasRuntimePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean canPostNotifications(Context context) {
        ensureChannel(context);
        if (!hasRuntimePermission(context)
                || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            NotificationChannel channel = manager == null
                    ? null : manager.getNotificationChannel(CHANNEL_ID);
            return channel != null
                    && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    /** Kept for callers that only need a yes/no notification capability check. */
    public static boolean hasPermission(Context context) {
        return canPostNotifications(context);
    }

    /** Posts a notification, or returns false when app/channel permission is unavailable. */
    public static boolean notify(Context context, String title, String message) {
        return notify(context, ID_COUNTER.getAndIncrement(), title, message);
    }

    /** Reuses one id so daily reminders replace instead of stacking. */
    public static boolean notifyDailyReminder(
            Context context, String title, String message) {
        return notify(context, DAILY_REMINDER_NOTIFICATION_ID, title, message);
    }

    /** Replaces the prior queue alert and opens the newly saved answer. */
    public static boolean notifyAnswerReady(
            Context context, long questionId, String title, String message) {
        Intent openAnswer =
                AnswerActivity.savedAnswerIntent(context, questionId)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, ANSWER_READY_NOTIFICATION_ID, openAnswer,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
        return notify(context, ANSWER_READY_NOTIFICATION_ID,
                title, message, contentIntent);
    }

    private static boolean notify(
            Context context, int notificationId, String title, String message) {
        ensureChannel(context);
        if (!canPostNotifications(context)) return false;

        Intent openApp = new Intent(context, MenuActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return notify(context, notificationId, title, message, contentIntent);
    }

    private static boolean notify(
            Context context, int notificationId, String title, String message,
            PendingIntent contentIntent) {
        ensureChannel(context);
        if (!canPostNotifications(context)) return false;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context)
                    .notify(notificationId, builder.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
