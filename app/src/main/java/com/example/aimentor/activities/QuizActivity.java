package com.example.aimentor.activities;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.aimentor.R;
import com.example.aimentor.ai.QuizQuestion;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/** Runs a practice quiz with instant feedback and records the result. */
public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT = "subject";

    private StudyRepository studyRepository;
    private SessionManager session;
    private List<QuizQuestion> questions;
    private String subject;

    private int index = 0;
    private int correctCount = 0;
    private boolean answered = false;

    private TextView tvProgress, tvQuestionPrompt, tvFeedback;
    private RadioGroup rgOptions;
    private RadioButton[] options;
    private MaterialButton btnAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        studyRepository = new StudyRepository(this);
        session = new SessionManager(this);

        subject = getIntent().getStringExtra(EXTRA_SUBJECT);
        if (subject == null) subject = "General";

        questions = studyRepository.generateQuiz(subject, 5);
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "Could not generate a quiz.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvProgress = findViewById(R.id.tvProgress);
        tvQuestionPrompt = findViewById(R.id.tvQuestionPrompt);
        tvFeedback = findViewById(R.id.tvFeedback);
        rgOptions = findViewById(R.id.rgOptions);
        options = new RadioButton[]{
                findViewById(R.id.rbOpt0), findViewById(R.id.rbOpt1),
                findViewById(R.id.rbOpt2), findViewById(R.id.rbOpt3)};
        btnAction = findViewById(R.id.btnAction);

        btnAction.setOnClickListener(v -> onAction());
        showQuestion();
    }

    private void showQuestion() {
        answered = false;
        QuizQuestion q = questions.get(index);
        tvProgress.setText("Question " + (index + 1) + " of " + questions.size());
        tvQuestionPrompt.setText(q.getPrompt());
        rgOptions.clearCheck();
        List<String> opts = q.getOptions();
        for (int i = 0; i < options.length; i++) {
            if (i < opts.size()) {
                options[i].setVisibility(RadioButton.VISIBLE);
                options[i].setText(opts.get(i));
                options[i].setEnabled(true);
            } else {
                options[i].setVisibility(RadioButton.GONE);
            }
        }
        tvFeedback.setVisibility(TextView.GONE);
        btnAction.setText(R.string.check_answer);
    }

    private int selectedIndex() {
        int checkedId = rgOptions.getCheckedRadioButtonId();
        for (int i = 0; i < options.length; i++) {
            if (options[i].getId() == checkedId) return i;
        }
        return -1;
    }

    private void onAction() {
        if (!answered) {
            int selected = selectedIndex();
            if (selected < 0) {
                Toast.makeText(this, "Please choose an answer.", Toast.LENGTH_SHORT).show();
                return;
            }
            QuizQuestion q = questions.get(index);
            answered = true;
            for (RadioButton rb : options) rb.setEnabled(false);

            boolean correct = q.isCorrect(selected);
            if (correct) correctCount++;
            tvFeedback.setVisibility(TextView.VISIBLE);
            tvFeedback.setText((correct ? "Correct! " : "Not quite. ") + q.getExplanation());
            tvFeedback.setTextColor(ContextCompat.getColor(this,
                    correct ? R.color.success : R.color.error));

            btnAction.setText(index == questions.size() - 1
                    ? R.string.finish_quiz : R.string.next_question);
        } else {
            if (index < questions.size() - 1) {
                index++;
                showQuestion();
            } else {
                finishQuiz();
            }
        }
    }

    private void finishQuiz() {
        int total = questions.size();
        StudyRepository.QuizResult result =
                studyRepository.recordQuiz(session.getCurrentUserId(), subject, correctCount, total);

        if (result.leveledUp) {
            NotificationHelper.notify(this, "Level up!",
                    "You reached level " + result.newLevel + ". Keep going!");
        }

        String message = "You scored " + correctCount + " / " + total
                + "\nXP earned: " + result.awardedXp;
        new AlertDialog.Builder(this)
                .setTitle("Quiz complete")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Done", (d, w) -> finish())
                .show();
    }
}
