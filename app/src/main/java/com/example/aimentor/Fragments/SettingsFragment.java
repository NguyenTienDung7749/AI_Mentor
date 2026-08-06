package com.example.aimentor.Fragments;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.aimentor.R;
import com.example.aimentor.activities.LoginActivity;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.ReminderScheduler;
import com.example.aimentor.util.SessionManager;
import com.example.aimentor.util.SecondFactorManager;
import com.example.aimentor.util.Gamification;
import com.example.aimentor.util.DropdownAdapters;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {

    private static final String STATE_AVATAR = "settings_selected_avatar";
    private static final String STATE_ACCENT = "settings_selected_accent";
    private static final String[] EDUCATION_VALUES = {
            "Middle School", "High School", "University"
    };
    private static final String[] STYLE_VALUES = {
            "Short", "Detailed", "Step-by-step"
    };

    private UserRepository userRepository;
    private StudyRepository studyRepository;
    private SessionManager session;
    private SecondFactorManager secondFactorManager;

    private TextView tvName, tvEmail, tvLevelInfo, tvBadges, tvXpProgress;
    private ProgressBar progressLevel;
    private Spinner spLevel, spStyle;
    private LinearLayout avatarRewardGallery, themeRewardGallery;
    private CheckBox cbMath, cbScience, cbProgramming, cbHistory, cbLanguages;
    private SwitchMaterial switchReminder, switchReviewReminder, switchWeeklySummary;
    private SwitchMaterial switchLocalLeaderboard;
    private MaterialButtonToggleGroup themeModeGroup;
    private MaterialButton btnSavePrefs, btnDeleteAccount, btnReminderTime;
    private MaterialButton btnSaveAppearance;
    private MaterialButton btnExportSummary, btnClearStudyData;
    private MaterialButton btnEditProfile;
    private MaterialButton btnTwoFactor;
    private TextView tvReminderStatus, tvUnlockStatus, tvTwoFactorStatus;
    private int currentLevel = 1;
    private int loadGeneration;
    private int mutationGeneration;
    private boolean bindingReminderState;
    private String selectedAvatar;
    private String selectedAccentTheme;

    private final ActivityResultLauncher<String> enableReminderPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::handleEnableReminderPermission);
    private final ActivityResultLauncher<String> testReminderPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!canRenderReminderAction()) return;
                        if (granted) {
                            sendReminderNow();
                        } else {
                            showReminderPermissionNeeded();
                        }
                    });

    public SettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository(requireContext());
        studyRepository = new StudyRepository(requireContext());
        session = new SessionManager(requireContext());
        secondFactorManager = new SecondFactorManager(requireContext());

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvLevelInfo = view.findViewById(R.id.tvLevelInfo);
        tvBadges = view.findViewById(R.id.tvBadges);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        progressLevel = view.findViewById(R.id.progressLevel);
        spLevel = view.findViewById(R.id.spLevel);
        spStyle = view.findViewById(R.id.spStyle);
        avatarRewardGallery = view.findViewById(R.id.avatarRewardGallery);
        themeRewardGallery = view.findViewById(R.id.themeRewardGallery);
        tvUnlockStatus = view.findViewById(R.id.tvUnlockStatus);
        btnSaveAppearance = view.findViewById(R.id.btnSaveAppearance);
        cbMath = view.findViewById(R.id.cbMath);
        cbScience = view.findViewById(R.id.cbScience);
        cbProgramming = view.findViewById(R.id.cbProgramming);
        cbHistory = view.findViewById(R.id.cbHistory);
        cbLanguages = view.findViewById(R.id.cbLanguages);
        themeModeGroup = view.findViewById(R.id.themeModeGroup);
        switchReminder = view.findViewById(R.id.switchReminder);
        switchReviewReminder = view.findViewById(R.id.switchReviewReminder);
        switchWeeklySummary = view.findViewById(R.id.switchWeeklySummary);
        switchLocalLeaderboard = view.findViewById(R.id.switchLocalLeaderboard);
        btnReminderTime = view.findViewById(R.id.btnReminderTime);
        tvReminderStatus = view.findViewById(R.id.tvReminderStatus);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvSettingsHeading), true);
        btnSavePrefs = view.findViewById(R.id.btnSavePrefs);
        MaterialButton btnRemind = view.findViewById(R.id.btnRemind);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnExportSummary = view.findViewById(R.id.btnExportSummary);
        btnClearStudyData = view.findViewById(R.id.btnClearStudyData);
        btnTwoFactor = view.findViewById(R.id.btnTwoFactor);
        tvTwoFactorStatus = view.findViewById(R.id.tvTwoFactorStatus);
        selectedAvatar = savedInstanceState == null
                ? session.getSelectedAvatar()
                : savedInstanceState.getString(
                        STATE_AVATAR, session.getSelectedAvatar());
        selectedAccentTheme = savedInstanceState == null
                ? session.getSelectedAccentTheme()
                : savedInstanceState.getString(
                        STATE_ACCENT, session.getSelectedAccentTheme());

        spLevel.setAdapter(makeAdapter(Arrays.asList(
                getResources().getStringArray(R.array.education_level_choices))));
        spStyle.setAdapter(makeAdapter(Arrays.asList(
                getResources().getStringArray(R.array.explanation_style_choices))));

        bindThemeMode();

        btnSavePrefs.setOnClickListener(v -> savePrefs());
        btnSaveAppearance.setOnClickListener(v -> saveAppearance());
        btnRemind.setOnClickListener(v -> sendReminder());
        switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (bindingReminderState) return;
            if (checked) {
                requestReminderPermissionOrEnable();
            } else {
                ReminderScheduler.disable(requireContext());
                bindReminderState();
                Toast.makeText(requireContext(),
                        R.string.reminder_disabled, Toast.LENGTH_SHORT).show();
            }
        });
        switchReviewReminder.setChecked(session.isReviewReminderEnabled());
        switchReviewReminder.setOnCheckedChangeListener((button, checked) ->
                session.setReviewReminderEnabled(checked));
        switchWeeklySummary.setChecked(session.isWeeklySummaryEnabled());
        switchWeeklySummary.setOnCheckedChangeListener((button, checked) ->
                session.setWeeklySummaryEnabled(checked));
        switchLocalLeaderboard.setChecked(session.isLeaderboardOptedIn());
        switchLocalLeaderboard.setOnCheckedChangeListener((button, checked) ->
                session.setLeaderboardOptedIn(checked));
        btnReminderTime.setOnClickListener(v -> chooseReminderTime());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        btnExportSummary.setOnClickListener(v -> exportStudySummary());
        btnClearStudyData.setOnClickListener(v -> showClearStudyDataDialog());
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnTwoFactor.setOnClickListener(v -> {
            if (secondFactorManager.isEnabled(session.getCurrentUserId())) {
                showDisableTwoFactorDialog();
            } else {
                showEnableTwoFactorDialog();
            }
        });
        bindReminderState();
        refreshTwoFactorState();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindReminderState();
        refreshTwoFactorState();
        loadUser();
    }

    private ArrayAdapter<String> makeAdapter(List<String> items) {
        return DropdownAdapters.create(requireContext(), items);
    }

    private void bindThemeMode() {
        int mode = session.getThemeMode();
        int checkedId = R.id.btnThemeSystem;
        if (mode == SessionManager.THEME_MODE_LIGHT) {
            checkedId = R.id.btnThemeLight;
        } else if (mode == SessionManager.THEME_MODE_DARK) {
            checkedId = R.id.btnThemeDark;
        }
        themeModeGroup.check(checkedId);
        themeModeGroup.addOnButtonCheckedListener((group, buttonId, isChecked) -> {
            if (!isChecked) return;
            int selectedMode = SessionManager.THEME_MODE_SYSTEM;
            int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (buttonId == R.id.btnThemeLight) {
                selectedMode = SessionManager.THEME_MODE_LIGHT;
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (buttonId == R.id.btnThemeDark) {
                selectedMode = SessionManager.THEME_MODE_DARK;
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            if (session.getThemeMode() == selectedMode) return;
            session.setThemeMode(selectedMode);
            AppCompatDelegate.setDefaultNightMode(nightMode);
        });
    }

    private void loadUser() {
        if (!session.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        long userId = session.getCurrentUserId();
        int generation = ++loadGeneration;
        userRepository.getUserAsync(userId, user -> {
            if (!canRenderLoad(generation) || user == null) return;
            renderUser(user);
            studyRepository.getProgressAsync(user.id, progress -> {
                if (!canRenderLoad(generation)) return;
                renderProgress(progress);
            });
        });
    }

    private boolean canRenderLoad(int generation) {
        return generation == loadGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void renderUser(User user) {
        if (user == null) return;
        tvName.setText(user.name);
        tvEmail.setText(user.email);

        selectSpinner(spLevel, EDUCATION_VALUES, user.educationLevel);
        selectSpinner(spStyle, STYLE_VALUES, user.explanationStyle);

        String subjects = user.subjects == null ? "" : user.subjects;
        cbMath.setChecked(subjects.contains("Mathematics"));
        cbScience.setChecked(subjects.contains("Science"));
        cbProgramming.setChecked(subjects.contains("Programming"));
        cbHistory.setChecked(subjects.contains("History"));
        cbLanguages.setChecked(subjects.contains("Languages"));
    }

    private void renderProgress(StudyRepository.Progress p) {
        currentLevel = p.level;
        tvLevelInfo.setText(getString(R.string.level_summary_with_xp,
                p.level, p.levelTitle, p.xp));
        progressLevel.setMax(com.example.aimentor.util.Gamification.XP_PER_LEVEL);
        progressLevel.setProgress(p.xpIntoLevel);
        progressLevel.setContentDescription(getString(
                R.string.level_progress_description, p.level, p.xpIntoLevel,
                com.example.aimentor.util.Gamification.XP_PER_LEVEL));
        tvXpProgress.setText(getString(R.string.xp_progress,
                p.xpIntoLevel, com.example.aimentor.util.Gamification.XP_PER_LEVEL,
                p.xpToNext));
        tvBadges.setText(p.badges.isEmpty()
                ? getString(R.string.badges_empty)
                : getString(R.string.badges_summary,
                        android.text.TextUtils.join(", ", p.badges)));
        bindAppearanceRewards(p.level);
    }

    private void bindAppearanceRewards(int level) {
        if (!Gamification.allAvatars().contains(selectedAvatar)) {
            selectedAvatar = "Learner";
        }
        if (!Gamification.allAccentThemes().contains(selectedAccentTheme)) {
            selectedAccentTheme = "Indigo";
        }
        bindAvatarGallery(level);
        bindThemeGallery(level);
        tvUnlockStatus.setText(getString(
                R.string.next_appearance_unlock, Gamification.nextUnlock(level)));
    }

    private void bindAvatarGallery(int level) {
        avatarRewardGallery.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String avatar : Gamification.allAvatars()) {
            View item = inflater.inflate(
                    R.layout.item_avatar_reward, avatarRewardGallery, false);
            MaterialCardView card = item.findViewById(R.id.cardAvatarReward);
            TextView symbol = item.findViewById(R.id.tvAvatarRewardSymbol);
            TextView name = item.findViewById(R.id.tvAvatarRewardName);
            TextView status = item.findViewById(R.id.tvAvatarRewardStatus);
            int requiredLevel = Gamification.requiredLevelForAvatar(avatar);
            boolean unlocked = level >= requiredLevel;
            boolean selected = avatar.equals(selectedAvatar);
            symbol.setText(Gamification.avatarSymbol(avatar));
            name.setText(avatar);
            status.setText(rewardStatus(selected, unlocked, requiredLevel));
            configureRewardCard(card, selected, unlocked);
            card.setContentDescription(getString(R.string.reward_option_description,
                    avatar, status.getText()));
            card.setOnClickListener(v -> {
                if (!unlocked) {
                    Toast.makeText(requireContext(),
                            R.string.appearance_locked, Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedAvatar = avatar;
                bindAvatarGallery(currentLevel);
            });
            avatarRewardGallery.addView(item);
        }
    }

    private void bindThemeGallery(int level) {
        themeRewardGallery.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String theme : Gamification.allAccentThemes()) {
            View item = inflater.inflate(
                    R.layout.item_theme_reward, themeRewardGallery, false);
            MaterialCardView card = item.findViewById(R.id.cardThemeReward);
            TextView name = item.findViewById(R.id.tvThemeRewardName);
            TextView status = item.findViewById(R.id.tvThemeRewardStatus);
            TextView check = item.findViewById(R.id.tvThemeRewardCheck);
            int requiredLevel = Gamification.requiredLevelForAccentTheme(theme);
            boolean unlocked = level >= requiredLevel;
            boolean selected = theme.equals(selectedAccentTheme);
            name.setText(theme);
            status.setText(rewardStatus(selected, unlocked, requiredLevel));
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            configureRewardCard(card, selected, unlocked);
            bindThemeSwatches(item, theme);
            card.setContentDescription(getString(R.string.reward_option_description,
                    theme, status.getText()));
            card.setOnClickListener(v -> {
                if (!unlocked) {
                    Toast.makeText(requireContext(),
                            R.string.appearance_locked, Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedAccentTheme = theme;
                bindThemeGallery(currentLevel);
            });
            themeRewardGallery.addView(item);
        }
    }

    private String rewardStatus(boolean selected, boolean unlocked, int requiredLevel) {
        if (selected) return getString(R.string.reward_selected);
        if (unlocked) return getString(R.string.reward_unlocked);
        return getString(R.string.reward_locked_level, requiredLevel);
    }

    private void configureRewardCard(MaterialCardView card, boolean selected,
                                     boolean unlocked) {
        int strokeAttr = selected
                ? com.google.android.material.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutline;
        int backgroundAttr = selected
                ? com.google.android.material.R.attr.colorSecondaryContainer
                : com.google.android.material.R.attr.colorSurface;
        card.setStrokeColor(MaterialColors.getColor(card, strokeAttr));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        card.setCardBackgroundColor(MaterialColors.getColor(card, backgroundAttr));
        card.setAlpha(unlocked ? 1f : 0.64f);
        card.setSelected(selected);
    }

    private void bindThemeSwatches(View item, String theme) {
        int[] palette = themePalette(theme);
        tintSwatch(item.findViewById(R.id.swatchPrimary), palette[0]);
        tintSwatch(item.findViewById(R.id.swatchSecondary), palette[1]);
        tintSwatch(item.findViewById(R.id.swatchTertiary), palette[2]);
    }

    private int[] themePalette(String theme) {
        if ("Ocean".equals(theme)) {
            return colorResources(R.color.ocean_primary, R.color.ocean_secondary,
                    R.color.ocean_tertiary);
        }
        if ("Forest".equals(theme)) {
            return colorResources(R.color.forest_primary, R.color.forest_secondary,
                    R.color.forest_tertiary);
        }
        if ("Sunset".equals(theme)) {
            return colorResources(R.color.sunset_primary, R.color.sunset_secondary,
                    R.color.sunset_tertiary);
        }
        if ("Material You".equals(theme)) {
            return new int[] {
                    MaterialColors.getColor(requireView(),
                            com.google.android.material.R.attr.colorPrimary),
                    MaterialColors.getColor(requireView(),
                            com.google.android.material.R.attr.colorSecondary),
                    MaterialColors.getColor(requireView(),
                            com.google.android.material.R.attr.colorTertiary)
            };
        }
        return colorResources(R.color.primary, R.color.accent,
                R.color.scholar_tertiary);
    }

    private int[] colorResources(int primary, int secondary, int tertiary) {
        return new int[] {
                ContextCompat.getColor(requireContext(), primary),
                ContextCompat.getColor(requireContext(), secondary),
                ContextCompat.getColor(requireContext(), tertiary)
        };
    }

    private void tintSwatch(View swatch, int color) {
        swatch.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void saveAppearance() {
        if (!Gamification.unlockedAvatars(currentLevel).contains(selectedAvatar)
                || !Gamification.unlockedAccentThemes(currentLevel)
                        .contains(selectedAccentTheme)) {
            Toast.makeText(requireContext(),
                    R.string.appearance_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        session.setSelectedAvatar(selectedAvatar);
        session.setSelectedAccentTheme(selectedAccentTheme);
        Toast.makeText(requireContext(),
                R.string.appearance_saved, Toast.LENGTH_SHORT).show();
        requireActivity().recreate();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_AVATAR, selectedAvatar);
        outState.putString(STATE_ACCENT, selectedAccentTheme);
    }

    @Override
    public void onDestroyView() {
        loadGeneration++;
        mutationGeneration++;
        super.onDestroyView();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void selectSpinner(Spinner spinner, String[] values, String value) {
        if (value == null) return;
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void savePrefs() {
        List<String> subjects = new ArrayList<>();
        if (cbMath.isChecked()) subjects.add("Mathematics");
        if (cbScience.isChecked()) subjects.add("Science");
        if (cbProgramming.isChecked()) subjects.add("Programming");
        if (cbHistory.isChecked()) subjects.add("History");
        if (cbLanguages.isChecked()) subjects.add("Languages");

        int generation = ++mutationGeneration;
        setMutationActionsEnabled(false);
        userRepository.updatePreferencesAsync(
                session.getCurrentUserId(),
                EDUCATION_VALUES[spLevel.getSelectedItemPosition()],
                android.text.TextUtils.join(",", subjects),
                STYLE_VALUES[spStyle.getSelectedItemPosition()],
                result -> {
                    if (!canRenderMutation(generation)) return;
                    setMutationActionsEnabled(true);
                    Toast.makeText(requireContext(),
                            result.success
                                    ? getString(R.string.preferences_saved)
                                    : result.message,
                            result.success
                                    ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                            .show();
                    if (result.success) loadUser();
                });
    }

    private boolean canRenderMutation(int generation) {
        return generation == mutationGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void setMutationActionsEnabled(boolean enabled) {
        btnSavePrefs.setEnabled(enabled);
        btnDeleteAccount.setEnabled(enabled);
        btnExportSummary.setEnabled(enabled);
        btnClearStudyData.setEnabled(enabled);
        btnTwoFactor.setEnabled(enabled);
        btnEditProfile.setEnabled(enabled);
    }

    private void refreshTwoFactorState() {
        if (tvTwoFactorStatus == null || btnTwoFactor == null) return;
        boolean enabled = secondFactorManager != null
                && secondFactorManager.isEnabled(session.getCurrentUserId());
        tvTwoFactorStatus.setText(enabled
                ? R.string.two_factor_status_on : R.string.two_factor_status_off);
        btnTwoFactor.setText(enabled
                ? R.string.two_factor_disable : R.string.two_factor_enable);
    }

    private void showEnableTwoFactorDialog() {
        long userId = session.getCurrentUserId();
        if (userId <= 0L) return;
        SecondFactorManager.Setup setup = secondFactorManager.createSetup();
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_totp_setup, null, false);
        TextView setupKey = dialogView.findViewById(R.id.tvTwoFactorSetupKey);
        TextInputLayout codeLayout = dialogView.findViewById(
                R.id.tilTwoFactorCode);
        TextInputEditText codeInput = dialogView.findViewById(
                R.id.etTwoFactorCode);
        MaterialButton copySetupKey = dialogView.findViewById(
                R.id.btnCopyTwoFactorSetupKey);
        setupKey.setText(setup.displaySecret);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.two_factor_setup_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.verify_and_enable, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            copySetupKey.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                            getString(R.string.two_factor_setup_title), setup.secret));
                    Toast.makeText(requireContext(), R.string.setup_key_copied,
                            Toast.LENGTH_SHORT).show();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                SecondFactorManager.Result result = secondFactorManager.enable(
                        userId, setup, valueOf(codeInput));
                if (!result.success) {
                    codeLayout.setError(result.message);
                    codeInput.requestFocus();
                    return;
                }
                codeLayout.setError(null);
                dialog.dismiss();
                refreshTwoFactorState();
                Toast.makeText(requireContext(), result.message,
                        Toast.LENGTH_LONG).show();
            });
        });
        dialog.show();
    }

    private void showDisableTwoFactorDialog() {
        long userId = session.getCurrentUserId();
        if (userId <= 0L) return;
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_totp_code, null, false);
        TextView help = dialogView.findViewById(R.id.tvTwoFactorDialogHelp);
        help.setText(R.string.two_factor_disable_help);
        TextInputLayout codeLayout = dialogView.findViewById(
                R.id.tilTwoFactorCode);
        TextInputEditText codeInput = dialogView.findViewById(
                R.id.etTwoFactorCode);
        // The dialog layout already contains sign-in guidance; the title makes
        // the destructive direction explicit without adding another layout.
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.two_factor_disable_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.two_factor_disable, null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    SecondFactorManager.Result result = secondFactorManager.disable(
                            userId, valueOf(codeInput));
                    if (!result.success) {
                        codeLayout.setError(result.message);
                        codeInput.requestFocus();
                        return;
                    }
                    dialog.dismiss();
                    refreshTwoFactorState();
                    Toast.makeText(requireContext(), result.message,
                            Toast.LENGTH_LONG).show();
                }));
        dialog.show();
    }

    private void showEditProfileDialog() {
        long userId = session.getCurrentUserId();
        if (userId <= 0L) return;
        int generation = ++loadGeneration;
        userRepository.getUserAsync(userId, user -> {
            if (!canRenderLoad(generation) || user == null) return;
            LinearLayout form = new LinearLayout(requireContext());
            form.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(24);
            form.setPadding(pad, dp(8), pad, 0);

            TextInputLayout tilName = new TextInputLayout(requireContext());
            TextInputEditText etName = new TextInputEditText(tilName.getContext());
            etName.setHint(R.string.full_name);
            etName.setText(user.name);
            etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
            tilName.addView(etName);
            form.addView(tilName);

            TextInputLayout tilEmail = new TextInputLayout(requireContext());
            TextInputEditText etEmail = new TextInputEditText(tilEmail.getContext());
            etEmail.setHint(R.string.email);
            etEmail.setText(user.email);
            etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            tilEmail.addView(etEmail);
            form.addView(tilEmail);

            TextInputLayout tilCurrent = new TextInputLayout(requireContext());
            TextInputEditText etCurrent = new TextInputEditText(tilCurrent.getContext());
            etCurrent.setHint(R.string.current_password);
            etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            tilCurrent.addView(etCurrent);
            form.addView(tilCurrent);

            TextInputLayout tilNew = new TextInputLayout(requireContext());
            TextInputEditText etNew = new TextInputEditText(tilNew.getContext());
            etNew.setHint(R.string.new_password_optional);
            etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            tilNew.addView(etNew);
            form.addView(tilNew);

            TextInputLayout tilConfirm = new TextInputLayout(requireContext());
            TextInputEditText etConfirm = new TextInputEditText(tilConfirm.getContext());
            etConfirm.setHint(R.string.confirm_new_password);
            etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            tilConfirm.addView(etConfirm);
            form.addView(tilConfirm);

            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.edit_profile_title)
                    .setView(form)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.save_preferences, null)
                    .create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        String name2 = valueOf(etName);
                        String email2 = valueOf(etEmail);
                        String currentPwd = valueOf(etCurrent);
                        String newPwd = valueOf(etNew);
                        String confirmPwd = valueOf(etConfirm);

                        if (!newPwd.isEmpty() && !newPwd.equals(confirmPwd)) {
                            tilConfirm.setError(getString(R.string.new_passwords_do_not_match));
                            etConfirm.requestFocus();
                            return;
                        }
                        tilConfirm.setError(null);

                        int gen = ++mutationGeneration;
                        setMutationActionsEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        dialog.setCancelable(false);
                        userRepository.updateProfileAsync(
                                userId, currentPwd, name2, email2,
                                newPwd.isEmpty() ? null : newPwd, result -> {
                                    if (!canRenderMutation(gen)) return;
                                    setMutationActionsEnabled(true);
                                    if (!result.success) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(true);
                                        dialog.setCancelable(true);
                                        tilCurrent.setError(result.message);
                                        etCurrent.requestFocus();
                                        return;
                                    }
                                    dialog.dismiss();
                                    Toast.makeText(requireContext(),
                                            R.string.profile_updated,
                                            Toast.LENGTH_SHORT).show();
                                    loadUser();
                                });
                    }));
            dialog.show();
        });
    }

    private String valueOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void sendReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasRuntimePermission(requireContext())) {
            testReminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        sendReminderNow();
    }

    private void sendReminderNow() {
        boolean posted = NotificationHelper.notifyDailyReminder(
                requireContext(), getString(R.string.reminder_title),
                getString(R.string.reminder_body));
        Toast.makeText(requireContext(),
                posted ? R.string.reminder_sent
                        : R.string.reminder_permission_needed,
                Toast.LENGTH_SHORT).show();
    }

    private void requestReminderPermissionOrEnable() {
        NotificationHelper.ensureChannel(requireContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasRuntimePermission(requireContext())) {
            enableReminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableReminder();
    }

    private void handleEnableReminderPermission(boolean granted) {
        if (!canRenderReminderAction()) return;
        if (granted) {
            enableReminder();
        } else {
            setReminderSwitchChecked(false);
            ReminderScheduler.disable(requireContext());
            showReminderPermissionNeeded();
        }
    }

    private void enableReminder() {
        if (!NotificationHelper.canPostNotifications(requireContext())) {
            setReminderSwitchChecked(false);
            ReminderScheduler.disable(requireContext());
            showReminderPermissionNeeded();
            return;
        }
        ReminderScheduler.enable(requireContext(),
                session.getReminderHour(), session.getReminderMinute());
        bindReminderState();
        Toast.makeText(requireContext(),
                R.string.reminder_enabled, Toast.LENGTH_SHORT).show();
    }

    private void chooseReminderTime() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hour, minute) -> {
                    session.setReminderTime(hour, minute);
                    if (session.isReminderEnabled()) {
                        ReminderScheduler.reschedule(
                                requireContext(), hour, minute);
                    }
                    bindReminderState();
                },
                session.getReminderHour(),
                session.getReminderMinute(),
                android.text.format.DateFormat.is24HourFormat(requireContext()));
        dialog.setTitle(R.string.reminder_choose_time);
        dialog.show();
    }

    private void bindReminderState() {
        if (switchReminder == null || btnReminderTime == null
                || tvReminderStatus == null || session == null) {
            return;
        }
        boolean enabled = session.isReminderEnabled();
        setReminderSwitchChecked(enabled);
        switchReviewReminder.setEnabled(enabled);
        switchWeeklySummary.setEnabled(enabled);
        String displayTime = formattedReminderTime();
        btnReminderTime.setText(
                getString(R.string.reminder_time_button, displayTime));
        if (enabled && !NotificationHelper.canPostNotifications(requireContext())) {
            tvReminderStatus.setText(R.string.reminder_permission_needed);
        } else {
            tvReminderStatus.setText(enabled
                    ? getString(R.string.reminder_schedule_status, displayTime)
                    : getString(R.string.reminder_schedule_off));
        }
    }

    private String formattedReminderTime() {
        java.util.Calendar time = java.util.Calendar.getInstance();
        time.set(java.util.Calendar.HOUR_OF_DAY, session.getReminderHour());
        time.set(java.util.Calendar.MINUTE, session.getReminderMinute());
        return android.text.format.DateFormat
                .getTimeFormat(requireContext()).format(time.getTime());
    }

    private void setReminderSwitchChecked(boolean checked) {
        bindingReminderState = true;
        switchReminder.setChecked(checked);
        bindingReminderState = false;
    }

    private boolean canRenderReminderAction() {
        return isAdded() && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private void showReminderPermissionNeeded() {
        Toast.makeText(requireContext(),
                R.string.reminder_permission_needed, Toast.LENGTH_LONG).show();
    }

    private void logout() {
        ReminderScheduler.disable(requireContext());
        session.logout();
        navigateToLogin();
    }

    private void showDeleteAccountDialog() {
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setHint(R.string.delete_account_password_hint);
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int horizontalPadding = dp(24);
        passwordInput.setPadding(horizontalPadding, dp(8), horizontalPadding, dp(8));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setView(passwordInput)
                .setNegativeButton(R.string.delete_account_cancel, null)
                .setPositiveButton(R.string.delete_account_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = passwordInput.getText() == null
                            ? "" : passwordInput.getText().toString();
                    int generation = ++mutationGeneration;
                    passwordInput.setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(false);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    setMutationActionsEnabled(false);
                    long deletingUserId = session.getCurrentUserId();
                    userRepository.deleteAccountAsync(
                            deletingUserId, password, result -> {
                                if (result.success) {
                                    ReminderScheduler.disable(requireContext());
                                    secondFactorManager.clear(deletingUserId);
                                    session.clearCurrentUserPreferences();
                                    session.logout();
                                }
                                if (!canRenderMutation(generation)) return;
                                if (!result.success) {
                                    passwordInput.setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            .setEnabled(true);
                                    dialog.setCancelable(true);
                                    dialog.setCanceledOnTouchOutside(true);
                                    setMutationActionsEnabled(true);
                                    passwordInput.setError(result.message);
                                    return;
                                }
                                dialog.dismiss();
                                Toast.makeText(requireContext(),
                                        R.string.delete_account_success,
                                        Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            });
                }));
        dialog.show();
    }

    private void exportStudySummary() {
        int generation = ++mutationGeneration;
        setMutationActionsEnabled(false);
        studyRepository.getProgressAsync(
                session.getCurrentUserId(), progress -> {
                    if (!canRenderMutation(generation)) return;
                    setMutationActionsEnabled(true);
                    StringBuilder summary = new StringBuilder();
                    summary.append(getString(R.string.export_summary_heading))
                            .append("\n")
                            .append(java.text.DateFormat.getDateTimeInstance()
                                    .format(new java.util.Date()))
                            .append("\n\n")
                            .append(getString(R.string.export_summary_level,
                                    progress.level, progress.levelTitle,
                                    progress.xp))
                            .append("\n")
                            .append(getString(R.string.export_summary_activity,
                                    progress.totalQuestions,
                                    progress.quizzesCompleted,
                                    progress.accuracyPercent))
                            .append("\n")
                            .append(getString(R.string.export_summary_review,
                                    progress.dueReviewCount,
                                    progress.learningCount,
                                    progress.masteredCount))
                            .append("\n")
                            .append(getResources().getQuantityString(
                                    R.plurals.current_streak_days,
                                    progress.currentStreakDays,
                                    progress.currentStreakDays));
                    if (!progress.subjectMastery.isEmpty()) {
                        summary.append("\n\n")
                                .append(getString(
                                        R.string.export_summary_subjects));
                        for (java.util.Map.Entry<String,
                                StudyRepository.SubjectMastery> entry
                                : progress.subjectMastery.entrySet()) {
                            summary.append("\n- ")
                                    .append(entry.getKey())
                                    .append(": ")
                                    .append(entry.getValue().label)
                                    .append(" (")
                                    .append(entry.getValue().accuracyPercent)
                                    .append("%)");
                        }
                    }
                    Intent share = new Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT,
                                    getString(R.string.export_summary_heading))
                            .putExtra(Intent.EXTRA_TEXT, summary.toString());
                    startActivity(Intent.createChooser(
                            share, getString(R.string.export_study_summary)));
                });
    }

    private void showClearStudyDataDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_study_data_title)
                .setMessage(R.string.clear_study_data_message)
                .setNegativeButton(R.string.delete_account_cancel, null)
                .setPositiveButton(R.string.clear_study_data_confirm,
                        (dialog, which) -> clearStudyData())
                .show();
    }

    private void clearStudyData() {
        int generation = ++mutationGeneration;
        setMutationActionsEnabled(false);
        studyRepository.clearStudyDataAsync(
                session.getCurrentUserId(), cleared -> {
                    if (!canRenderMutation(generation)) return;
                    setMutationActionsEnabled(true);
                    Toast.makeText(requireContext(),
                            cleared
                                    ? R.string.clear_study_data_success
                                    : R.string.clear_study_data_failed,
                            cleared ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                            .show();
                    if (cleared) loadUser();
                });
    }

    private void navigateToLogin() {
        Intent login = new Intent(requireContext(), LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        requireActivity().finish();
    }
}
