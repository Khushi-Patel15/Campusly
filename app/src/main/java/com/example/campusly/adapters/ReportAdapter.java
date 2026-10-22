package com.example.campusly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusly.R;
import com.example.campusly.databinding.ItemReportBinding;
import com.example.campusly.models.Report;
import com.example.campusly.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface Listener {
        void onViewPost(Report report);

        void onDismissReport(Report report);

        void onRemovePost(Report report);
    }

    private final List<Report> reports = new ArrayList<>();
    private final Listener listener;

    public ReportAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setReports(List<Report> newReports) {
        reports.clear();
        reports.addAll(newReports);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportBinding binding = ItemReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        ItemReportBinding binding = holder.binding;
        Context context = binding.getRoot().getContext();

        String reason = report.getReason() == null ? "" : report.getReason().toUpperCase(Locale.ROOT);
        binding.reportReasonText.setText(context.getString(R.string.report_header,
                reason, TimeUtils.relative(context, report.getCreatedAt())));

        boolean hasDetails = report.getDescription() != null && !report.getDescription().isEmpty();
        binding.reportDescriptionText.setVisibility(hasDetails ? View.VISIBLE : View.GONE);
        binding.reportDescriptionText.setText(report.getDescription());

        binding.reportPostAuthorText.setText(report.getPostAuthorName());
        binding.reportPostContentText.setText(report.getPostContent());

        binding.reportViewButton.setOnClickListener(v -> listener.onViewPost(report));
        binding.reportDismissButton.setOnClickListener(v -> listener.onDismissReport(report));
        binding.reportRemoveButton.setOnClickListener(v -> listener.onRemovePost(report));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {

        final ItemReportBinding binding;

        ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
