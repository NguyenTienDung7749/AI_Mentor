package com.example.aimentor.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private static final String[] SUBJECT_VALUES = {
            "Auto", SubjectClassifier.MATH, SubjectClassifier.SCIENCE,
            SubjectClassifier.PROGRAMMING, SubjectClassifier.HISTORY,
            SubjectClassifier.LANGUAGES, SubjectClassifier.GENERAL
    };

    private StudyRepository studyRepository;
    private UserRepository userRepository;
    private SessionManager session;
    private HomeUiStateViewModel uiState;

    private TextView tvGreeting, tvLevelTitle, tvXp, tvStats, tvInsights;
    private ProgressBar progressXp;
    private ProgressBar progressAsk;
    private Spinner spSubject;
    private TextInputEditText etQuestion;
    private MaterialButton btnAsk, btnChooseImage, btnTakePhoto;
    private ProgressBar progressOcr;
    private TextView tvAskStatus, tvOcrStatus;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && uiState != null) {
                    uiState.recognizeText(uri, false);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (uiState == null) return;
                Uri pendingCameraUri = uiState.getPendingCameraUri();
                if (success && pendingCameraUri != null) {
                    uiState.recognizeText(pendingCameraUri, true);
                } else {
                    uiState.deletePendingCameraFile();
                }
            });

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
        uiState = new ViewModelProvider(this)
                .get(HomeUiStateViewModel.class);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvLevelTitle = view.findViewById(R.id.tvLevelTitle);
        tvXp = view.findViewById(R.id.tvXp);
        tvStats = view.findViewById(R.id.tvStats);
        tvInsights = view.findViewById(R.id.tvInsights);
        progressXp = view.findViewById(R.id.progressXp);
        spSubject = view.findViewById(R.id.spSubject);
        etQuestion = view.findViewById(R.id.etQuestion);
        btnAsk = view.findViewById(R.id.btnAsk);
        btnChooseImage = view.findViewById(R.id.btnChooseImage);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        progressAsk = view.findViewById(R.id.progressAsk);
        progressOcr = view.findViewById(R.id.progressOcr);
        tvAskStatus = view.findViewById(R.id.tvAskStatus);
        tvOcrStatus = view.findViewById(R.id.tvOcrStatus);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvAskHeading), true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.subject_choices));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(adapter);

        String restoredDraft = uiState.getQuestionDraft();
        etQuestion.setText(restoredDraft);
        etQuestion.setSelection(restoredDraft.length());
        spSubject.setSelection(Math.max(0,
                Math.min(uiState.getSubjectPosition(), SUBJECT_VALUES.length - 1)));
        etQuestion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                uiState.setQuestionDraft(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        spSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView,
                                       int position, long id) {
                uiState.setSubjectPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnAsk.setOnClickListener(v -> ask());
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        uiState.getAskState().observe(
                getViewLifecycleOwner(), this::renderAskState);
        uiState.getOcrState().observe(
                getViewLifecycleOwner(), this::renderOcrState);
    }

    private void chooseImage() {
        if (uiState.isAsking() || uiState.isScanning()) return;
        imagePicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void takePhoto() {
        if (uiState.isAsking() || uiState.isScanning()) return;
        try {
            cameraLauncher.launch(uiState.prepareCameraCapture());
        } catch (RuntimeException | IOException error) {
            uiState.deletePendingCameraFile();
            Toast.makeText(requireContext(), R.string.camera_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateInputActions() {
        boolean enabled = !uiState.isAsking() && !uiState.isScanning();
        btnAsk.setEnabled(enabled);
        btnChooseImage.setEnabled(enabled);
        btnTakePhoto.setEnabled(enabled);
    }

    private void ask() {
        if (uiState.isAsking() || uiState.isScanning()) return;

        String question = etQuestion.getText() == null ? "" : etQuestion.getText().toString().trim();
        int selectedSubject = spSubject.getSelectedItemPosition();
        String subjectHint = SUBJECT_VALUES[Math.max(0,
                Math.min(selectedSubject, SUBJECT_VALUES.length - 1))];

        uiState.ask(
                session.getCurrentUserId(), question, subjectHint);
    }

    private void renderAskState(HomeUiStateViewModel.AskUiState state) {
        boolean loading =
                state != null && state.status == HomeUiStateViewModel.AskStatus.LOADING;
        setAsking(loading);
        if (state == null
                || state.status != HomeUiStateViewModel.AskStatus.RESULT
                || state.result == null) {
            return;
        }
        StudyRepository.AskResult result = state.result;
        uiState.consumeAskResult();
        handleAskResult(result);
    }

    private void handleAskResult(@NonNull StudyRepository.AskResult result) {
        setAsking(false);
        if (!result.success) {
            String message = result.message == null || result.message.trim().isEmpty()
                    ? getString(R.string.answer_error_default) : result.message;
            tvAskStatus.setText(message);
            tvAskStatus.setTextColor(requireContext().getColor(R.color.error));
            tvAskStatus.setVisibility(View.VISIBLE);
            tvAskStatus.announceForAccessibility(message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            return;
        }
        etQuestion.setText("");
        uiState.setQuestionDraft("");
        if (result.source == AnswerSource.LOCAL_FALLBACK) {
            Toast.makeText(requireContext(), R.string.offline_fallback_notice,
                    Toast.LENGTH_LONG).show();
        }
        if (result.leveledUp) {
            Toast.makeText(requireContext(), R.string.level_up_toast,
                    Toast.LENGTH_SHORT).show();
        }
        Intent intent = result.saved
                ? AnswerActivity.savedAnswerIntent(requireContext(), result.questionId)
                : AnswerActivity.transientAnswerIntent(requireContext(), result.answer);
        startActivity(intent);
    }

    private void setAsking(boolean asking) {
        updateInputActions();
        progressAsk.setVisibility(asking ? View.VISIBLE : View.GONE);
        tvAskStatus.setVisibility(asking ? View.VISIBLE : View.GONE);
        if (asking) {
            tvAskStatus.setText(R.string.preparing_answer);
            tvAskStatus.setTextColor(requireContext().getColor(R.color.text_secondary));
        }
    }

    private void renderOcrState(HomeUiStateViewModel.OcrUiState state) {
        HomeUiStateViewModel.OcrStatus status = state == null
                ? HomeUiStateViewModel.OcrStatus.IDLE : state.status;
        progressOcr.setVisibility(
                status == HomeUiStateViewModel.OcrStatus.LOADING
                        ? View.VISIBLE : View.GONE);
        switch (status) {
            case LOADING:
                tvOcrStatus.setText(R.string.ocr_recognizing);
                tvOcrStatus.setVisibility(View.VISIBLE);
                break;
            case READY:
                tvOcrStatus.setText(R.string.ocr_ready);
                tvOcrStatus.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                tvOcrStatus.setText(R.string.ocr_no_text);
                tvOcrStatus.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                tvOcrStatus.setText(R.string.ocr_failed);
                tvOcrStatus.setVisibility(View.VISIBLE);
                break;
            case IDLE:
            default:
                tvOcrStatus.setVisibility(View.GONE);
                break;
        }
        updateInputActions();

        if (state == null || !state.eventPending) return;
        if (status == HomeUiStateViewModel.OcrStatus.READY) {
            etQuestion.setText(state.extractedText);
            etQuestion.setSelection(state.extractedText.length());
            etQuestion.requestFocus();
        } else if (status == HomeUiStateViewModel.OcrStatus.EMPTY) {
            Toast.makeText(requireContext(), R.string.ocr_no_text,
                    Toast.LENGTH_LONG).show();
        } else if (status == HomeUiStateViewModel.OcrStatus.ERROR) {
            Toast.makeText(requireContext(), R.string.ocr_failed,
                    Toast.LENGTH_LONG).show();
        }
        uiState.consumeOcrEvent();
    }

    @Override
    public void onDestroyView() {
        captureUiState();
        super.onDestroyView();
    }

    private void captureUiState() {
        if (uiState == null || etQuestion == null) return;
        uiState.setQuestionDraft(etQuestion.getText() == null
                ? "" : etQuestion.getText().toString());
        uiState.setSubjectPosition(spSubject.getSelectedItemPosition());
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
                ? user.name : getString(R.string.default_student_name);
        tvGreeting.setText(getString(R.string.home_greeting, name));

        StudyRepository.Progress p = studyRepository.getProgress(userId);
        tvLevelTitle.setText(getString(
                R.string.level_summary, p.level, p.levelTitle));
        progressXp.setProgress(p.xpIntoLevel);
        progressXp.setContentDescription(getString(R.string.level_progress_description,
                p.level, p.xpIntoLevel,
                com.example.aimentor.util.Gamification.XP_PER_LEVEL));
        tvXp.setText(getString(R.string.home_xp_progress, p.xp, p.xpToNext));
        tvStats.setText(getString(
                R.string.home_stats, p.totalQuestions, p.accuracyPercent));

        StringBuilder insights = new StringBuilder();
        for (String s : p.insights) insights.append("\u2022 ").append(s).append("\n");
        tvInsights.setText(insights.toString().trim());
    }
}
