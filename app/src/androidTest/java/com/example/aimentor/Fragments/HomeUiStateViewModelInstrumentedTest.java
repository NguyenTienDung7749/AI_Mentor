package com.example.aimentor.Fragments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.aimentor.repo.StudyRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class HomeUiStateViewModelInstrumentedTest {

    @Test
    public void askWhileLoading_startsOnlyOneRepositoryRequest() {
        Application application = application();
        CountingStudyRepository repository =
                new CountingStudyRepository(application);
        HomeUiStateViewModel viewModel = new HomeUiStateViewModel(
                application, new SavedStateHandle(), repository);
        AtomicBoolean firstStarted = new AtomicBoolean();
        AtomicBoolean secondStarted = new AtomicBoolean();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            firstStarted.set(viewModel.ask(
                    1L, "What is gravity?", "Science"));
            secondStarted.set(viewModel.ask(
                    1L, "Explain it again", "Science"));
        });

        assertTrue(firstStarted.get());
        assertFalse(secondStarted.get());
        assertEquals(1, repository.askCount);
        assertTrue(viewModel.isAsking());
    }

    @Test
    public void savedStateHandle_restoresDraftAndSubject() {
        Application application = application();
        SavedStateHandle state = new SavedStateHandle();
        HomeUiStateViewModel first = new HomeUiStateViewModel(
                application, state, new CountingStudyRepository(application));

        first.setQuestionDraft("A retained draft");
        first.setSubjectPosition(3);
        HomeUiStateViewModel restored = new HomeUiStateViewModel(
                application, state, new CountingStudyRepository(application));

        assertEquals("A retained draft", restored.getQuestionDraft());
        assertEquals(3, restored.getSubjectPosition());
    }

    private Application application() {
        return (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getApplicationContext();
    }

    private static final class CountingStudyRepository
            extends StudyRepository {
        int askCount;

        CountingStudyRepository(Application application) {
            super(application);
        }

        @Override
        public void askAsync(
                long userId, String questionText, String subjectHint,
                @androidx.annotation.NonNull AskCallback callback) {
            askCount++;
        }
    }
}
