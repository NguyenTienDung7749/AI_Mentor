package com.example.aimentor.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aimentor.R;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.data.Question;
import com.example.aimentor.repo.StudyRepository;
import com.google.android.material.button.MaterialButton;

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
    private Question question;
    private MaterialButton btnBookmark, btnReviewed;
    private boolean transientAnswer;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        studyRepository = new StudyRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        transientAnswer = getIntent().getBooleanExtra(EXTRA_TRANSIENT, false);
        question = transientAnswer
                ? readTransientQuestion()
                : studyRepository.getQuestion(
                        getIntent().getLongExtra(EXTRA_QUESTION_ID, -1));
        if (question == null) {
            Toast.makeText(this, "Answer not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvSubject = findViewById(R.id.tvSubject);
        TextView tvAnswerSource = findViewById(R.id.tvAnswerSource);
        TextView tvQuestion = findViewById(R.id.tvQuestion);
        TextView tvAnswer = findViewById(R.id.tvAnswer);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnReviewed = findViewById(R.id.btnReviewed);

        tvSubject.setText(question.subject + "  |  " + question.difficulty);
        tvAnswerSource.setText(buildSourceLabel(question));
        tvQuestion.setText(question.questionText);
        tvAnswer.setText(question.answerText);

        if (transientAnswer) {
            findViewById(R.id.answerActions).setVisibility(View.GONE);
        } else {
            refreshBookmarkButton();
            refreshReviewedButton();

            btnBookmark.setOnClickListener(v -> {
                studyRepository.toggleBookmark(question.id);
                question = studyRepository.getQuestion(question.id);
                refreshBookmarkButton();
                Toast.makeText(this, question.bookmarked ? "Bookmarked" : "Removed bookmark",
                        Toast.LENGTH_SHORT).show();
            });

            btnReviewed.setOnClickListener(v -> {
                boolean awarded = studyRepository.markReviewed(question.id);
                question = studyRepository.getQuestion(question.id);
                refreshReviewedButton();
                Toast.makeText(this, awarded ? "Marked as reviewed (+2 XP)" : "Already reviewed",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void refreshBookmarkButton() {
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
