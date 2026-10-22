package com.example.campusly.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

// A report from a student about a post, stored in the "reports" collection.
// The post's author and text are copied in, so an admin can still see what
// was reported after the post itself has been removed.
@IgnoreExtraProperties
public class Report {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_DISMISSED = "dismissed";
    public static final String STATUS_REMOVED = "removed";

    @DocumentId
    private String reportId;
    private String postId;
    private String postAuthorId;
    private String postAuthorName;
    private String postContent;
    private String reportedBy;
    private String reason;
    private String description;
    private String status;
    private Timestamp createdAt;

    public Report() {
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPostAuthorId() { return postAuthorId; }
    public void setPostAuthorId(String postAuthorId) { this.postAuthorId = postAuthorId; }

    public String getPostAuthorName() { return postAuthorName; }
    public void setPostAuthorName(String postAuthorName) { this.postAuthorName = postAuthorName; }

    public String getPostContent() { return postContent; }
    public void setPostContent(String postContent) { this.postContent = postContent; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
