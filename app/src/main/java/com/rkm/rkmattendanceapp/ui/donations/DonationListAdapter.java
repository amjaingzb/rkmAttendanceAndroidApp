// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationListAdapter.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rkm.rkmattendanceapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DonationListAdapter extends RecyclerView.Adapter<DonationListAdapter.ViewHolder> {

    public interface OnDonationClickListener {
        void onDonationClick(DonationRecord donationRecord);
    }

    private List<DonationRecord> donationRecords = new ArrayList<>();
    private OnDonationClickListener listener;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public void setOnDonationClickListener(OnDonationClickListener listener) {
        this.listener = listener;
    }

    public void setDonations(List<DonationRecord> records) {
        this.donationRecords = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_donation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonationRecord record = donationRecords.get(position);
        holder.bind(record);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDonationClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return donationRecords.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView devoteeNameText, purposeText, amountText, paymentMethodText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            devoteeNameText = itemView.findViewById(R.id.text_devotee_name);
            purposeText = itemView.findViewById(R.id.text_purpose);
            amountText = itemView.findViewById(R.id.text_amount);
            paymentMethodText = itemView.findViewById(R.id.text_payment_method);
        }

        void bind(DonationRecord record) {
            devoteeNameText.setText(record.devoteeName);
            purposeText.setText(itemView.getContext().getString(R.string.donation_purpose_format, record.donation.purpose));
            amountText.setText(currencyFormatter.format(record.donation.amount));
            paymentMethodText.setText(record.donation.paymentMethod);
        }
    }
}
