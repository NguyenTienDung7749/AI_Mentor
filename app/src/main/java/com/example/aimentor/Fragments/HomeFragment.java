package com.example.aimentor.Fragments;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.ai.ImageAttachment;
import com.example.aimentor.ai.SubjectClassifier;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.DropdownAdapters;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private static final String[] SUBJECT_VALUES = {
            "Auto", SubjectClassifier.MATH, SubjectClassifier.SCIENCE,
            SubjectClassifier.PROGRAMMING, SubjectClassifier.HISTORY,
            SubjectClassifier.LANGUAGES, SubjectClassifier.GENERAL
    };
    private static final String[] STYLE_VALUES = {
            "Short", "Detailed", "Step-by-step"
    };

    private StudyRepository studyRepository;
    private UserRepository userRepository;
    private SessionManager session;
    private HomeUiStateViewModel uiState;

    private TextView tvGreeting, tvLevelTitle, tvXp, tvInsights, tvHomeAvatar;
    private TextView tvQuestionsStat, tvAccuracyStat, tvQuizStat;
    private ProgressBar progressXp;
    private ProgressBar progressAsk;
    private AutoCompleteTextView spSubject;
    private TextInputEditText etQuestion;
    private MaterialButton btnAsk, btnChooseImage, btnTakePhoto;
    private MaterialButtonToggleGroup responseStyleGroup;
    private MaterialCardView cardImageAttachment;
    private ImageView ivImageAttachment;
    private ImageButton btnRemoveImage;
    private ProgressBar progressImage;
    private TextView tvAskStatus, tvImageStatus;
    private int refreshGeneration;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && uiState != null) {
                    uiState.attachImage(uri, false);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (uiState == null) return;
                Uri pendingCameraUri = uiState.getPendingCameraUri();
                if (success && pendingCameraUri != null) {
                    uiState.attachImage(pendingCameraUri, true);
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
        tvHomeAvatar = view.findViewById(R.id.tvHomeAvatar);
        tvLevelTitle = view.findViewById(R.id.tvLevelTitle);
        tvXp = view.findViewById(R.id.tvXp);
        tvQuestionsStat = view.findViewById(R.id.tvQuestionsStat);
        tvAccuracyStat = view.findViewById(R.id.tvAccuracyStat);
        tvQuizStat = view.findViewById(R.id.tvQuizStat);
        tvInsights = view.findViewById(R.id.tvInsights);
        progressXp = view.findViewById(R.id.progressXp);
        spSubject = view.findViewById(R.id.spSubject);
        etQuestion = view.findViewById(R.id.etQuestion);
        btnAsk = view.findViewById(R.id.btnAsk);
        btnChooseImage = view.findViewById(R.id.btnChooseImage);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        responseStyleGroup = view.findViewById(R.id.responseStyleGroup);
        cardImageAttachment = view.findViewById(R.id.cardImageAttachment);
        ivImageAttachment = view.findViewById(R.id.ivImageAttachment);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        progressAsk = view.findViewById(R.id.progressAsk);
        progressImage = view.findViewById(R.id.progressImage);
        tvAskStatus = view.findViewById(R.id.tvAskStatus);
        tvImageStatus = view.findViewById(R.id.tvImageStatus);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvAskHeading), true);

        String[] subjectLabels = getResources().getStringArray(R.array.subject_choices);
        DropdownAdapters.bind(spSubject, subjectLabels);

        String restoredDraft = uiState.getQuestionDraft();
        etQuestion.setText(restoredDraft);
        etQuestion.setSelection(restoredDraft.length());
        int restoredSubject = Math.max(0,
                Math.min(uiState.getSubjectPosition(), SUBJECT_VALUES.length - 1));
        spSubject.setText(subjectLabels[restoredSubject], false);
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
        spSubject.setOnItemClickListener((parent, selectedView, position, id) ->
                uiState.setSubjectPosition(position));

        int restoredStyle = uiState.getStylePosition();
        checkStyleButton(restoredStyle);
        responseStyleGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (isChecked) uiState.setStylePosition(
                            stylePositionForButton(checkedId));
                });

        btnAsk.setOnClickListener(v -> ask());
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnRemoveImage.setOnClickListener(v -> uiState.clearImage());
        uiState.getAskState().observe(
                getViewLifecycleOwner(), this::renderAskState);
        uiState.getImageState().observe(
                getViewLifecycleOwner(), this::renderImageState);
    }

    private void chooseImage() {
        if (uiState.isAsking() || uiState.isPreparingImage()) return;
        imagePicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void takePhoto() {
        if (uiState.isAsking() || uiState.isPreparingImage()) return;
        try {
            cameraLauncher.launch(uiState.prepareCameraCapture());
        } catch (RuntimeException | IOException error) {
            uiState.deletePendingCameraFile();
            Toast.makeText(requireContext(), R.string.camera_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateInputActions() {
        boolean enabled = !uiState.isAsking() && !uiState.isPreparingImage();
        btnAsk.setEnabled(enabled);
        btnChooseImage.setEnabled(enabled);
        btnTakePhoto.setEnabled(enabled);
        btnRemoveImage.setEnabled(enabled);
        for (int i = 0; i < responseStyleGroup.getChildCount(); i++) {
            responseStyleGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    private void ask() {
        if (uiState.isAsking() || uiState.isPreparingImage()) return;

        String question = etQuestion.getText() == null ? "" : etQuestion.getText().toString().trim();
        int selectedSubject = uiState.getSubjectPosition();
        String subjectHint = SUBJECT_VALUES[Math.max(0,
                Math.min(selectedSubject, SUBJECT_VALUES.length - 1))];

        int stylePosition = Math.max(0,
                Math.min(uiState.getStylePosition(), STYLE_VALUES.length - 1));
        ImageAttachment image = uiState.getImageAttachment();
        uiState.ask(session.getCurrentUserId(), question, subjectHint,
                STYLE_VALUES[stylePosition], image);
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
            tvAskStatus.setTextColor(MaterialColors.getColor(
                    tvAskStatus, com.google.android.material.R.attr.colorError));
            tvAskStatus.setVisibility(View.VISIBLE);
            tvAskStatus.announceForAccessibility(message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            return;
        }
        etQuestion.setText("");
        uiState.setQuestionDraft("");
        uiState.clearImage();
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
            tvAskStatus.setTextColor(MaterialColors.getColor(
                    tvAskStatus,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
    }

    private void renderImageState(HomeUiStateViewModel.ImageUiState state) {
        HomeUiStateViewModel.ImageStatus status = state == null
                ? HomeUiStateViewModel.ImageStatus.IDLE : state.status;
        progressImage.setVisibility(
                status == HomeUiStateViewModel.ImageStatus.LOADING
                        ? View.VISIBLE : View.GONE);
        switch (status) {
            case LOADING:
                cardImageAttachment.setVisibility(View.GONE);
                tvImageStatus.setText(R.string.image_preparing);
                tvImageStatus.setVisibility(View.VISIBLE);
                break;
            case READY:
                if (state != null && state.previewBytes != null) {
                    ivImageAttachment.setImageBitmap(
                            BitmapFactory.decodeByteArray(state.previewBytes,
                                    0, state.previewBytes.length));
                    cardImageAttachment.setVisibility(View.VISIBLE);
                }
                tvImageStatus.setText(R.string.image_ready);
                tvImageStatus.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                cardImageAttachment.setVisibility(View.GONE);
                tvImageStatus.setText(R.string.image_failed);
                tvImageStatus.setVisibility(View.VISIBLE);
                break;
            case IDLE:
            default:
                cardImageAttachment.setVisibility(View.GONE);
                ivImageAttachment.setImageDrawable(null);
                tvImageStatus.setVisibility(View.GONE);
                break;
        }
        updateInputActions();

        if (state == null || !state.eventPending) return;
        if (status == HomeUiStateViewModel.ImageStatus.READY) {
            etQuestion.requestFocus();
        } else if (status == HomeUiStateViewModel.ImageStatus.ERROR) {
            Toast.makeText(requireContext(), R.string.image_failed,
                    Toast.LENGTH_LONG).show();
        }
        uiState.consumeImageEvent();
    }

    @Override
    public void onDestroyView() {
        refreshGeneration++;
        captureUiState();
        super.onDestroyView();
    }

    private void captureUiState() {
        if (uiState == null || etQuestion == null) return;
        uiState.setQuestionDraft(etQuestion.getText() == null
                ? "" : etQuestion.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        long userId = session.getCurrentUserId();
        int generation = ++refreshGeneration;
        userRepository.getUserAsync(userId, user -> {
            if (!canRenderRefresh(generation)) return;
            renderUser(user);
        });
        studyRepository.getProgressAsync(userId, progress -> {
            if (!canRenderRefresh(generation)) return;
            renderProgress(progress);
        });
    }

    private boolean canRenderRefresh(int generation) {
        return generation == refreshGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void renderUser(User user) {
        String name = (user != null && user.name != null && !user.name.isEmpty())
                ? user.name : getString(R.string.default_student_name);
        tvGreeting.setText(getString(R.string.home_greeting, name));
        String avatar = session.getSelectedAvatar();
        tvHomeAvatar.setText(Gamification.avatarSymbol(avatar));
        tvHomeAvatar.setContentDescription(getString(
                R.string.selected_avatar_description, avatar));
        if (user != null && !uiState.hasStyleSelection()) {
            int position = stylePosition(user.explanationStyle);
            uiState.setStylePosition(position);
            checkStyleButton(position);
        }
    }

    private int stylePosition(String style) {
        if (style != null && style.equalsIgnoreCase("Short")) return 0;
        if (style != null && style.equalsIgnoreCase("Detailed")) return 1;
        return 2;
    }

    private int stylePositionForButton(int buttonId) {
        if (buttonId == R.id.btnStyleShort) return 0;
        if (buttonId == R.id.btnStyleDetailed) return 1;
        return 2;
    }

    private void checkStyleButton(int position) {
        int safe = Math.max(0, Math.min(position, STYLE_VALUES.length - 1));
        responseStyleGroup.check(safe == 0
                ? R.id.btnStyleShort
                : safe == 1 ? R.id.btnStyleDetailed : R.id.btnStyleStep);
    }

    private void renderProgress(StudyRepository.Progress p) {
        tvLevelTitle.setText(getString(
                R.string.level_summary, p.level, p.levelTitle));
        progressXp.setProgress(p.xpIntoLevel);
        progressXp.setContentDescription(getString(R.string.level_progress_description,
                p.level, p.xpIntoLevel,
                com.example.aimentor.util.Gamification.XP_PER_LEVEL));
        tvXp.setText(getString(R.string.home_xp_progress, p.xp, p.xpToNext));
        tvQuestionsStat.setText(getString(
                R.string.stat_number, p.totalQuestions));
        tvAccuracyStat.setText(getString(
                R.string.stat_percent, p.accuracyPercent));
        tvQuizStat.setText(getString(
                R.string.stat_number, p.quizzesCompleted));

        StringBuilder insights = new StringBuilder();
        for (String s : p.insights) insights.append("\u2022 ").append(s).append("\n");
        tvInsights.setText(insights.length() == 0
                ? getString(R.string.home_no_insight)
                : insights.toString().trim());
    }
}
