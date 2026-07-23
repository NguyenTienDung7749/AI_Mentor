package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.aimentor.R;
import com.example.aimentor.activities.LoginActivity;
import com.example.aimentor.data.User;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.repo.UserRepository;
import com.example.aimentor.util.NotificationHelper;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {

    private UserRepository userRepository;
    private StudyRepository studyRepository;
    private SessionManager session;

    private TextView tvName, tvEmail, tvLevelInfo, tvBadges;
    private Spinner spLevel, spStyle;
    private CheckBox cbMath, cbScience, cbProgramming, cbHistory, cbLanguages;
    private SwitchMaterial switchTheme;

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

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvLevelInfo = view.findViewById(R.id.tvLevelInfo);
        tvBadges = view.findViewById(R.id.tvBadges);
        spLevel = view.findViewById(R.id.spLevel);
        spStyle = view.findViewById(R.id.spStyle);
        cbMath = view.findViewById(R.id.cbMath);
        cbScience = view.findViewById(R.id.cbScience);
        cbProgramming = view.findViewById(R.id.cbProgramming);
        cbHistory = view.findViewById(R.id.cbHistory);
        cbLanguages = view.findViewById(R.id.cbLanguages);
        switchTheme = view.findViewById(R.id.switchTheme);
        MaterialButton btnSavePrefs = view.findViewById(R.id.btnSavePrefs);
        MaterialButton btnRemind = view.findViewById(R.id.btnRemind);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        spLevel.setAdapter(makeAdapter(Arrays.asList("Middle School", "High School", "University")));
        spStyle.setAdapter(makeAdapter(Arrays.asList("Short", "Detailed", "Step-by-step")));

        loadUser();

        switchTheme.setChecked(session.isDarkTheme());
        switchTheme.setOnCheckedChangeListener((b, checked) -> {
            session.setDarkTheme(checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnSavePrefs.setOnClickListener(v -> savePrefs());
        btnRemind.setOnClickListener(v -> sendReminder());
        btnLogout.setOnClickListener(v -> logout());
    }

    private ArrayAdapter<String> makeAdapter(List<String> items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void loadUser() {
        User user = userRepository.getUser(session.getCurrentUserId());
        if (user == null) return;
        tvName.setText(user.name);
        tvEmail.setText(user.email);

        StudyRepository.Progress p = studyRepository.getProgress(user.id);
        tvLevelInfo.setText("Level " + p.level + " - " + p.levelTitle + " (" + p.xp + " XP)");
        tvBadges.setText(p.badges.isEmpty()
                ? "Badges: none yet - keep learning!"
                : "Badges: " + android.text.TextUtils.join(", ", p.badges));

        selectSpinner(spLevel, user.educationLevel);
        selectSpinner(spStyle, user.explanationStyle);

        String subjects = user.subjects == null ? "" : user.subjects;
        cbMath.setChecked(subjects.contains("Mathematics"));
        cbScience.setChecked(subjects.contains("Science"));
        cbProgramming.setChecked(subjects.contains("Programming"));
        cbHistory.setChecked(subjects.contains("History"));
        cbLanguages.setChecked(subjects.contains("Languages"));
    }

    private void selectSpinner(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i))) {
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

        userRepository.updatePreferences(session.getCurrentUserId(),
                (String) spLevel.getSelectedItem(),
                android.text.TextUtils.join(",", subjects),
                (String) spStyle.getSelectedItem());
        Toast.makeText(requireContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }

    private void sendReminder() {
        boolean posted = NotificationHelper.notify(requireContext(),
                "Time to review!",
                "Revisit your bookmarked answers and try a quick practice quiz today.");
        Toast.makeText(requireContext(),
                posted ? "Reminder sent to your notifications."
                        : "Enable notifications to receive reminders.",
                Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        session.logout();
        Intent login = new Intent(requireContext(), LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        requireActivity().finish();
    }
}
