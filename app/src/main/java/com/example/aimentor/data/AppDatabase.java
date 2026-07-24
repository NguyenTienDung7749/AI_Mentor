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
 * Read-heavy screens use repository executors. {@code allowMainThreadQueries()}
 * remains temporarily enabled only until the authentication and mutation paths
 * are migrated in Batch 14B; removing it earlier would break those flows.
 */
@Database(
        entities = {User.class, Question.class, QuizAttempt.class},
        version = 3,
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

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ai_mentor.db")
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
