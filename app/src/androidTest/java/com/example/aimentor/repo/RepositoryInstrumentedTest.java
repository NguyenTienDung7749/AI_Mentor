package com.example.aimentor.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.aimentor.ai.AiAnswer;
import com.example.aimentor.ai.AiEngine;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.AppDatabase;
import com.example.aimentor.data.Question;
import com.example.aimentor.data.QuizAttempt;
import com.example.aimentor.data.User;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.SecurityUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration coverage for the Room-backed critical flows. Every test uses an
 * in-memory database, so installed accounts and study history are untouched.
 */
@RunWith(AndroidJUnit4.class)
public class RepositoryInstrumentedTest {

    private AppDatabase database;
    private MutableAiEngine engine;
    private StudyRepository studyRepository;
    private UserRepository userRepository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .build();
        engine = new MutableAiEngine();
        studyRepository = new StudyRepository(
                database, engine, new Handler(Looper.getMainLooper()));
        userRepository = new UserRepository(database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void ask_savesOnlyRemoteAnswersAndAwardsXpOnlyForSavedAnswer() {
        long userId = insertUser("learner@example.com", 0);

        engine.source = AnswerSource.REMOTE;
        StudyRepository.AskResult online = studyRepository.ask(
                userId, "What is gravity?", "Science");

        assertTrue(online.success);
        assertTrue(online.saved);
        assertEquals(1, database.questionDao().countForUser(userId));
        assertEquals(Gamification.XP_ASK, database.userDao().findById(userId).xp);
        assertEquals(AnswerSource.REMOTE.name(),
                database.questionDao()
                        .findByIdForUser(userId, online.questionId).answerSource);
        assertEquals("Science,Programming", engine.lastSubjects);

        engine.source = AnswerSource.LOCAL_FALLBACK;
        StudyRepository.AskResult offline = studyRepository.ask(
                userId, "Explain inertia.", "Science");

        assertTrue(offline.success);
        assertFalse(offline.saved);
        assertEquals(-1L, offline.questionId);
        assertEquals(1, database.questionDao().countForUser(userId));
        assertEquals(Gamification.XP_ASK, database.userDao().findById(userId).xp);
    }

    @Test
    public void ask_accountDeletedDuringGeneration_doesNotCreateOrphanAnswer() {
        long userId = insertUser("deleted-during-answer@example.com", 0);
        engine.onAnswer = () -> database.runInTransaction(() -> {
            database.questionDao().deleteForUser(userId);
            database.quizAttemptDao().deleteForUser(userId);
            database.userDao().deleteById(userId);
        });

        StudyRepository.AskResult result = studyRepository.ask(
                userId, "Explain atomic database writes.", "Programming");

        assertFalse(result.success);
        assertFalse(result.saved);
        assertEquals(-1L, result.questionId);
        assertNull(database.userDao().findById(userId));
        assertEquals(0, database.questionDao().countForUser(userId));
    }

    @Test
    public void ask_oversizedInput_isRejectedBeforeAiOrRoomWrite() {
        long userId = insertUser("oversized@example.com", 0);
        StringBuilder oversizedBuilder = new StringBuilder();
        for (int index = 0; index < 600; index++) {
            oversizedBuilder.append("Study input ");
        }
        String oversized = oversizedBuilder.toString();

        StudyRepository.AskResult result = studyRepository.ask(
                userId, oversized, "Auto");

        assertFalse(result.success);
        assertEquals(0, engine.answerCalls);
        assertEquals(0, database.questionDao().countForUser(userId));
        assertEquals(0, database.userDao().findById(userId).xp);
    }

    @Test
    public void historySearchBookmarkAndReview_areIsolatedAndIdempotent() {
        long firstUser = insertUser("first@example.com", 10);
        long secondUser = insertUser("second@example.com", 50);
        long firstQuestion = insertQuestion(
                firstUser, "Explain Java Arrays", "PROGRAMMING", true);
        insertQuestion(firstUser, "World Cup winner", "Sports/Entertainment", false);
        insertQuestion(firstUser, "Temporary offline answer", "Science",
                false, AnswerSource.LOCAL_FALLBACK);
        insertQuestion(secondUser, "Private second-user question", "Science", true);

        List<Question> history = studyRepository.getHistory(firstUser);
        assertEquals(2, history.size());
        assertEquals(3, database.questionDao().countForUser(firstUser));
        assertEquals(1, studyRepository.search(firstUser, "java arrays").size());
        assertEquals(1, studyRepository.search(firstUser, "JAVA ARRAYS").size());
        assertTrue(studyRepository.search(firstUser, "private").isEmpty());
        assertTrue(studyRepository.search(firstUser, "temporary offline").isEmpty());
        assertEquals(1, studyRepository.getBookmarked(firstUser).size());
        assertEquals(1, studyRepository.getBySubject(firstUser, "General").size());

        studyRepository.toggleBookmark(firstUser, firstQuestion);
        assertTrue(studyRepository.getBookmarked(firstUser).isEmpty());

        assertTrue(studyRepository.markReviewed(firstUser, firstQuestion));
        assertFalse(studyRepository.markReviewed(firstUser, firstQuestion));
        assertTrue(studyRepository.recordReviewDuration(
                firstUser, firstQuestion, 12_000L));
        assertFalse(studyRepository.recordReviewDuration(
                firstUser, firstQuestion, 999L));
        Question reviewed = studyRepository.getQuestion(firstUser, firstQuestion);
        assertTrue(reviewed.reviewedAt > 0L);
        assertEquals(12_000L, reviewed.reviewDurationMs);
        assertEquals(10 + Gamification.XP_REVIEW,
                database.userDao().findById(firstUser).xp);
        assertEquals(50, database.userDao().findById(secondUser).xp);
    }

    @Test
    public void questionActions_requireMatchingOwner() {
        long owner = insertUser("owner@example.com", 10);
        long otherUser = insertUser("other@example.com", 50);
        long questionId = insertQuestion(
                owner, "Private study answer", "Science", false);

        assertNull(studyRepository.getQuestion(otherUser, questionId));
        assertFalse(studyRepository.toggleBookmark(otherUser, questionId));
        assertFalse(studyRepository.markReviewed(otherUser, questionId));

        Question unchanged = studyRepository.getQuestion(owner, questionId);
        assertFalse(unchanged.bookmarked);
        assertFalse(unchanged.reviewed);
        assertEquals(10, database.userDao().findById(owner).xp);
        assertEquals(50, database.userDao().findById(otherUser).xp);
    }

    @Test
    public void concurrentReview_awardsXpExactlyOnce() throws Exception {
        long userId = insertUser("review-race@example.com", 20);
        long questionId = insertQuestion(
                userId, "Review exactly once", "Science", false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> first;
        Future<Boolean> second;
        try {
            first = executor.submit(() -> {
                start.await();
                return studyRepository.markReviewed(userId, questionId);
            });
            second = executor.submit(() -> {
                start.await();
                return studyRepository.markReviewed(userId, questionId);
            });
            start.countDown();
            int awardedCount = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, awardedCount);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(studyRepository.getQuestion(userId, questionId).reviewed);
        assertEquals(20 + Gamification.XP_REVIEW,
                database.userDao().findById(userId).xp);
    }

    @Test
    public void concurrentQuizWrites_preserveEveryXpAward() throws Exception {
        long userId = insertUser("concurrent@example.com", 0);
        int writeCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<StudyRepository.QuizResult>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < writeCount; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return studyRepository.recordQuiz(
                            userId, "Science", 1, 1);
                }));
            }
            start.countDown();
            for (Future<StudyRepository.QuizResult> future : futures) {
                assertTrue(future.get(10, TimeUnit.SECONDS).recorded);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(writeCount,
                database.quizAttemptDao().countForUser(userId));
        assertEquals(writeCount * Gamification.XP_QUIZ_CORRECT,
                database.userDao().findById(userId).xp);
    }

    @Test
    public void invalidQuizAttempt_isNotPersistedOrRewarded() {
        long userId = insertUser("invalid-quiz@example.com", 25);

        StudyRepository.QuizResult empty =
                studyRepository.recordQuiz(userId, "Science", 4, 0);
        StudyRepository.QuizResult missingUser =
                studyRepository.recordQuiz(99999L, "Science", 1, 1);

        assertFalse(empty.recorded);
        assertFalse(missingUser.recorded);
        assertEquals(0, database.quizAttemptDao().countForUser(userId));
        assertEquals(25, database.userDao().findById(userId).xp);
    }

    @Test
    public void quizAndProgress_useClampedRealRoomData() {
        long userId = insertUser("progress@example.com", 90);
        insertQuestion(userId, "Question one", "Science", true);

        StudyRepository.QuizResult first =
                studyRepository.recordQuiz(userId, "Science", 3, 4);
        StudyRepository.QuizResult second =
                studyRepository.recordQuiz(userId, "Unknown category", 8, 2);
        StudyRepository.Progress progress = studyRepository.getProgress(userId);

        assertEquals(15, first.awardedXp);
        assertTrue(first.leveledUp);
        assertEquals(10, second.awardedXp);
        assertEquals(2, progress.quizzesCompleted);
        assertEquals(5, progress.totalCorrect);
        assertEquals(6, progress.totalAnswered);
        assertEquals(83, progress.accuracyPercent);
        assertEquals(115, progress.xp);
        assertEquals(2, progress.level);
        assertEquals(1, progress.totalQuestions);
        assertEquals(1, progress.bookmarkedCount);
        assertEquals(Integer.valueOf(1),
                progress.subjectQuizCounts.get(SubjectClassifier.SCIENCE));
        assertEquals(Integer.valueOf(1),
                progress.subjectQuizCounts.get(SubjectClassifier.GENERAL));
        assertTrue(progress.activityCountLast7Days >= 3);
        assertTrue(progress.badges.contains("First Question"));
        assertTrue(progress.badges.contains("Quiz Starter"));
    }

    @Test
    public void progress_emptyData_hasStableZeroState() {
        long userId = insertUser("empty@example.com", 0);

        StudyRepository.Progress progress = studyRepository.getProgress(userId);

        assertEquals(0, progress.totalQuestions);
        assertEquals(0, progress.quizzesCompleted);
        assertEquals(0, progress.totalAnswered);
        assertEquals(0, progress.accuracyPercent);
        assertEquals(1, progress.level);
        assertEquals(100, progress.xpToNext);
        assertEquals("-", progress.topSubject);
        assertEquals(7, progress.last7Days.size());
        assertTrue(progress.badges.isEmpty());
        assertFalse(progress.insights.isEmpty());
    }

    @Test
    public void progress_buildsRealAccuracyTrendsAndRepeatedTopics() {
        long userId = insertUser("trends@example.com", 0);
        insertQuestion(userId, "Explain Android lifecycle", "Programming", false);
        insertQuestion(userId, "Compare Android lifecycle states", "Programming", false);
        long now = System.currentTimeMillis();
        insertQuizAt(userId, 3, 4, now - TimeUnit.DAYS.toMillis(1));
        insertQuizAt(userId, 1, 4, now - TimeUnit.DAYS.toMillis(10));

        StudyRepository.Progress progress = studyRepository.getProgress(userId);

        assertEquals(75, progress.current7DayAccuracy);
        assertEquals(25, progress.previous7DayAccuracy);
        assertEquals(4, progress.current7DayAnswered);
        assertEquals(4, progress.previous7DayAnswered);
        assertEquals(50, progress.current30DayAccuracy);
        assertFalse(progress.repeatedTopics.isEmpty());
        assertEquals("android", progress.repeatedTopics.get(0).topic);
        assertEquals(2, progress.repeatedTopics.get(0).questionCount);
    }

    @Test
    public void asyncReadCallbacks_deliverRealDataOnMainThread()
            throws Exception {
        long userId = insertUser("async-read@example.com", 35);
        insertQuestion(userId, "Async Room query", "Programming", false);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicBoolean progressOnMain = new AtomicBoolean();
        AtomicBoolean userOnMain = new AtomicBoolean();
        AtomicReference<StudyRepository.Progress> progress =
                new AtomicReference<>();
        AtomicReference<User> user = new AtomicReference<>();

        studyRepository.getProgressAsync(userId, value -> {
            progress.set(value);
            progressOnMain.set(
                    Looper.myLooper() == Looper.getMainLooper());
            completed.countDown();
        });
        userRepository.getUserAsync(userId, value -> {
            user.set(value);
            userOnMain.set(
                    Looper.myLooper() == Looper.getMainLooper());
            completed.countDown();
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertTrue(progressOnMain.get());
        assertTrue(userOnMain.get());
        assertEquals(1, progress.get().totalQuestions);
        assertEquals(35, progress.get().xp);
        assertEquals(userId, user.get().id);
    }

    @Test
    public void asyncAuthenticationAndOnboarding_runWithoutMainThreadRoom()
            throws Exception {
        AtomicBoolean callbacksOnMain = new AtomicBoolean(true);
        AtomicReference<UserRepository.Result> registered =
                new AtomicReference<>();
        CountDownLatch registerDone = new CountDownLatch(1);
        userRepository.registerAsync(
                "Async Student", "async-auth@example.com", "Study2026",
                result -> {
                    callbacksOnMain.set(callbacksOnMain.get()
                            && Looper.myLooper() == Looper.getMainLooper());
                    registered.set(result);
                    registerDone.countDown();
                });

        assertTrue(registerDone.await(5, TimeUnit.SECONDS));
        assertTrue(registered.get().success);
        long userId = registered.get().userId;

        AtomicReference<UserRepository.Result> loggedIn =
                new AtomicReference<>();
        CountDownLatch loginDone = new CountDownLatch(1);
        userRepository.loginAsync(
                "ASYNC-AUTH@example.com", "Study2026", result -> {
                    callbacksOnMain.set(callbacksOnMain.get()
                            && Looper.myLooper() == Looper.getMainLooper());
                    loggedIn.set(result);
                    loginDone.countDown();
                });
        assertTrue(loginDone.await(5, TimeUnit.SECONDS));
        assertTrue(loggedIn.get().success);
        assertEquals(userId, loggedIn.get().userId);

        AtomicReference<UserRepository.Result> onboarded =
                new AtomicReference<>();
        CountDownLatch onboardingDone = new CountDownLatch(1);
        userRepository.saveOnboardingAsync(
                userId, "University", "Science,Programming", "Detailed",
                result -> {
                    callbacksOnMain.set(callbacksOnMain.get()
                            && Looper.myLooper() == Looper.getMainLooper());
                    onboarded.set(result);
                    onboardingDone.countDown();
                });
        assertTrue(onboardingDone.await(5, TimeUnit.SECONDS));
        assertTrue(onboarded.get().success);
        assertTrue(callbacksOnMain.get());
        User user = database.userDao().findById(userId);
        assertTrue(user.onboardingCompleted);
        assertEquals("University", user.educationLevel);
        assertEquals("Detailed", user.explanationStyle);
    }

    @Test
    public void asyncStudyAndPreferenceMutations_persistAtomically()
            throws Exception {
        long userId = insertUser("async-mutations@example.com", 0);
        long questionId = insertQuestion(
                userId, "Async mutation", "Science", false);
        CountDownLatch completed = new CountDownLatch(4);
        AtomicReference<UserRepository.Result> preferences =
                new AtomicReference<>();
        AtomicReference<Boolean> bookmarked = new AtomicReference<>();
        AtomicReference<Boolean> reviewed = new AtomicReference<>();
        AtomicReference<StudyRepository.QuizResult> quiz =
                new AtomicReference<>();

        userRepository.updatePreferencesAsync(
                userId, "Middle School", "Science", "Short", result -> {
                    preferences.set(result);
                    completed.countDown();
                });
        studyRepository.toggleBookmarkAsync(
                userId, questionId, result -> {
                    bookmarked.set(result);
                    completed.countDown();
                });
        studyRepository.markReviewedAsync(
                userId, questionId, result -> {
                    reviewed.set(result);
                    completed.countDown();
                });
        studyRepository.recordQuizAsync(
                userId, "Science", 2, 3, result -> {
                    quiz.set(result);
                    completed.countDown();
                });

        assertTrue(completed.await(10, TimeUnit.SECONDS));
        assertTrue(preferences.get().success);
        assertTrue(bookmarked.get());
        assertTrue(reviewed.get());
        assertTrue(quiz.get().recorded);

        User user = database.userDao().findById(userId);
        Question question =
                database.questionDao().findByIdForUser(userId, questionId);
        assertEquals("Middle School", user.educationLevel);
        assertEquals(Gamification.XP_REVIEW
                + 2 * Gamification.XP_QUIZ_CORRECT, user.xp);
        assertTrue(question.bookmarked);
        assertTrue(question.reviewed);
        assertEquals(1, database.quizAttemptDao().countForUser(userId));
    }

    @Test
    public void asyncDeleteAccount_removesOnlyAuthenticatedUserData()
            throws Exception {
        long deletedUser =
                insertPasswordUser("async-delete@example.com", "Study2026");
        long retainedUser =
                insertPasswordUser("async-keep@example.com", "Keep2026");
        insertQuestion(deletedUser, "Delete asynchronously", "Science", false);
        insertQuestion(retainedUser, "Keep asynchronously", "History", false);
        insertQuiz(deletedUser);
        insertQuiz(retainedUser);

        AtomicReference<UserRepository.Result> deletion =
                new AtomicReference<>();
        AtomicBoolean callbackOnMain = new AtomicBoolean(false);
        CountDownLatch completed = new CountDownLatch(1);
        userRepository.deleteAccountAsync(
                deletedUser, "Study2026", result -> {
                    callbackOnMain.set(
                            Looper.myLooper() == Looper.getMainLooper());
                    deletion.set(result);
                    completed.countDown();
                });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertTrue(callbackOnMain.get());
        assertTrue(deletion.get().success);
        assertNull(database.userDao().findById(deletedUser));
        assertEquals(0, database.questionDao().countForUser(deletedUser));
        assertEquals(0, database.quizAttemptDao().countForUser(deletedUser));
        assertTrue(database.userDao().findById(retainedUser) != null);
        assertEquals(1, database.questionDao().countForUser(retainedUser));
        assertEquals(1, database.quizAttemptDao().countForUser(retainedUser));
    }

    @Test
    public void accountDeletion_requiresPasswordAndPreservesOtherUserData() {
        long firstUser = insertPasswordUser("delete@example.com", "Study2026");
        long secondUser = insertPasswordUser("keep@example.com", "Keep2026");
        insertQuestion(firstUser, "Delete me", "Science", false);
        insertQuestion(secondUser, "Keep me", "History", false);
        insertQuiz(firstUser);
        insertQuiz(secondUser);

        UserRepository.Result rejected =
                userRepository.deleteAccount(firstUser, "wrong-password");
        assertFalse(rejected.success);
        assertEquals(1, database.questionDao().countForUser(firstUser));

        UserRepository.Result deleted =
                userRepository.deleteAccount(firstUser, "Study2026");
        assertTrue(deleted.success);
        assertNull(database.userDao().findById(firstUser));
        assertEquals(0, database.questionDao().countForUser(firstUser));
        assertEquals(0, database.quizAttemptDao().countForUser(firstUser));

        assertTrue(database.userDao().findById(secondUser) != null);
        assertEquals(1, database.questionDao().countForUser(secondUser));
        assertEquals(1, database.quizAttemptDao().countForUser(secondUser));
    }

    @Test
    public void registrationAndLogin_normalizeIdentityAndRejectDuplicate() {
        UserRepository.Result registered = userRepository.register(
                "Student", "  Student@Example.COM ", "Study2026");
        assertTrue(registered.success);
        assertEquals("student@example.com",
                database.userDao().findById(registered.userId).email);

        UserRepository.Result duplicate = userRepository.register(
                "Other", "student@example.com", "Study2026");
        assertFalse(duplicate.success);

        UserRepository.Result wrong =
                userRepository.login("STUDENT@example.com", "wrong");
        assertFalse(wrong.success);

        UserRepository.Result loggedIn =
                userRepository.login("STUDENT@example.com", "Study2026");
        assertTrue(loggedIn.success);
        assertEquals(registered.userId, loggedIn.userId);
    }

    private long insertUser(String email, int xp) {
        User user = new User();
        user.email = email;
        user.name = "Test Student";
        user.xp = xp;
        user.educationLevel = "University";
        user.explanationStyle = "Detailed";
        user.subjects = "Science,Programming";
        return database.userDao().insert(user);
    }

    private long insertPasswordUser(String email, String password) {
        String salt = SecurityUtils.generateSalt();
        User user = new User();
        user.email = email;
        user.name = "Test Student";
        user.salt = salt;
        user.passwordHash = SecurityUtils.hashPassword(password, salt);
        return database.userDao().insert(user);
    }

    private long insertQuestion(long userId, String text, String subject,
                                boolean bookmarked) {
        return insertQuestion(
                userId, text, subject, bookmarked, AnswerSource.REMOTE);
    }

    private long insertQuestion(long userId, String text, String subject,
                                boolean bookmarked, AnswerSource source) {
        Question question = new Question();
        question.userId = userId;
        question.questionText = text;
        question.answerText = "Saved answer for " + text;
        question.subject = subject;
        question.bookmarked = bookmarked;
        question.answerSource = source.name();
        question.createdAt = System.currentTimeMillis();
        return database.questionDao().insert(question);
    }

    private void insertQuiz(long userId) {
        insertQuizAt(userId, 1, 1, System.currentTimeMillis());
    }

    private void insertQuizAt(long userId, int correct, int total, long createdAt) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.userId = userId;
        attempt.subject = SubjectClassifier.GENERAL;
        attempt.correct = correct;
        attempt.total = total;
        attempt.createdAt = createdAt;
        database.quizAttemptDao().insert(attempt);
    }

    private static class MutableAiEngine implements AiEngine {
        AnswerSource source = AnswerSource.REMOTE;
        Runnable onAnswer;
        String lastSubjects = "";
        int answerCalls;

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint) {
            return answer(question, educationLevel, explanationStyle,
                    subjectHint, "");
        }

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint,
                               String subjects) {
            answerCalls++;
            if (onAnswer != null) onAnswer.run();
            lastSubjects = subjects;
            AiAnswer answer = new AiAnswer();
            answer.setSubject(subjectHint);
            answer.setDifficulty("Intermediate");
            answer.setDirectAnswer("Test answer");
            answer.setSource(source);
            answer.setModelName(source == AnswerSource.REMOTE
                    ? "test-online-model" : "offline-rules");
            return answer;
        }

        @Override
        public List<QuizQuestion> generateQuiz(String subject, int count) {
            return Collections.emptyList();
        }

        @Override
        public String name() {
            return "test-engine";
        }
    }
}
