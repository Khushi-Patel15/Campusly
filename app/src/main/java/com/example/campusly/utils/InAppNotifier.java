package com.example.campusly.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campusly.activities.LoginActivity;
import com.example.campusly.activities.NotificationsActivity;
import com.example.campusly.activities.SignupActivity;
import com.example.campusly.activities.SplashActivity;
import com.example.campusly.models.AppNotification;
import com.example.campusly.repositories.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

// Watches the logged-in user's notifications while the app is open (on any
// screen) and plays the shooting-star popup when new ones arrive. Registered
// once in CampuslyApp. It stops listening when the app goes to the background.
public class InAppNotifier implements Application.ActivityLifecycleCallbacks {

    // Notifications arriving within this window are shown as one popup.
    private static final long GROUP_WINDOW_MS = 1200;
    // Allows for small clock differences between the phone and the server.
    private static final long CLOCK_TOLERANCE_MS = 30_000;

    private final NotificationRepository repository = new NotificationRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppNotification> pending = new ArrayList<>();
    private final Runnable showPending = this::showPending;

    private Activity currentActivity;
    private int visibleActivities = 0;
    private ListenerRegistration listener;
    private String listeningUserId;
    private long listeningSince;
    private boolean firstSnapshotDone;

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        visibleActivities++;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        startListeningIfNeeded();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        visibleActivities--;
        if (visibleActivities <= 0) {
            stopListening();
        }
    }

    private void startListeningIfNeeded() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopListening();
            return;
        }
        if (listener != null && user.getUid().equals(listeningUserId)) {
            return;
        }

        stopListening();
        listeningUserId = user.getUid();
        listeningSince = System.currentTimeMillis();
        firstSnapshotDone = false;

        listener = repository.myNotificationsQuery(listeningUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    // The first result is just the existing list, not new arrivals.
                    if (!firstSnapshotDone) {
                        firstSnapshotDone = true;
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;
                        AppNotification n = change.getDocument().toObject(AppNotification.class,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        // Older ones can appear here when the server catches up with
                        // the phone's cached copy; only animate genuinely new ones.
                        if (n.getCreatedAt() != null && n.getCreatedAt().toDate().getTime()
                                >= listeningSince - CLOCK_TOLERANCE_MS) {
                            pending.add(n);
                        }
                    }

                    if (!pending.isEmpty()) {
                        handler.removeCallbacks(showPending);
                        handler.postDelayed(showPending, GROUP_WINDOW_MS);
                    }
                });
    }

    private void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        listeningUserId = null;
        handler.removeCallbacks(showPending);
        pending.clear();
    }

    private void showPending() {
        List<AppNotification> toShow = new ArrayList<>(pending);
        pending.clear();

        Activity activity = currentActivity;
        if (activity == null || toShow.isEmpty() || !shouldShowOn(activity)) return;
        ShootingStarPopup.show(activity, toShow);
    }

    // No popup on the Notifications screen (you're already looking at them)
    // or on the login/sign-up screens.
    private static boolean shouldShowOn(Activity activity) {
        return !(activity instanceof NotificationsActivity
                || activity instanceof SplashActivity
                || activity instanceof LoginActivity
                || activity instanceof SignupActivity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
