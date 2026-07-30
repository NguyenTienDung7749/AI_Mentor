package com.example.aimentor.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.aimentor.data.AppDatabase;
import com.example.aimentor.data.User;
import com.example.aimentor.data.UserDao;
import com.example.aimentor.util.PasswordValidator;
import com.example.aimentor.util.SecurityUtils;
import com.example.aimentor.util.Validators;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Registration and authentication backed by the local Room database. */
public class UserRepository {

    private static final ExecutorService IO_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final AppDatabase database;
    private final UserDao userDao;
    private final Handler mainHandler;

    public UserRepository(Context context) {
        this(AppDatabase.getInstance(context),
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Package-private injection seam for isolated Room instrumentation tests.
     * It never replaces the production singleton or the student's real data.
     */
    UserRepository(AppDatabase database) {
        this(database, new Handler(Looper.getMainLooper()));
    }

    UserRepository(AppDatabase database, Handler mainHandler) {
        this.database = database;
        this.userDao = database.userDao();
        this.mainHandler = mainHandler;
    }

    public static class Result {
        public final boolean success;
        public final String message;
        public final long userId;
        Result(boolean success, String message, long userId) {
            this.success = success;
            this.message = message;
            this.userId = userId;
        }
    }

    public interface ResultCallback {
        void onResult(@NonNull Result result);
    }

    private void runAsync(
            @NonNull Supplier<Result> operation,
            @NonNull String failureMessage,
            @NonNull ResultCallback callback) {
        IO_EXECUTOR.execute(() -> {
            Result result;
            try {
                result = operation.get();
            } catch (RuntimeException operationFailed) {
                result = new Result(false, failureMessage, -1);
            }
            Result delivered = result;
            mainHandler.post(() -> callback.onResult(delivered));
        });
    }

    @WorkerThread
    public Result register(String name, String email, String password) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!Validators.isValidEmail(cleanEmail)) {
            return new Result(false, "Please enter a valid email address.", -1);
        }
        if (!PasswordValidator.isAcceptable(password)) {
            return new Result(false, PasswordValidator.requirementMessage(), -1);
        }
        if (userDao.countByEmail(cleanEmail) > 0) {
            return new Result(false, "An account with this email already exists.", -1);
        }
        String salt = SecurityUtils.generateSalt();
        User user = new User();
        user.email = cleanEmail;
        user.name = (name == null || name.trim().isEmpty()) ? cleanEmail.split("@")[0] : name.trim();
        user.salt = salt;
        user.passwordHash = SecurityUtils.hashPassword(password, salt);
        user.onboardingCompleted = false;
        long id = userDao.insert(user);
        return new Result(true, "Account created.", id);
    }

    public void registerAsync(
            String name, String email, String password,
            @NonNull ResultCallback callback) {
        runAsync(() -> register(name, email, password),
                "Could not create the account. Please try again.", callback);
    }

    @WorkerThread
    public Result login(String email, String password) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!Validators.isValidEmail(cleanEmail)) {
            return new Result(false, "Please enter a valid email address.", -1);
        }
        User user = userDao.findByEmail(cleanEmail);
        if (user == null) {
            return new Result(false, "No account found for this email.", -1);
        }
        String safePassword = password == null ? "" : password;
        if (!SecurityUtils.verify(safePassword, user.salt, user.passwordHash)) {
            return new Result(false, "Incorrect password.", -1);
        }
        if (SecurityUtils.needsUpgrade(user.passwordHash)) {
            String upgradedSalt = SecurityUtils.generateSalt();
            String upgradedHash =
                    SecurityUtils.hashPassword(safePassword, upgradedSalt);
            userDao.updateCredentials(user.id, upgradedSalt, upgradedHash);
        }
        return new Result(true, "Welcome back!", user.id);
    }

    public void loginAsync(
            String email, String password,
            @NonNull ResultCallback callback) {
        runAsync(() -> login(email, password),
                "Could not sign in. Please try again.", callback);
    }

    public interface UserCallback {
        void onResult(User user);
    }

    @WorkerThread
    public User getUser(long id) {
        return userDao.findById(id);
    }

    public void getUserAsync(
            long id, @NonNull UserCallback callback) {
        IO_EXECUTOR.execute(() -> {
            User user;
            try {
                user = getUser(id);
            } catch (RuntimeException readFailed) {
                user = null;
            }
            User delivered = user;
            mainHandler.post(() -> callback.onResult(delivered));
        });
    }

    @WorkerThread
    public Result saveOnboarding(
            long userId, String level, String subjects, String style) {
        User user = userDao.findById(userId);
        if (user == null) {
            return new Result(false, "Account not found.", -1);
        }
        boolean updated = userDao.completeOnboarding(
                userId, level, subjects, style) == 1;
        return new Result(updated,
                updated ? "Learning profile saved."
                        : "Could not save the learning profile. Please try again.",
                updated ? userId : -1);
    }

    public void saveOnboardingAsync(
            long userId, String level, String subjects, String style,
            @NonNull ResultCallback callback) {
        runAsync(() -> saveOnboarding(userId, level, subjects, style),
                "Could not save the learning profile. Please try again.",
                callback);
    }

    @WorkerThread
    public Result updatePreferences(
            long userId, String level, String subjects, String style) {
        User user = userDao.findById(userId);
        if (user == null) {
            return new Result(false, "Account not found.", -1);
        }
        boolean updated = userDao.updatePreferences(
                userId, level, subjects, style) == 1;
        return new Result(updated,
                updated ? "Preferences saved."
                        : "Could not save preferences. Please try again.",
                updated ? userId : -1);
    }

    public void updatePreferencesAsync(
            long userId, String level, String subjects, String style,
            @NonNull ResultCallback callback) {
        runAsync(() -> updatePreferences(userId, level, subjects, style),
                "Could not save preferences. Please try again.", callback);
    }

    /**
     * Deletes only the authenticated student's local account and related study
     * records. The transaction prevents a partially deleted account.
     */
    @WorkerThread
    public Result deleteAccount(long userId, String password) {
        User user = userDao.findById(userId);
        if (user == null) {
            return new Result(false, "Account not found.", -1);
        }
        if (!SecurityUtils.verify(password == null ? "" : password,
                user.salt, user.passwordHash)) {
            return new Result(false, "Incorrect password.", -1);
        }
        try {
            database.runInTransaction(() -> {
                database.questionDao().deleteForUser(userId);
                database.quizAttemptDao().deleteForUser(userId);
                database.pendingQuestionDao().deleteForUser(userId);
                userDao.deleteById(userId);
            });
        } catch (RuntimeException deletionFailed) {
            return new Result(false, "Could not delete local data. Please try again.", -1);
        }
        return new Result(true, "Account and local study data deleted.", userId);
    }

    public void deleteAccountAsync(
            long userId, String password,
            @NonNull ResultCallback callback) {
        runAsync(() -> deleteAccount(userId, password),
                "Could not delete local data. Please try again.", callback);
    }
}
