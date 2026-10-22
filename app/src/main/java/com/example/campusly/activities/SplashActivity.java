package com.example.campusly.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusly.R;
import com.example.campusly.utils.NavigationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }

        // reload() fetches the latest verified status. If it fails because we're
        // offline, we fall back to the last known status instead of blocking the app.
        user.reload().addOnCompleteListener(reloadTask -> {
            if (!reloadTask.isSuccessful()
                    && reloadTask.getException() instanceof FirebaseAuthInvalidUserException) {
                // Account was deleted or disabled in the Firebase Console.
                auth.signOut();
                NavigationUtils.openAndClear(this, LoginActivity.class);
                return;
            }

            if (!user.isEmailVerified()) {
                NavigationUtils.openAndClear(this, VerifyEmailActivity.class);
                return;
            }

            // Firestore rules read "email_verified" from the login token, which only
            // updates when the token is refreshed, so force a refresh here.
            user.getIdToken(true).addOnCompleteListener(tokenTask ->
                    NavigationUtils.openAndClear(this, MainActivity.class));
        });
    }
}