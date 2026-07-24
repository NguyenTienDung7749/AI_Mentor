package com.example.aimentor.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.aimentor.R;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
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

    private QuizViewModel quizViewModel;
    private SessionManager session;
    private ArrayList<QuizQuestion> questions = new ArrayList<>();
    private ArrayList<QuizQuestion> wrongQuestions = new ArrayList<>();
    private String subject;
    private String difficulty;
    private String requestedSubject;
    private String requestedDifficulty;

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

    private TextView tvProgress, tvQuestionPrompt, tvFeedback, tvQuizType, tvQuizError;
    private RadioGroup rgOptions;
    private RadioButton[] options;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        btnAction.setOnClickListener(v -> onAction());
        btnRetryQuiz.setOnClickListener(v -> requestQuiz());

        requestedSubject = getIntent().getStringExtra(EXTRA_SUBJECT);
        requestedDifficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);
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
                requestedDifficulty, 5);
    }

    private void showLoadingState() {
        quizLoadingState.setVisibility(View.VISIBLE);
        quizErrorState.setVisibility(View.GONE);
        quizContent.setVisibility(View.GONE);
        btnAction.setEnabled(false);
        btnRetryQuiz.setEnabled(false);
    }

    private void bindViews() {
        tvProgress = findViewById(R.id.tvProgress);
        tvQuestionPrompt = findViewById(R.id.tvQuestionPrompt);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvQuizType = findViewById(R.id.tvQuizType);
        tvQuizError = findViewById(R.id.tvQuizError);
        quizProgressIndicator = findViewById(R.id.quizProgressIndicator);
        quizLoadingState = findViewById(R.id.quizLoadingState);
        quizErrorState = findViewById(R.id.quizErrorState);
        quizContent = findViewById(R.id.quizContent);
        rgOptions = findViewById(R.id.rgOptions);
        textAnswerLayout = findViewById(R.id.textAnswerLayout);
        etTextAnswer = findViewById(R.id.etTextAnswer);
        options = new RadioButton[]{
                findViewById(R.id.rbOpt0), findViewById(R.id.rbOpt1),
                findViewById(R.id.rbOpt2), findViewById(R.id.rbOpt3)};
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
        btnAction.setEnabled(true);
        showQuestion();
    }

    private void showQuestion() {
        if (questions.isEmpty() || index < 0 || index >= questions.size()) {
            finish();
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
        rgOptions.clearCheck();
        boolean textAnswer = question.requiresTextAnswer();
        rgOptions.setVisibility(textAnswer ? View.GONE : View.VISIBLE);
        textAnswerLayout.setVisibility(textAnswer ? View.VISIBLE : View.GONE);
        etTextAnswer.setEnabled(true);
        etTextAnswer.setText("");

        List<String> questionOptions = question.getOptions();
        for (int i = 0; i < options.length; i++) {
            if (i < questionOptions.size()) {
                options[i].setVisibility(View.VISIBLE);
                options[i].setText(questionOptions.get(i));
                options[i].setEnabled(true);
            } else {
                options[i].setVisibility(View.GONE);
            }
        }
        tvFeedback.setVisibility(View.GONE);
        btnAction.setText(R.string.check_answer);

        if (restoreAnswered) {
            if (textAnswer) {
                etTextAnswer.setText(restoredTextAnswer);
                renderAnsweredState(question, -1, restoredTextAnswer, restoredCorrect);
            } else if (restoredSelection >= 0
                    && restoredSelection < questionOptions.size()) {
                options[restoredSelection].setChecked(true);
                renderAnsweredState(question, restoredSelection, "", restoredCorrect);
            }
        } else if (textAnswer && !restoredTextAnswer.isEmpty()) {
            etTextAnswer.setText(restoredTextAnswer);
            etTextAnswer.setSelection(restoredTextAnswer.length());
        } else if (restoredSelection >= 0
                && restoredSelection < questionOptions.size()) {
            options[restoredSelection].setChecked(true);
        }
    }

    private int labelForType(QuizQuestion.Type type) {
        if (type == QuizQuestion.Type.TRUE_FALSE) return R.string.quiz_true_false;
        if (type == QuizQuestion.Type.SHORT_ANSWER) return R.string.quiz_short_answer;
        if (type == QuizQuestion.Type.FILL_IN_THE_BLANK) return R.string.quiz_fill_blank;
        return R.string.quiz_multiple_choice;
    }

    private int selectedIndex() {
        int checkedId = rgOptions.getCheckedRadioButtonId();
        for (int i = 0; i < options.length; i++) {
            if (options[i].getId() == checkedId) return i;
        }
        return -1;
    }

    private void onAction() {
        if (questions.isEmpty()) return;
        if (!answered) {
            QuizQuestion question = questions.get(index);
            int selected = question.requiresTextAnswer() ? -1 : selectedIndex();
            String textAnswer = question.requiresTextAnswer() && etTextAnswer.getText() != null
                    ? etTextAnswer.getText().toString().trim() : "";
            if ((!question.requiresTextAnswer() && selected < 0)
                    || (question.requiresTextAnswer() && textAnswer.isEmpty())) {
                Toast.makeText(this, R.string.quiz_choose_answer,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            boolean correct = question.requiresTextAnswer()
                    ? question.isCorrect(textAnswer) : question.isCorrect(selected);
            if (correct) {
                correctCount++;
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
        for (RadioButton option : options) option.setEnabled(false);
        etTextAnswer.setEnabled(false);

        tvFeedback.setVisibility(View.VISIBLE);
        if (correct) {
            tvFeedback.setText(getString(
                    R.string.quiz_correct_feedback, question.getExplanation()));
        } else {
            tvFeedback.setText(getString(R.string.quiz_incorrect_feedback_answer,
                    question.getDisplayAnswer(), question.getExplanation()));
        }
        int feedbackColor = correct
                ? ContextCompat.getColor(this, R.color.success)
                : MaterialColors.getColor(tvFeedback,
                        com.google.android.material.R.attr.colorError);
        tvFeedback.setTextColor(feedbackColor);
        btnAction.setText(index == questions.size() - 1
                ? R.string.finish_quiz : R.string.next_question);
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
                + "\n" + getString(R.string.quiz_xp_earned, awardedXp);
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
        showQuestion();
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
        int savedSelection = answered ? lastSelectedIndex : selectedIndex();
        String savedText = lastTextAnswer;
        if (!answered && etTextAnswer.getText() != null) {
            savedText = etTextAnswer.getText().toString();
        }
        outState.putInt(STATE_SELECTED, savedSelection);
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
}
