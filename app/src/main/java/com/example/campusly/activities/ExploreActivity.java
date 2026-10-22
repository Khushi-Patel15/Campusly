package com.example.campusly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusly.R;
import com.example.campusly.adapters.PostAdapter;
import com.example.campusly.databinding.ActivityExploreBinding;
import com.example.campusly.models.Post;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.PostRepository;
import com.example.campusly.utils.Categories;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.PostOptions;
import com.example.campusly.utils.UiUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Explore = the most-liked recent posts ("trending"), plus a search box.
// Firestore can't sort by "number of likes" (likes are a list), so we load the
// newest 100 posts and sort/search them on the phone. Fine for a campus app.
public class ExploreActivity extends AppCompatActivity implements PostAdapter.Listener {

    private static final String TAG = "ExploreActivity";
    private static final int EXPLORE_LIMIT = 100;

    private ActivityExploreBinding binding;
    private final PostRepository repository = new PostRepository();
    private PostAdapter adapter;
    private ListenerRegistration listener;
    private final List<Post> trendingPosts = new ArrayList<>();
    private String userId;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExploreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        userId = user.getUid();

        adapter = new PostAdapter(userId, this);
        binding.explorePostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.explorePostsRecyclerView.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showMatchingPosts();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new AdminRepository().checkIsAdmin(userId, admin -> isAdmin = admin);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter == null) return;

        listener = repository.feedQuery(null, EXPLORE_LIMIT).addSnapshotListener((snapshots, error) -> {
            binding.exploreProgress.setVisibility(View.GONE);
            if (error != null) {
                Log.e(TAG, "Loading posts failed", error);
                if (trendingPosts.isEmpty()) {
                    showMessage(R.string.feed_error);
                }
                return;
            }

            trendingPosts.clear();
            trendingPosts.addAll(PostRepository.toPosts(snapshots.getDocuments()));
            // Most likes first. The list arrives newest-first and this sort
            // keeps that order for posts with the same number of likes.
            trendingPosts.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));
            showMatchingPosts();
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

    // Matches the search text against the post text, author name and category.
    private void showMatchingPosts() {
        String query = binding.searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);

        List<Post> matches = new ArrayList<>();
        for (Post post : trendingPosts) {
            if (query.isEmpty() || matches(post, query)) {
                matches.add(post);
            }
        }

        adapter.setPosts(matches);
        if (!matches.isEmpty()) {
            binding.exploreEmptyText.setVisibility(View.GONE);
        } else if (binding.exploreProgress.getVisibility() != View.VISIBLE) {
            showMessage(query.isEmpty() ? R.string.feed_empty : R.string.explore_no_results);
        }
    }

    private boolean matches(Post post, String query) {
        String category = getString(Categories.labelFor(post.getCategory()));
        return contains(post.getContent(), query)
                || contains(post.getAuthorName(), query)
                || contains(category, query);
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private void showMessage(int messageRes) {
        binding.exploreEmptyText.setText(messageRes);
        binding.exploreEmptyText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostOptions(Post post, View anchor) {
        PostOptions.show(this, anchor, post, userId, isAdmin, null);
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
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.EXTRA_POST_ID, post.getPostId());
        startActivity(intent);
    }
}
