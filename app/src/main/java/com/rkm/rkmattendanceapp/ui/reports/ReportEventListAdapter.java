// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportEventListAdapter.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.db.EventDao;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

public class ReportEventListAdapter extends RecyclerView.Adapter<ReportEventListAdapter.ViewHolder> {

    public interface OnEventReportClickListener {
        void onExportClick(EventDao.EventWithAttendance event);
    }

    private List<EventDao.EventWithAttendance> events = new ArrayList<>();
    private OnEventReportClickListener listener;

    public void setOnEventReportClickListener(OnEventReportClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<EventDao.EventWithAttendance> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_report_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameText, eventDateText, totalAttendedText;
        private final ImageButton exportButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameText = itemView.findViewById(R.id.text_event_name);
            eventDateText = itemView.findViewById(R.id.text_event_date);
            totalAttendedText = itemView.findViewById(R.id.text_total_attended);
            exportButton = itemView.findViewById(R.id.button_export);
        }

        void bind(EventDao.EventWithAttendance event) {
            eventNameText.setText(event.eventName);
            eventDateText.setText(event.eventDate);
            totalAttendedText.setText(itemView.getContext().getString(R.string.total_attended_format, event.totalAttended));
            
            // Enable or disable the button based on attendance count
            if (event.totalAttended > 0) {
                exportButton.setEnabled(true);
                exportButton.setAlpha(1.0f);
                exportButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onExportClick(event);
                    }
                });
            } else {
                exportButton.setEnabled(false);
                exportButton.setAlpha(0.5f);
                exportButton.setOnClickListener(null);
            }
        }
    }
}
