package com.example.campusly.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.databinding.ActivitySignupBinding;
import com.example.campusly.utils.AppConfig;
import com.example.campusly.utils.DropdownUtils;
import com.example.campusly.utils.NavigationUtils;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.signupSubtitleText.setText(
                getString(R.string.signup_subtitle, AppConfig.COLLEGE_EMAIL_DOMAIN));
        DropdownUtils.setUp(this, binding.schoolSpinner, R.array.schools, R.string.select_school);
        DropdownUtils.setUp(this, binding.yearSpinner, R.array.years, R.string.select_year);
        binding.signupButton.setOnClickListener(v -> attemptSignup());
        binding.goToLoginText.setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String department = DropdownUtils.getSelected(binding.schoolSpinner);
        String year = DropdownUtils.getSelected(binding.yearSpinner);
        String password = binding.passwordInput.getText().toString();
        String confirmPassword = binding.confirmPasswordInput.getText().toString();

        if (name.isEmpty()) {
            binding.nameInput.setError(getString(R.string.error_field_required));
            return;
        }
        if (!AppConfig.isCollegeEmail(email)) {
            binding.emailInput.setError(
                    getString(R.string.error_college_email, AppConfig.COLLEGE_EMAIL_DOMAIN));
            return;
        }
        if (department == null) {
            showToast(R.string.error_pick_school);
            return;
        }
        if (year == null) {
            showToast(R.string.error_pick_year);
            return;
        }
        if (password.length() < AppConfig.MIN_PASSWORD_LENGTH) {
            binding.passwordInput.setError(
                    getString(R.string.error_password_short, AppConfig.MIN_PASSWORD_LENGTH));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInput.setError(getString(R.string.error_password_mismatch));
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                setLoading(false);
                handleSignupError(task.getException());
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                setLoading(false);
                showToast(R.string.error_signup_failed);
                return;
            }

            user.sendEmailVerification();
            saveProfile(user, name, department, year);
        });
    }

    private void saveProfile(FirebaseUser user, String name, String department, String year) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("name", name);
        profile.put("email", user.getEmail());
        profile.put("department", department);
        profile.put("year", year);
        profile.put("bio", "");
        profile.put("profileImageUrl", "");
        profile.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(profile)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (!task.isSuccessful()) {
                        // The login account exists even if this failed, so still continue.
                        showToast(R.string.error_profile_save);
                    }
                    NavigationUtils.openAndClear(this, VerifyEmailActivity.class);
                });
    }

    private void handleSignupError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            showToast(R.string.error_network);
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            binding.emailInput.setError(getString(R.string.error_email_in_use));
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            binding.passwordInput.setError(getString(R.string.error_password_weak));
        } else {
            showToast(R.string.error_signup_failed);
        }
    }

    private void setLoading(boolean loading) {
        binding.signupProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.signupButton.setEnabled(!loading);
        binding.signupButton.setAlpha(loading ? 0.5f : 1f);
    }

    private void showToast(int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
    }
}
