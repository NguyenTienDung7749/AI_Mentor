package com.example.aimentor.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.aimentor.R;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.ProgressMetrics;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Runs a personalized mixed-format quiz and records the first-attempt result once. */
public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_COUNT = "question_count";

    private static final String STATE_QUESTIONS = "questions";
    private static final String STATE_WRONG = "wrong_questions";
    private static final String STATE_SUBJECT = "resolved_subject";
    private static final String STATE_DIFFICULTY = "resolved_difficulty";
    private static final String STATE_INDEX = "index";
    private static final String STATE_CORRECT = "correct";
    private static final String STATE_ANSWERED = "answered";
    private static final String STATE_RETRY = "retry_round";
    private static final String STATE_RECORDED = "result_recorded";
    private static final String STATE_AWARDED_XP = "awarded_xp";
    private static final String STATE_SELECTED = "selected_index";
    private static final String STATE_TEXT_ANSWER = "text_answer";
    private static final String STATE_LAST_CORRECT = "last_correct";

    /** Kahoot-style option background drawables (red, blue, yellow, green). */
    private static final int[] OPTION_BACKGROUNDS = {
            R.drawable.bg_quiz_option_red,
            R.drawable.bg_quiz_option_blue,
            R.drawable.bg_quiz_option_yellow,
            R.drawable.bg_quiz_option_green
    };

    private QuizViewModel quizViewModel;
    private SessionManager session;
    private ArrayList<QuizQuestion> questions = new ArrayList<>();
    private ArrayList<QuizQuestion> wrongQuestions = new ArrayList<>();
    private String subject;
    private String difficulty;
    private String requestedSubject;
    private String requestedDifficulty;
    private int requestedCount = 5;

    private int index;
    private int correctCount;
    private int awardedXp;
    private int lastSelectedIndex = -1;
    private String lastTextAnswer = "";
    private boolean answered;
    private boolean retryRound;
    private boolean resultRecorded;
    private boolean lastAnswerCorrect;
    private boolean gameInitialized;

    private TextView tvProgress, tvQuestionPrompt, tvFeedback, tvQuizType,
            tvQuizError, tvScore, tvTimer;
    private CountDownTimer countDownTimer;
    private TextView[] optionCards;
    private TextInputLayout textAnswerLayout;
    private TextInputEditText etTextAnswer;
    private MaterialButton btnAction, btnRetryQuiz;
    private View quizLoadingState, quizContent, quizErrorState;
    private LinearProgressIndicator quizProgressIndicator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        WindowUiHelper.apply(this);

        session = new SessionManager(this);
        quizViewModel = new ViewModelProvider(this)
                .get(QuizViewModel.class);
        bindViews();

        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());
        btnAction.setOnClickListener(v -> onAction());
        btnRetryQuiz.setOnClickListener(v -> requestQuiz());

        requestedSubject = getIntent().getStringExtra(EXTRA_SUBJECT);
        requestedDifficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        requestedCount = Math.max(1, Math.min(
                getIntent().getIntExtra(EXTRA_COUNT, 5), 20));
        gameInitialized =
                savedInstanceState != null && restoreState(savedInstanceState);
        quizViewModel.getLoadState().observe(this, this::renderLoadState);
        quizViewModel.getRecordState().observe(this, this::renderRecordState);
        if (gameInitialized) {
            showLoadedQuiz();
        } else {
            requestQuiz();
        }
    }

    private void requestQuiz() {
        if (gameInitialized) return;
        quizViewModel.loadQuiz(
                session.getCurrentUserId(), requestedSubject,
                requestedDifficulty, requestedCount);
    }

    private void showLoadingState() {
        quizLoadingState.setVisibility(View.VISIBLE);
        quizErrorState.setVisibility(View.GONE);
        quizContent.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);
        btnAction.setEnabled(false);
        btnRetryQuiz.setEnabled(false);
        tvProgress.setText("");
    }

    private void bindViews() {
        tvProgress = findViewById(R.id.tvProgress);
        tvQuestionPrompt = findViewById(R.id.tvQuestionPrompt);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvQuizType = findViewById(R.id.tvQuizType);
        tvQuizError = findViewById(R.id.tvQuizError);
        tvScore = findViewById(R.id.tvScore);
        tvTimer = findViewById(R.id.tvTimer);
        quizProgressIndicator = findViewById(R.id.quizProgressIndicator);
        quizLoadingState = findViewById(R.id.quizLoadingState);
        quizErrorState = findViewById(R.id.quizErrorState);
        quizContent = findViewById(R.id.quizContent);
        textAnswerLayout = findViewById(R.id.textAnswerLayout);
        etTextAnswer = findViewById(R.id.etTextAnswer);

        optionCards = new TextView[]{
                findViewById(R.id.optionA), findViewById(R.id.optionB),
                findViewById(R.id.optionC), findViewById(R.id.optionD)};

        for (int i = 0; i < optionCards.length; i++) {
            final int optIndex = i;
            optionCards[i].setOnClickListener(v -> onOptionSelected(optIndex));
        }

        btnAction = findViewById(R.id.btnAction);
        btnRetryQuiz = findViewById(R.id.btnRetryQuiz);
        tvFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        tvQuizError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
    }

    private void renderLoadState(QuizViewModel.LoadUiState state) {
        if (state == null || gameInitialized) return;
        if (state.status == QuizViewModel.LoadStatus.LOADING) {
            showLoadingState();
            return;
        }
        if (state.status != QuizViewModel.LoadStatus.RESULT
                || state.result == null) {
            return;
        }
        StudyRepository.QuizLoadResult result = state.result;
        if (!result.success || result.questions.isEmpty()) {
            String message = result.message == null || result.message.trim().isEmpty()
                    ? getString(R.string.quiz_error_default) : result.message;
            quizLoadingState.setVisibility(View.GONE);
            quizContent.setVisibility(View.GONE);
            quizErrorState.setVisibility(View.VISIBLE);
            btnRetryQuiz.setEnabled(true);
            tvQuizError.setText(message);
            tvQuizError.announceForAccessibility(message);
            return;
        }
        subject = result.subject;
        difficulty = result.difficulty;
        questions = new ArrayList<>(result.questions);
        gameInitialized = true;
        showLoadedQuiz();
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
    }

    private void showLoadedQuiz() {
        gameInitialized = true;
        quizLoadingState.setVisibility(View.GONE);
        quizErrorState.setVisibility(View.GONE);
        quizContent.setVisibility(View.VISIBLE);
        btnAction.setVisibility(View.VISIBLE);
        btnAction.setEnabled(true);
        tvScore.setVisibility(View.VISIBLE);
        updateScoreDisplay(false);
        showQuestion();
    }

    private void showQuestion() {
        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
        if (questions.isEmpty() || index < 0 || index >= questions.size()) {
            finish(); // Safety net — should not happen during normal flow
            return;
        }
        QuizQuestion question = questions.get(index);
        boolean restoreAnswered = answered;
        int restoredSelection = lastSelectedIndex;
        String restoredTextAnswer = lastTextAnswer;
        boolean restoredCorrect = lastAnswerCorrect;

        answered = false;
        lastSelectedIndex = -1;
        lastTextAnswer = "";
        tvProgress.setText(getString(retryRound
                        ? R.string.quiz_retry_progress : R.string.quiz_question_progress,
                index + 1, questions.size(), subject, difficulty));
        quizProgressIndicator.setMax(questions.size());
        quizProgressIndicator.setProgressCompat(index + 1, true);
        tvQuizType.setText(labelForType(question.getType()));
        tvQuestionPrompt.setText(question.getPrompt());

        boolean textAnswer = question.requiresTextAnswer();
        View optionsGrid = findViewById(R.id.optionsGrid);
        optionsGrid.setVisibility(textAnswer ? View.GONE : View.VISIBLE);
        textAnswerLayout.setVisibility(textAnswer ? View.VISIBLE : View.GONE);
        etTextAnswer.setEnabled(true);
        etTextAnswer.setText("");

        List<String> questionOptions = question.getOptions();
        for (int i = 0; i < optionCards.length; i++) {
            optionCards[i].setBackgroundResource(OPTION_BACKGROUNDS[i]);
            optionCards[i].setAlpha(1f);
            optionCards[i].setScaleX(1f);
            optionCards[i].setScaleY(1f);
            optionCards[i].setEnabled(true);

            if (i < questionOptions.size()) {
                optionCards[i].setVisibility(View.VISIBLE);
                optionCards[i].setText(questionOptions.get(i));
            } else {
                optionCards[i].setVisibility(View.GONE);
            }
        }
        View row2 = findViewById(R.id.optionsRow2);
        if (row2 != null) {
            row2.setVisibility(questionOptions.size() > 2 ? View.VISIBLE : View.GONE);
        }
        tvFeedback.setVisibility(View.GONE);
        tvFeedback.setBackgroundResource(R.drawable.bg_info_panel);
        btnAction.setText(R.string.check_answer);

        // Animate options appearing with stagger effect
        if (!restoreAnswered) {
            startTimer(question);
            for (int i = 0; i < optionCards.length; i++) {
                if (optionCards[i].getVisibility() == View.VISIBLE) {
                    optionCards[i].setTranslationY(100f);
                    optionCards[i].setAlpha(0f);
                    optionCards[i].animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setStartDelay(i * 80L)
                            .setDuration(300)
                            .setInterpolator(new OvershootInterpolator(0.8f))
                            .start();
                }
            }
        }

        if (restoreAnswered) {
            if (textAnswer) {
                etTextAnswer.setText(restoredTextAnswer);
                renderAnsweredState(question, -1, restoredTextAnswer, restoredCorrect);
            } else if (restoredSelection >= 0
                    && restoredSelection < questionOptions.size()) {
                renderAnsweredState(question, restoredSelection, "", restoredCorrect);
            }
        } else if (textAnswer && !restoredTextAnswer.isEmpty()) {
            etTextAnswer.setText(restoredTextAnswer);
            etTextAnswer.setSelection(restoredTextAnswer.length());
        }
    }

    /** Called when a colored option card is tapped. */
    private void onOptionSelected(int selectedIndex) {
        if (answered) return;
        if (countDownTimer != null) countDownTimer.cancel();
        // Visual selection: scale up the selected card, dim others
        for (int i = 0; i < optionCards.length; i++) {
            if (optionCards[i].getVisibility() != View.VISIBLE) continue;
            if (i == selectedIndex) {
                optionCards[i].animate().scaleX(1.05f).scaleY(1.05f)
                        .setDuration(150).start();
                optionCards[i].setAlpha(1f);
            } else {
                optionCards[i].animate().scaleX(1f).scaleY(1f)
                        .setDuration(150).start();
                optionCards[i].setAlpha(0.6f);
            }
        }
        lastSelectedIndex = selectedIndex;
    }

    private int labelForType(QuizQuestion.Type type) {
        if (type == QuizQuestion.Type.TRUE_FALSE) return R.string.quiz_true_false;
        if (type == QuizQuestion.Type.SHORT_ANSWER) return R.string.quiz_short_answer;
        if (type == QuizQuestion.Type.FILL_IN_THE_BLANK) return R.string.quiz_fill_blank;
        return R.string.quiz_multiple_choice;
    }

    private void onAction() {
        if (questions.isEmpty()) return;
        if (!answered) {
            QuizQuestion question = questions.get(index);
            int selected = question.requiresTextAnswer() ? -1 : lastSelectedIndex;
            String textAnswer = question.requiresTextAnswer() && etTextAnswer.getText() != null
                    ? etTextAnswer.getText().toString().trim() : "";
            if ((!question.requiresTextAnswer() && selected < 0)
                    || (question.requiresTextAnswer() && textAnswer.isEmpty())) {
                Toast.makeText(this, R.string.quiz_choose_answer,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (countDownTimer != null) countDownTimer.cancel();
            boolean correct = question.requiresTextAnswer()
                    ? question.isCorrect(textAnswer) : question.isCorrect(selected);
            if (correct) {
                correctCount++;
                updateScoreDisplay(true);
            } else if (!retryRound && !wrongQuestions.contains(question)) {
                wrongQuestions.add(question);
            }
            renderAnsweredState(question, selected, textAnswer, correct);
            return;
        }

        if (index < questions.size() - 1) {
            index++;
            answered = false;
            lastSelectedIndex = -1;
            lastTextAnswer = "";
            showQuestion();
        } else {
            finishQuiz();
        }
    }

    private void renderAnsweredState(QuizQuestion question, int selected,
                                     String textAnswer, boolean correct) {
        answered = true;
        lastSelectedIndex = selected;
        lastTextAnswer = textAnswer == null ? "" : textAnswer;
        lastAnswerCorrect = correct;
        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
        tvTimer.setVisibility(View.GONE);
        for (TextView card : optionCards) card.setEnabled(false);
        etTextAnswer.setEnabled(false);

        tvFeedback.setVisibility(View.VISIBLE);
        if (correct) {
            tvFeedback.setText(getString(
                    R.string.quiz_correct_feedback, question.getExplanation()));
            tvFeedback.setBackgroundResource(R.drawable.bg_quiz_feedback_correct);
            tvFeedback.setTextColor(0xFFFFFFFF);
        } else {
            tvFeedback.setText(getString(R.string.quiz_incorrect_feedback_answer,
                    question.getDisplayAnswer(), question.getExplanation()));
            tvFeedback.setBackgroundResource(R.drawable.bg_quiz_feedback_incorrect);
            tvFeedback.setTextColor(0xFFFFFFFF);
        }

        if (!question.requiresTextAnswer()) {
            // Animate correct/incorrect options
            int correctIndex = question.getCorrectIndex();
            for (int i = 0; i < optionCards.length; i++) {
                if (optionCards[i].getVisibility() != View.VISIBLE) continue;
                if (i == correctIndex) {
                    // Correct answer: pulse glow
                    animateCorrectOption(optionCards[i]);
                } else if (i == selected && !correct) {
                    // Wrong answer selected: shake
                    animateWrongOption(optionCards[i]);
                } else {
                    // Dim unselected wrong options
                    optionCards[i].animate().alpha(0.3f).setDuration(300).start();
                }
            }
        }

        // Fade in feedback
        tvFeedback.setAlpha(0f);
        tvFeedback.animate().alpha(1f).setDuration(400).start();

        btnAction.setText(index == questions.size() - 1
                ? R.string.finish_quiz : R.string.next_question);
    }

    /** Pulse animation for the correct answer option. */
    private void animateCorrectOption(View view) {
        view.setAlpha(1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f);
        scaleX.setDuration(500);
        scaleY.setDuration(500);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    /** Shake animation for wrong answer option. */
    private void animateWrongOption(View view) {
        view.setAlpha(0.7f);
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f);
        shake.setDuration(400);
        shake.start();
    }

    /** Updates the score badge display. */
    private void updateScoreDisplay(boolean animate) {
        tvScore.setText(String.valueOf(correctCount));
        if (animate) {
            tvScore.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                    .withEndAction(() -> tvScore.animate()
                            .scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
        }
    }

    private void finishQuiz() {
        if (retryRound) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.quiz_review_complete)
                    .setMessage(getResources().getQuantityString(
                            R.plurals.quiz_reviewed_mistakes,
                            questions.size(), questions.size()))
                    .setCancelable(false)
                    .setPositiveButton(R.string.done, (dialog, which) -> finish())
                    .show();
            return;
        }
        if (!resultRecorded) {
            quizViewModel.recordQuiz(
                    session.getCurrentUserId(), subject,
                    correctCount, questions.size());
            return;
        }
        showCompletionDialog();
    }

    private void renderRecordState(QuizViewModel.RecordUiState state) {
        if (state == null || resultRecorded) return;
        if (state.status == QuizViewModel.RecordStatus.LOADING) {
            btnAction.setEnabled(false);
            return;
        }
        if (state.status != QuizViewModel.RecordStatus.RESULT
                || state.result == null) {
            return;
        }
        StudyRepository.QuizResult result = state.result;
        quizViewModel.consumeRecordResult();
        if (!result.recorded) {
            btnAction.setEnabled(true);
            Toast.makeText(this, R.string.quiz_save_failed,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        resultRecorded = true;
        awardedXp = result.awardedXp;
        btnAction.setEnabled(true);
        if (result.leveledUp) {
            NotificationHelper.notify(this,
                    getString(R.string.level_up_notification_title),
                    getString(R.string.level_up_notification_body, result.newLevel));
        }
        showCompletionDialog();
    }

    private void showCompletionDialog() {
        String message = getString(R.string.quiz_score,
                correctCount, questions.size())
                + "\n" + getString(R.string.quiz_xp_earned, awardedXp)
                + "\n" + getString(R.string.quiz_mastery_signal,
                ProgressMetrics.masteryLabel(
                        correctCount, questions.size(), 0));
        if (!wrongQuestions.isEmpty()) {
            message += "\n" + getResources().getQuantityString(
                    R.plurals.quiz_questions_to_retry,
                    wrongQuestions.size(), wrongQuestions.size());
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quiz_complete)
                .setMessage(message)
                .setCancelable(false);
        if (wrongQuestions.isEmpty()) {
            dialog.setPositiveButton(R.string.done, (d, w) -> finish());
        } else {
            dialog.setPositiveButton(R.string.retry_mistakes, (d, w) -> startRetryRound())
                    .setNegativeButton(R.string.done, (d, w) -> finish());
        }
        dialog.show();
    }

    private void startRetryRound() {
        questions = new ArrayList<>(wrongQuestions);
        wrongQuestions.clear();
        retryRound = true;
        index = 0;
        correctCount = 0;
        answered = false;
        lastSelectedIndex = -1;
        lastTextAnswer = "";
        updateScoreDisplay(false);
        showQuestion();
    }

    private void startTimer(QuizQuestion question) {
        if (countDownTimer != null) countDownTimer.cancel();
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setBackgroundResource(R.drawable.bg_timer_circle);
        tvTimer.setTextColor(0xFFFFFFFF);
        // Entrance animation
        tvTimer.setScaleX(0f);
        tvTimer.setScaleY(0f);
        tvTimer.animate().scaleX(1f).scaleY(1f).setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();

        long durationMs;
        if (question.requiresTextAnswer()) {
            durationMs = 60_000L; // 60s for text input
        } else if (question.getType() == QuizQuestion.Type.TRUE_FALSE) {
            durationMs = 20_000L; // 20s for true/false
        } else {
            durationMs = 30_000L; // 30s for 4-option multiple choice
        }
        tvTimer.setText(String.valueOf(durationMs / 1000));
        final boolean[] urgentMode = {false};
        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isFinishing()) { cancel(); return; }
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText(String.valueOf(seconds));
                if (seconds <= 5 && !urgentMode[0]) {
                    urgentMode[0] = true;
                    tvTimer.setBackgroundResource(R.drawable.bg_timer_circle_urgent);
                    tvTimer.setTextColor(0xFFE21B3C);
                }
                if (urgentMode[0]) {
                    // Pulse animation each second
                    tvTimer.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150)
                            .withEndAction(() -> tvTimer.animate()
                                    .scaleX(1f).scaleY(1f).setDuration(150).start())
                            .start();
                }
            }

            @Override
            public void onFinish() {
                if (isFinishing()) return;
                tvTimer.setText("0");
                onTimeUp();
            }
        }.start();
    }

    private void onTimeUp() {
        if (answered || isFinishing() || questions.isEmpty()
                || index < 0 || index >= questions.size()) return;
        Toast.makeText(this, "Time's up!", Toast.LENGTH_SHORT).show();
        QuizQuestion question = questions.get(index);
        if (!retryRound && !wrongQuestions.contains(question)) {
            wrongQuestions.add(question);
        }
        renderAnsweredState(question, -1, "", false);
        // Auto-advance to next question after 2 seconds
        tvFeedback.postDelayed(() -> {
            if (isFinishing()) return;
            if (index < questions.size() - 1) {
                index++;
                answered = false;
                lastSelectedIndex = -1;
                lastTextAnswer = "";
                showQuestion();
            } else {
                finishQuiz();
            }
        }, 2000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_QUESTIONS, questions);
        outState.putSerializable(STATE_WRONG, wrongQuestions);
        outState.putString(STATE_SUBJECT, subject);
        outState.putString(STATE_DIFFICULTY, difficulty);
        outState.putInt(STATE_INDEX, index);
        outState.putInt(STATE_CORRECT, correctCount);
        outState.putBoolean(STATE_ANSWERED, answered);
        outState.putBoolean(STATE_RETRY, retryRound);
        outState.putBoolean(STATE_RECORDED, resultRecorded);
        outState.putInt(STATE_AWARDED_XP, awardedXp);
        outState.putInt(STATE_SELECTED, lastSelectedIndex);
        String savedText = lastTextAnswer;
        if (!answered && etTextAnswer.getText() != null) {
            savedText = etTextAnswer.getText().toString();
        }
        outState.putString(STATE_TEXT_ANSWER, savedText);
        outState.putBoolean(STATE_LAST_CORRECT, lastAnswerCorrect);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private boolean restoreState(Bundle state) {
        Serializable savedQuestions = state.getSerializable(STATE_QUESTIONS);
        if (!(savedQuestions instanceof ArrayList)
                || ((ArrayList<?>) savedQuestions).isEmpty()) {
            return false;
        }
        questions = (ArrayList<QuizQuestion>) savedQuestions;
        Serializable savedWrong = state.getSerializable(STATE_WRONG);
        if (savedWrong instanceof ArrayList) {
            wrongQuestions = (ArrayList<QuizQuestion>) savedWrong;
        }
        subject = state.getString(STATE_SUBJECT, "General");
        difficulty = state.getString(STATE_DIFFICULTY, "Intermediate");
        index = Math.min(state.getInt(STATE_INDEX, 0), questions.size() - 1);
        correctCount = state.getInt(STATE_CORRECT, 0);
        answered = state.getBoolean(STATE_ANSWERED, false);
        retryRound = state.getBoolean(STATE_RETRY, false);
        resultRecorded = state.getBoolean(STATE_RECORDED, false);
        awardedXp = state.getInt(STATE_AWARDED_XP, 0);
        lastSelectedIndex = state.getInt(STATE_SELECTED, -1);
        lastTextAnswer = state.getString(STATE_TEXT_ANSWER, "");
        lastAnswerCorrect = state.getBoolean(STATE_LAST_CORRECT, false);
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
