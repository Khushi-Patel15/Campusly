package com.example.campusly.utils;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;

import com.example.campusly.R;
import com.example.campusly.databinding.DialogReportBinding;
import com.example.campusly.models.Post;
import com.example.campusly.repositories.AdminRepository;
import com.example.campusly.repositories.PostRepository;

// The ⋮ menu on a post, shared by the feed and the post details screen:
//   your own post        -> Delete
//   someone else's post  -> Report (+ "Remove post (admin)" for admins)
public final class PostOptions {

    private static final String TAG = "PostOptions";
    private static final int MENU_DELETE = 1;
    private static final int MENU_REPORT = 2;
    private static final int MENU_ADMIN_REMOVE = 3;

    private PostOptions() {
    }

    // onDeleting runs just before a delete starts (may be null); the post
    // details screen uses it to know the post is disappearing on purpose.
    public static void show(Activity activity, View anchor, Post post, String userId,
                            boolean isAdmin, Runnable onDeleting) {
        if (post.isAnonymous()) {
            // The post doesn't say who wrote it, so ask the private author
            // document (only readable by the real author or an admin).
            new PostRepository().isMyAnonymousPost(post.getPostId(), userId, isMine ->
                    showMenu(activity, anchor, post, userId, isAdmin, isMine, onDeleting));
        } else {
            showMenu(activity, anchor, post, userId, isAdmin,
                    userId.equals(post.getAuthorId()), onDeleting);
        }
    }

    private static void showMenu(Activity activity, View anchor, Post post, String userId,
                                 boolean isAdmin, boolean isOwnPost, Runnable onDeleting) {
        if (activity.isFinishing()) return;

        PopupMenu menu = new PopupMenu(activity, anchor);
        if (isOwnPost) {
            menu.getMenu().add(0, MENU_DELETE, 0, R.string.post_option_delete);
        } else {
            menu.getMenu().add(0, MENU_REPORT, 0, R.string.post_option_report);
            if (isAdmin) {
                menu.getMenu().add(0, MENU_ADMIN_REMOVE, 1, R.string.post_option_admin_remove);
            }
        }

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == MENU_DELETE) {
                confirmDelete(activity, post, userId, false, onDeleting);
            } else if (id == MENU_ADMIN_REMOVE) {
                confirmDelete(activity, post, userId, true, onDeleting);
            } else if (id == MENU_REPORT) {
                showReportDialog(activity, post, userId);
            }
            return true;
        });
        menu.show();
    }

    private static void confirmDelete(Activity activity, Post post, String userId,
                                      boolean asAdmin, Runnable onDeleting) {
        UiUtils.confirm(activity, R.string.delete_post_title, R.string.delete_post_message,
                R.string.delete_post_confirm, () -> {
                    if (onDeleting != null) {
                        onDeleting.run();
                    }
                    // Admin removals also close any open reports about the post.
                    (asAdmin
                            ? new AdminRepository().removePost(post.getPostId(), post.isAnonymous(), userId)
                            : new PostRepository().deletePost(post))
                            .addOnSuccessListener(unused ->
                                    UiUtils.toast(activity, R.string.post_deleted))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Deleting post failed", e);
                                UiUtils.toast(activity, R.string.post_delete_failed);
                            });
                });
    }

    private static void showReportDialog(Activity activity, Post post, String userId) {
        if (!NetworkUtils.isOnline(activity)) {
            UiUtils.toast(activity, R.string.report_offline);
            return;
        }

        DialogReportBinding binding = DialogReportBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.report_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.report_submit, null)
                .create();

        // Set the click handler after show() so a missing reason doesn't close the dialog.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String reason = selectedReason(binding);
                    if (reason == null) {
                        UiUtils.toast(activity, R.string.report_pick_reason);
                        return;
                    }
                    String details = binding.reportDescriptionInput.getText().toString().trim();

                    v.setEnabled(false);
                    new PostRepository().reportPost(post, userId, reason, details)
                            .addOnSuccessListener(unused -> {
                                UiUtils.toast(activity, R.string.report_sent);
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Report failed", e);
                                UiUtils.toast(activity, R.string.report_failed);
                                dialog.dismiss();
                            });
                }));
        dialog.show();
    }

    private static String selectedReason(DialogReportBinding binding) {
        int checkedId = binding.reasonGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.reasonSpam) return "spam";
        if (checkedId == R.id.reasonHarassment) return "harassment";
        if (checkedId == R.id.reasonInappropriate) return "inappropriate";
        if (checkedId == R.id.reasonOther) return "other";
        return null;
    }
}
