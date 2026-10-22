package com.example.campusly.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusly.R;
import com.example.campusly.adapters.PostAdapter;
import com.example.campusly.databinding.ActivityMainBinding;
import com.example.campusly.models.Post;
import com.example.campusly.models.WeatherResponse;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.LocalRepository;
import com.example.campusly.repositories.PostRepository;
import com.example.campusly.repositories.WeatherRepository;
import com.example.campusly.utils.AppConfig;
import com.example.campusly.utils.Categories;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.NetworkUtils;
import com.example.campusly.utils.PostOptions;
import com.example.campusly.utils.UiUtils;
import com.example.campusly.utils.WeatherCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PostAdapter.Listener {

    private static final String TAG = "MainActivity";
    private static final int FEED_LIMIT = 50;

    private ActivityMainBinding binding;
    private final PostRepository repository = new PostRepository();
    private final WeatherRepository weatherRepository = new WeatherRepository();
    private LocalRepository localRepository;
    private PostAdapter adapter;
    private ListenerRegistration feedListener;
    private String userId;
    private boolean hasShownLiveFeed = false;
    private boolean isAdmin = false;

    private TextView[] categoryChips;
    // Same order as categoryChips; null means "All Posts".
    private final String[] chipCategories = {
            null,
            Categories.EVENTS,
            Categories.SPORTS,
            Categories.EDUCATION,
            Categories.GOSSIP,
            Categories.ANNOUNCEMENTS
    };
    private TextView selectedChip;
    private String selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        userId = user.getUid();

        localRepository = new LocalRepository(this);
        adapter = new PostAdapter(userId, this);
        binding.postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.postsRecyclerView.setAdapter(adapter);

        setupNotificationButton();
        setupCategoryChips();
        setupBottomNavigation();

        binding.weatherCard.setOnClickListener(v -> loadWeather(true));
        loadWeather(false);

        // Only changes which menu options appear; the rules do the real checking.
        new AdminRepository().checkIsAdmin(userId, admin -> isAdmin = admin);
    }

    private void loadWeather(boolean forceRefresh) {
        binding.weatherProgress.setVisibility(View.VISIBLE);
        binding.weatherText.setText(R.string.weather_loading);
        binding.weatherCard.setClickable(false);

        weatherRepository.fetchCampusWeather(forceRefresh, new WeatherRepository.Callback() {
            @Override
            public void onSuccess(WeatherResponse.Current weather) {
                if (isDestroyed()) return;
                binding.weatherProgress.setVisibility(View.GONE);
                binding.weatherText.setText(getString(R.string.weather_summary,
                        WeatherCodes.emojiFor(weather.weatherCode),
                        Math.round(weather.temperature),
                        getString(WeatherCodes.descriptionFor(weather.weatherCode)),
                        AppConfig.CAMPUS_NAME));
                binding.weatherCard.setClickable(true);
            }

            @Override
            public void onError() {
                if (isDestroyed()) return;
                binding.weatherProgress.setVisibility(View.GONE);
                binding.weatherText.setText(R.string.weather_error);
                binding.weatherCard.setClickable(true);
            }
        });
    }

    // Listen only while the screen is visible, so we don't keep downloading
    // updates in the background.
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            startFeedListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopFeedListener();
    }

    private void startFeedListener() {
        stopFeedListener();
        hasShownLiveFeed = false;
        binding.feedProgress.setVisibility(View.VISIBLE);
        binding.feedEmptyText.setVisibility(View.GONE);
        binding.offlineBanner.setVisibility(View.GONE);

        // Show the Room copy straight away while Firestore connects.
        if (selectedCategory == null) {
            showSavedFeed();
        }

        // MetadataChanges.INCLUDE: also tell us when the data switches from
        // "cached" to "confirmed by the server", so the offline banner can
        // disappear as soon as the connection comes back.
        feedListener = repository.feedQuery(selectedCategory, FEED_LIMIT)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null) {
                        // If this says an index is required, the Logcat message
                        // contains a link that creates it in the Firebase Console.
                        Log.e(TAG, "Loading feed failed", error);
                        binding.feedProgress.setVisibility(View.GONE);
                        // Keep whatever is already on screen (e.g. saved posts).
                        if (adapter.getItemCount() == 0) {
                            showEmptyMessage(R.string.feed_error);
                        }
                        return;
                    }

                    boolean fromCache = snapshots.getMetadata().isFromCache();
                    List<Post> posts = toPosts(snapshots.getDocuments());

                    // Firestore's own cache is empty (e.g. fresh install) and we
                    // haven't heard from the server yet: keep the Room copy.
                    if (fromCache && posts.isEmpty()) {
                        if (selectedCategory == null) {
                            showSavedFeed();
                        } else if (!NetworkUtils.isOnline(this)) {
                            showOfflineEmpty();
                        }
                        return;
                    }

                    hasShownLiveFeed = true;
                    binding.feedProgress.setVisibility(View.GONE);
                    binding.offlineBanner.setVisibility(
                            fromCache && !NetworkUtils.isOnline(this) ? View.VISIBLE : View.GONE);
                    showPosts(posts);

                    // Only server-confirmed "All Posts" data replaces the Room copy.
                    if (!fromCache && selectedCategory == null) {
                        localRepository.saveFeed(posts);
                    }
                });
    }

    private void showSavedFeed() {
        boolean offline = !NetworkUtils.isOnline(this);
        localRepository.loadSavedFeed(saved -> {
            // Live data (or a different category) arrived while Room was loading.
            if (hasShownLiveFeed || selectedCategory != null) return;

            if (!saved.isEmpty()) {
                binding.feedProgress.setVisibility(View.GONE);
                binding.offlineBanner.setVisibility(offline ? View.VISIBLE : View.GONE);
                showPosts(saved);
            } else if (offline) {
                showOfflineEmpty();
            }
            // Online with nothing saved yet: keep the spinner until the server replies.
        });
    }

    private void showOfflineEmpty() {
        binding.feedProgress.setVisibility(View.GONE);
        binding.offlineBanner.setVisibility(View.VISIBLE);
        adapter.setPosts(new ArrayList<>());
        showEmptyMessage(R.string.feed_offline_empty);
    }

    private void showPosts(List<Post> posts) {
        adapter.setPosts(posts);
        if (posts.isEmpty()) {
            showEmptyMessage(R.string.feed_empty);
        } else {
            binding.feedEmptyText.setVisibility(View.GONE);
        }
    }

    private static List<Post> toPosts(List<DocumentSnapshot> docs) {
        List<Post> posts = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            // ESTIMATE gives a just-created post a temporary time until the
            // server confirms it, instead of leaving it blank.
            Post post = doc.toObject(Post.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            if (post != null) {
                posts.add(post);
            }
        }
        return posts;
    }

    private void stopFeedListener() {
        if (feedListener != null) {
            feedListener.remove();
            feedListener = null;
        }
    }

    private void showEmptyMessage(int messageRes) {
        binding.feedEmptyText.setText(messageRes);
        binding.feedEmptyText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostOptions(Post post, View anchor) {
        PostOptions.show(this, anchor, post, userId, isAdmin, null);
    }

    @Override
    public void onSharePost(Post post) {
        UiUtils.sharePost(this, post);
    }

    // The feed listener picks up the change straight away (even before the
    // server confirms), so the heart updates instantly without extra code here.
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

    private void setupNotificationButton() {
        binding.notificationButton.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void setupCategoryChips() {
        categoryChips = new TextView[]{
                binding.categoryAll,
                binding.categoryEvents,
                binding.categorySports,
                binding.categoryEducation,
                binding.categoryGossip,
                binding.categoryAnnouncements
        };
        selectedChip = binding.categoryAll;

        for (int i = 0; i < categoryChips.length; i++) {
            TextView chip = categoryChips[i];
            String category = chipCategories[i];
            chip.setOnClickListener(v -> selectChip(chip, category));
        }
    }

    private void selectChip(TextView chip, String category) {
        if (chip == selectedChip) return;

        selectedChip.setBackgroundResource(R.drawable.bg_category_unselected);
        selectedChip.setTextColor(getColor(R.color.text_secondary));
        selectedChip.setTypeface(null, Typeface.NORMAL);

        chip.setBackgroundResource(R.drawable.bg_category_selected);
        chip.setTextColor(getColor(R.color.campus_black));
        chip.setTypeface(null, Typeface.BOLD);

        selectedChip = chip;
        selectedCategory = category;
        startFeedListener();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_explore) {
                startActivity(new Intent(this, ExploreActivity.class));
            } else if (id == R.id.nav_create) {
                startActivity(new Intent(this, CreatePostActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }

            return false;
        });
    }
}
