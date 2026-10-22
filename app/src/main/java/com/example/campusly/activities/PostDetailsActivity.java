package com.example.campusly.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusly.R;
import com.example.campusly.adapters.CommentAdapter;
import com.example.campusly.adapters.PostAdapter;
import com.example.campusly.databinding.ActivityPostDetailsBinding;
import com.example.campusly.models.Comment;
import com.example.campusly.models.Post;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.PostRepository;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.PostOptions;
import com.example.campusly.utils.UiUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class PostDetailsActivity extends AppCompatActivity
        implements PostAdapter.Listener, CommentAdapter.Listener {

    public static final String EXTRA_POST_ID = "postId";
    private static final String TAG = "PostDetailsActivity";

    private ActivityPostDetailsBinding binding;
    private final PostRepository repository = new PostRepository();
    private CommentAdapter commentAdapter;
    private ListenerRegistration postListener;
    private ListenerRegistration commentsListener;

    private String userId;
    private String postId;
    private String myName;
    private boolean deletingPost = false;
    private boolean isAdmin = false;
    // The latest copy of the post, needed to notify its author about comments.
    private Post currentPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        userId = user.getUid();

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) {
            finish();
            return;
        }

        commentAdapter = new CommentAdapter(userId, this);
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);

        binding.sendCommentButton.setOnClickListener(v -> sendComment());
        loadMyName();

        new AdminRepository().checkIsAdmin(userId, admin -> {
            isAdmin = admin;
            commentAdapter.setAdmin(admin);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (commentAdapter != null) {
            listenToPost();
            listenToComments();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postListener != null) {
            postListener.remove();
            postListener = null;
        }
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }

    // Comments store the author's name, and the rules check it matches the
    // profile, so sending stays disabled until the name is loaded.
    private void loadMyName() {
        setSending(true);
        repository.getUserProfile(userId)
                .addOnSuccessListener(doc -> {
                    myName = doc.getString("name");
                    setSending(myName == null || myName.isEmpty());
                })
                .addOnFailureListener(e -> UiUtils.toast(this, R.string.error_profile_load));
    }

    private void listenToPost() {
        postListener = repository.postRef(postId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Loading post failed", error);
                UiUtils.toast(this, R.string.feed_error);
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                if (!deletingPost) {
                    UiUtils.toast(this, R.string.post_not_found);
                }
                finish();
                return;
            }

            Post post = snapshot.toObject(Post.class,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            if (post == null) return;

            currentPost = post;
            binding.postCard.getRoot().setVisibility(View.VISIBLE);
            PostAdapter.bindPost(binding.postCard, post, userId, this);
        });
    }

    private void listenToComments() {
        commentsListener = repository.commentsQuery(postId).addSnapshotListener((snapshots, error) -> {
            binding.commentsProgress.setVisibility(View.GONE);

            if (error != null) {
                Log.e(TAG, "Loading comments failed", error);
                showCommentsMessage(R.string.comments_error);
                return;
            }

            List<Comment> comments = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Comment comment = doc.toObject(Comment.class,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                if (comment != null) {
                    comments.add(comment);
                }
            }

            commentAdapter.setComments(comments);
            binding.commentsTitleText.setText(
                    getString(R.string.comments_title_count, comments.size()));
            if (comments.isEmpty()) {
                showCommentsMessage(R.string.comments_empty);
            } else {
                binding.commentsEmptyText.setVisibility(View.GONE);
            }
        });
    }

    private void showCommentsMessage(int messageRes) {
        binding.commentsEmptyText.setText(messageRes);
        binding.commentsEmptyText.setVisibility(View.VISIBLE);
    }

    private void sendComment() {
        String content = binding.commentInput.getText().toString().trim();
        if (content.isEmpty() || myName == null || currentPost == null) {
            return;
        }

        setSending(true);
        repository.addComment(currentPost, userId, myName, content)
                .addOnSuccessListener(ref -> {
                    binding.commentInput.setText("");
                    setSending(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sending comment failed", e);
                    setSending(false);
                    UiUtils.toast(this, R.string.comment_failed);
                });
    }

    private void setSending(boolean sending) {
        binding.sendCommentButton.setEnabled(!sending);
        binding.sendCommentButton.setAlpha(sending ? 0.4f : 1f);
    }

    @Override
    public void onPostOptions(Post post, View anchor) {
        PostOptions.show(this, anchor, post, userId, isAdmin, () -> deletingPost = true);
    }

    @Override
    public void onSharePost(Post post) {
        UiUtils.sharePost(this, post);
    }

    @Override
    public void onLikePost(Post post) {
        repository.toggleLike(post, userId).addOnFailureListener(e -> {
            Log.e(TAG, "Like failed", e);
            UiUtils.toast(this, R.string.like_failed);
        });
    }

    @Override
    public void onCommentPost(Post post) {
        binding.commentInput.requestFocus();
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        keyboard.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDeleteComment(Comment comment) {
        UiUtils.confirm(this, R.string.delete_comment_title, R.string.delete_post_message,
                R.string.delete_post_confirm, () ->
                        repository.deleteComment(postId, comment.getCommentId())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Deleting comment failed", e);
                                    UiUtils.toast(this, R.string.comment_delete_failed);
                                }));
    }
}
