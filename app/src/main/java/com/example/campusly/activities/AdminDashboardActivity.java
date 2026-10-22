package com.example.campusly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusly.R;
import com.example.campusly.adapters.ReportAdapter;
import com.example.campusly.databinding.ActivityAdminDashboardBinding;
import com.example.campusly.models.Post;
import com.example.campusly.models.Report;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.utils.NavigationUtils;
import com.example.campusly.utils.UiUtils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements ReportAdapter.Listener {

    private static final String TAG = "AdminDashboard";

    private ActivityAdminDashboardBinding binding;
    private final AdminRepository adminRepository = new AdminRepository();
    private ReportAdapter adapter;
    private ListenerRegistration reportsListener;
    private String adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavigationUtils.openAndClear(this, LoginActivity.class);
            return;
        }
        adminId = user.getUid();

        adapter = new ReportAdapter(this);
        binding.reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.reportsRecyclerView.setAdapter(adapter);

        // Only a friendly guard: even if someone opened this screen without
        // being an admin, the security rules would refuse to show any reports.
        adminRepository.checkIsAdmin(adminId, isAdmin -> {
            if (!isAdmin) {
                UiUtils.toast(this, R.string.admin_only);
                finish();
                return;
            }
            listenToReports();
            loadStats();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportsListener != null) {
            reportsListener.remove();
        }
    }

    private void listenToReports() {
        reportsListener = adminRepository.openReportsQuery().addSnapshotListener((snapshots, error) -> {
            binding.reportsProgress.setVisibility(View.GONE);

            if (error != null) {
                Log.e(TAG, "Loading reports failed", error);
                showReportsMessage(R.string.admin_reports_error);
                return;
            }

            List<Report> reports = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Report report = doc.toObject(Report.class,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                if (report != null) {
                    reports.add(report);
                }
            }
            // Newest first.
            reports.sort((a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            adapter.setReports(reports);
            binding.statOpenReports.setText(String.valueOf(reports.size()));
            if (reports.isEmpty()) {
                showReportsMessage(R.string.admin_no_reports);
            } else {
                binding.reportsEmptyText.setVisibility(View.GONE);
            }
        });
    }

    private void showReportsMessage(int messageRes) {
        binding.reportsEmptyText.setText(messageRes);
        binding.reportsEmptyText.setVisibility(View.VISIBLE);
    }

    // Counts are fetched from the server without downloading every document.
    // If one fails (e.g. offline) its tile just keeps showing "–".
    private void loadStats() {
        showCount(adminRepository.countPosts(), binding.statPosts);
        showCount(adminRepository.countUsers(), binding.statUsers);
        showCount(adminRepository.countResolvedReports(), binding.statResolvedReports);
    }

    private void showCount(Task<AggregateQuerySnapshot> task, TextView target) {
        task.addOnSuccessListener(result -> target.setText(String.valueOf(result.getCount())))
                .addOnFailureListener(e -> Log.w(TAG, "Count failed", e));
    }

    @Override
    public void onViewPost(Report report) {
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.EXTRA_POST_ID, report.getPostId());
        startActivity(intent);
    }

    @Override
    public void onDismissReport(Report report) {
        adminRepository.dismissReport(report.getReportId(), adminId)
                .addOnSuccessListener(unused -> {
                    UiUtils.toast(this, R.string.admin_report_dismissed);
                    loadStats();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Dismiss failed", e);
                    UiUtils.toast(this, R.string.admin_action_failed);
                });
    }

    @Override
    public void onRemovePost(Report report) {
        UiUtils.confirm(this, R.string.admin_remove_title, R.string.admin_remove_message,
                R.string.delete_post_confirm, () ->
                        adminRepository.removePost(report.getPostId(),
                                Post.ANONYMOUS_ID.equals(report.getPostAuthorId()), adminId)
                                .addOnSuccessListener(unused -> {
                                    UiUtils.toast(this, R.string.post_deleted);
                                    loadStats();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Remove failed", e);
                                    UiUtils.toast(this, R.string.admin_action_failed);
                                }));
    }
}
