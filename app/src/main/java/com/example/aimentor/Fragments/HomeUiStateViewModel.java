package com.example.aimentor.Fragments;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/**
 * Keeps unsent Home screen input across configuration changes without writing
 * a private draft to Room or SharedPreferences.
 */
public class HomeUiStateViewModel extends ViewModel {
    private static final String KEY_DRAFT = "home_draft";
    private static final String KEY_SUBJECT = "home_subject";
    private static final String KEY_OCR_STATUS = "home_ocr_status";
    private static final String KEY_OCR_VISIBLE = "home_ocr_visible";

    private final SavedStateHandle state;

    public HomeUiStateViewModel(SavedStateHandle state) {
        this.state = state;
    }

    String getQuestionDraft() {
        String value = state.get(KEY_DRAFT);
        return value == null ? "" : value;
    }

    void setQuestionDraft(String value) {
        state.set(KEY_DRAFT, value == null ? "" : value);
    }

    int getSubjectPosition() {
        Integer value = state.get(KEY_SUBJECT);
        return value == null ? 0 : value;
    }

    void setSubjectPosition(int value) {
        state.set(KEY_SUBJECT, value);
    }

    String getOcrStatus() {
        String value = state.get(KEY_OCR_STATUS);
        return value == null ? "" : value;
    }

    void setOcrStatus(CharSequence value) {
        state.set(KEY_OCR_STATUS, value == null ? "" : value.toString());
    }

    boolean isOcrStatusVisible() {
        Boolean value = state.get(KEY_OCR_VISIBLE);
        return value != null && value;
    }

    void setOcrStatusVisible(boolean value) {
        state.set(KEY_OCR_VISIBLE, value);
    }
}
