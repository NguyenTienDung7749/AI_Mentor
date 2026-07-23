package com.example.aimentor.repo;

import android.content.Context;

import com.example.aimentor.data.AppDatabase;
import com.example.aimentor.data.User;
import com.example.aimentor.data.UserDao;
import com.example.aimentor.util.PasswordValidator;
import com.example.aimentor.util.SecurityUtils;
import com.example.aimentor.util.Validators;

/** Registration and authentication backed by the local Room database. */
public class UserRepository {

    private final UserDao userDao;

    public UserRepository(Context context) {
        this.userDao = AppDatabase.getInstance(context).userDao();
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

    public Result register(String name, String email, String password) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
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

    public Result login(String email, String password) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        if (!Validators.isValidEmail(cleanEmail)) {
            return new Result(false, "Please enter a valid email address.", -1);
        }
        User user = userDao.findByEmail(cleanEmail);
        if (user == null) {
            return new Result(false, "No account found for this email.", -1);
        }
        if (!SecurityUtils.verify(password == null ? "" : password, user.salt, user.passwordHash)) {
            return new Result(false, "Incorrect password.", -1);
        }
        return new Result(true, "Welcome back!", user.id);
    }

    public User getUser(long id) {
        return userDao.findById(id);
    }

    public void saveOnboarding(long userId, String level, String subjects, String style) {
        User user = userDao.findById(userId);
        if (user == null) return;
        user.educationLevel = level;
        user.subjects = subjects;
        user.explanationStyle = style;
        user.onboardingCompleted = true;
        userDao.update(user);
    }

    public void updatePreferences(long userId, String level, String subjects, String style) {
        User user = userDao.findById(userId);
        if (user == null) return;
        user.educationLevel = level;
        user.subjects = subjects;
        user.explanationStyle = style;
        userDao.update(user);
    }
}
