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
    // 1. Add a listener field
    private OnEventListener listener;

    // 2. Define the listener interface
    public interface OnEventListener {
        void onEventClick(Event event);
    }

    // 3. Create a setter for the listener
    public void setOnEventListener(OnEventListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event, parent, false);
        // 4. Pass the listener to the ViewHolder
        return new EventViewHolder(view, listener, () -> eventList);
    }

    // ... onBindViewHolder and getItemCount are unchanged ...
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(eventList.get(position));
    }
    @Override
    public int getItemCount() {
        return eventList.size();
    }
    public void setEvents(List<Event> events) {
        this.eventList = events;
        notifyDataSetChanged();
    }


    // 5. Update the ViewHolder to handle the click
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameTextView;
        private final TextView eventDateTextView;

        // Functional interface to get the current list from the adapter
        interface EventListProvider {
            List<Event> getList();
        }

        public EventViewHolder(@NonNull View itemView, OnEventListener listener, EventListProvider eventListProvider) {
            super(itemView);
            eventNameTextView = itemView.findViewById(R.id.text_event_name);
            eventDateTextView = itemView.findViewById(R.id.text_event_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onEventClick(eventListProvider.getList().get(position));
                }
            });
        }

        public void bind(Event event) {
            eventNameTextView.setText(event.getEventName());
            eventDateTextView.setText(event.getEventDate());
        }
    }
}