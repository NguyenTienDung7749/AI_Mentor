package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingQuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PendingQuestion pendingQuestion);

    @Query("SELECT * FROM pending_questions WHERE id = :id LIMIT 1")
    PendingQuestion findById(long id);

    @Query("SELECT * FROM pending_questions WHERE userId = :userId "
            + "ORDER BY createdAt DESC")
    List<PendingQuestion> getForUser(long userId);

    @Query("SELECT * FROM pending_questions WHERE userId = :userId "
            + "AND status IN ('PENDING', 'SENDING', 'FAILED') "
            + "ORDER BY createdAt DESC")
    List<PendingQuestion> getActiveForUser(long userId);

    @Query("SELECT * FROM pending_questions "
            + "WHERE status IN ('PENDING', 'FAILED') "
            + "AND attemptCount < :maxAttempts "
            + "ORDER BY createdAt ASC LIMIT :limit")
    List<PendingQuestion> getRetryable(int maxAttempts, int limit);

    @Query("SELECT COUNT(*) FROM pending_questions WHERE userId = :userId "
            + "AND status IN ('PENDING', 'SENDING', 'FAILED')")
    int countActiveForUser(long userId);

    @Query("SELECT COUNT(*) FROM pending_questions "
            + "WHERE status IN ('PENDING', 'FAILED') "
            + "AND attemptCount < :maxAttempts")
    int countRetryable(int maxAttempts);

    @Query("UPDATE pending_questions SET status = 'FAILED', "
            + "lastError = 'Interrupted before completion', "
            + "updatedAt = :updatedAt WHERE status = 'SENDING' "
            + "AND updatedAt < :staleBefore")
    int recoverStaleSending(long staleBefore, long updatedAt);

    @Query("UPDATE pending_questions SET status = 'SENDING', "
            + "updatedAt = :updatedAt WHERE id = :id")
    int markSending(long id, long updatedAt);

    @Query("UPDATE pending_questions SET status = 'FAILED', "
            + "attemptCount = attemptCount + 1, lastError = :message, "
            + "updatedAt = :updatedAt WHERE id = :id")
    int markFailed(long id, String message, long updatedAt);

    @Query("UPDATE pending_questions SET status = 'SENT', "
            + "savedQuestionId = :questionId, lastError = '', "
            + "updatedAt = :updatedAt WHERE id = :id")
    int markSent(long id, long questionId, long updatedAt);

    @Query("UPDATE pending_questions SET status = 'PENDING', "
            + "attemptCount = 0, lastError = '', updatedAt = :updatedAt "
            + "WHERE id = :id AND userId = :userId")
    int retryNow(long userId, long id, long updatedAt);

    @Query("DELETE FROM pending_questions WHERE userId = :userId")
    void deleteForUser(long userId);
}
