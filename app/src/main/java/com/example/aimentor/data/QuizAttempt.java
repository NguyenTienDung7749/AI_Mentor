package com.example.aimentor.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** The result of one completed practice quiz (used for accuracy statistics). */
@Entity(tableName = "quiz_attempts", indices = {@Index("userId")})
public class QuizAttempt {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String subject = "General";
    public int correct = 0;
    public int total = 0;
    public long createdAt = System.currentTimeMillis();
}
