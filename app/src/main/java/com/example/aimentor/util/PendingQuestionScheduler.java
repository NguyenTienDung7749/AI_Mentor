package com.example.aimentor.util;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.aimentor.worker.PendingQuestionWorker;

import java.util.concurrent.TimeUnit;

/** Schedules one network-constrained worker for all queued questions. */
public final class PendingQuestionScheduler {

    public static final String UNIQUE_WORK_NAME = "pending_question_sync";

    private PendingQuestionScheduler() { }

    public static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(PendingQuestionWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                15, TimeUnit.SECONDS)
                        .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        UNIQUE_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        request);
    }
}
