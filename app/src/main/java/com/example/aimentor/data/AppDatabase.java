package com.example.aimentor.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room database — the local source of truth for accounts, successful online
 * answers and quiz results stored on the device.
 * Temporary offline guidance is intentionally not persisted.
 *
 * All reads and mutations are dispatched through repository executors; Room's
 * main-thread guard remains enabled to prevent accidental UI-thread database
 * access from being introduced later.
 */
@Database(
        entities = {User.class, Question.class, QuizAttempt.class},
        version = 4,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract QuestionDao questionDao();
    public abstract QuizAttemptDao quizAttemptDao();

    private static volatile AppDatabase INSTANCE;

    /** Preserves existing question history while adding AI provenance metadata. */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE questions "
                    + "ADD COLUMN answerSource TEXT NOT NULL DEFAULT 'LEGACY'");
            database.execSQL("ALTER TABLE questions "
                    + "ADD COLUMN modelName TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE questions "
                    + "ADD COLUMN responseTimeMs INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * One-time reset requested for the assignment demo. It removes legacy,
     * cached and offline question history while preserving users and quizzes.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM questions");
        }
    };

    /** Adds non-destructive study-review analytics to existing saved answers. */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE questions "
                    + "ADD COLUMN reviewedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE questions "
                    + "ADD COLUMN reviewDurationMs INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ai_mentor.db")
                            .addMigrations(
                                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
