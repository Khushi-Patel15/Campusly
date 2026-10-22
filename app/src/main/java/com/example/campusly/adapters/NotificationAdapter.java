package com.example.campusly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusly.R;
import com.example.campusly.databinding.ItemNotificationBinding;
import com.example.campusly.models.AppNotification;
import com.example.campusly.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface Listener {
        void onNotificationClick(AppNotification notification);

        void onNotificationLongClick(AppNotification notification);
    }

    private final List<AppNotification> notifications = new ArrayList<>();
    private final Listener listener;

    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<AppNotification> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);
        ItemNotificationBinding binding = holder.binding;
        Context context = binding.getRoot().getContext();

        boolean isComment = AppNotification.TYPE_COMMENT.equals(notification.getType());
        binding.notificationIconText.setText(isComment ? "💬" : "❤️");
        binding.notificationTitleText.setText(context.getString(
                isComment ? R.string.notification_comment : R.string.notification_like,
                notification.getFromName()));
        binding.notificationPreviewText.setText(notification.getPreview());
        binding.notificationTimeText.setText(
                TimeUtils.relative(context, notification.getCreatedAt()));

        binding.getRoot().setOnClickListener(v -> listener.onNotificationClick(notification));
        binding.getRoot().setOnLongClickListener(v -> {
            listener.onNotificationLongClick(notification);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        final ItemNotificationBinding binding;

        NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
