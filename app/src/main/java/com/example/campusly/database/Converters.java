package com.example.campusly.database;

import android.text.TextUtils;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Room can only store simple values (text, numbers), so a list of user ids is
// stored as one comma-separated string. Firebase user ids never contain commas.
public class Converters {

    @TypeConverter
    public static String fromList(List<String> list) {
        return list == null ? "" : TextUtils.join(",", list);
    }

    @TypeConverter
    public static List<String> toList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }
}