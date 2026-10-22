package com.example.campusly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.databinding.ActivityProfileBinding;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.LocalRepository;
import com.example.campusly.utils.NavigationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }

        binding.profileEmailText.setText(user.getEmail());
        userId = user.getUid();

        binding.editProfileButton.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        new AdminRepository().checkIsAdmin(user.getUid(), isAdmin ->
                binding.adminDashboardButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE));
        binding.adminDashboardButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminDashboardActivity.class)));

        // Wipe the on-phone saved feed and draft first, so the next person to
        // log in on this phone can't see them.
        binding.logoutButton.setOnClickListener(v -> {
            binding.logoutButton.setEnabled(false);
            new LocalRepository(this).clearAll(() -> {
                AdminRepository.clearCache();
                FirebaseAuth.getInstance().signOut();
                NavigationUtils.openAndClear(this, LoginActivity.class);
            });
        });
    }

    // Loaded here instead of onCreate so changes made on the Edit Profile
    // screen show up as soon as you come back.
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            loadProfile(userId);
        }
    }

    private void loadProfile(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this::showProfile)
                .addOnFailureListener(e -> {
                    binding.profileNameText.setText("");
                    Toast.makeText(this, R.string.error_profile_load, Toast.LENGTH_LONG).show();
                });
    }

    private void showProfile(DocumentSnapshot doc) {
        if (!doc.exists()) {
            binding.profileNameText.setText("");
            binding.profileBioText.setText(R.string.profile_missing);
            return;
        }

        String name = valueOrEmpty(doc.getString("name"));
        String department = valueOrEmpty(doc.getString("department"));
        String year = valueOrEmpty(doc.getString("year"));
        String bio = valueOrEmpty(doc.getString("bio"));

        binding.profileNameText.setText(name);
        binding.profileInitialText.setText(
                name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT));
        binding.profileMetaText.setText(department + " · " + year);
        binding.profileBioText.setText(bio.isEmpty() ? getString(R.string.profile_no_bio) : bio);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
