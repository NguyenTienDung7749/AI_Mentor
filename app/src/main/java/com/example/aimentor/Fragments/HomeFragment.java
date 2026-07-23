package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;

public class HomeFragment extends Fragment {

    private StudyRepository studyRepository;
    private UserRepository userRepository;
    private SessionManager session;

    private TextView tvGreeting, tvLevelTitle, tvXp, tvStats, tvInsights;
    private ProgressBar progressXp;
    private ProgressBar progressAsk;
    private Spinner spSubject;
    private TextInputEditText etQuestion;
    private MaterialButton btnAsk;
    private TextView tvAskStatus;
    private boolean isAsking;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studyRepository = new StudyRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        session = new SessionManager(requireContext());

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvLevelTitle = view.findViewById(R.id.tvLevelTitle);
        tvXp = view.findViewById(R.id.tvXp);
        tvStats = view.findViewById(R.id.tvStats);
        tvInsights = view.findViewById(R.id.tvInsights);
        progressXp = view.findViewById(R.id.progressXp);
        spSubject = view.findViewById(R.id.spSubject);
        etQuestion = view.findViewById(R.id.etQuestion);
        btnAsk = view.findViewById(R.id.btnAsk);
        progressAsk = view.findViewById(R.id.progressAsk);
        tvAskStatus = view.findViewById(R.id.tvAskStatus);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Auto", "Mathematics", "Science", "Programming",
                        "History", "Languages", "General"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(adapter);

        btnAsk.setOnClickListener(v -> ask());
    }

    private void ask() {
        if (isAsking) return;

        String question = etQuestion.getText() == null ? "" : etQuestion.getText().toString().trim();
        String subjectHint = (String) spSubject.getSelectedItem();

        setAsking(true);
        studyRepository.askAsync(session.getCurrentUserId(), question, subjectHint,
                this::handleAskResult);
    }

    private void handleAskResult(@NonNull StudyRepository.AskResult result) {
        if (!isAdded()
                || getView() == null
                || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }

        setAsking(false);
        if (!result.success) {
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
            return;
        }
        etQuestion.setText("");
        if (result.leveledUp) {
            Toast.makeText(requireContext(), "Level up! Keep learning.", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(requireContext(), AnswerActivity.class);
        intent.putExtra(AnswerActivity.EXTRA_QUESTION_ID, result.questionId);
        startActivity(intent);
    }

    private void setAsking(boolean asking) {
        isAsking = asking;
        btnAsk.setEnabled(!asking);
        progressAsk.setVisibility(asking ? View.VISIBLE : View.GONE);
        tvAskStatus.setVisibility(asking ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        long userId = session.getCurrentUserId();
        User user = userRepository.getUser(userId);
        String name = (user != null && user.name != null && !user.name.isEmpty())
                ? user.name : "there";
        tvGreeting.setText("Hi, " + name + "!");

        StudyRepository.Progress p = studyRepository.getProgress(userId);
        tvLevelTitle.setText("Level " + p.level + " - " + p.levelTitle);
        progressXp.setProgress(p.xpIntoLevel);
        tvXp.setText(p.xp + " XP  (" + p.xpToNext + " to next level)");
        tvStats.setText("Questions asked: " + p.totalQuestions
                + "   |   Quiz accuracy: " + p.accuracyPercent + "%");

        StringBuilder insights = new StringBuilder();
        for (String s : p.insights) insights.append("\u2022 ").append(s).append("\n");
        tvInsights.setText(insights.toString().trim());
    }
}
