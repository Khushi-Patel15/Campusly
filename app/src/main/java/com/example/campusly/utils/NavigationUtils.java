package com.example.campusly.utils;

import android.app.Activity;
import android.content.Intent;

public final class NavigationUtils {

    private NavigationUtils() {
    }

    // Opens a screen and removes everything behind it, so Back can't return
    // to the login screen after logging in (or to the app after logging out).
    public static void openAndClear(Activity from, Class<? extends Activity> to) {
        Intent intent = new Intent(from, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        from.finish();
    }
}