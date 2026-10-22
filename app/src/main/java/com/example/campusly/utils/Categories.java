package com.example.campusly.utils;

import com.example.campusly.R;

public final class Categories {

    // These exact values are stored in Firestore and checked by the security rules.
    public static final String EVENTS = "events";
    public static final String SPORTS = "sports";
    public static final String EDUCATION = "education";
    public static final String GOSSIP = "gossip";
    public static final String ANNOUNCEMENTS = "announcements";

    private Categories() {
    }

    public static int labelFor(String category) {
        if (category == null) {
            return R.string.category_all;
        }
        switch (category) {
            case EVENTS:
                return R.string.category_events;
            case SPORTS:
                return R.string.category_sports;
            case EDUCATION:
                return R.string.category_education;
            case GOSSIP:
                return R.string.category_gossip;
            case ANNOUNCEMENTS:
                return R.string.category_announcements;
            default:
                return R.string.category_all;
        }
    }
}