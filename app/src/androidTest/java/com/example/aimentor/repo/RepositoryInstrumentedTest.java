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

import java.util.Collections;
import java.util.List;

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
                database.questionDao().findById(online.questionId).answerSource);

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
    public void historySearchBookmarkAndReview_areIsolatedAndIdempotent() {
        long firstUser = insertUser("first@example.com", 10);
        long secondUser = insertUser("second@example.com", 50);
        long firstQuestion = insertQuestion(
                firstUser, "Explain Java Arrays", "PROGRAMMING", true);
        insertQuestion(firstUser, "World Cup winner", "Sports/Entertainment", false);
        long hiddenFallback = insertQuestion(
                firstUser, "Temporary offline answer", "Science", false);
        Question fallback = database.questionDao().findById(hiddenFallback);
        fallback.answerSource = AnswerSource.LOCAL_FALLBACK.name();
        database.questionDao().update(fallback);
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

        studyRepository.toggleBookmark(firstQuestion);
        assertTrue(studyRepository.getBookmarked(firstUser).isEmpty());

        assertTrue(studyRepository.markReviewed(firstQuestion));
        assertFalse(studyRepository.markReviewed(firstQuestion));
        assertEquals(10 + Gamification.XP_REVIEW,
                database.userDao().findById(firstUser).xp);
        assertEquals(50, database.userDao().findById(secondUser).xp);
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
        Question question = new Question();
        question.userId = userId;
        question.questionText = text;
        question.answerText = "Saved answer for " + text;
        question.subject = subject;
        question.bookmarked = bookmarked;
        question.answerSource = AnswerSource.REMOTE.name();
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

        @Override
        public AiAnswer answer(String question, String educationLevel,
                               String explanationStyle, String subjectHint) {
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
