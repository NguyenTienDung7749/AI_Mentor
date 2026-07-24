package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ReminderSchedulerInstrumentedTest {

    private Context context;
    private long originalUserId;
    private boolean originalEnabled;
    private int originalHour;
    private int originalMinute;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getApplicationContext();
        SessionManager session = new SessionManager(context);
        originalUserId = session.getCurrentUserId();
        originalEnabled = session.isReminderEnabled();
        originalHour = session.getReminderHour();
        originalMinute = session.getReminderMinute();
        await(ReminderScheduler.disable(context));
    }

    @After
    public void tearDown() throws Exception {
        SessionManager session = new SessionManager(context);
        session.setReminderTime(originalHour, originalMinute);
        if (originalUserId > 0L) {
            session.setCurrentUserId(originalUserId);
        } else {
            session.logout();
        }
        if (originalEnabled && originalUserId > 0L) {
            await(ReminderScheduler.enable(
                    context, originalHour, originalMinute));
        } else {
            await(ReminderScheduler.disable(context));
        }
    }

    @Test
    public void preferences_roundTripAndClampTime() {
        SessionManager session = new SessionManager(context);

        session.setReminderTime(28, -4);
        session.setReminderEnabled(true);

        assertEquals(23, session.getReminderHour());
        assertEquals(0, session.getReminderMinute());
        assertTrue(session.isReminderEnabled());
        session.logout();
        assertFalse(session.isReminderEnabled());
    }

    @Test
    public void schedule_isUniqueAndDisableCancelsIt() throws Exception {
        Calendar later = Calendar.getInstance();
        later.add(Calendar.HOUR_OF_DAY, 12);

        await(ReminderScheduler.enable(context,
                later.get(Calendar.HOUR_OF_DAY), later.get(Calendar.MINUTE)));
        await(ReminderScheduler.enable(context,
                later.get(Calendar.HOUR_OF_DAY), later.get(Calendar.MINUTE)));

        List<WorkInfo> scheduled = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_WORK_NAME)
                .get(10, TimeUnit.SECONDS);
        assertEquals(1L, unfinishedCount(scheduled));

        await(ReminderScheduler.disable(context));
        List<WorkInfo> cancelled = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_WORK_NAME)
                .get(10, TimeUnit.SECONDS);
        assertEquals(0L, unfinishedCount(cancelled));
    }

    @Test
    public void ensureChannel_createsDefaultImportanceChannel() {
        NotificationHelper.ensureChannel(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        assertNotNull(manager);
        NotificationChannel channel =
                manager.getNotificationChannel(NotificationHelper.CHANNEL_ID);
        assertNotNull(channel);
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT,
                channel.getImportance());
    }

    private long unfinishedCount(List<WorkInfo> workInfos) {
        long count = 0L;
        for (WorkInfo info : workInfos) {
            if (!info.getState().isFinished()) count++;
        }
        return count;
    }

    private void await(androidx.work.Operation operation) throws Exception {
        operation.getResult().get(10, TimeUnit.SECONDS);
    }
}
