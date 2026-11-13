package com.example.campusly;

import android.app.Application;

import com.example.campusly.utils.InAppNotifier;

// Runs once when the app starts, before any screen. Used to switch on the
// shooting-star notification popups for every screen in the app.
// Registered in AndroidManifest.xml with android:name=".CampuslyApp".
public class CampuslyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new InAppNotifier());
    }
}
