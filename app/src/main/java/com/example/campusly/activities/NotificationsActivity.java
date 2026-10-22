package com.example.campusly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusly.R;
import com.example.campusly.adapters.NotificationAdapter;
import com.example.campusly.databinding.ActivityNotificationsBinding;
import com.example.campusly.models.AppNotification;
import com.example.campusly.repositories.NotificationRepository;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.UiUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

// Live list of likes and comments on your posts. Tap one to open the post,
// long-press to delete it.
public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.Listener {

    private static final String TAG = "NotificationsActivity";

    private ActivityNotificationsBinding binding;
    private final NotificationRepository repository = new NotificationRepository();
    private NotificationAdapter adapter;
    private ListenerRegistration listener;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        userId = user.getUid();

        adapter = new NotificationAdapter(this);
        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.notificationsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter == null) return;

        listener = repository.myNotificationsQuery(userId).addSnapshotListener((snapshots, error) -> {
            binding.notificationsProgress.setVisibility(View.GONE);
            if (error != null) {
                Log.e(TAG, "Loading notifications failed", error);
                showMessage(R.string.notifications_error);
                return;
            }

            List<AppNotification> notifications = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                AppNotification notification = doc.toObject(AppNotification.class,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                if (notification != null) {
                    notifications.add(notification);
                }
            }

            adapter.setNotifications(notifications);
            if (notifications.isEmpty()) {
                showMessage(R.string.notifications_empty);
            } else {
                binding.notificationsEmptyText.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    private void showMessage(int messageRes) {
        binding.notificationsEmptyText.setText(messageRes);
        binding.notificationsEmptyText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.EXTRA_POST_ID, notification.getPostId());
        startActivity(intent);
    }

    @Override
    public void onNotificationLongClick(AppNotification notification) {
        UiUtils.confirm(this, R.string.notification_delete_title, R.string.delete_post_message,
                R.string.delete_post_confirm, () ->
                        repository.delete(userId, notification.getNotificationId())
                                .addOnFailureListener(e ->
                                        UiUtils.toast(this, R.string.admin_action_failed)));
    }
}
