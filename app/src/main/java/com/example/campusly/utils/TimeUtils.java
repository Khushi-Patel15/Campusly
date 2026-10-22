package com.example.campusly.utils;

import android.content.Context;
import android.text.format.DateUtils;

import com.example.campusly.R;
import com.google.firebase.Timestamp;

public final class TimeUtils {

    private TimeUtils() {
    }

    // "Just now", "5 minutes ago", "2 hours ago", "Yesterday", ...
    public static CharSequence relative(Context context, Timestamp time) {
        if (time == null) {
            return context.getString(R.string.post_time_now);
        }
        long then = time.toDate().getTime();
        long now = System.currentTimeMillis();
        if (now - then < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.post_time_now);
        }
        return DateUtils.getRelativeTimeSpanString(then, now, DateUtils.MINUTE_IN_MILLIS);
    }
}