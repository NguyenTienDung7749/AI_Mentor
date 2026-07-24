package com.example.aimentor.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.aimentor.repo.StudyRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class QuizViewModelInstrumentedTest {

    @Test
    public void loadWhileLoading_startsOnlyOneRepositoryRequest() {
        Application application = (Application)
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getApplicationContext();
        CountingStudyRepository repository =
                new CountingStudyRepository(application);
        QuizViewModel viewModel =
                new QuizViewModel(application, repository);
        AtomicBoolean firstStarted = new AtomicBoolean();
        AtomicBoolean secondStarted = new AtomicBoolean();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            firstStarted.set(viewModel.loadQuiz(
                    1L, "Science", "Intermediate", 5));
            secondStarted.set(viewModel.loadQuiz(
                    1L, "Science", "Intermediate", 5));
        });

        assertTrue(firstStarted.get());
        assertFalse(secondStarted.get());
        assertEquals(1, repository.quizCount);
    }

    @Test
    public void recordWhileSaving_startsOnlyOneRepositoryWrite() {
        Application application = (Application)
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getApplicationContext();
        CountingStudyRepository repository =
                new CountingStudyRepository(application);
        QuizViewModel viewModel =
                new QuizViewModel(application, repository);
        AtomicBoolean firstStarted = new AtomicBoolean();
        AtomicBoolean secondStarted = new AtomicBoolean();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            firstStarted.set(viewModel.recordQuiz(
                    1L, "Science", 4, 5));
            secondStarted.set(viewModel.recordQuiz(
                    1L, "Science", 4, 5));
        });

        assertTrue(firstStarted.get());
        assertFalse(secondStarted.get());
        assertEquals(1, repository.recordCount);
    }

    private static final class CountingStudyRepository
            extends StudyRepository {
        int quizCount;
        int recordCount;

        CountingStudyRepository(Application application) {
            super(application);
        }

        @Override
        public void generateQuizAsync(
                long userId, String requestedSubject,
                String requestedDifficulty, int count,
                @androidx.annotation.NonNull QuizLoadCallback callback) {
            quizCount++;
        }

        @Override
        public void recordQuizAsync(
                long userId, String subject, int correct, int total,
                @androidx.annotation.NonNull
                DataCallback<QuizResult> callback) {
            recordCount++;
        }
    }
}
