// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDonationDaysAdapter.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.DailySummary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportDonationDaysAdapter extends RecyclerView.Adapter<ReportDonationDaysAdapter.ViewHolder> {

    public interface OnDayReportClickListener {
        void onEmailClick(DailySummary summary);
        void onShareClick(DailySummary summary);
    }

    private List<DailySummary> summaries = new ArrayList<>();
    private OnDayReportClickListener listener;

    private final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US);
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public void setOnDayReportClickListener(OnDayReportClickListener listener) {
        this.listener = listener;
    }

    public void setSummaries(List<DailySummary> summaries) {
        this.summaries = summaries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_report_donation_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailySummary summary = summaries.get(position);
        holder.bind(summary);
        holder.emailButton.setOnClickListener(v -> {
            if (listener != null) listener.onEmailClick(summary);
        });
        holder.shareButton.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(summary);
        });
    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText, totalAmountText, summaryDetailsText;
        private final ImageButton emailButton, shareButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.text_date);
            totalAmountText = itemView.findViewById(R.id.text_total_amount);
            summaryDetailsText = itemView.findViewById(R.id.text_summary_details);
            emailButton = itemView.findViewById(R.id.button_email);
            shareButton = itemView.findViewById(R.id.button_share);
        }

        void bind(DailySummary summary) {
            try {
                LocalDate date = LocalDate.parse(summary.date, inputFormatter);
                dateText.setText(outputFormatter.format(date));
            } catch (Exception e) {
                dateText.setText(summary.date); // Fallback to raw date
            }
            
            totalAmountText.setText("Total: " + currencyFormatter.format(summary.totalAmount));
            
            String batchText = summary.batchCount + (summary.batchCount == 1 ? " Batch" : " Batches");
            String donationText = summary.donationCount + (summary.donationCount == 1 ? " Donation" : " Donations");
            summaryDetailsText.setText(batchText + ", " + donationText);
        }
    }
}
