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

        // reload() checks the account still exists. If it fails because we're
        // offline, we carry on with the saved login instead of blocking the app.
        user.reload().addOnCompleteListener(reloadTask -> {
            if (!reloadTask.isSuccessful()
                    && reloadTask.getException() instanceof FirebaseAuthInvalidUserException) {
                // Account was deleted or disabled in the Firebase Console.
                auth.signOut();
                NavigationUtils.openAndClear(this, LoginActivity.class);
                return;
            }
            NavigationUtils.openAndClear(this, MainActivity.class);
        });
    }
}
