// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListAdapter.java
package com.rkm.rkmattendanceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

// This adapter is now simpler and works with the basic Devotee model.
public class DevoteeListAdapter extends RecyclerView.Adapter<DevoteeListAdapter.DevoteeViewHolder> {

    // 1. Change the internal list type to List<Devotee>
    private List<Devotee> devoteeList = new ArrayList<>();
    private OnDevoteeClickListener listener;

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
        Devotee current = devoteeList.get(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        return devoteeList.size();
    }

    // 2. Change the public setter method's parameter to List<Devotee>
    public void setDevotees(List<Devotee> devotees) {
        this.devoteeList = devotees;
        notifyDataSetChanged();
    }

    static class DevoteeViewHolder extends RecyclerView.ViewHolder {
        private final TextView devoteeNameTextView;
        private final TextView devoteeMobileTextView;

        // 3. Update the provider interface
        interface DevoteeListProvider {
            List<Devotee> getList();
        }

        public DevoteeViewHolder(@NonNull View itemView, OnDevoteeClickListener listener, DevoteeListProvider listProvider) {
            super(itemView);
            devoteeNameTextView = itemView.findViewById(R.id.text_devotee_name);
            devoteeMobileTextView = itemView.findViewById(R.id.text_devotee_mobile);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // 4. This now passes the Devotee object directly
                    listener.onDevoteeClick(listProvider.getList().get(position));
                }
            });
        }

        // 5. Update the bind method to accept a Devotee
        public void bind(Devotee devotee) {
            devoteeNameTextView.setText(devotee.getFullName());
            devoteeMobileTextView.setText(devotee.getMobileE164());
        }
    }
}