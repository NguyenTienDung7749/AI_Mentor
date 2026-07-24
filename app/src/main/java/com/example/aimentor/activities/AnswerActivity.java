package com.example.aimentor.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aimentor.R;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.data.Question;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.AppearanceManager;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

/** Displays a single question and its saved AI answer (works offline). */
public class AnswerActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "question_id";
    private static final String EXTRA_TRANSIENT = "transient_answer";
    private static final String EXTRA_QUESTION_TEXT = "question_text";
    private static final String EXTRA_SUBJECT = "answer_subject";
    private static final String EXTRA_DIFFICULTY = "answer_difficulty";
    private static final String EXTRA_ANSWER_TEXT = "answer_text";
    private static final String EXTRA_SOURCE = "answer_source";
    private static final String EXTRA_MODEL = "answer_model";
    private static final String EXTRA_RESPONSE_TIME = "answer_response_time";

    private StudyRepository studyRepository;
    private SessionManager session;
    private Question question;
    private MaterialButton btnBookmark, btnReviewed;
    private TextView tvSubject, tvAnswerSource, tvQuestion, tvAnswer;
    private View answerContent, answerActions;
    private ProgressBar progressAnswer;
    private boolean transientAnswer;
    private int loadGeneration;
    private int actionGeneration;
    private long visibleSinceElapsed;

    public static Intent savedAnswerIntent(Context context, long questionId) {
        Intent intent = new Intent(context, AnswerActivity.class);
        intent.putExtra(EXTRA_QUESTION_ID, questionId);
        return intent;
    }

    public static Intent transientAnswerIntent(Context context, Question question) {
        Intent intent = new Intent(context, AnswerActivity.class);
        intent.putExtra(EXTRA_TRANSIENT, true);
        intent.putExtra(EXTRA_QUESTION_TEXT, question.questionText);
        intent.putExtra(EXTRA_SUBJECT, question.subject);
        intent.putExtra(EXTRA_DIFFICULTY, question.difficulty);
        intent.putExtra(EXTRA_ANSWER_TEXT, question.answerText);
        intent.putExtra(EXTRA_SOURCE, question.answerSource);
        intent.putExtra(EXTRA_MODEL, question.modelName);
        intent.putExtra(EXTRA_RESPONSE_TIME, question.responseTimeMs);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppearanceManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);
        WindowUiHelper.apply(this);

        studyRepository = new StudyRepository(this);
        session = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        bindViews();

        transientAnswer = getIntent().getBooleanExtra(EXTRA_TRANSIENT, false);
        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnReviewed.setOnClickListener(v -> markReviewed());
        if (transientAnswer) {
            question = readTransientQuestion();
            if (question == null) {
                showNotFoundAndFinish();
                return;
            }
            renderQuestion();
        } else {
            loadSavedQuestion(null);
        }
    }

    private void bindViews() {
        tvSubject = findViewById(R.id.tvSubject);
        tvAnswerSource = findViewById(R.id.tvAnswerSource);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvAnswer = findViewById(R.id.tvAnswer);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnReviewed = findViewById(R.id.btnReviewed);
        progressAnswer = findViewById(R.id.progressAnswer);
        answerContent = findViewById(R.id.answerContent);
        answerActions = findViewById(R.id.answerActions);
    }

    private void renderQuestion() {
        progressAnswer.setVisibility(View.GONE);
        answerContent.setVisibility(View.VISIBLE);
        tvSubject.setText(getString(R.string.answer_subject_difficulty,
                question.subject, question.difficulty));
        tvAnswerSource.setText(buildSourceLabel(question));
        tvQuestion.setText(question.questionText);
        tvAnswer.setText(formatAnswerText(question.answerText));

        if (transientAnswer) {
            answerActions.setVisibility(View.GONE);
        } else {
            answerActions.setVisibility(View.VISIBLE);
            refreshBookmarkButton();
            refreshReviewedButton();
        }
    }

    private void loadSavedQuestion(@Nullable Runnable afterLoad) {
        int generation = ++loadGeneration;
        if (question == null) {
            progressAnswer.setVisibility(View.VISIBLE);
            answerContent.setVisibility(View.GONE);
            answerActions.setVisibility(View.GONE);
        }
        studyRepository.getQuestionAsync(
                session.getCurrentUserId(),
                getIntent().getLongExtra(EXTRA_QUESTION_ID, -1),
                loadedQuestion -> {
                    if (!canRenderLoad(generation)) return;
                    if (loadedQuestion == null) {
                        showNotFoundAndFinish();
                        return;
                    }
                    question = loadedQuestion;
                    renderQuestion();
                    if (afterLoad != null) afterLoad.run();
                });
    }

    @Override
    protected void onDestroy() {
        loadGeneration++;
        actionGeneration++;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        visibleSinceElapsed = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onStop() {
        if (!transientAnswer && question != null && question.id > 0
                && visibleSinceElapsed > 0L) {
            long duration = SystemClock.elapsedRealtime() - visibleSinceElapsed;
            studyRepository.recordReviewDurationAsync(
                    session.getCurrentUserId(), question.id, duration);
        }
        visibleSinceElapsed = 0L;
        super.onStop();
    }

    private boolean canRenderLoad(int generation) {
        return generation == loadGeneration && !isFinishing() && !isDestroyed();
    }

    private void toggleBookmark() {
        if (question == null) return;
        int generation = ++actionGeneration;
        setActionButtonsEnabled(false);
        studyRepository.toggleBookmarkAsync(
                session.getCurrentUserId(), question.id, changed -> {
                    if (!canHandleAction(generation)) return;
                    loadSavedQuestion(() -> Toast.makeText(this,
                            question.bookmarked
                                    ? R.string.bookmark_added
                                    : R.string.bookmark_removed,
                            Toast.LENGTH_SHORT).show());
                });
    }

    private void markReviewed() {
        if (question == null) return;
        int generation = ++actionGeneration;
        setActionButtonsEnabled(false);
        studyRepository.markReviewedAsync(
                session.getCurrentUserId(), question.id, awarded -> {
                    if (!canHandleAction(generation)) return;
                    loadSavedQuestion(() -> Toast.makeText(this, awarded
                                    ? R.string.review_awarded
                                    : R.string.review_already_done,
                            Toast.LENGTH_SHORT).show());
                });
    }

    private boolean canHandleAction(int generation) {
        return generation == actionGeneration
                && !isFinishing() && !isDestroyed();
    }

    private void setActionButtonsEnabled(boolean enabled) {
        btnBookmark.setEnabled(enabled);
        btnReviewed.setEnabled(enabled);
    }

    private void showNotFoundAndFinish() {
        Toast.makeText(this, R.string.answer_not_found, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void refreshBookmarkButton() {
        btnBookmark.setEnabled(true);
        btnBookmark.setText(question.bookmarked ? R.string.bookmarked : R.string.bookmark);
    }

    private void refreshReviewedButton() {
        btnReviewed.setEnabled(!question.reviewed);
        btnReviewed.setText(question.reviewed ? R.string.reviewed : R.string.mark_reviewed);
    }

    private String cleanStoredAnswer(String value) {
        if (value == null) return "";
        return value.replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
    }

    private CharSequence formatAnswerText(String value) {
        String cleaned = cleanStoredAnswer(value).trim();
        if (cleaned.isEmpty()) return "";

        SpannableStringBuilder formatted = new SpannableStringBuilder();
        boolean firstContentLine = true;
        int primary = MaterialColors.getColor(tvAnswer,
                com.google.android.material.R.attr.colorPrimary);
        for (String rawLine : cleaned.split("\\R", -1)) {
            String line = rawLine.trim();
            if (firstContentLine && isRepeatedMetadata(line)) {
                firstContentLine = false;
                continue;
            }
            if (!line.isEmpty()) firstContentLine = false;
            if (line.startsWith("- ")) {
                line = "• " + line.substring(2).trim();
            }

            int start = formatted.length();
            if (formatted.length() > 0) formatted.append('\n');
            formatted.append(line);
            int end = formatted.length();
            if (isSectionHeading(line)) {
                formatted.setSpan(new StyleSpan(Typeface.BOLD),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                formatted.setSpan(new ForegroundColorSpan(primary),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                formatted.setSpan(new RelativeSizeSpan(1.08f),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        while (formatted.length() > 0
                && formatted.charAt(formatted.length() - 1) == '\n') {
            formatted.delete(formatted.length() - 1, formatted.length());
        }
        return formatted;
    }

    private boolean isRepeatedMetadata(String line) {
        return line.startsWith("Subject:") || line.startsWith("Môn học:");
    }

    private boolean isSectionHeading(String line) {
        String heading = line.endsWith(":")
                ? line.substring(0, line.length() - 1).trim() : line;
        return "Answer".equals(heading)
                || "Câu trả lời".equals(heading)
                || "Step-by-step".equals(heading)
                || "Giải thích từng bước".equals(heading)
                || "In simple terms".equals(heading)
                || "Nói một cách đơn giản".equals(heading)
                || "Key concepts".equals(heading)
                || "Khái niệm chính".equals(heading)
                || "Common mistakes to avoid".equals(heading)
                || "Những lỗi thường gặp cần tránh".equals(heading)
                || "Follow-up questions to practise".equals(heading)
                || "Câu hỏi luyện tập thêm".equals(heading);
    }

    private Question readTransientQuestion() {
        Question temporary = new Question();
        temporary.id = -1L;
        temporary.questionText = stringExtra(EXTRA_QUESTION_TEXT);
        temporary.subject = stringExtra(EXTRA_SUBJECT);
        temporary.difficulty = stringExtra(EXTRA_DIFFICULTY);
        temporary.answerText = stringExtra(EXTRA_ANSWER_TEXT);
        temporary.answerSource = stringExtra(EXTRA_SOURCE);
        temporary.modelName = stringExtra(EXTRA_MODEL);
        temporary.responseTimeMs = getIntent().getLongExtra(EXTRA_RESPONSE_TIME, 0L);
        return temporary.answerText.isEmpty() ? null : temporary;
    }

    private String stringExtra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value;
    }

    private String buildSourceLabel(Question savedQuestion) {
        AnswerSource source = AnswerSource.fromStorage(savedQuestion.answerSource);
        String model = savedQuestion.modelName == null ? "" : savedQuestion.modelName.trim();
        String label;
        switch (source) {
            case REMOTE:
                label = model.isEmpty()
                        ? getString(R.string.answer_source_online)
                        : getString(R.string.answer_source_online_model, model);
                break;
            case LOCAL:
                label = getString(R.string.answer_source_offline);
                break;
            case LOCAL_FALLBACK:
                label = getString(R.string.answer_source_offline_fallback);
                break;
            case LEGACY:
            default:
                label = getString(R.string.answer_source_saved);
                break;
        }
        if (savedQuestion.responseTimeMs > 0L) {
            label += getString(R.string.answer_response_time_suffix,
                    savedQuestion.responseTimeMs / 1000.0);
        }
        return label;
    }
}
