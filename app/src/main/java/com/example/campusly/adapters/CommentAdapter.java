package com.example.campusly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusly.databinding.ItemCommentBinding;
import com.example.campusly.models.Comment;
import com.example.campusly.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface Listener {
        void onDeleteComment(Comment comment);
    }

    private final List<Comment> comments = new ArrayList<>();
    private final String currentUserId;
    private final Listener listener;
    private boolean isAdmin = false;
    // Ids of anonymous comments that belong to this user (checked separately,
    // since the comment itself doesn't say who wrote it).
    private final Set<String> myAnonymousCommentIds = new HashSet<>();

    public CommentAdapter(String currentUserId, Listener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    // Admins can delete anyone's comment (the security rules allow it too).
    public void setAdmin(boolean admin) {
        isAdmin = admin;
        notifyDataSetChanged();
    }

    public void markAsMine(String commentId) {
        if (myAnonymousCommentIds.add(commentId)) {
            notifyDataSetChanged();
        }
    }

    public void setComments(List<Comment> newComments) {
        comments.clear();
        comments.addAll(newComments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        ItemCommentBinding binding = holder.binding;
        Context context = binding.getRoot().getContext();

        binding.commentAuthorText.setText(comment.getAuthorName());
        binding.commentTimeText.setText(TimeUtils.relative(context, comment.getCreatedAt()));
        binding.commentContentText.setText(comment.getContent());

        boolean canDelete = isAdmin
                || currentUserId.equals(comment.getAuthorId())
                || myAnonymousCommentIds.contains(comment.getCommentId());
        binding.commentDeleteText.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        binding.commentDeleteText.setOnClickListener(v -> listener.onDeleteComment(comment));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        final ItemCommentBinding binding;

        CommentViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
