package com.example.aimentor.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aimentor.R;
import com.example.aimentor.activities.AnswerActivity;
import com.example.aimentor.adapters.QuestionAdapter;
import com.example.aimentor.data.Question;
import com.example.aimentor.repo.StudyRepository;
import com.example.aimentor.util.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CategoryFragment extends Fragment implements QuestionAdapter.Listener {

    private static final String STATE_SEARCH = "library_search";
    private static final String STATE_SUBJECT_POSITION = "library_subject_position";
    private static final String STATE_BOOKMARKED = "library_bookmarked";
    private static final String[] FILTER_SUBJECT_VALUES = {
            "", "Mathematics", "Science", "Programming",
            "History", "Languages", "General"
    };

    private StudyRepository studyRepository;
    private SessionManager session;
    private QuestionAdapter adapter;

    private TextInputEditText etSearch;
    private Spinner spSubjectFilter;
    private SwitchMaterial switchBookmarked;
    private RecyclerView rvHistory;
    private TextView tvEmpty, tvSuggest;
    private int refreshGeneration;
    private int suggestionGeneration;
    private final Set<Long> pendingBookmarkIds = new HashSet<>();

    public CategoryFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studyRepository = new StudyRepository(requireContext());
        session = new SessionManager(requireContext());

        etSearch = view.findViewById(R.id.etSearch);
        spSubjectFilter = view.findViewById(R.id.spSubjectFilter);
        switchBookmarked = view.findViewById(R.id.switchBookmarked);
        rvHistory = view.findViewById(R.id.rvHistory);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvSuggest = view.findViewById(R.id.tvSuggest);
        ViewCompat.setAccessibilityHeading(
                view.findViewById(R.id.tvLibraryHeading), true);

        adapter = new QuestionAdapter(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.subject_filter_choices));
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubjectFilter.setAdapter(filterAdapter);

        if (savedInstanceState != null) {
            String search = savedInstanceState.getString(STATE_SEARCH, "");
            etSearch.setText(search);
            etSearch.setSelection(search.length());
            int position = savedInstanceState.getInt(STATE_SUBJECT_POSITION, 0);
            spSubjectFilter.setSelection(Math.max(0,
                    Math.min(position, FILTER_SUBJECT_VALUES.length - 1)));
            switchBookmarked.setChecked(
                    savedInstanceState.getBoolean(STATE_BOOKMARKED, false));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { refresh(); }
            public void afterTextChanged(Editable s) { }
        });
        spSubjectFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) { refresh(); }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        switchBookmarked.setOnCheckedChangeListener((b, checked) -> refresh());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        refreshSuggestion();
    }

    private void refresh() {
        long userId = session.getCurrentUserId();
        int generation = ++refreshGeneration;
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        boolean bookmarkedOnly = switchBookmarked.isChecked();
        int subjectPosition = spSubjectFilter.getSelectedItemPosition();
        String subjectFilter = FILTER_SUBJECT_VALUES[Math.max(0,
                Math.min(subjectPosition, FILTER_SUBJECT_VALUES.length - 1))];

        StudyRepository.DataCallback<List<Question>> callback = base -> {
            if (!canRenderRefresh(generation)) return;
            renderQuestions(base, query, bookmarkedOnly, subjectFilter);
        };
        if (bookmarkedOnly) {
            studyRepository.getBookmarkedAsync(userId, callback);
        } else if (!query.isEmpty()) {
            studyRepository.searchAsync(userId, query, callback);
        } else {
            studyRepository.getHistoryAsync(userId, callback);
        }
    }

    private void refreshSuggestion() {
        int generation = ++suggestionGeneration;
        studyRepository.getProgressAsync(
                session.getCurrentUserId(), progress -> {
                    if (!canRenderSuggestion(generation)) return;
                    renderSuggestion(progress);
                });
    }

    private void renderQuestions(
            List<Question> base, String query,
            boolean bookmarkedOnly, String subjectFilter) {
        List<Question> filtered = new ArrayList<>();
        for (Question q : base) {
            if (bookmarkedOnly && !query.isEmpty() && !matchesQuery(q, query)) {
                continue;
            }
            if (!subjectFilter.isEmpty() && !subjectFilter.equals(q.subject)) {
                continue;
            }
            filtered.add(q);
        }

        adapter.setItems(filtered);
        boolean empty = filtered.isEmpty();
        boolean filtering = bookmarkedOnly || !query.isEmpty()
                || !subjectFilter.isEmpty();
        tvEmpty.setText(filtering
                ? R.string.empty_filtered_history : R.string.empty_history);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderSuggestion(StudyRepository.Progress p) {
        if (p.totalQuestions >= 3 && !"-".equals(p.topSubject)) {
            tvSuggest.setVisibility(View.VISIBLE);
            tvSuggest.setText(getString(R.string.suggested_review, p.topSubject));
        } else {
            tvSuggest.setVisibility(View.GONE);
        }
    }

    private boolean canRenderRefresh(int generation) {
        return generation == refreshGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    private boolean canRenderSuggestion(int generation) {
        return generation == suggestionGeneration
                && isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void onDestroyView() {
        refreshGeneration++;
        suggestionGeneration++;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SEARCH, etSearch.getText() == null
                ? "" : etSearch.getText().toString());
        outState.putInt(STATE_SUBJECT_POSITION,
                spSubjectFilter.getSelectedItemPosition());
        outState.putBoolean(STATE_BOOKMARKED, switchBookmarked.isChecked());
    }

    private boolean matchesQuery(Question question, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        String questionText = question.questionText == null
                ? "" : question.questionText.toLowerCase(Locale.ROOT);
        String answerText = question.answerText == null
                ? "" : question.answerText.toLowerCase(Locale.ROOT);
        return questionText.contains(normalizedQuery)
                || answerText.contains(normalizedQuery);
    }

    @Override
    public void onOpen(Question question) {
        Intent intent = new Intent(requireContext(), AnswerActivity.class);
        intent.putExtra(AnswerActivity.EXTRA_QUESTION_ID, question.id);
        startActivity(intent);
    }

    @Override
    public void onBookmarkToggle(Question question) {
        if (!pendingBookmarkIds.add(question.id)) return;
        studyRepository.toggleBookmarkAsync(
                session.getCurrentUserId(), question.id, changed -> {
                    pendingBookmarkIds.remove(question.id);
                    if (!isViewStarted()) return;
                    refresh();
                });
    }

    private boolean isViewStarted() {
        return isAdded()
                && getView() != null
                && getViewLifecycleOwner().getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
    }
}
