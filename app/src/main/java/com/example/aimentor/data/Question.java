package com.example.aimentor.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.aimentor.ai.AnswerSource;

/** A question asked by a student together with the AI answer (saved for offline history). */
@Entity(
        tableName = "questions",
        indices = {
                @Index("userId"),
                @Index("cacheKey"),
                @Index("requestKey")
        })
public class Question {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String questionText = "";
    public String subject = "General";
    public String difficulty = "";
    public String answerText = "";
    public boolean bookmarked = false;
    // True only when a saved answer was explicitly reused by the learner.
    public boolean reused = false;
    public boolean reviewed = false; // student has reviewed this saved answer (awards review XP once)

    @ColumnInfo(defaultValue = "0")
    public long reviewedAt = 0L;

    @ColumnInfo(defaultValue = "0")
    public long reviewDurationMs = 0L;

    @NonNull
    @ColumnInfo(defaultValue = "'LEGACY'")
    public String answerSource = AnswerSource.LEGACY.name();

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String modelName = "";

    @ColumnInfo(defaultValue = "0")
    public long responseTimeMs = 0L;

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String explanationStyle = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String cacheKey = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String requestKey = "";

    public long createdAt = System.currentTimeMillis();
}
