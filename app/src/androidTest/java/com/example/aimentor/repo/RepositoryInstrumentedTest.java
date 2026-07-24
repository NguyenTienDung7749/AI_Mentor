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
                .allowMainThreadQueries()
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
        QuizAttempt attempt = new QuizAttempt();
        attempt.userId = userId;
        attempt.subject = SubjectClassifier.GENERAL;
        attempt.correct = 1;
        attempt.total = 1;
        database.quizAttemptDao().insert(attempt);
    }

    private static class MutableAiEngine implements AiEngine {
        AnswerSource source = AnswerSource.REMOTE;
        Runnable onAnswer;

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint) {
            if (onAnswer != null) onAnswer.run();
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
