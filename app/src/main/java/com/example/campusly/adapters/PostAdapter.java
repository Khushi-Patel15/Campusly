package com.example.campusly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusly.R;
import com.example.campusly.databinding.ItemPostBinding;
import com.example.campusly.models.Post;
import com.example.campusly.utils.Categories;
import com.example.campusly.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface Listener {
        // The ⋮ button was tapped; anchor is the button, for placing the menu.
        void onPostOptions(Post post, View anchor);

        void onSharePost(Post post);

        void onLikePost(Post post);

        void onCommentPost(Post post);
    }

    private final List<Post> posts = new ArrayList<>();
    private final String currentUserId;
    private final Listener listener;

    public PostAdapter(String currentUserId, Listener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setPosts(List<Post> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        bindPost(holder.binding, posts.get(position), currentUserId, listener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        final ItemPostBinding binding;

        PostViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // Shared by the feed and the post details screen so a post card looks
    // and behaves the same everywhere.
    public static void bindPost(ItemPostBinding binding, Post post,
                                String currentUserId, Listener listener) {
        Context context = binding.getRoot().getContext();

        String name = post.getAuthorName() == null ? "" : post.getAuthorName();
        if (post.isAnonymous()) {
            binding.postAvatarText.setText("?");
        } else {
            binding.postAvatarText.setText(
                    name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT));
        }
        binding.postAuthorText.setText(name);
        binding.postMetaText.setText(context.getString(R.string.post_meta,
                TimeUtils.relative(context, post.getCreatedAt()),
                context.getString(Categories.labelFor(post.getCategory()))));
        binding.postContentText.setText(post.getContent());

        boolean liked = post.isLikedBy(currentUserId);
        int likes = post.getLikeCount();
        if (likes == 0) {
            binding.likeButton.setText(R.string.action_like);
        } else {
            binding.likeButton.setText(context.getString(
                    liked ? R.string.like_count_liked : R.string.like_count, likes));
        }
        binding.likeButton.setTextColor(context.getColor(
                liked ? R.color.neon_lime : R.color.text_secondary));

        binding.postMoreButton.setOnClickListener(v -> listener.onPostOptions(post, v));
        binding.shareButton.setOnClickListener(v -> listener.onSharePost(post));
        binding.likeButton.setOnClickListener(v -> listener.onLikePost(post));
        binding.commentButton.setOnClickListener(v -> listener.onCommentPost(post));
    }
}
