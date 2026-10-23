package com.example.campusly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.databinding.ActivityLoginBinding;
import com.example.campusly.utils.AppConfig;
import com.example.campusly.utils.NavigationUtils;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.forgotPasswordText.setOnClickListener(v -> sendPasswordReset());
        binding.goToSignupText.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    private void attemptLogin() {
        String email = getEmail();
        String password = binding.passwordInput.getText().toString();

        if (!AppConfig.isCollegeEmail(email)) {
            binding.emailInput.setError(
                    getString(R.string.error_college_email, AppConfig.COLLEGE_EMAIL_DOMAIN));
            return;
        }
        if (password.isEmpty()) {
            binding.passwordInput.setError(getString(R.string.error_password_required));
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            setLoading(false);

            if (!task.isSuccessful()) {
                boolean offline = task.getException() instanceof FirebaseNetworkException;
                showToast(offline ? R.string.error_network : R.string.error_login_failed);
                return;
            }

            NavigationUtils.openAndClear(this, MainActivity.class);
        });
    }

    private void sendPasswordReset() {
        String email = getEmail();

        if (!AppConfig.isCollegeEmail(email)) {
            binding.emailInput.setError(
                    getString(R.string.error_college_email, AppConfig.COLLEGE_EMAIL_DOMAIN));
            return;
        }

        setLoading(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            setLoading(false);

            if (!task.isSuccessful() && task.getException() instanceof FirebaseNetworkException) {
                showToast(R.string.error_network);
                return;
            }
            // Same message whether or not the account exists, so this screen
            // can't be used to find out which emails have accounts.
            showToast(R.string.reset_email_sent);
        });
    }

    private String getEmail() {
        return binding.emailInput.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private void setLoading(boolean loading) {
        binding.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.loginButton.setAlpha(loading ? 0.5f : 1f);
        binding.forgotPasswordText.setEnabled(!loading);
    }

    private void showToast(int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
    }
}
