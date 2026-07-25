package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
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
import com.example.aimentor.util.DropdownAdapters;
import com.google.android.material.button.MaterialButton;

public class QuizFragment extends Fragment {

    private static final String STATE_SUBJECT_POSITION = "quiz_subject_position";
    private static final String STATE_DIFFICULTY_POSITION = "quiz_difficulty_position";
    private static final String[] QUIZ_SUBJECT_VALUES = {
            "Personalized (from history)", "Mathematics", "Science",
            "Programming", "History", "Languages", "General"
    };
    private static final String[] QUIZ_DIFFICULTY_VALUES = {
            "Adaptive", "Beginner", "Intermediate", "Advanced"
    };

    private StudyRepository studyRepository;
    private SessionManager session;
    private AutoCompleteTextView actQuizSubject, actQuizDifficulty;
    private TextView tvQuizCompleted, tvQuizAccuracy, tvQuizCorrect;
    private int selectedSubjectPosition;
    private int selectedDifficultyPosition;
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

        actQuizSubject = view.findViewById(R.id.actQuizSubject);
        actQuizDifficulty = view.findViewById(R.id.actQuizDifficulty);
        tvQuizCompleted = view.findViewById(R.id.tvQuizCompleted);
        tvQuizAccuracy = view.findViewById(R.id.tvQuizAccuracy);
        tvQuizCorrect = view.findViewById(R.id.tvQuizCorrect);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvQuizHeading), true);
        MaterialButton btnStartQuiz = view.findViewById(R.id.btnStartQuiz);

        String[] subjectLabels =
                getResources().getStringArray(R.array.quiz_subject_choices);
        DropdownAdapters.bind(actQuizSubject, subjectLabels);
        actQuizSubject.setOnItemClickListener((parent, selected, position, id) ->
                selectedSubjectPosition = position);

        String[] difficultyLabels =
                getResources().getStringArray(R.array.quiz_difficulty_choices);
        DropdownAdapters.bind(actQuizDifficulty, difficultyLabels);
        actQuizDifficulty.setOnItemClickListener((parent, selected, position, id) ->
                selectedDifficultyPosition = position);

        if (savedInstanceState != null) {
            selectedSubjectPosition = Math.max(0, Math.min(
                    savedInstanceState.getInt(STATE_SUBJECT_POSITION, 0),
                    QUIZ_SUBJECT_VALUES.length - 1));
            selectedDifficultyPosition = Math.max(0, Math.min(
                    savedInstanceState.getInt(STATE_DIFFICULTY_POSITION, 0),
                    QUIZ_DIFFICULTY_VALUES.length - 1));
        }
        actQuizSubject.setText(subjectLabels[selectedSubjectPosition], false);
        actQuizDifficulty.setText(
                difficultyLabels[selectedDifficultyPosition], false);

        btnStartQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QuizActivity.class);
            int subjectPosition = Math.max(0, Math.min(
                    selectedSubjectPosition,
                    QUIZ_SUBJECT_VALUES.length - 1));
            int difficultyPosition = Math.max(0, Math.min(
                    selectedDifficultyPosition,
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
                    tvQuizCompleted.setText(getString(
                            R.string.stat_number, progress.quizzesCompleted));
                    tvQuizAccuracy.setText(getString(
                            R.string.stat_percent, progress.accuracyPercent));
                    tvQuizCorrect.setText(getString(R.string.quiz_correct_ratio,
                            progress.totalCorrect, progress.totalAnswered));
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SUBJECT_POSITION, selectedSubjectPosition);
        outState.putInt(STATE_DIFFICULTY_POSITION, selectedDifficultyPosition);
    }
}
