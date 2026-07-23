package com.example.aimentor.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database — the single local source of truth. This makes the whole app
 * work offline: questions, answers and quiz results are all stored on device.
 *
 * For this MVP {@code allowMainThreadQueries()} is enabled to keep the code
 * simple for a junior team; the data volume per user is small. Moving DB access
 * to a background executor is listed as a future improvement in the report.
 */
@Database(
        entities = {User.class, Question.class, QuizAttempt.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract QuestionDao questionDao();
    public abstract QuizAttemptDao quizAttemptDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ai_mentor.db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
