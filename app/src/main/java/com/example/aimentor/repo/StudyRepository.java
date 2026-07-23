package com.example.aimentor.repo;

import android.content.Context;

import com.example.aimentor.ai.AiAnswer;
import com.example.aimentor.ai.AiEngine;
import com.example.aimentor.ai.LocalAiEngine;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.data.AppDatabase;
import com.example.aimentor.data.Question;
import com.example.aimentor.data.QuestionDao;
import com.example.aimentor.data.QuizAttempt;
import com.example.aimentor.data.QuizAttemptDao;
import com.example.aimentor.data.User;
import com.example.aimentor.data.UserDao;
import com.example.aimentor.util.ContentModerator;
import com.example.aimentor.util.Gamification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Core study logic: asking questions (with a similar-question cache to save AI
 * cost), generating practice quizzes, recording results, awarding XP and
 * computing progress statistics. All data is local (offline first).
 */
public class StudyRepository {

    private final UserDao userDao;
    private final QuestionDao questionDao;
    private final QuizAttemptDao quizDao;
    private final AiEngine engine;

    public StudyRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.questionDao = db.questionDao();
        this.quizDao = db.quizAttemptDao();
        this.engine = new LocalAiEngine();
    }

    public AiEngine getEngine() {
        return engine;
    }

    // ----------------------------------------------------------------- Ask

    public static class AskResult {
        public final boolean success;
        public final String message;
        public final long questionId;
        public final boolean reused;
        public final boolean leveledUp;
        AskResult(boolean success, String message, long questionId, boolean reused, boolean leveledUp) {
            this.success = success;
            this.message = message;
            this.questionId = questionId;
            this.reused = reused;
            this.leveledUp = leveledUp;
        }
    }

    public AskResult ask(long userId, String questionText, String subjectHint) {
        ContentModerator.Result mod = ContentModerator.check(questionText);
        if (!mod.allowed) {
            return new AskResult(false, mod.reason, -1, false, false);
        }
        User user = userDao.findById(userId);
        if (user == null) {
            return new AskResult(false, "Please sign in again.", -1, false, false);
        }

        String normalized = normalize(questionText);
        Question cached = findSimilar(userId, normalized);

        Question q = new Question();
        q.userId = userId;
        q.questionText = questionText.trim();
        boolean reused = false;

        if (cached != null) {
            // Reuse the earlier answer instead of regenerating (AI cost saving).
            q.subject = cached.subject;
            q.difficulty = cached.difficulty;
            q.answerText = cached.answerText
                    + "\n\n(Answer reused from a very similar earlier question to save AI cost.)";
            q.reused = true;
            reused = true;
        } else {
            AiAnswer answer = engine.answer(questionText,
                    user.educationLevel, user.explanationStyle, subjectHint);
            q.subject = answer.getSubject();
            q.difficulty = answer.getDifficulty();
            q.answerText = answer.toDisplayString();
        }

        long id = questionDao.insert(q);
        boolean leveledUp = addXp(user, Gamification.XP_ASK);
        return new AskResult(true, reused ? "Reused a saved answer." : "Answer ready.",
                id, reused, leveledUp);
    }

    private Question findSimilar(long userId, String normalized) {
        if (normalized.isEmpty()) return null;
        List<Question> recent = questionDao.getRecent(userId, 100);
        for (Question prev : recent) {
            if (normalize(prev.questionText).equals(normalized)) {
                return prev;
            }
        }
        return null;
    }

    private String normalize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        String lower = text.toLowerCase(Locale.ROOT);
        boolean lastSpace = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                lastSpace = false;
            } else if (!lastSpace) {
                sb.append(' ');
                lastSpace = true;
            }
        }
        return sb.toString().trim();
    }

    // --------------------------------------------------------------- Library

    public List<Question> getHistory(long userId) {
        return questionDao.getForUser(userId);
    }

    public List<Question> search(long userId, String query) {
        if (query == null || query.trim().isEmpty()) return getHistory(userId);
        return questionDao.search(userId, query.trim());
    }

    public List<Question> getBookmarked(long userId) {
        return questionDao.getBookmarked(userId);
    }

    public List<Question> getBySubject(long userId, String subject) {
        return questionDao.getBySubject(userId, subject);
    }

    public Question getQuestion(long id) {
        return questionDao.findById(id);
    }

    public void toggleBookmark(long questionId) {
        Question q = questionDao.findById(questionId);
        if (q != null) {
            q.bookmarked = !q.bookmarked;
            questionDao.update(q);
        }
    }

    /** Marks a saved answer as reviewed, awarding review XP the first time only. */
    public boolean markReviewed(long questionId) {
        Question q = questionDao.findById(questionId);
        if (q == null || q.reviewed) return false;
        q.reviewed = true;
        questionDao.update(q);
        User user = userDao.findById(q.userId);
        if (user != null) addXp(user, Gamification.XP_REVIEW);
        return true;
    }

    // ------------------------------------------------------------------ Quiz

    public List<QuizQuestion> generateQuiz(String subject, int count) {
        return engine.generateQuiz(subject, count);
    }

    public static class QuizResult {
        public final int awardedXp;
        public final boolean leveledUp;
        public final int newLevel;
        QuizResult(int awardedXp, boolean leveledUp, int newLevel) {
            this.awardedXp = awardedXp;
            this.leveledUp = leveledUp;
            this.newLevel = newLevel;
        }
    }

    public QuizResult recordQuiz(long userId, String subject, int correct, int total) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.userId = userId;
        attempt.subject = subject;
        attempt.correct = correct;
        attempt.total = total;
        quizDao.insert(attempt);

        User user = userDao.findById(userId);
        int awarded = correct * Gamification.XP_QUIZ_CORRECT;
        boolean leveledUp = false;
        if (user != null) leveledUp = addXp(user, awarded);
        int newLevel = user != null ? Gamification.levelForXp(user.xp) : 1;
        return new QuizResult(awarded, leveledUp, newLevel);
    }

    // -------------------------------------------------------------- Progress

    private boolean addXp(User user, int amount) {
        int before = Gamification.levelForXp(user.xp);
        user.xp += amount;
        userDao.update(user);
        int after = Gamification.levelForXp(user.xp);
        return after > before;
    }

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
        public String topSubject = "-";
        public final Map<String, Integer> subjectCounts = new LinkedHashMap<>();
        public final List<String> badges = new ArrayList<>();
        public final List<String> insights = new ArrayList<>();
    }

    public Progress getProgress(long userId) {
        Progress p = new Progress();
        User user = userDao.findById(userId);
        int xp = user != null ? user.xp : 0;
        p.xp = xp;
        p.level = Gamification.levelForXp(xp);
        p.levelTitle = Gamification.titleForLevel(p.level);
        p.xpIntoLevel = Gamification.xpIntoLevel(xp);
        p.xpToNext = Gamification.xpToNextLevel(xp);

        List<Question> all = questionDao.getForUser(userId);
        p.totalQuestions = all.size();
        int topCount = 0;
        for (Question q : all) {
            if (q.bookmarked) p.bookmarkedCount++;
            String s = q.subject == null ? "General" : q.subject;
            int c = (p.subjectCounts.containsKey(s) ? p.subjectCounts.get(s) : 0) + 1;
            p.subjectCounts.put(s, c);
            if (c > topCount) {
                topCount = c;
                p.topSubject = s;
            }
        }

        p.quizzesCompleted = quizDao.countForUser(userId);
        p.totalCorrect = quizDao.totalCorrect(userId);
        p.totalAnswered = quizDao.totalAnswered(userId);
        p.accuracyPercent = p.totalAnswered > 0
                ? Math.round((p.totalCorrect * 100f) / p.totalAnswered) : 0;

        p.badges.addAll(Gamification.earnedBadges(
                p.totalQuestions, p.quizzesCompleted, p.totalCorrect, p.level));

        buildInsights(p);
        return p;
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
    }
}
