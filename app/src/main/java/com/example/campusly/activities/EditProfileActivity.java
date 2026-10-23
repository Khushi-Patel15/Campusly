package com.example.campusly.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.databinding.ActivityEditProfileBinding;
import com.example.campusly.repositories.PostRepository;
import com.example.campusly.utils.DropdownUtils;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.NetworkUtils;
import com.example.campusly.utils.UiUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// Lets users change their name, school, year and bio. Email can't be
// changed here (the security rules block it), because it's tied to login.
// Posts made earlier keep the name they were posted with.
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ActivityEditProfileBinding binding;
    private final PostRepository repository = new PostRepository();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        userId = user.getUid();

        DropdownUtils.setUp(this, binding.schoolSpinner, R.array.schools, R.string.select_school);
        DropdownUtils.setUp(this, binding.yearSpinner, R.array.years, R.string.select_year);
        binding.saveProfileButton.setOnClickListener(v -> save());
        loadCurrentValues();
    }

    private void loadCurrentValues() {
        setLoading(true);
        repository.getUserProfile(userId)
                .addOnSuccessListener(doc -> {
                    binding.nameInput.setText(doc.getString("name"));
                    DropdownUtils.select(binding.schoolSpinner, doc.getString("department"));
                    DropdownUtils.select(binding.yearSpinner, doc.getString("year"));
                    binding.bioInput.setText(doc.getString("bio"));
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    UiUtils.toast(this, R.string.error_profile_load);
                    finish();
                });
    }

    private void save() {
        String name = binding.nameInput.getText().toString().trim();
        String department = DropdownUtils.getSelected(binding.schoolSpinner);
        String year = DropdownUtils.getSelected(binding.yearSpinner);
        String bio = binding.bioInput.getText().toString().trim();

        if (name.isEmpty()) {
            binding.nameInput.setError(getString(R.string.error_field_required));
            return;
        }
        if (department == null) {
            UiUtils.toast(this, R.string.error_pick_school);
            return;
        }
        if (year == null) {
            UiUtils.toast(this, R.string.error_pick_year);
            return;
        }
        // An offline save would only finish once the internet returns, so the
        // screen would appear stuck; ask the user to connect instead.
        if (!NetworkUtils.isOnline(this)) {
            UiUtils.toast(this, R.string.error_network);
            return;
        }

        Map<String, Object> changes = new HashMap<>();
        changes.put("name", name);
        changes.put("department", department);
        changes.put("year", year);
        changes.put("bio", bio);

        setLoading(true);
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(changes)
                .addOnSuccessListener(unused -> {
                    UiUtils.toast(this, R.string.edit_profile_saved);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Saving profile failed", e);
                    setLoading(false);
                    UiUtils.toast(this, R.string.edit_profile_failed);
                });
    }

    private void setLoading(boolean loading) {
        binding.editProfileProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveProfileButton.setEnabled(!loading);
        binding.saveProfileButton.setAlpha(loading ? 0.5f : 1f);
    }
}
