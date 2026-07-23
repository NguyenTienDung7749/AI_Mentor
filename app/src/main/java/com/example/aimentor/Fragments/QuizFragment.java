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
import androidx.fragment.app.Fragment;

import com.example.aimentor.R;
import com.example.aimentor.activities.QuizActivity;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public class QuizFragment extends Fragment {

    private StudyRepository studyRepository;
    private SessionManager session;
    private Spinner spQuizSubject, spQuizDifficulty;
    private TextView tvQuizStats;

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
        MaterialButton btnStartQuiz = view.findViewById(R.id.btnStartQuiz);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Personalized (from history)", "Mathematics", "Science", "Programming",
                        "History", "Languages", "General"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQuizSubject.setAdapter(adapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Adaptive", "Beginner", "Intermediate", "Advanced"));
        difficultyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spQuizDifficulty.setAdapter(difficultyAdapter);

        btnStartQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QuizActivity.class);
            intent.putExtra(QuizActivity.EXTRA_SUBJECT, (String) spQuizSubject.getSelectedItem());
            intent.putExtra(QuizActivity.EXTRA_DIFFICULTY,
                    (String) spQuizDifficulty.getSelectedItem());
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        StudyRepository.Progress p = studyRepository.getProgress(session.getCurrentUserId());
        tvQuizStats.setText("Quizzes completed: " + p.quizzesCompleted
                + "\nOverall accuracy: " + p.accuracyPercent + "%"
                + "\nCorrect answers: " + p.totalCorrect + " / " + p.totalAnswered);
    }
}
