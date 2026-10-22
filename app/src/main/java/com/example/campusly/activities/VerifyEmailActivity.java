package com.example.campusly.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.databinding.ActivityVerifyEmailBinding;
import com.example.campusly.utils.NavigationUtils;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private ActivityVerifyEmailBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }

        binding.verifyMessageText.setText(getString(R.string.verify_message, user.getEmail()));
        binding.checkVerifiedButton.setOnClickListener(v -> checkVerified());
        binding.resendButton.setOnClickListener(v -> resendEmail());
        binding.useOtherAccountText.setOnClickListener(v -> {
            auth.signOut();
            NavigationUtils.openAndClear(this, LoginActivity.class);
        });
    }

    private void checkVerified() {
        setLoading(true);
        user.reload().addOnCompleteListener(reloadTask -> {
            if (!reloadTask.isSuccessful()) {
                setLoading(false);
                boolean offline = reloadTask.getException() instanceof FirebaseNetworkException;
                showToast(offline ? R.string.error_network : R.string.error_generic);
                return;
            }

            if (!user.isEmailVerified()) {
                setLoading(false);
                showToast(R.string.verify_not_yet);
                return;
            }

            // Refresh the login token so Firestore rules see email_verified = true.
            user.getIdToken(true).addOnCompleteListener(tokenTask ->
                    NavigationUtils.openAndClear(this, com.example.campusly.activities.MainActivity.class));
        });
    }

    private void resendEmail() {
        setLoading(true);
        user.sendEmailVerification().addOnCompleteListener(task -> {
            setLoading(false);
            showToast(task.isSuccessful() ? R.string.verify_resent : R.string.verify_resend_failed);
        });
    }

    private void setLoading(boolean loading) {
        binding.verifyProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.checkVerifiedButton.setEnabled(!loading);
        binding.resendButton.setEnabled(!loading);
        binding.checkVerifiedButton.setAlpha(loading ? 0.5f : 1f);
        binding.resendButton.setAlpha(loading ? 0.5f : 1f);
    }

    private void showToast(int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
    }
}