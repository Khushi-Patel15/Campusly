package com.example.campusly.repositories;

import com.example.campusly.models.Report;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// Admin-only Firestore operations. Important: the checks in the app only
// decide which buttons to show. What admins are actually ALLOWED to do is
// enforced by firestore.rules, which look up the same "admins" collection.
public class AdminRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    private static final String ADMINS = "admins";
    private static final String REPORTS = "reports";
    private static final String POSTS = "posts";
    private static final String USERS = "users";

    // Remembered after the first check so every screen doesn't ask again.
    private static String cachedUserId;
    private static boolean cachedIsAdmin;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // A user is an admin if admins/{theirUserId} exists. That document can only
    // be created from the Firebase Console; the rules block it from the app.
    public void checkIsAdmin(String userId, Callback<Boolean> callback) {
        if (userId.equals(cachedUserId)) {
            callback.onResult(cachedIsAdmin);
            return;
        }
        db.collection(ADMINS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    cachedUserId = userId;
                    cachedIsAdmin = doc.exists();
                    callback.onResult(cachedIsAdmin);
                })
                // Not cached, so it's checked again next time (e.g. once back online).
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Called on logout so the next user on this phone is checked fresh.
    public static void clearCache() {
        cachedUserId = null;
        cachedIsAdmin = false;
    }

    // No orderBy here, so no extra Firestore index is needed; the dashboard
    // sorts the (small) list itself.
    public Query openReportsQuery() {
        return db.collection(REPORTS).whereEqualTo("status", Report.STATUS_OPEN);
    }

    public Task<Void> dismissReport(String reportId, String adminId) {
        return db.collection(REPORTS).document(reportId)
                .update(resolution(Report.STATUS_DISMISSED, adminId));
    }

    // Deletes the post and closes every open report about it in one batch,
    // so either everything happens or nothing does.
    public Task<Void> removePost(String postId, boolean anonymous, String adminId) {
        return openReportsQuery().whereEqualTo("postId", postId).get()
                .continueWithTask(reportsTask -> {
                    WriteBatch batch = db.batch();
                    batch.delete(db.collection(POSTS).document(postId));
                    if (anonymous) {
                        batch.delete(db.collection(POSTS).document(postId)
                                .collection("private").document("author"));
                    }
                    if (reportsTask.isSuccessful()) {
                        for (DocumentSnapshot report : reportsTask.getResult()) {
                            batch.update(report.getReference(),
                                    resolution(Report.STATUS_REMOVED, adminId));
                        }
                    }
                    return batch.commit();
                });
    }

    public Task<AggregateQuerySnapshot> countPosts() {
        return db.collection(POSTS).count().get(AggregateSource.SERVER);
    }

    public Task<AggregateQuerySnapshot> countUsers() {
        return db.collection(USERS).count().get(AggregateSource.SERVER);
    }

    public Task<AggregateQuerySnapshot> countResolvedReports() {
        return db.collection(REPORTS)
                .whereIn("status", Arrays.asList(Report.STATUS_DISMISSED, Report.STATUS_REMOVED))
                .count().get(AggregateSource.SERVER);
    }

    private static Map<String, Object> resolution(String status, String adminId) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        update.put("resolvedBy", adminId);
        update.put("resolvedAt", FieldValue.serverTimestamp());
        return update;
    }
}
