package com.example.campusly.utils;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.campusly.R;
import com.example.campusly.activities.NotificationsActivity;
import com.example.campusly.activities.PostDetailsActivity;
import com.example.campusly.databinding.ViewNotificationPopupBinding;
import com.example.campusly.models.AppNotification;
import com.example.campusly.views.ShootingStarView;

import java.util.List;

// Shows new notifications as a shooting star crossing the top of the current
// screen, followed by a small card. Tapping the card opens the post (or the
// Notifications screen when several arrived together).
public final class ShootingStarPopup {

    private static final long STAR_DURATION_MS = 1100;
    private static final long CARD_VISIBLE_MS = 3500;
    private static final long CARD_ANIM_MS = 300;

    private ShootingStarPopup() {
    }

    public static void show(Activity activity, List<AppNotification> notifications) {
        if (notifications.isEmpty() || activity.isFinishing()) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        float density = activity.getResources().getDisplayMetrics().density;

        // A see-through layer on top of the screen. It isn't clickable, so taps
        // outside the card still reach the screen underneath.
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ShootingStarView star = new ShootingStarView(activity);
        overlay.addView(star, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ViewNotificationPopupBinding card =
                ViewNotificationPopupBinding.inflate(LayoutInflater.from(activity), overlay, false);
        bindCard(activity, card, notifications);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        int side = (int) (16 * density);
        cardParams.setMargins(side, (int) (12 * density) + topInset(root), side, 0);
        overlay.addView(card.getRoot(), cardParams);

        float hiddenOffset = -24 * density;
        card.getRoot().setAlpha(0f);
        card.getRoot().setTranslationY(hiddenOffset);

        Runnable remove = () -> root.removeView(overlay);
        Runnable hideCard = () -> card.getRoot().animate()
                .alpha(0f).translationY(hiddenOffset).setDuration(CARD_ANIM_MS)
                .withEndAction(remove);
        Runnable showCard = () -> card.getRoot().animate()
                .alpha(1f).translationY(0f).setDuration(CARD_ANIM_MS)
                .withEndAction(() -> card.getRoot().postDelayed(hideCard, CARD_VISIBLE_MS));

        card.getRoot().setOnClickListener(v -> {
            open(activity, notifications);
            card.getRoot().removeCallbacks(hideCard);
            remove.run();
        });

        root.addView(overlay);

        if (animationsEnabled(activity)) {
            star.play(STAR_DURATION_MS, showCard, () -> { });
        } else {
            // "Remove animations" is on in Android settings: no star, just the card.
            showCard.run();
        }
    }

    private static void bindCard(Activity activity, ViewNotificationPopupBinding card,
                                 List<AppNotification> notifications) {
        if (notifications.size() == 1) {
            AppNotification n = notifications.get(0);
            boolean isComment = AppNotification.TYPE_COMMENT.equals(n.getType());
            card.popupIconText.setText(isComment ? "💬" : "❤️");
            card.popupTitleText.setText(activity.getString(
                    isComment ? R.string.notification_comment : R.string.notification_like,
                    n.getFromName()));
            card.popupPreviewText.setText(n.getPreview());
        } else {
            card.popupIconText.setText("✨");
            card.popupTitleText.setText(activity.getString(
                    R.string.popup_many_title, notifications.size()));
            card.popupPreviewText.setText(R.string.popup_many_preview);
        }
    }

    private static void open(Activity activity, List<AppNotification> notifications) {
        Intent intent;
        if (notifications.size() == 1) {
            intent = new Intent(activity, PostDetailsActivity.class);
            intent.putExtra(PostDetailsActivity.EXTRA_POST_ID, notifications.get(0).getPostId());
        } else {
            intent = new Intent(activity, NotificationsActivity.class);
        }
        activity.startActivity(intent);
    }

    // Keeps the card below the status bar on phones that draw behind it.
    private static int topInset(ViewGroup root) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
        if (insets == null) return 0;
        Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        int[] location = new int[2];
        root.getLocationOnScreen(location);
        // If the screen content already starts below the status bar, no extra space is needed.
        return Math.max(0, bars.top - location[1]);
    }

    private static boolean animationsEnabled(Activity activity) {
        float scale = Settings.Global.getFloat(activity.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale > 0f;
    }
}
