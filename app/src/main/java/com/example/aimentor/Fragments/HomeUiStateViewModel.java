package com.example.aimentor.Fragments;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.StudyInputPolicy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;

/**
 * Owns Home input and long-running Ask/OCR operations across configuration
 * changes. Fragments only render this state, so callbacks never target an old
 * view and one user action cannot start duplicate work after rotation.
 */
public class HomeUiStateViewModel extends AndroidViewModel {

    private static final String KEY_DRAFT = "home_draft";
    private static final String KEY_SUBJECT = "home_subject";
    private static final String KEY_OCR_STATE = "home_ocr_state";
    private static final String KEY_CAMERA_URI = "home_camera_uri";
    private static final String KEY_CAMERA_FILE = "home_camera_file";

    enum AskStatus { IDLE, LOADING, RESULT }

    static final class AskUiState {
        final AskStatus status;
        final StudyRepository.AskResult result;

        private AskUiState(
                AskStatus status, StudyRepository.AskResult result) {
            this.status = status;
            this.result = result;
        }

        static AskUiState idle() {
            return new AskUiState(AskStatus.IDLE, null);
        }

        static AskUiState loading() {
            return new AskUiState(AskStatus.LOADING, null);
        }

        static AskUiState result(StudyRepository.AskResult result) {
            return new AskUiState(AskStatus.RESULT, result);
        }
    }

    enum OcrStatus { IDLE, LOADING, READY, EMPTY, ERROR }

    static final class OcrUiState {
        final OcrStatus status;
        final String extractedText;
        final boolean eventPending;
        final boolean truncated;

        private OcrUiState(
                OcrStatus status, String extractedText,
                boolean eventPending, boolean truncated) {
            this.status = status;
            this.extractedText = extractedText == null ? "" : extractedText;
            this.eventPending = eventPending;
            this.truncated = truncated;
        }

        static OcrUiState idle() {
            return new OcrUiState(OcrStatus.IDLE, "", false, false);
        }

        static OcrUiState loading() {
            return new OcrUiState(OcrStatus.LOADING, "", false, false);
        }

        static OcrUiState terminal(
                OcrStatus status, String extractedText, boolean eventPending) {
            return terminal(status, extractedText, eventPending, false);
        }

        static OcrUiState terminal(
                OcrStatus status, String extractedText,
                boolean eventPending, boolean truncated) {
            return new OcrUiState(
                    status, extractedText, eventPending, truncated);
        }
    }

    private final SavedStateHandle savedState;
    private final StudyRepository studyRepository;
    private final MutableLiveData<AskUiState> askState =
            new MutableLiveData<>(AskUiState.idle());
    private final MutableLiveData<OcrUiState> ocrState;

    private TextRecognizer activeRecognizer;
    private boolean cleared;

    public HomeUiStateViewModel(
            @NonNull Application application, SavedStateHandle savedState) {
        this(application, savedState, new StudyRepository(application));
    }

    /** Test seam for verifying operation deduplication without network calls. */
    HomeUiStateViewModel(
            @NonNull Application application,
            SavedStateHandle savedState,
            StudyRepository studyRepository) {
        super(application);
        this.savedState = savedState;
        this.studyRepository = studyRepository;
        OcrStatus restored = restoredOcrStatus();
        // A process restart cannot retain the ML Kit task itself.
        if (restored == OcrStatus.LOADING) restored = OcrStatus.ERROR;
        this.ocrState = new MutableLiveData<>(
                restored == OcrStatus.IDLE
                        ? OcrUiState.idle()
                        : OcrUiState.terminal(restored, "", false));
    }

    LiveData<AskUiState> getAskState() {
        return askState;
    }

    LiveData<OcrUiState> getOcrState() {
        return ocrState;
    }

    boolean isAsking() {
        AskUiState current = askState.getValue();
        return current != null && current.status == AskStatus.LOADING;
    }

    boolean isScanning() {
        OcrUiState current = ocrState.getValue();
        return current != null && current.status == OcrStatus.LOADING;
    }

    boolean ask(long userId, String question, String subjectHint) {
        if (isAsking() || isScanning() || cleared) return false;
        askState.setValue(AskUiState.loading());
        studyRepository.askAsync(userId, question, subjectHint, result -> {
            if (!cleared) askState.setValue(AskUiState.result(result));
        });
        return true;
    }

