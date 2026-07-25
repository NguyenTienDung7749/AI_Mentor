package com.example.aimentor.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.aimentor.ai.AiAnswer;
import com.example.aimentor.ai.AiEngine;
import com.example.aimentor.ai.AiEngineFactory;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.ai.ImageAttachment;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.ai.QuizGenerationConfig;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.AppDatabase;
import com.example.aimentor.data.Question;
import com.example.aimentor.data.QuestionDao;
import com.example.aimentor.data.QuizAttempt;
import com.example.aimentor.data.QuizAttemptDao;
import com.example.aimentor.data.User;
import com.example.aimentor.data.UserDao;
import com.example.aimentor.util.ContentModerator;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.LearningAnalytics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Core study logic: asking fresh online questions, generating practice
 * quizzes, recording results, awarding XP and computing progress statistics.
 * Only successful remote answers are persisted.
 */
public class StudyRepository {

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final long MAX_REVIEW_SESSION_MS = 30L * 60L * 1000L;

    private final AppDatabase database;
    private final UserDao userDao;
    private final QuestionDao questionDao;
    private final QuizAttemptDao quizDao;
    private final AiEngine engine;
    private final Handler mainHandler;

    public StudyRepository(Context context) {
        this(AppDatabase.getInstance(context), AiEngineFactory.create(),
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Package-private injection seam used by instrumented tests with an
     * in-memory Room database. Production callers continue to use Context.
     */
    StudyRepository(AppDatabase db, AiEngine engine, Handler mainHandler) {
        this.database = db;
        this.userDao = db.userDao();
        this.questionDao = db.questionDao();
        this.quizDao = db.quizAttemptDao();
        this.engine = engine;
        this.mainHandler = mainHandler;
    }

    public AiEngine getEngine() {
        return engine;
    }

    public interface DataCallback<T> {
        void onResult(T value);
    }

    private <T> void executeAsync(
            @NonNull Supplier<T> operation, T fallback,
            @NonNull DataCallback<T> callback) {
        IO_EXECUTOR.execute(() -> {
            T value;
            try {
                value = operation.get();
            } catch (RuntimeException operationFailed) {
                value = fallback;
            }
            T delivered = value;
            mainHandler.post(() -> callback.onResult(delivered));
        });
    }

    // ----------------------------------------------------------------- Ask

    public static class AskResult {
        public final boolean success;
        public final String message;
        public final long questionId;
        public final boolean saved;
        public final boolean leveledUp;
        public final AnswerSource source;
        public final Question answer;

        AskResult(boolean success, String message, long questionId, boolean saved,
                  boolean leveledUp, AnswerSource source, Question answer) {
            this.success = success;
            this.message = message;
            this.questionId = questionId;
            this.saved = saved;
            this.leveledUp = leveledUp;
            this.source = source == null ? AnswerSource.LEGACY : source;
            this.answer = answer;
        }
    }

    public interface AskCallback {
        void onResult(@NonNull AskResult result);
    }

    /**
     * Runs question processing and Room access away from the UI thread, then
     * delivers the result on the main thread. The local engine remains the
     * active implementation in Batch 1; a remote engine can use this same seam.
     */
    public void askAsync(long userId, String questionText, String subjectHint,
                         @NonNull AskCallback callback) {
        askAsync(userId, questionText, subjectHint,
                "", null, callback);
    }

    public void askAsync(long userId, String questionText, String subjectHint,
                         String explanationStyle, ImageAttachment image,
                         @NonNull AskCallback callback) {
        IO_EXECUTOR.execute(() -> {
            AskResult result;
            try {
                result = ask(userId, questionText, subjectHint,
                        explanationStyle, image);
            } catch (RuntimeException ignored) {
                result = new AskResult(false,
                        "Couldn’t connect to AI Mentor. Check your internet connection and try again.",
                        -1, false, false, AnswerSource.LEGACY, null);
            }
            AskResult deliveredResult = result;
            mainHandler.post(() -> callback.onResult(deliveredResult));
        });
    }

    @WorkerThread
    public AskResult ask(long userId, String questionText, String subjectHint) {
        return ask(userId, questionText, subjectHint, "", null);
    }

    @WorkerThread
    public AskResult ask(long userId, String questionText, String subjectHint,
                         String explanationStyle, ImageAttachment image) {
        ContentModerator.Result mod = ContentModerator.check(questionText);
        if (!mod.allowed) {
            return new AskResult(false, mod.reason, -1, false, false,
                    AnswerSource.LEGACY, null);
        }
        User user = userDao.findById(userId);
        if (user == null) {
            return new AskResult(false, "Please sign in again.", -1, false, false,
                    AnswerSource.LEGACY, null);
        }

        long startedAt = System.nanoTime();
        String resolvedStyle = explanationStyle == null
                || explanationStyle.trim().isEmpty()
                ? user.explanationStyle : explanationStyle.trim();
        AiAnswer generated = image == null
                ? engine.answer(questionText, user.educationLevel,
                resolvedStyle, subjectHint, user.subjects)
                : engine.answerWithImage(questionText, image,
                user.educationLevel, resolvedStyle, subjectHint, user.subjects);
        long elapsedNanos = System.nanoTime() - startedAt;

        AnswerSource answerSource = generated.getSource();
        Question q = new Question();
        q.userId = userId;
        q.questionText = questionText.trim();
        q.subject = generated.getSubject();
        q.difficulty = generated.getDifficulty();
        q.answerText = generated.toDisplayString(
                questionText, resolvedStyle);
        q.answerSource = answerSource.name();
        q.modelName = generated.getModelName();
        q.responseTimeMs = Math.max(0L, elapsedNanos / 1_000_000L);

        if (answerSource == AnswerSource.REMOTE && generated.isOutOfScope()) {
            return new AskResult(true, "Academic-scope guidance shown.",
                    -1L, false, false, answerSource, q);
        }
        if (answerSource == AnswerSource.REMOTE) {
            return saveRemoteAnswer(userId, q, answerSource);
        }

        return new AskResult(false,
                "Couldn’t connect to AI Mentor. Check your internet connection and try again.",
                -1L, false, false, answerSource, null);
    }

    /**
     * Re-checks the account after the network request and saves the answer and
     * XP in one transaction. If account deletion wins the race, no orphan
     * question is inserted.
     */
    private AskResult saveRemoteAnswer(
            long userId, Question question, AnswerSource answerSource) {
        try {
            return database.runInTransaction(() -> {
                User currentUser = userDao.findById(userId);
                if (currentUser == null) {
                    return new AskResult(false,
                            "Your account is no longer available. Please sign in again.",
                            -1L, false, false, AnswerSource.LEGACY, null);
                }
                int beforeLevel = Gamification.levelForXp(currentUser.xp);
                long questionId = questionDao.insert(question);
                if (userDao.addXp(userId, Gamification.XP_ASK) != 1) {
                    throw new IllegalStateException("Could not award question XP");
                }
                User updatedUser = userDao.findById(userId);
                if (updatedUser == null) {
                    throw new IllegalStateException("Account disappeared during save");
                }
                question.id = questionId;
                boolean leveledUp =
                        Gamification.levelForXp(updatedUser.xp) > beforeLevel;
                return new AskResult(true, "Online answer ready and saved.",
                        questionId, true, leveledUp, answerSource, question);
            });
        } catch (RuntimeException saveFailed) {
            return new AskResult(false,
                    "The answer arrived but could not be saved. Please try again.",
                    -1L, false, false, AnswerSource.LEGACY, null);
        }
    }

    // --------------------------------------------------------------- Library

    @WorkerThread
    public List<Question> getHistory(long userId) {
        return normalizeSubjects(questionDao.getForUser(userId));
    }

    public void getHistoryAsync(
            long userId, @NonNull DataCallback<List<Question>> callback) {
        executeAsync(() -> getHistory(userId), new ArrayList<>(), callback);
    }

    @WorkerThread
    public List<Question> search(long userId, String query) {
        if (query == null || query.trim().isEmpty()) return getHistory(userId);
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Question> matches = new ArrayList<>();
        for (Question question : getHistory(userId)) {
            String questionText = question.questionText == null
                    ? "" : question.questionText.toLowerCase(Locale.ROOT);
            String answerText = question.answerText == null
                    ? "" : question.answerText.toLowerCase(Locale.ROOT);
            if (questionText.contains(needle) || answerText.contains(needle)) {
                matches.add(question);
            }
        }
        return matches;
    }

    public void searchAsync(
            long userId, String query,
            @NonNull DataCallback<List<Question>> callback) {
        executeAsync(() -> search(userId, query), new ArrayList<>(), callback);
    }

    @WorkerThread
    public List<Question> getBookmarked(long userId) {
        return normalizeSubjects(questionDao.getBookmarked(userId));
    }

    public void getBookmarkedAsync(
            long userId, @NonNull DataCallback<List<Question>> callback) {
        executeAsync(() -> getBookmarked(userId), new ArrayList<>(), callback);
    }

    @WorkerThread
    public List<Question> getBySubject(long userId, String subject) {
        List<Question> matches = new ArrayList<>();
        String normalizedSubject = SubjectClassifier.normalize(subject);
        for (Question question : getHistory(userId)) {
            if (normalizedSubject.equals(question.subject)) {
                matches.add(question);
            }
        }
        return matches;
    }

    @WorkerThread
    public Question getQuestion(long userId, long questionId) {
        return normalizeSubject(
                questionDao.findByIdForUser(userId, questionId));
    }

    public void getQuestionAsync(
            long userId, long questionId,
            @NonNull DataCallback<Question> callback) {
        executeAsync(() -> getQuestion(userId, questionId), null, callback);
    }

    @WorkerThread
    public boolean toggleBookmark(long userId, long questionId) {
        return questionDao.toggleBookmark(userId, questionId) == 1;
    }

    public void toggleBookmarkAsync(
            long userId, long questionId,
            @NonNull DataCallback<Boolean> callback) {
        executeAsync(
                () -> toggleBookmark(userId, questionId), false, callback);
    }

    /** Marks a saved answer as reviewed, awarding review XP the first time only. */
    @WorkerThread
    public boolean markReviewed(long userId, long questionId) {
        try {
            return database.runInTransaction(() -> {
                if (userDao.findById(userId) == null) return false;
                int marked = questionDao.markReviewedIfNeeded(
                        userId, questionId, System.currentTimeMillis());
                if (marked != 1) return false;
                if (userDao.addXp(userId, Gamification.XP_REVIEW) != 1) {
                    throw new IllegalStateException("Could not award review XP");
                }
                return true;
            });
        } catch (RuntimeException updateFailed) {
            return false;
        }
    }

    public void markReviewedAsync(
            long userId, long questionId,
            @NonNull DataCallback<Boolean> callback) {
        executeAsync(
                () -> markReviewed(userId, questionId), false, callback);
    }

    /** Adds genuine foreground reading time without affecting XP or review state. */
    @WorkerThread
    public boolean recordReviewDuration(long userId, long questionId, long durationMs) {
        long safeDuration = Math.min(durationMs, MAX_REVIEW_SESSION_MS);
        if (userId <= 0 || questionId <= 0 || safeDuration < 1000L) return false;
        return questionDao.addReviewDuration(
                userId, questionId, safeDuration) == 1;
    }

    public void recordReviewDurationAsync(
            long userId, long questionId, long durationMs) {
        IO_EXECUTOR.execute(() -> {
            try {
                recordReviewDuration(userId, questionId, durationMs);
            } catch (RuntimeException ignored) {
                // Reading analytics must never interrupt the answer screen.
            }
        });
    }

    // ------------------------------------------------------------------ Quiz

    public List<QuizQuestion> generateQuiz(String subject, int count) {
        return engine.generateQuiz(subject, count);
    }

    public static class QuizLoadResult {
        public final boolean success;
        public final String message;
        public final String subject;
        public final String difficulty;
        public final List<QuizQuestion> questions;

        QuizLoadResult(boolean success, String message, String subject,
                       String difficulty, List<QuizQuestion> questions) {
            this.success = success;
            this.message = message;
            this.subject = subject;
            this.difficulty = difficulty;
            this.questions = questions == null ? new ArrayList<>() : questions;
        }
    }

    public interface QuizLoadCallback {
        void onResult(@NonNull QuizLoadResult result);
    }

    public void generateQuizAsync(long userId, String requestedSubject,
                                  String requestedDifficulty, int count,
                                  @NonNull QuizLoadCallback callback) {
        IO_EXECUTOR.execute(() -> {
            QuizLoadResult result;
            try {
                result = generatePersonalizedQuiz(
                        userId, requestedSubject, requestedDifficulty, count);
            } catch (RuntimeException ignored) {
                result = new QuizLoadResult(false,
                        "Could not prepare a quiz. Please try again.",
                        SubjectClassifier.GENERAL, "Intermediate", null);
            }
            QuizLoadResult delivered = result;
            mainHandler.post(() -> callback.onResult(delivered));
        });
    }

    @WorkerThread
    private QuizLoadResult generatePersonalizedQuiz(
            long userId, String requestedSubject,
            String requestedDifficulty, int count) {
        List<Question> history = normalizeSubjects(questionDao.getForUser(userId));
        boolean personalized = requestedSubject == null
                || requestedSubject.toLowerCase(Locale.ROOT).startsWith("personalized");
        String resolvedSubject = personalized
                ? mostStudiedSubject(history)
                : SubjectClassifier.normalize(requestedSubject);
        String resolvedDifficulty = resolveQuizDifficulty(
                userId, requestedDifficulty, userDao.findById(userId));

        List<String> topics = new ArrayList<>();
        for (Question question : history) {
            if (resolvedSubject.equals(question.subject)
                    && question.questionText != null
                    && !question.questionText.trim().isEmpty()) {
                topics.add(question.questionText.trim());
                if (topics.size() >= 5) break;
            }
        }
        QuizGenerationConfig config = new QuizGenerationConfig(
                resolvedSubject, resolvedDifficulty, count, topics);
        List<QuizQuestion> generated = engine.generateQuiz(config);
        if (generated == null || generated.isEmpty()) {
            return new QuizLoadResult(false,
                    "Could not prepare a quiz. Please try again.",
                    resolvedSubject, resolvedDifficulty, generated);
        }
        String mode = personalized && !topics.isEmpty()
                ? "Personalized from your recent questions."
                : "Quiz prepared for the selected subject.";
        return new QuizLoadResult(true, mode, resolvedSubject,
                resolvedDifficulty, generated);
    }

    private String mostStudiedSubject(List<Question> history) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String best = SubjectClassifier.GENERAL;
        int bestCount = 0;
        for (Question question : history) {
            String subject = SubjectClassifier.normalize(question.subject);
            int count = (counts.containsKey(subject) ? counts.get(subject) : 0) + 1;
            counts.put(subject, count);
            if (count > bestCount) {
                best = subject;
                bestCount = count;
            }
        }
        return best;
    }

    private String resolveQuizDifficulty(
            long userId, String requestedDifficulty, User user) {
        if (requestedDifficulty != null
                && !requestedDifficulty.equalsIgnoreCase("Adaptive")) {
            if (requestedDifficulty.equalsIgnoreCase("Beginner")) return "Beginner";
            if (requestedDifficulty.equalsIgnoreCase("Advanced")) return "Advanced";
            return "Intermediate";
        }
        int answered = quizDao.totalAnswered(userId);
        if (answered >= 5) {
            int accuracy = Math.round(
                    quizDao.totalCorrect(userId) * 100f / Math.max(1, answered));
            if (accuracy >= 80) return "Advanced";
            if (accuracy < 50) return "Beginner";
            return "Intermediate";
        }
        String level = user == null || user.educationLevel == null
                ? "" : user.educationLevel.toLowerCase(Locale.ROOT);
        if (level.contains("middle")) return "Beginner";
        if (level.contains("university") || level.contains("college")) return "Advanced";
        return "Intermediate";
    }

    public static class QuizResult {
        public final int awardedXp;
        public final boolean leveledUp;
        public final int newLevel;
        public final boolean recorded;
        QuizResult(int awardedXp, boolean leveledUp, int newLevel,
                   boolean recorded) {
            this.awardedXp = awardedXp;
            this.leveledUp = leveledUp;
            this.newLevel = newLevel;
            this.recorded = recorded;
        }
    }

    @WorkerThread
    public QuizResult recordQuiz(long userId, String subject, int correct, int total) {
        int safeTotal = Math.max(0, total);
        if (safeTotal == 0) {
            User user = userDao.findById(userId);
            int level = user == null ? 1 : Gamification.levelForXp(user.xp);
            return new QuizResult(0, false, level, false);
        }
        int safeCorrect = Math.max(0, Math.min(correct, safeTotal));
        try {
            return database.runInTransaction(() -> {
                User currentUser = userDao.findById(userId);
                if (currentUser == null) {
                    return new QuizResult(0, false, 1, false);
                }
                int beforeLevel = Gamification.levelForXp(currentUser.xp);
                QuizAttempt attempt = new QuizAttempt();
                attempt.userId = userId;
                attempt.subject = SubjectClassifier.normalize(subject);
                attempt.correct = safeCorrect;
                attempt.total = safeTotal;
                quizDao.insert(attempt);

                int awarded = safeCorrect * Gamification.XP_QUIZ_CORRECT;
                if (awarded > 0 && userDao.addXp(userId, awarded) != 1) {
                    throw new IllegalStateException("Could not award quiz XP");
                }
                User updatedUser = userDao.findById(userId);
                if (updatedUser == null) {
                    throw new IllegalStateException("Account disappeared during quiz save");
                }
                int newLevel = Gamification.levelForXp(updatedUser.xp);
                return new QuizResult(
                        awarded, newLevel > beforeLevel, newLevel, true);
            });
        } catch (RuntimeException saveFailed) {
            return new QuizResult(0, false, 1, false);
        }
    }

    public void recordQuizAsync(
            long userId, String subject, int correct, int total,
            @NonNull DataCallback<QuizResult> callback) {
        executeAsync(() -> recordQuiz(
                        userId, subject, correct, total),
                new QuizResult(0, false, 1, false), callback);
    }

    // -------------------------------------------------------------- Progress

    public static class Progress {
        public int totalQuestions;
        public int bookmarkedCount;
        public int quizzesCompleted;
        public int totalCorrect;
        public int totalAnswered;
        public int accuracyPercent;
        public int xp;
        public int level;
        public String levelTitle = "Beginner";
        public int xpIntoLevel;
        public int xpToNext;
        public int activityCountLast7Days;
        public int activeDaysLast7Days;
        public int reviewedAnswers;
        public long totalReviewDurationMs;
        public int current7DayAccuracy;
        public int previous7DayAccuracy;
        public int current7DayAnswered;
        public int previous7DayAnswered;
        public int current30DayAccuracy;
        public int previous30DayAccuracy;
        public int current30DayAnswered;
        public int previous30DayAnswered;
        public String topSubject = "-";
        public final Map<String, Integer> subjectCounts = new LinkedHashMap<>();
        public final Map<String, Integer> subjectQuizCounts = new LinkedHashMap<>();
        public final List<DailyActivity> last7Days = new ArrayList<>();
        public final List<String> badges = new ArrayList<>();
        public final List<String> insights = new ArrayList<>();
        public final List<LearningAnalytics.TopicFrequency> repeatedTopics =
                new ArrayList<>();
    }

    public static class DailyActivity {
        public final long dayStart;
        public int count;

        DailyActivity(long dayStart) {
            this.dayStart = dayStart;
        }
    }

    @WorkerThread
    public Progress getProgress(long userId) {
        Progress p = new Progress();
        User user = userDao.findById(userId);
        int xp = user != null ? user.xp : 0;
        p.xp = xp;
        p.level = Gamification.levelForXp(xp);
        p.levelTitle = Gamification.titleForLevel(p.level);
        p.xpIntoLevel = Gamification.xpIntoLevel(xp);
        p.xpToNext = Gamification.xpToNextLevel(xp);

        List<Question> all = normalizeSubjects(questionDao.getForUser(userId));
        p.totalQuestions = all.size();
        int topCount = 0;
        for (Question q : all) {
            if (q.bookmarked) p.bookmarkedCount++;
            if (q.reviewed) p.reviewedAnswers++;
            p.totalReviewDurationMs += Math.max(0L, q.reviewDurationMs);
            String s = SubjectClassifier.normalize(q.subject);
            int c = (p.subjectCounts.containsKey(s) ? p.subjectCounts.get(s) : 0) + 1;
            p.subjectCounts.put(s, c);
            if (c > topCount) {
                topCount = c;
                p.topSubject = s;
            }
        }

        List<QuizAttempt> attempts = quizDao.getForUser(userId);
        p.quizzesCompleted = attempts.size();
        for (QuizAttempt attempt : attempts) {
            p.totalCorrect += Math.max(0, Math.min(attempt.correct, attempt.total));
            p.totalAnswered += Math.max(0, attempt.total);
            String subject = SubjectClassifier.normalize(attempt.subject);
            int count = (p.subjectQuizCounts.containsKey(subject)
                    ? p.subjectQuizCounts.get(subject) : 0) + 1;
            p.subjectQuizCounts.put(subject, count);
        }
        p.accuracyPercent =
                Gamification.accuracyPercent(p.totalCorrect, p.totalAnswered);
        buildAccuracyTrends(p, attempts, System.currentTimeMillis());
        List<String> questionTexts = new ArrayList<>();
        for (Question question : all) questionTexts.add(question.questionText);
        p.repeatedTopics.addAll(
                LearningAnalytics.repeatedTopics(questionTexts, 3));
        buildWeeklyActivity(p, all, attempts);

        p.badges.addAll(Gamification.earnedBadges(
                p.totalQuestions, p.quizzesCompleted, p.totalCorrect, p.level));

        buildInsights(p);
        return p;
    }

    public void getProgressAsync(
            long userId, @NonNull DataCallback<Progress> callback) {
        executeAsync(() -> getProgress(userId), emptyProgress(), callback);
    }

    private Progress emptyProgress() {
        Progress progress = new Progress();
        progress.level = Gamification.levelForXp(0);
        progress.levelTitle = Gamification.titleForLevel(progress.level);
        progress.xpIntoLevel = Gamification.xpIntoLevel(0);
        progress.xpToNext = Gamification.xpToNextLevel(0);
        buildWeeklyActivity(progress, new ArrayList<>(), new ArrayList<>());
        buildInsights(progress);
        return progress;
    }

    private List<Question> normalizeSubjects(List<Question> questions) {
        List<Question> visible = new ArrayList<>();
        if (questions == null) return visible;
        for (Question question : questions) {
            if (question != null
                    && !AnswerSource.LOCAL_FALLBACK.name()
                    .equalsIgnoreCase(question.answerSource)) {
                visible.add(normalizeSubject(question));
            }
        }
        return visible;
    }

    private Question normalizeSubject(Question question) {
        if (question != null) {
            question.subject = SubjectClassifier.normalize(question.subject);
        }
        return question;
    }

    private void buildWeeklyActivity(Progress progress, List<Question> questions,
                                     List<QuizAttempt> attempts) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -6);

