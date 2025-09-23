// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDevoteeAdapter.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

public class ReportDevoteeAdapter extends RecyclerView.Adapter<ReportDevoteeAdapter.ViewHolder> {

    private List<DevoteeDao.EnrichedDevotee> devotees = new ArrayList<>();

    public void setDevotees(List<DevoteeDao.EnrichedDevotee> devotees) {
        this.devotees = devotees;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_report_devotee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(devotees.get(position));
    }

    @Override
    public int getItemCount() {
        return devotees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText, mobileText, attendanceText, groupText, lastSeenText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_devotee_name);
            mobileText = itemView.findViewById(R.id.text_devotee_mobile);
            attendanceText = itemView.findViewById(R.id.text_total_attendance);
            groupText = itemView.findViewById(R.id.text_whatsapp_group);
            lastSeenText = itemView.findViewById(R.id.text_last_seen);
        }

        void bind(DevoteeDao.EnrichedDevotee enriched) {
            nameText.setText(enriched.devotee().getFullName());
            mobileText.setText(enriched.devotee().getMobileE164());

            // Handle attendance badge visibility
            if (enriched.cumulativeAttendance() > 0) {
                attendanceText.setText(itemView.getContext().getString(R.string.total_attendance_format, enriched.cumulativeAttendance()));
                attendanceText.setVisibility(View.VISIBLE);
            } else {
                attendanceText.setVisibility(View.GONE);
            }

            // Handle WhatsApp group badge text and color
            if (enriched.whatsAppGroup() != null && enriched.whatsAppGroup() > 0) {
                groupText.setText(itemView.getContext().getString(R.string.whatsapp_group_format, enriched.whatsAppGroup()));
                Drawable greenBg = ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_status_prereg);
                groupText.setBackground(greenBg); // Green for "In Group"
                groupText.setVisibility(View.VISIBLE);
            } else {
                groupText.setText(R.string.not_in_group);
                Drawable grayBg = ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_status_present);
                groupText.setBackground(grayBg); // Gray for "Not in Group"
                groupText.setVisibility(View.VISIBLE);
            }

            // Handle last seen text visibility
            if (enriched.lastAttendanceDate() != null && !enriched.lastAttendanceDate().isEmpty()) {
                lastSeenText.setText(itemView.getContext().getString(R.string.last_seen_format, enriched.lastAttendanceDate()));
                lastSeenText.setVisibility(View.VISIBLE);
            } else {
                lastSeenText.setVisibility(View.GONE);
            }
        }
    }
}
