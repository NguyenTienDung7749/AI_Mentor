package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    /**
     * Awards XP without a read-modify-write race. The affected-row count also
     * lets callers detect that the account disappeared before a transaction
     * completed.
     */
    @Query("UPDATE users SET xp = xp + :amount "
            + "WHERE id = :userId AND :amount >= 0")
    int addXp(long userId, int amount);

    @Query("UPDATE users SET xp = 0 WHERE id = :userId")
    int resetXp(long userId);

    @Query("UPDATE users SET salt = :salt, passwordHash = :passwordHash "
            + "WHERE id = :userId")
    int updateCredentials(long userId, String salt, String passwordHash);

    @Query("UPDATE users SET educationLevel = :level, subjects = :subjects, "
            + "explanationStyle = :style, onboardingCompleted = 1 "
            + "WHERE id = :userId")
    int completeOnboarding(
            long userId, String level, String subjects, String style);

    @Query("UPDATE users SET educationLevel = :level, subjects = :subjects, "
            + "explanationStyle = :style WHERE id = :userId")
    int updatePreferences(
            long userId, String level, String subjects, String style);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(long id);

    @Query("SELECT id, name, xp FROM users "
            + "ORDER BY xp DESC, createdAt ASC LIMIT :limit")
    List<LocalLeaderboardRow> getLocalLeaderboard(int limit);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(String email);

    @Query("DELETE FROM users WHERE id = :userId")
    int deleteById(long userId);
}
