package com.example.aimentor.activities;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.aimentor.repo.StudyRepository;

/**
 * Retains asynchronous quiz generation across configuration changes and
 * prevents a rotation from starting a duplicate request.
 */
public class QuizViewModel extends AndroidViewModel {

    enum LoadStatus { IDLE, LOADING, RESULT }

    static final class LoadUiState {
        final LoadStatus status;
        final StudyRepository.QuizLoadResult result;

        private LoadUiState(
                LoadStatus status, StudyRepository.QuizLoadResult result) {
            this.status = status;
            this.result = result;
        }

        static LoadUiState idle() {
            return new LoadUiState(LoadStatus.IDLE, null);
        }

        static LoadUiState loading() {
            return new LoadUiState(LoadStatus.LOADING, null);
        }

        static LoadUiState result(StudyRepository.QuizLoadResult result) {
            return new LoadUiState(LoadStatus.RESULT, result);
        }
    }

    private final StudyRepository studyRepository;
    private final MutableLiveData<LoadUiState> loadState =
            new MutableLiveData<>(LoadUiState.idle());
    private boolean cleared;

    public QuizViewModel(@NonNull Application application) {
        this(application, new StudyRepository(application));
    }

    /** Test seam for checking request deduplication without calling an AI. */
    QuizViewModel(
            @NonNull Application application,
            StudyRepository studyRepository) {
        super(application);
        this.studyRepository = studyRepository;
    }

    LiveData<LoadUiState> getLoadState() {
        return loadState;
    }

    boolean loadQuiz(
            long userId, String subject, String difficulty, int count) {
        LoadUiState current = loadState.getValue();
        if (cleared
                || (current != null
                && current.status == LoadStatus.LOADING)) {
            return false;
        }
        loadState.setValue(LoadUiState.loading());
        studyRepository.generateQuizAsync(
                userId, subject, difficulty, count, result -> {
                    if (!cleared) {
                        loadState.setValue(LoadUiState.result(result));
                    }
                });
        return true;
    }

    @Override
    protected void onCleared() {
        cleared = true;
        super.onCleared();
    }
}
