package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.activities.QuizActivity;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class QuizFragment extends Fragment {

    private static final String[] QUIZ_SUBJECT_VALUES = {
            "Personalized (from history)", "Mathematics", "Science",
            "Programming", "History", "Languages", "General"
    };
    private static final String[] QUIZ_DIFFICULTY_VALUES = {
            "Adaptive", "Beginner", "Intermediate", "Advanced"
    };

    private StudyRepository studyRepository;
    private SessionManager session;
    private Spinner spQuizSubject, spQuizDifficulty;
    private TextView tvQuizStats;
    private int refreshGeneration;

    public QuizFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studyRepository = new StudyRepository(requireContext());
        session = new SessionManager(requireContext());

        spQuizSubject = view.findViewById(R.id.spQuizSubject);
        spQuizDifficulty = view.findViewById(R.id.spQuizDifficulty);
        tvQuizStats = view.findViewById(R.id.tvQuizStats);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvQuizHeading), true);
        MaterialButton btnStartQuiz = view.findViewById(R.id.btnStartQuiz);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.quiz_subject_choices));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQuizSubject.setAdapter(adapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.quiz_difficulty_choices));
        difficultyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spQuizDifficulty.setAdapter(difficultyAdapter);

        btnStartQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QuizActivity.class);
            int subjectPosition = Math.max(0, Math.min(
                    spQuizSubject.getSelectedItemPosition(),
                    QUIZ_SUBJECT_VALUES.length - 1));
            int difficultyPosition = Math.max(0, Math.min(
                    spQuizDifficulty.getSelectedItemPosition(),
                    QUIZ_DIFFICULTY_VALUES.length - 1));
            intent.putExtra(QuizActivity.EXTRA_SUBJECT,
                    QUIZ_SUBJECT_VALUES[subjectPosition]);
            intent.putExtra(QuizActivity.EXTRA_DIFFICULTY,
                    QUIZ_DIFFICULTY_VALUES[difficultyPosition]);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int generation = ++refreshGeneration;
        studyRepository.getProgressAsync(
                session.getCurrentUserId(), progress -> {
                    if (!canRenderRefresh(generation)) return;
                    tvQuizStats.setText(getString(R.string.quiz_stats,
                            progress.quizzesCompleted,
                            progress.accuracyPercent,
                            progress.totalCorrect,
                            progress.totalAnswered));
                });
    }

    private boolean canRenderRefresh(int generation) {
        return generation == refreshGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void onDestroyView() {
        refreshGeneration++;
        super.onDestroyView();
    }
}
