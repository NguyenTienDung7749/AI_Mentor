package com.example.aimentor.activities;

import android.os.Bundle;
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

    private StudyRepository studyRepository;
    private Question question;
    private MaterialButton btnBookmark, btnReviewed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        studyRepository = new StudyRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        long id = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1);
        question = studyRepository.getQuestion(id);
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

    private void refreshBookmarkButton() {
        btnBookmark.setText(question.bookmarked ? R.string.bookmarked : R.string.bookmark);
    }

    private void refreshReviewedButton() {
        btnReviewed.setEnabled(!question.reviewed);
        btnReviewed.setText(question.reviewed ? R.string.reviewed : R.string.mark_reviewed);
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
            case CACHE:
                label = model.isEmpty()
                        ? getString(R.string.answer_source_cached)
                        : getString(R.string.answer_source_cached_model, model);
                break;
            case LEGACY:
            default:
                label = getString(R.string.answer_source_saved);
                break;
        }
        if (savedQuestion.responseTimeMs > 0L && source != AnswerSource.CACHE) {
            label += getString(R.string.answer_response_time_suffix,
                    savedQuestion.responseTimeMs / 1000.0);
        }
        return label;
    }
}
