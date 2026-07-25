package com.example.aimentor.util;

import android.content.Context;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import com.example.aimentor.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates selection-only dropdowns. Unlike the default ArrayAdapter filter,
 * these menus always show the complete catalog after a value is selected.
 */
public final class DropdownAdapters {

    private DropdownAdapters() { }

    public static void bind(
            @NonNull AutoCompleteTextView view, @NonNull String[] items) {
        ArrayAdapter<String> adapter = create(view.getContext(), items);
        view.setThreshold(0);
        view.setAdapter(adapter);
        view.setOnClickListener(ignored -> view.showDropDown());
    }

    public static ArrayAdapter<String> create(
            @NonNull Context context, @NonNull String[] items) {
        return create(context, Arrays.asList(items));
    }

    public static ArrayAdapter<String> create(
            @NonNull Context context, @NonNull List<String> items) {
        ArrayAdapter<String> adapter = new CompleteCatalogAdapter(
                context, new ArrayList<>(items));
        adapter.setDropDownViewResource(R.layout.item_dropdown_option);
        return adapter;
    }

    private static final class CompleteCatalogAdapter
            extends ArrayAdapter<String> {

        private final List<String> catalog;
        private final Filter catalogFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = catalog;
                results.count = catalog.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(
                    CharSequence constraint, FilterResults results) {
                if (results.values instanceof List) {
                    notifyDataSetChanged();
                }
            }
        };

        CompleteCatalogAdapter(Context context, List<String> items) {
            super(context, R.layout.item_dropdown_option,
                    android.R.id.text1, items);
            catalog = items;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return catalogFilter;
        }
    }
}
