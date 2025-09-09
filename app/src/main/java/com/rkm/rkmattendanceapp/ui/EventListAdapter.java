// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListAdapter.java
package com.rkm.rkmattendanceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

    private List<Event> eventList = new ArrayList<>();

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event currentEvent = eventList.get(position);
        holder.bind(currentEvent);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void setEvents(List<Event> events) {
        this.eventList = events;
        notifyDataSetChanged(); // Tell the RecyclerView to refresh
    }

    // ViewHolder class
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameTextView;
        private final TextView eventDateTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameTextView = itemView.findViewById(R.id.text_event_name);
            eventDateTextView = itemView.findViewById(R.id.text_event_date);
        }

        public void bind(Event event) {
            eventNameTextView.setText(event.getEventName());
            eventDateTextView.setText(event.getEventDate());
        }
    }
}