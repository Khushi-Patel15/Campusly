package com.example.campusly.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

// Firestore fills this class from a "posts" document automatically.
// It needs an empty constructor plus a getter and setter for each field.
// @IgnoreExtraProperties: posts made before Stage 6B still have old
// likeCount/commentCount fields, which we now ignore.
@IgnoreExtraProperties
public class Post {

    // Gossip posts are stored with these instead of the real author. The real
    // author id is kept in posts/{postId}/private/author, which only the
    // author and admins can read (see firestore.rules).
    public static final String ANONYMOUS_ID = "anonymous";
    public static final String ANONYMOUS_NAME = "Anonymous";

    @DocumentId
    private String postId;
    private String authorId;
    private String authorName;
    private String content;
    private String category;
    private Timestamp createdAt;
    private List<String> likedBy = new ArrayList<>();

    public Post() {
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy == null ? new ArrayList<>() : likedBy;
    }

    // @Exclude: these are calculated, not stored in Firestore.
    @Exclude
    public int getLikeCount() {
        return likedBy.size();
    }

    @Exclude
    public boolean isAnonymous() {
        return ANONYMOUS_ID.equals(authorId);
    }

    @Exclude
    public boolean isLikedBy(String userId) {
        return likedBy.contains(userId);
    }
}
