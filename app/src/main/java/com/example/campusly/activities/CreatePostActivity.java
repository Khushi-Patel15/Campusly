package com.example.campusly.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.database.DraftEntity;
import com.example.campusly.databinding.ActivityCreatePostBinding;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.LocalRepository;
import com.example.campusly.repositories.PostRepository;
import com.example.campusly.utils.Categories;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.NetworkUtils;
import com.example.campusly.utils.UiUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreatePostActivity extends AppCompatActivity {

    private static final String TAG = "CreatePostActivity";

    private ActivityCreatePostBinding binding;
    private final PostRepository repository = new PostRepository();
    private LocalRepository localRepository;
    private FirebaseUser user;
    private String authorName;
    // True once the text has been posted or deliberately saved, so leaving
    // the screen doesn't save it (again) as a draft.
    private boolean draftHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }

        localRepository = new LocalRepository(this);
        binding.postSubmitButton.setOnClickListener(v -> submitPost());
        binding.categoryGroup.setOnCheckedChangeListener((group, checkedId) ->
                binding.anonymousNote.setVisibility(
                        checkedId == R.id.optionGossip ? View.VISIBLE : View.GONE));
        loadAuthorName();
        restoreDraft();

        new AdminRepository().checkIsAdmin(user.getUid(), isAdmin ->
                binding.optionAnnouncements.setVisibility(isAdmin ? View.VISIBLE : View.GONE));
    }

    // Leaving the screen (Back, Home button, another app) keeps the text as a
    // draft; clearing the text removes the draft.
    @Override
    protected void onPause() {
        super.onPause();
        if (user == null || draftHandled) return;

        boolean saved = saveOrClearDraft();
        if (saved && isFinishing()) {
            UiUtils.toast(this, R.string.draft_saved);
        }
    }

    private void restoreDraft() {
        localRepository.loadDraft(user.getUid(), draft -> {
            if (draft == null || binding.postInput.length() > 0) return;
            binding.postInput.setText(draft.content);
            binding.postInput.setSelection(binding.postInput.length());
            checkCategory(draft.category);
            UiUtils.toast(this, R.string.draft_restored);
        });
    }

    private boolean saveOrClearDraft() {
        String content = binding.postInput.getText().toString().trim();
        if (content.isEmpty()) {
            localRepository.deleteDraft(user.getUid());
            return false;
        }
        localRepository.saveDraft(user.getUid(), content, selectedCategory());
        return true;
    }

    // The post stores the author's name, and the security rules check it matches
    // their profile. If it can't load (e.g. offline) the user can still write
    // and save a draft; posting just waits until the name is available.
    private void loadAuthorName() {
        setLoading(true);
        repository.getUserProfile(user.getUid())
                .addOnSuccessListener(doc -> {
                    authorName = doc.getString("name");
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Couldn't load profile name", e);
                    setLoading(false);
                });
    }

    private void submitPost() {
        String content = binding.postInput.getText().toString().trim();
        String category = selectedCategory();

        if (content.isEmpty()) {
            binding.postInput.setError(getString(R.string.create_post_empty));
            return;
        }
        if (category == null) {
            UiUtils.toast(this, R.string.create_post_pick_category);
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            localRepository.saveDraft(user.getUid(), content, category);
            draftHandled = true;
            UiUtils.toast(this, R.string.draft_saved_offline);
            finish();
            return;
        }

        if (authorName == null || authorName.isEmpty()) {
            UiUtils.toast(this, R.string.error_profile_load);
            loadAuthorName();
            return;
        }

        setLoading(true);
        repository.createPost(user.getUid(), authorName, content, category)
                .addOnSuccessListener(ref -> {
                    draftHandled = true;
                    localRepository.deleteDraft(user.getUid());
                    UiUtils.toast(this, R.string.create_post_success);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Creating post failed", e);
                    setLoading(false);
                    UiUtils.toast(this, R.string.create_post_failed);
                });
    }

    private String selectedCategory() {
        int checkedId = binding.categoryGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.optionEvents) {
            return Categories.EVENTS;
        } else if (checkedId == R.id.optionSports) {
            return Categories.SPORTS;
        } else if (checkedId == R.id.optionEducation) {
            return Categories.EDUCATION;
        } else if (checkedId == R.id.optionGossip) {
            return Categories.GOSSIP;
        } else if (checkedId == R.id.optionAnnouncements) {
            return Categories.ANNOUNCEMENTS;
        }
        return null;
    }

    private void checkCategory(String category) {
        if (Categories.EVENTS.equals(category)) {
            binding.categoryGroup.check(R.id.optionEvents);
        } else if (Categories.SPORTS.equals(category)) {
            binding.categoryGroup.check(R.id.optionSports);
        } else if (Categories.EDUCATION.equals(category)) {
            binding.categoryGroup.check(R.id.optionEducation);
        } else if (Categories.GOSSIP.equals(category)) {
            binding.categoryGroup.check(R.id.optionGossip);
        } else if (Categories.ANNOUNCEMENTS.equals(category)) {
            binding.categoryGroup.check(R.id.optionAnnouncements);
        }
    }

    private void setLoading(boolean loading) {
        binding.createPostProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.postSubmitButton.setEnabled(!loading);
        binding.postSubmitButton.setAlpha(loading ? 0.5f : 1f);
    }
}
