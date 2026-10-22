package com.example.campusly.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.campusly.R;
import com.example.campusly.models.Post;

public final class UiUtils {

    private UiUtils() {
    }

    public static void toast(Context context, int messageRes) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show();
    }

    public static void confirm(Context context, int titleRes, int messageRes,
                               int confirmRes, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(confirmRes, (dialog, which) -> onConfirm.run())
                .show();
    }

    public static void sharePost(Context context, Post post) {
        String text = context.getString(R.string.share_post_text,
                post.getAuthorName(), post.getContent());
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(send,
                context.getString(R.string.share_post_chooser)));
    }
}