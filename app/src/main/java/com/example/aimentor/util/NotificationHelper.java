package com.example.aimentor.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.aimentor.R;

/**
 * Posts local notifications (review reminders, motivational messages,
 * level-up alerts). Implements the notification requirement at MVP level;
 * scheduled daily/weekly summaries are noted as a future enhancement.
 */
public final class NotificationHelper {

    private static final String CHANNEL_ID = "study_reminders";
    private static final String CHANNEL_NAME = "Study reminders";
    private static int idCounter = 1000;

    private NotificationHelper() { }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminders and motivation from AI Study Mentor");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Posts a notification. Returns false if the runtime permission is missing. */
    public static boolean notify(Context context, String title, String message) {
        ensureChannel(context);
        if (!hasPermission(context)) return false;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(idCounter++, builder.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
