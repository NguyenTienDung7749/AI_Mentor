package com.example.aimentor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert
    long insert(Question question);

    @Query("SELECT * FROM questions WHERE userId = :userId ORDER BY createdAt DESC")
    List<Question> getForUser(long userId);

    @Query("SELECT * FROM questions WHERE userId = :userId AND bookmarked = 1 ORDER BY createdAt DESC")
    List<Question> getBookmarked(long userId);

    @Query("SELECT * FROM questions WHERE userId = :userId AND (questionText LIKE '%' || :q || '%' "
            + "OR answerText LIKE '%' || :q || '%') ORDER BY createdAt DESC")
    List<Question> search(long userId, String q);

    @Query("SELECT * FROM questions WHERE userId = :userId AND subject = :subject ORDER BY createdAt DESC")
    List<Question> getBySubject(long userId, String subject);

    @Query("SELECT COUNT(*) FROM questions WHERE userId = :userId")
    int countForUser(long userId);

    @Query("SELECT * FROM questions WHERE id = :questionId AND userId = :userId LIMIT 1")
    Question findByIdForUser(long userId, long questionId);

    @Query("UPDATE questions SET bookmarked = CASE WHEN bookmarked = 1 THEN 0 ELSE 1 END "
            + "WHERE id = :questionId AND userId = :userId")
    int toggleBookmark(long userId, long questionId);

    @Query("UPDATE questions SET reviewed = 1, reviewedAt = :reviewedAt "
            + "WHERE id = :questionId AND userId = :userId AND reviewed = 0")
    int markReviewedIfNeeded(long userId, long questionId, long reviewedAt);

    @Query("UPDATE questions SET reviewDurationMs = reviewDurationMs + :durationMs "
            + "WHERE id = :questionId AND userId = :userId")
    int addReviewDuration(long userId, long questionId, long durationMs);

    @Query("DELETE FROM questions WHERE userId = :userId")
    void deleteForUser(long userId);
}
