package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuizAttemptDao {

    @Insert
    long insert(QuizAttempt attempt);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY createdAt DESC")
    List<QuizAttempt> getForUser(long userId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId")
    int countForUser(long userId);

    @Query("SELECT IFNULL(SUM(correct), 0) FROM quiz_attempts WHERE userId = :userId")
    int totalCorrect(long userId);

    @Query("SELECT IFNULL(SUM(total), 0) FROM quiz_attempts WHERE userId = :userId")
    int totalAnswered(long userId);

    @Query("DELETE FROM quiz_attempts WHERE userId = :userId")
    void deleteForUser(long userId);
}
