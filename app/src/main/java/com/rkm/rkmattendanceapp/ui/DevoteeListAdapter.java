// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListAdapter.java
package com.rkm.rkmattendanceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

public class DevoteeListAdapter extends RecyclerView.Adapter<DevoteeListAdapter.DevoteeViewHolder> {

    private List<DevoteeDao.EnrichedDevotee> devoteeList = new ArrayList<>();
    private OnDevoteeClickListener listener;

    // Interface for click events
    public interface OnDevoteeClickListener {
        void onDevoteeClick(Devotee devotee);
    }

    public void setOnDevoteeClickListener(OnDevoteeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DevoteeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_devotee, parent, false);
        return new DevoteeViewHolder(view, listener, () -> devoteeList);
    }

    @Override
    public void onBindViewHolder(@NonNull DevoteeViewHolder holder, int position) {
        DevoteeDao.EnrichedDevotee current = devoteeList.get(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        return devoteeList.size();
    }

    public void setDevotees(List<DevoteeDao.EnrichedDevotee> devotees) {
        this.devoteeList = devotees;
        notifyDataSetChanged();
    }

    static class DevoteeViewHolder extends RecyclerView.ViewHolder {
        private final TextView devoteeNameTextView;
        private final TextView devoteeMobileTextView;

        // Functional interface to get the current list from the adapter
        interface DevoteeListProvider {
            List<DevoteeDao.EnrichedDevotee> getList();
        }

        public DevoteeViewHolder(@NonNull View itemView, OnDevoteeClickListener listener, DevoteeListProvider listProvider) {
            super(itemView);
            devoteeNameTextView = itemView.findViewById(R.id.text_devotee_name);
            devoteeMobileTextView = itemView.findViewById(R.id.text_devotee_mobile);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // Pass the plain Devotee object to the listener
                    listener.onDevoteeClick(listProvider.getList().get(position).devotee());
                }
            });
        }

        public void bind(DevoteeDao.EnrichedDevotee enrichedDevotee) {
            devoteeNameTextView.setText(enrichedDevotee.devotee().getFullName());
            devoteeMobileTextView.setText(enrichedDevotee.devotee().getMobileE164());
        }
    }
}