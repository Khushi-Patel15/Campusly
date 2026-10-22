package com.example.campusly.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.campusly.models.Post;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// One row per post in the saved "All Posts" feed. This is a copy of what the
// server last sent, used only for showing posts while offline.
@Entity(tableName = "cached_posts")
public class CachedPostEntity {

    @PrimaryKey
    @NonNull
    public String postId = "";
    public String authorId;
    public String authorName;
    public String content;
    public String category;
    public long createdAtMillis;
    public List<String> likedBy = new ArrayList<>();

    public static CachedPostEntity fromPost(Post post) {
        CachedPostEntity entity = new CachedPostEntity();
        entity.postId = post.getPostId();
        entity.authorId = post.getAuthorId();
        entity.authorName = post.getAuthorName();
        entity.content = post.getContent();
        entity.category = post.getCategory();
        entity.createdAtMillis = post.getCreatedAt() == null
                ? System.currentTimeMillis()
                : post.getCreatedAt().toDate().getTime();
        entity.likedBy = new ArrayList<>(post.getLikedBy());
        return entity;
    }

    public Post toPost() {
        Post post = new Post();
        post.setPostId(postId);
        post.setAuthorId(authorId);
        post.setAuthorName(authorName);
        post.setContent(content);
        post.setCategory(category);
        post.setCreatedAt(new Timestamp(new Date(createdAtMillis)));
        post.setLikedBy(likedBy);
        return post;
    }
}