package com.example.campusly.utils;

import java.util.Locale;

public final class AppConfig {

    // Placeholder: replace with the confirmed official NMIMS student email domain.
    // The same domain must also be set in the Firestore rules, or verified users will be blocked.
    public static final String COLLEGE_EMAIL_DOMAIN = "@nmims.in";

    public static final int MIN_PASSWORD_LENGTH = 8;
    // Location used for the weather widget on Home. These are Indore city
    // coordinates; weather is the same across the city, so the exact campus
    // spot isn't needed.
    public static final String CAMPUS_NAME = "NMIMS Indore";
    public static final double CAMPUS_LATITUDE = 22.72;
    public static final double CAMPUS_LONGITUDE = 75.86;

    private AppConfig() {
    }

    public static boolean isCollegeEmail(String email) {
        return email != null
                && email.trim().toLowerCase(Locale.ROOT).endsWith(COLLEGE_EMAIL_DOMAIN);
    }
}