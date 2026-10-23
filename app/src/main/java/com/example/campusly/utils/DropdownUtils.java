package com.example.campusly.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.campusly.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Shared setup for the School and Year dropdowns on Sign Up and Edit Profile.
// The first row is a "Select ..." placeholder that doesn't count as a choice.
public final class DropdownUtils {

    private DropdownUtils() {
    }

    public static void setUp(Context context, Spinner spinner, int optionsArrayRes, int placeholderRes) {
        List<String> rows = new ArrayList<>();
        rows.add(context.getString(placeholderRes));
        rows.addAll(Arrays.asList(context.getResources().getStringArray(optionsArrayRes)));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_spinner, rows);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    // Returns null while the placeholder is still selected.
    public static String getSelected(Spinner spinner) {
        if (spinner.getSelectedItemPosition() <= 0) {
            return null;
        }
        return (String) spinner.getSelectedItem();
    }

    // Selects a saved value. Values from before the dropdowns existed (typed
    // by hand) aren't in the list, so the placeholder stays selected and the
    // user has to pick one.
    public static void select(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 1; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }
}
