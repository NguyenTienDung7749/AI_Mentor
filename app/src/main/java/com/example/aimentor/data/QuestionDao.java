package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert
    long insert(Question question);

    @Update
    void update(Question question);

    @Query("SELECT * FROM questions WHERE userId = :userId ORDER BY createdAt DESC")
    List<Question> getForUser(long userId);

    @Query("SELECT * FROM questions WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    List<Question> getRecent(long userId, int limit);

    @Query("SELECT * FROM questions WHERE userId = :userId AND bookmarked = 1 ORDER BY createdAt DESC")
    List<Question> getBookmarked(long userId);

    @Query("SELECT * FROM questions WHERE userId = :userId AND (questionText LIKE '%' || :q || '%' "
            + "OR answerText LIKE '%' || :q || '%') ORDER BY createdAt DESC")
    List<Question> search(long userId, String q);

    @Query("SELECT * FROM questions WHERE userId = :userId AND subject = :subject ORDER BY createdAt DESC")
    List<Question> getBySubject(long userId, String subject);

    @Query("SELECT COUNT(*) FROM questions WHERE userId = :userId")
    int countForUser(long userId);

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    Question findById(long id);
}