    void consumeAskResult() {
        AskUiState current = askState.getValue();
        if (current != null && current.status == AskStatus.RESULT) {
            askState.setValue(AskUiState.idle());
        }
    }

    boolean recognizeText(@NonNull Uri imageUri, boolean deleteAfterReading) {
        if (isAsking() || isScanning() || cleared) return false;
        setOcrState(OcrUiState.loading());

        InputImage image;
        try {
            image = InputImage.fromFilePath(getApplication(), imageUri);
        } catch (IOException | RuntimeException error) {
            if (deleteAfterReading) deletePendingCameraFile();
            setOcrState(OcrUiState.terminal(OcrStatus.ERROR, "", true));
            return true;
        }

        TextRecognizer recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
        activeRecognizer = recognizer;
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    if (cleared) return;
                    StudyInputPolicy.LimitedText extracted =
                            StudyInputPolicy.limitOcrText(result.getText());
                    setOcrState(extracted.text.isEmpty()
                            ? OcrUiState.terminal(OcrStatus.EMPTY, "", true)
                            : OcrUiState.terminal(
                                    OcrStatus.READY, extracted.text,
                                    true, extracted.truncated));
                })
                .addOnFailureListener(error -> {
                    if (!cleared) {
                        setOcrState(OcrUiState.terminal(
                                OcrStatus.ERROR, "", true));
                    }
                })
                .addOnCompleteListener(task -> {
                    if (activeRecognizer == recognizer) {
                        recognizer.close();
                        activeRecognizer = null;
                    }
                    if (deleteAfterReading) deletePendingCameraFile();
                });
        return true;
    }

    void consumeOcrEvent() {
        OcrUiState current = ocrState.getValue();
        if (current == null || !current.eventPending) return;
        setOcrState(OcrUiState.terminal(
                current.status, "", false, current.truncated));
    }

    Uri prepareCameraCapture() throws IOException {
        deletePendingCameraFile();
        File imageDirectory =
                new File(getApplication().getCacheDir(), "ocr");
        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            throw new IOException("Unable to create OCR cache directory");
        }
        File imageFile =
                File.createTempFile("study_question_", ".jpg", imageDirectory);
        try {
            Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                    getApplication(),
                    getApplication().getPackageName() + ".fileprovider",
                    imageFile);
            savedState.set(KEY_CAMERA_FILE, imageFile.getAbsolutePath());
            savedState.set(KEY_CAMERA_URI, imageUri.toString());
            return imageUri;
        } catch (RuntimeException error) {
            imageFile.delete();
            throw error;
        }
    }

    Uri getPendingCameraUri() {
        String value = savedState.get(KEY_CAMERA_URI);
        return value == null || value.trim().isEmpty()
                ? null : Uri.parse(value);
    }

    void deletePendingCameraFile() {
        String filePath = savedState.get(KEY_CAMERA_FILE);
        if (filePath != null && !filePath.trim().isEmpty()) {
            File imageFile = new File(filePath);
            if (imageFile.exists()) imageFile.delete();
        }
        savedState.set(KEY_CAMERA_FILE, null);
        savedState.set(KEY_CAMERA_URI, null);
    }

    String getQuestionDraft() {
        String value = savedState.get(KEY_DRAFT);
        return value == null ? "" : value;
    }

    void setQuestionDraft(String value) {
        savedState.set(KEY_DRAFT, value == null ? "" : value);
    }

    int getSubjectPosition() {
        Integer value = savedState.get(KEY_SUBJECT);
        return value == null ? 0 : value;
    }

    void setSubjectPosition(int value) {
        savedState.set(KEY_SUBJECT, value);
    }

    private void setOcrState(OcrUiState value) {
        savedState.set(KEY_OCR_STATE, value.status.name());
        ocrState.setValue(value);
    }

    private OcrStatus restoredOcrStatus() {
        String value = savedState.get(KEY_OCR_STATE);
        if (value == null) return OcrStatus.IDLE;
        try {
            return OcrStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return OcrStatus.IDLE;
        }
    }

    @Override
    protected void onCleared() {
        cleared = true;
        if (activeRecognizer != null) {
            activeRecognizer.close();
            activeRecognizer = null;
        }
        deletePendingCameraFile();
        super.onCleared();
    }
}
