package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    /**
     * Awards XP without a read-modify-write race. The affected-row count also
     * lets callers detect that the account disappeared before a transaction
     * completed.
     */
    @Query("UPDATE users SET xp = xp + :amount "
            + "WHERE id = :userId AND :amount >= 0")
    int addXp(long userId, int amount);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(long id);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(String email);

    @Query("DELETE FROM users WHERE id = :userId")
    int deleteById(long userId);
}
