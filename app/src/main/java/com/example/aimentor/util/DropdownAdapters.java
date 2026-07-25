package com.example.aimentor.util;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.example.aimentor.R;

import java.util.Arrays;
import java.util.List;

/** Creates consistently padded, vertically centred dropdown rows across the app. */
public final class DropdownAdapters {

    private DropdownAdapters() { }

    public static ArrayAdapter<String> create(
            @NonNull Context context, @NonNull String[] items) {
        return create(context, Arrays.asList(items));
    }

    public static ArrayAdapter<String> create(
            @NonNull Context context, @NonNull List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, R.layout.item_dropdown_option,
                android.R.id.text1, items);
        adapter.setDropDownViewResource(R.layout.item_dropdown_option);
        return adapter;
    }
}
