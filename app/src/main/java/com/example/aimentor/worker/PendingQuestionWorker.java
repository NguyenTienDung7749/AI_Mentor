package com.example.aimentor.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.aimentor.repo.StudyRepository;

/** Retries pending questions whenever Android reports a usable network. */
public class PendingQuestionWorker extends Worker {

    public PendingQuestionWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        StudyRepository.PendingSyncResult result =
                new StudyRepository(getApplicationContext())
                        .syncPendingQuestions();
        if (result == StudyRepository.PendingSyncResult.RETRY) {
            return Result.retry();
        }
        return Result.success();
    }
}
