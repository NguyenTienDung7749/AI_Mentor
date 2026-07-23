package com.example.aimentor.Fragments;

import android.content.Intent;
import android.net.Uri;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class HomeFragment extends Fragment {

    private static final String STATE_CAMERA_URI = "ocr_camera_uri";
    private static final String STATE_CAMERA_FILE = "ocr_camera_file";

    private StudyRepository studyRepository;
    private UserRepository userRepository;
    private SessionManager session;

    private TextView tvGreeting, tvLevelTitle, tvXp, tvStats, tvInsights;
    private ProgressBar progressXp;
    private ProgressBar progressAsk;
    private Spinner spSubject;
    private TextInputEditText etQuestion;
    private MaterialButton btnAsk, btnChooseImage, btnTakePhoto;
    private ProgressBar progressOcr;
    private TextView tvAskStatus, tvOcrStatus;
    private boolean isAsking;
    private boolean isScanning;
    private Uri pendingCameraUri;
    private File pendingCameraFile;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    recognizeText(uri, false);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraUri != null) {
                    recognizeText(pendingCameraUri, true);
                } else {
                    deletePendingCameraFile();
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

        if (savedInstanceState != null) {
            String cameraUri = savedInstanceState.getString(STATE_CAMERA_URI);
            String cameraFile = savedInstanceState.getString(STATE_CAMERA_FILE);
            if (cameraUri != null) pendingCameraUri = Uri.parse(cameraUri);
            if (cameraFile != null) pendingCameraFile = new File(cameraFile);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Auto", "Mathematics", "Science", "Programming",
                        "History", "Languages", "General"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(adapter);

        btnAsk.setOnClickListener(v -> ask());
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
    }

    private void chooseImage() {
        if (isAsking || isScanning) return;
        imagePicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void takePhoto() {
        if (isAsking || isScanning) return;
        try {
            File imageDirectory = new File(requireContext().getCacheDir(), "ocr");
            if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
                throw new IOException("Unable to create OCR cache directory");
            }
            pendingCameraFile = File.createTempFile(
                    "study_question_", ".jpg", imageDirectory);
            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pendingCameraFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (RuntimeException | IOException error) {
            deletePendingCameraFile();
            Toast.makeText(requireContext(), R.string.camera_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void recognizeText(@NonNull Uri imageUri, boolean deleteAfterReading) {
        if (!isAdded()) return;
        setScanning(true);
        tvOcrStatus.setText(R.string.ocr_recognizing);
        tvOcrStatus.setVisibility(View.VISIBLE);

        InputImage image;
        try {
            image = InputImage.fromFilePath(requireContext(), imageUri);
        } catch (IOException error) {
            finishOcrWithError(deleteAfterReading);
            return;
        }

        TextRecognizer recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    if (!isAdded() || getView() == null) return;
                    String extracted = result.getText() == null
                            ? "" : result.getText().trim();
                    if (extracted.isEmpty()) {
                        tvOcrStatus.setText(R.string.ocr_no_text);
                        Toast.makeText(requireContext(), R.string.ocr_no_text,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    etQuestion.setText(extracted);
                    etQuestion.setSelection(extracted.length());
                    tvOcrStatus.setText(R.string.ocr_ready);
                    etQuestion.requestFocus();
                })
                .addOnFailureListener(error -> {
                    if (!isAdded() || getView() == null) return;
                    tvOcrStatus.setText(R.string.ocr_failed);
                    Toast.makeText(requireContext(), R.string.ocr_failed,
                            Toast.LENGTH_LONG).show();
                })
                .addOnCompleteListener(task -> {
                    recognizer.close();
                    if (deleteAfterReading) deletePendingCameraFile();
                    if (isAdded() && getView() != null) {
                        setScanning(false);
                    } else {
                        isScanning = false;
                    }
                });
    }

    private void finishOcrWithError(boolean deleteAfterReading) {
        if (deleteAfterReading) deletePendingCameraFile();
        setScanning(false);
        tvOcrStatus.setText(R.string.ocr_failed);
        tvOcrStatus.setVisibility(View.VISIBLE);
        Toast.makeText(requireContext(), R.string.ocr_failed,
                Toast.LENGTH_LONG).show();
    }

    private void setScanning(boolean scanning) {
        isScanning = scanning;
        progressOcr.setVisibility(scanning ? View.VISIBLE : View.GONE);
        updateInputActions();
    }

    private void updateInputActions() {
        boolean enabled = !isAsking && !isScanning;
        btnAsk.setEnabled(enabled);
        btnChooseImage.setEnabled(enabled);
        btnTakePhoto.setEnabled(enabled);
    }

    private void deletePendingCameraFile() {
        if (pendingCameraFile != null && pendingCameraFile.exists()) {
            // The file is an app-created temporary image inside cache/ocr.
            pendingCameraFile.delete();
        }
        pendingCameraFile = null;
        pendingCameraUri = null;
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
        if (result.source == AnswerSource.LOCAL_FALLBACK) {
            Toast.makeText(requireContext(), R.string.offline_fallback_notice,
                    Toast.LENGTH_LONG).show();
        }
        if (result.leveledUp) {
            Toast.makeText(requireContext(), "Level up! Keep learning.", Toast.LENGTH_SHORT).show();
        }
        Intent intent = result.saved
                ? AnswerActivity.savedAnswerIntent(requireContext(), result.questionId)
                : AnswerActivity.transientAnswerIntent(requireContext(), result.answer);
        startActivity(intent);
    }

    private void setAsking(boolean asking) {
        isAsking = asking;
        updateInputActions();
        progressAsk.setVisibility(asking ? View.VISIBLE : View.GONE);
        tvAskStatus.setVisibility(asking ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCameraUri != null) {
            outState.putString(STATE_CAMERA_URI, pendingCameraUri.toString());
        }
        if (pendingCameraFile != null) {
            outState.putString(STATE_CAMERA_FILE, pendingCameraFile.getAbsolutePath());
        }
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
