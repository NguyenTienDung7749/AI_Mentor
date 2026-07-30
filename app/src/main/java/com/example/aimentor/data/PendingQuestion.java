package com.example.aimentor.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A student question waiting for a network-backed answer. */
@Entity(
        tableName = "pending_questions",
        indices = {
                @Index("userId"),
                @Index("status"),
                @Index(value = "requestKey", unique = true)
        })
public class PendingQuestion {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SENT = "SENT";
    public static final int MAX_AUTOMATIC_ATTEMPTS = 5;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    @NonNull
    public String questionText = "";

    @NonNull
    public String subjectHint = "";

    @NonNull
    public String explanationStyle = "";

    @NonNull
    public String imageMimeType = "";

    @NonNull
    public String imageBase64 = "";

    @NonNull
    public String requestKey = "";

    @NonNull
    public String status = STATUS_PENDING;

    public int attemptCount;

    @NonNull
    public String lastError = "";

    @ColumnInfo(defaultValue = "0")
    public long savedQuestionId;

    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();
}
