package com.example.campusly.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

// Filled automatically from a document in posts/{postId}/comments.
public class Comment {

    @DocumentId
    private String commentId;
    private String authorId;
    private String authorName;
    private String content;
    private Timestamp createdAt;

    public Comment() {
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Comments on anonymous (gossip) posts are anonymous too; the real author
    // is kept in comments/{commentId}/private/author, like for posts.
    @Exclude
    public boolean isAnonymous() {
        return Post.ANONYMOUS_ID.equals(authorId);
    }
}