        for (int day = 0; day < 7; day++) {
            progress.last7Days.add(new DailyActivity(calendar.getTimeInMillis()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        for (Question question : questions) {
            addActivity(progress, question.createdAt);
        }
        for (QuizAttempt attempt : attempts) {
            addActivity(progress, attempt.createdAt);
        }
        for (DailyActivity day : progress.last7Days) {
            progress.activityCountLast7Days += day.count;
            if (day.count > 0) progress.activeDaysLast7Days++;
        }
    }

    private void addActivity(Progress progress, long timestamp) {
        for (int index = 0; index < progress.last7Days.size(); index++) {
            DailyActivity day = progress.last7Days.get(index);
            long nextDayStart = index + 1 < progress.last7Days.size()
                    ? progress.last7Days.get(index + 1).dayStart
                    : day.dayStart + 24L * 60L * 60L * 1000L;
            if (timestamp >= day.dayStart && timestamp < nextDayStart) {
                day.count++;
                return;
            }
        }
    }

    private void buildAccuracyTrends(
            Progress progress, List<QuizAttempt> attempts, long now) {
        int[] current7 = quizTotals(attempts, now - 7L * DAY_MS, now);
        int[] previous7 = quizTotals(
                attempts, now - 14L * DAY_MS, now - 7L * DAY_MS);
        int[] current30 = quizTotals(attempts, now - 30L * DAY_MS, now);
        int[] previous30 = quizTotals(
                attempts, now - 60L * DAY_MS, now - 30L * DAY_MS);
        progress.current7DayAnswered = current7[1];
        progress.previous7DayAnswered = previous7[1];
        progress.current30DayAnswered = current30[1];
        progress.previous30DayAnswered = previous30[1];
        progress.current7DayAccuracy =
                Gamification.accuracyPercent(current7[0], current7[1]);
        progress.previous7DayAccuracy =
                Gamification.accuracyPercent(previous7[0], previous7[1]);
        progress.current30DayAccuracy =
                Gamification.accuracyPercent(current30[0], current30[1]);
        progress.previous30DayAccuracy =
                Gamification.accuracyPercent(previous30[0], previous30[1]);
    }

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private int[] quizTotals(List<QuizAttempt> attempts, long start, long end) {
        int correct = 0;
        int answered = 0;
        for (QuizAttempt attempt : attempts) {
            if (attempt.createdAt < start || attempt.createdAt >= end) continue;
            int total = Math.max(0, attempt.total);
            answered += total;
            correct += Math.max(0, Math.min(attempt.correct, total));
        }
        return new int[]{correct, answered};
    }

    private void buildInsights(Progress p) {
        if (p.totalQuestions == 0) {
            p.insights.add("Ask your first question to start your learning journey!");
        } else if (!p.topSubject.equals("-")) {
            p.insights.add("You frequently ask questions about " + p.topSubject + ".");
        }
        if (p.totalAnswered > 0) {
            String note = p.accuracyPercent >= 70 ? " Great work — keep it up!"
                    : " Keep practising to improve.";
            p.insights.add("Your quiz accuracy is " + p.accuracyPercent + "%." + note);
        }
        if (p.bookmarkedCount > 0) {
            p.insights.add("You have " + p.bookmarkedCount
                    + " bookmarked answer(s) waiting to be reviewed.");
        }
        if (!p.repeatedTopics.isEmpty()) {
            LearningAnalytics.TopicFrequency topic = p.repeatedTopics.get(0);
            p.insights.add("Recurring topic: " + topic.topic + " appeared in "
                    + topic.questionCount + " saved questions.");
        }
    }
}
