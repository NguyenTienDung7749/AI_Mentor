package com.example.aimentor.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.aimentor.ai.AnswerSource;

/** A question asked by a student together with the AI answer (saved for offline history). */
@Entity(tableName = "questions", indices = {@Index("userId")})
public class Question {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String questionText = "";
    public String subject = "General";
    public String difficulty = "";
    public String answerText = "";
    public boolean bookmarked = false;
    public boolean reused = false;   // answer reused from a similar earlier question (AI cost saving)
    public boolean reviewed = false; // student has reviewed this saved answer (awards review XP once)

    @NonNull
    @ColumnInfo(defaultValue = "'LEGACY'")
    public String answerSource = AnswerSource.LEGACY.name();

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String modelName = "";

    @ColumnInfo(defaultValue = "0")
    public long responseTimeMs = 0L;

    public long createdAt = System.currentTimeMillis();
}
