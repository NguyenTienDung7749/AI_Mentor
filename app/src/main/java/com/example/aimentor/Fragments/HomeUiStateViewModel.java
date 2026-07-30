package com.example.aimentor.Fragments;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.aimentor.ai.ImageAttachment;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.ImageAttachmentPreparer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the Home composer and its long-running operations across configuration
 * changes, including one in-memory image attachment.
 */
public class HomeUiStateViewModel extends AndroidViewModel {

    private static final String KEY_DRAFT = "home_draft";
    private static final String KEY_SUBJECT = "home_subject";
    private static final String KEY_STYLE = "home_style";
    private static final String KEY_ALLOW_CACHE = "home_allow_cache";
    private static final String KEY_CAMERA_URI = "home_camera_uri";
    private static final String KEY_CAMERA_FILE = "home_camera_file";

    enum AskStatus { IDLE, LOADING, RESULT }

    static final class AskUiState {
        final AskStatus status;
        final StudyRepository.AskResult result;

        private AskUiState(AskStatus status, StudyRepository.AskResult result) {
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

    enum ImageStatus { IDLE, LOADING, READY, ERROR }

    static final class ImageUiState {
        final ImageStatus status;
        final byte[] previewBytes;
        final boolean eventPending;

        private ImageUiState(
                ImageStatus status, byte[] previewBytes, boolean eventPending) {
            this.status = status;
            this.previewBytes = previewBytes;
            this.eventPending = eventPending;
        }

        static ImageUiState idle() {
            return new ImageUiState(ImageStatus.IDLE, null, false);
        }

        static ImageUiState loading() {
            return new ImageUiState(ImageStatus.LOADING, null, false);
        }

        static ImageUiState ready(byte[] previewBytes) {
            return new ImageUiState(ImageStatus.READY, previewBytes, true);
        }

        static ImageUiState error() {
            return new ImageUiState(ImageStatus.ERROR, null, true);
        }
    }

    private final SavedStateHandle savedState;
    private final StudyRepository studyRepository;
    private final ExecutorService imageExecutor =
            Executors.newSingleThreadExecutor();
    private final MutableLiveData<AskUiState> askState =
            new MutableLiveData<>(AskUiState.idle());
    private final MutableLiveData<ImageUiState> imageState =
            new MutableLiveData<>(ImageUiState.idle());

    private ImageAttachment imageAttachment;
    private int imageGeneration;
    private boolean cleared;

    public HomeUiStateViewModel(
            @NonNull Application application, SavedStateHandle savedState) {
        this(application, savedState, new StudyRepository(application));
    }

    /** Test seam for verifying operation de-duplication without network calls. */
    HomeUiStateViewModel(
            @NonNull Application application,
            SavedStateHandle savedState,
            StudyRepository studyRepository) {
        super(application);
        this.savedState = savedState;
        this.studyRepository = studyRepository;
    }

    LiveData<AskUiState> getAskState() {
        return askState;
    }

    LiveData<ImageUiState> getImageState() {
        return imageState;
    }

    boolean isAsking() {
        AskUiState current = askState.getValue();
        return current != null && current.status == AskStatus.LOADING;
    }

    boolean isPreparingImage() {
        ImageUiState current = imageState.getValue();
        return current != null && current.status == ImageStatus.LOADING;
    }

    boolean ask(long userId, String question, String subjectHint) {
        return ask(userId, question, subjectHint, "", imageAttachment);
    }

    boolean ask(long userId, String question, String subjectHint,
                String explanationStyle, ImageAttachment image) {
        if (isAsking() || isPreparingImage() || cleared) return false;
        askState.setValue(AskUiState.loading());
        studyRepository.askAsync(userId, question, subjectHint,
                explanationStyle, image, result -> {
                    if (!cleared) askState.setValue(AskUiState.result(result));
                });
        return true;
    }

    boolean ask(long userId, String question, String subjectHint,
                String explanationStyle, ImageAttachment image,
                boolean allowCache) {
        if (isAsking() || isPreparingImage() || cleared) return false;
        askState.setValue(AskUiState.loading());
        studyRepository.askAsync(userId, question, subjectHint,
                explanationStyle, image, allowCache, result -> {
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

    boolean attachImage(@NonNull Uri imageUri, boolean deleteAfterReading) {
        if (isAsking() || isPreparingImage() || cleared) return false;
        int generation = ++imageGeneration;
        imageState.setValue(ImageUiState.loading());
        imageExecutor.execute(() -> {
            ImageAttachmentPreparer.Result result = null;
            try {
                result = ImageAttachmentPreparer.prepare(
                        getApplication(), imageUri);
            } catch (IOException | RuntimeException ignored) {
                // UI receives a sanitized state; no image content is logged.
            } finally {
                if (deleteAfterReading) deletePendingCameraFile();
            }
            if (cleared || generation != imageGeneration) return;
            if (result == null) {
                imageAttachment = null;
                imageState.postValue(ImageUiState.error());
            } else {
                imageAttachment = result.attachment;
                imageState.postValue(ImageUiState.ready(result.previewBytes));
            }
        });
        return true;
    }

    ImageAttachment getImageAttachment() {
        return imageAttachment;
    }

    void consumeImageEvent() {
        ImageUiState current = imageState.getValue();
        if (current == null || !current.eventPending) return;
        imageState.setValue(new ImageUiState(
                current.status, current.previewBytes, false));
    }

    void clearImage() {
        imageGeneration++;
        imageAttachment = null;
        imageState.setValue(ImageUiState.idle());
        deletePendingCameraFile();
    }

    Uri prepareCameraCapture() throws IOException {
        deletePendingCameraFile();
        File imageDirectory =
                new File(getApplication().getCacheDir(), "question-images");
        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            throw new IOException("Unable to create image cache directory");
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

    int getStylePosition() {
        Integer value = savedState.get(KEY_STYLE);
        return value == null ? 2 : value;
    }

    boolean hasStyleSelection() {
        return savedState.contains(KEY_STYLE);
    }

    void setStylePosition(int value) {
        savedState.set(KEY_STYLE, value);
    }

    boolean isCacheAllowed() {
        Boolean value = savedState.get(KEY_ALLOW_CACHE);
        return value == null || value;
    }

    void setCacheAllowed(boolean allowed) {
        savedState.set(KEY_ALLOW_CACHE, allowed);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        imageGeneration++;
        imageExecutor.shutdownNow();
        imageAttachment = null;
        deletePendingCameraFile();
        super.onCleared();
    }
}
