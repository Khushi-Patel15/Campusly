package com.example.campusly.repositories;

import com.example.campusly.models.Comment;
import com.example.campusly.models.Post;
import com.example.campusly.utils.Categories;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// All Firestore reads and writes for posts and comments live here, so the
// screens only deal with showing data, not with how it is stored.
public class PostRepository {

    private static final String USERS = "users";
    private static final String POSTS = "posts";
    private static final String COMMENTS = "comments";
    private static final String REPORTS = "reports";
    private static final String PRIVATE = "private";
    private static final int COMMENTS_LIMIT = 200;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentSnapshot> getUserProfile(String userId) {
        return db.collection(USERS).document(userId).get();
    }

    // category == null means every category.
    public Query feedQuery(String category, int limit) {
        Query query = db.collection(POSTS);
        if (category != null) {
            query = query.whereEqualTo("category", category);
        }
        return query.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit);
    }

    public DocumentReference postRef(String postId) {
        return db.collection(POSTS).document(postId);
    }

    public Query commentsQuery(String postId) {
        return postRef(postId).collection(COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(COMMENTS_LIMIT);
    }

    // Gossip posts are anonymous: the post itself says "Anonymous", and the
    // real author id goes into a private document only the author and admins
    // can read. Both are written in one batch, so you never get one without
    // the other.
    public Task<Void> createPost(String userId, String authorName,
                                 String content, String category) {
        boolean anonymous = Categories.GOSSIP.equals(category);
        DocumentReference ref = db.collection(POSTS).document();

        Map<String, Object> post = new HashMap<>();
        post.put("authorId", anonymous ? Post.ANONYMOUS_ID : userId);
        post.put("authorName", anonymous ? Post.ANONYMOUS_NAME : authorName);
        post.put("content", content);
        post.put("category", category);
        post.put("createdAt", FieldValue.serverTimestamp());
        post.put("likedBy", new ArrayList<String>());

        WriteBatch batch = db.batch();
        batch.set(ref, post);
        if (anonymous) {
            Map<String, Object> author = new HashMap<>();
            author.put("uid", userId);
            batch.set(privateAuthorRef(ref.getId()), author);
        }
        return batch.commit();
    }

    public DocumentReference privateAuthorRef(String postId) {
        return postRef(postId).collection(PRIVATE).document("author");
    }

    // Only the author (or an admin) is allowed to read the private document,
    // so if the read fails or names someone else, it isn't this user's post.
    public void isMyAnonymousPost(String postId, String userId, Callback<Boolean> callback) {
        privateAuthorRef(postId).get()
                .addOnSuccessListener(doc -> callback.onResult(userId.equals(doc.getString("uid"))))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public static List<Post> toPosts(List<DocumentSnapshot> docs) {
        List<Post> posts = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Post post = doc.toObject(Post.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            if (post != null) {
                posts.add(post);
            }
        }
        return posts;
    }

    // arrayUnion/arrayRemove only add or remove this one user, so two people
    // liking at the same moment can't overwrite each other's like.
    // Liking (not un-liking) also notifies the post's author.
    public Task<Void> toggleLike(Post post, String userId) {
        boolean liking = !post.isLikedBy(userId);
        Object change = liking
                ? FieldValue.arrayUnion(userId)
                : FieldValue.arrayRemove(userId);
        return postRef(post.getPostId()).update("likedBy", change)
                .addOnSuccessListener(unused -> {
                    if (liking) {
                        new NotificationRepository().notifyLike(post, userId);
                    }
                });
    }

    public Task<Void> deletePost(Post post) {
        WriteBatch batch = db.batch();
        batch.delete(postRef(post.getPostId()));
        if (post.isAnonymous()) {
            batch.delete(privateAuthorRef(post.getPostId()));
        }
        return batch.commit();
    }

    // Commenting also notifies the post's author. On an anonymous (gossip)
    // post the comment is anonymous too: it says "Anonymous" and the real
    // author goes into a private document, written in the same batch.
    public Task<Void> addComment(Post post, String userId, String authorName, String content) {
        boolean anonymous = post.isAnonymous();
        DocumentReference ref = postRef(post.getPostId()).collection(COMMENTS).document();

        Map<String, Object> comment = new HashMap<>();
        comment.put("authorId", anonymous ? Post.ANONYMOUS_ID : userId);
        comment.put("authorName", anonymous ? Post.ANONYMOUS_NAME : authorName);
        comment.put("content", content);
        comment.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(ref, comment);
        if (anonymous) {
            Map<String, Object> author = new HashMap<>();
            author.put("uid", userId);
            batch.set(ref.collection(PRIVATE).document("author"), author);
        }
        return batch.commit()
                .addOnSuccessListener(unused ->
                        new NotificationRepository().notifyComment(post, userId, content));
    }

    private DocumentReference commentRef(String postId, String commentId) {
        return postRef(postId).collection(COMMENTS).document(commentId);
    }

    public void isMyAnonymousComment(String postId, String commentId, String userId,
                                     Callback<Boolean> callback) {
        commentRef(postId, commentId).collection(PRIVATE).document("author").get()
                .addOnSuccessListener(doc -> callback.onResult(userId.equals(doc.getString("uid"))))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public Task<Void> deleteComment(String postId, Comment comment) {
        DocumentReference ref = commentRef(postId, comment.getCommentId());
        WriteBatch batch = db.batch();
        batch.delete(ref);
        if (comment.isAnonymous()) {
            batch.delete(ref.collection(PRIVATE).document("author"));
        }
        return batch.commit();
    }

    // The report's id is "{postId}_{userId}", so the same person can't report
    // the same post twice (the rules refuse to overwrite an existing report).
    public Task<Void> reportPost(Post post, String userId, String reason, String description) {
        Map<String, Object> report = new HashMap<>();
        report.put("postId", post.getPostId());
        report.put("postAuthorId", post.getAuthorId());
        report.put("postAuthorName", post.getAuthorName());
        report.put("postContent", post.getContent());
        report.put("reportedBy", userId);
        report.put("reason", reason);
        report.put("description", description);
        report.put("status", "open");
        report.put("createdAt", FieldValue.serverTimestamp());
        return db.collection(REPORTS).document(post.getPostId() + "_" + userId).set(report);
    }
}
