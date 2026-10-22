package com.example.campusly.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

// One in-app notification, stored in users/{recipientId}/notifications.
// Called AppNotification so it doesn't clash with Android's own Notification class.
@IgnoreExtraProperties
public class AppNotification {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_COMMENT = "comment";

    @DocumentId
    private String notificationId;
    private String type;
    private String fromUserId;
    private String fromName;
    private String postId;
    // Short preview: the post's text for likes, the comment's text for comments.
    private String preview;
    private Timestamp createdAt;

    public AppNotification() {
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
