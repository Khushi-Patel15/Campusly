package com.example.campusly.repositories;

import android.util.Log;

import com.example.campusly.models.AppNotification;
import com.example.campusly.models.Post;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

// Creates and reads in-app notifications. Sending a notification is
// "best effort": if it fails, the like or comment itself still worked, so
// failures are only logged.
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static final int PREVIEW_LENGTH = 80;
    private static final int LIST_LIMIT = 50;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference inbox(String userId) {
        return db.collection("users").document(userId).collection("notifications");
    }

    public Query myNotificationsQuery(String userId) {
        return inbox(userId).orderBy("createdAt", Query.Direction.DESCENDING).limit(LIST_LIMIT);
    }

    public Task<Void> delete(String userId, String notificationId) {
        return inbox(userId).document(notificationId).delete();
    }

    // The id "like_{postId}_{fromUserId}" means one like notification per
    // person per post: liking again after un-liking can't create a second one
    // (the rules don't allow overwriting it).
    public void notifyLike(Post post, String fromUserId) {
        if (!shouldNotify(post, fromUserId)) return;
        String id = "like_" + post.getPostId() + "_" + fromUserId;
        send(post, fromUserId, AppNotification.TYPE_LIKE, post.getContent(), id);
    }

    public void notifyComment(Post post, String fromUserId, String commentText) {
        if (!shouldNotify(post, fromUserId)) return;
        send(post, fromUserId, AppNotification.TYPE_COMMENT, commentText, null);
    }

    // Anonymous (gossip) posts don't say who wrote them, so there is nobody
    // we're allowed to notify. And nobody is notified about their own actions.
    private static boolean shouldNotify(Post post, String fromUserId) {
        return post.getAuthorId() != null
                && !post.isAnonymous()
                && !post.getAuthorId().equals(fromUserId);
    }

    // The sender's name must match their profile (the rules check it), so it's
    // read from the profile rather than passed in.
    private void send(Post post, String fromUserId, String type, String text, String id) {
        new PostRepository().getUserProfile(fromUserId)
                .addOnSuccessListener(profile -> {
                    String fromName = profile.getString("name");
                    if (fromName == null) return;

                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", type);
                    notification.put("fromUserId", fromUserId);
                    notification.put("fromName", fromName);
                    notification.put("postId", post.getPostId());
                    notification.put("preview", shorten(text));
                    notification.put("createdAt", FieldValue.serverTimestamp());

                    CollectionReference inbox = inbox(post.getAuthorId());
                    (id == null ? inbox.document() : inbox.document(id))
                            .set(notification)
                            .addOnFailureListener(e -> Log.w(TAG, "Notification not sent", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Couldn't load sender name", e));
    }

    private static String shorten(String text) {
        if (text == null) return "";
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= PREVIEW_LENGTH
                ? oneLine
                : oneLine.substring(0, PREVIEW_LENGTH - 1) + "…";
    }
}
