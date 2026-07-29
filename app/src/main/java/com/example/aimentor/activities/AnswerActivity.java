package com.example.aimentor.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.aimentor.util.AnswerMarkdownFormatter;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.WindowUiHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import ru.noties.jlatexmath.JLatexMathDrawable;

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
    private Markwon markwon;
    private String lastFormattedMarkdown = "";

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
        markwon = Markwon.builder(this)
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(
                        tvAnswer.getTextSize(),
                        builder -> builder.inlinesEnabled(true)))
                .build();
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
        lastFormattedMarkdown =
                AnswerMarkdownFormatter.format(question.answerText);
        markwon.setMarkdown(tvAnswer, lastFormattedMarkdown);
        renderInlineLatex(tvAnswer);
        installCopyOverride(tvAnswer);

        if (transientAnswer) {
            answerActions.setVisibility(View.GONE);
        } else {
            answerActions.setVisibility(View.VISIBLE);
            refreshBookmarkButton();
            refreshReviewedButton();
        }
    }

    /**
     * Overrides the default text-selection copy so that the clipboard receives
     * the raw Markdown/LaTeX source instead of garbage replacement characters
     * produced by JLatexMath image spans.
     */
    private void installCopyOverride(TextView tv) {
        ActionMode.Callback copyCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true; // allow the default action mode to appear
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == android.R.id.copy
                        || item.getItemId() == android.R.id.cut) {
                    int start = tv.getSelectionStart();
                    int end = tv.getSelectionEnd();
                    // Use the source markdown if the entire text is selected or
                    // if the selection likely spans a LaTeX region (object
                    // replacement characters would appear).
                    CharSequence raw = tv.getText();
                    String selected = (start >= 0 && end > start)
                            ? raw.subSequence(start, end).toString() : "";
                    String toCopy;
                    if (selected.contains("\uFFFC") || (start == 0
                            && end == raw.length())) {
                        // Selection contains image span garbage; use source
                        toCopy = lastFormattedMarkdown;
                    } else {
                        toCopy = selected;
                    }
                    ClipboardManager clip = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clip != null) {
                        clip.setPrimaryClip(
                                ClipData.newPlainText("answer", toCopy));
                    }
                    Toast.makeText(AnswerActivity.this,
                            R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                            .show();
                    mode.finish();
                    return true;
                }
                return false;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) { }
        };
        tv.setCustomSelectionActionModeCallback(copyCallback);
    }

    /**
     * Post-processes rendered text to replace any remaining {@code $...$}
     * inline LaTeX that JLatexMathPlugin failed to render.  Creates
     * JLatexMathDrawable image spans so formulas display as rendered math
     * instead of raw dollar-sign delimited text.
     */
    private void renderInlineLatex(TextView tv) {
        CharSequence text = tv.getText();
        if (text == null) return;
        String str = text.toString();

        // Match $...$ that are NOT part of $$ (block math)
        Pattern inlinePattern = Pattern.compile(
                "(?<!\\$)\\$(?!\\$)(.+?)(?<!\\$)\\$(?!\\$)");
        Matcher matcher = inlinePattern.matcher(str);

        // Collect matches first (we'll process in reverse to keep indices valid)
        List<int[]> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new int[]{matcher.start(), matcher.end()});
        }
        if (matches.isEmpty()) return;

        SpannableStringBuilder ssb = text instanceof SpannableStringBuilder
                ? (SpannableStringBuilder) text
                : new SpannableStringBuilder(text);
        float textSize = tv.getTextSize();
        int textColor = tv.getCurrentTextColor();

        // Process in reverse order so earlier indices remain valid
        for (int i = matches.size() - 1; i >= 0; i--) {
            int start = matches.get(i)[0];
            int end = matches.get(i)[1];
            // Extract the LaTeX content (without the $ delimiters)
            String latex = str.substring(start + 1, end - 1).trim();
            if (latex.isEmpty()) continue;
            try {
                JLatexMathDrawable drawable = JLatexMathDrawable.builder(latex)
                        .textSize(textSize)
                        .color(textColor)
                        .build();
                drawable.setBounds(0, 0,
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
                ImageSpan span = new ImageSpan(drawable,
                        DynamicDrawableSpan.ALIGN_BOTTOM);
                ssb.setSpan(span, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (Exception ignored) {
                // If rendering fails for this formula, leave it as raw text
            }
        }
        tv.setText(ssb, TextView.BufferType.SPANNABLE);
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
