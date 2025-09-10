// In: ui/SearchResultAdapter.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Context;
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

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<DevoteeDao.EnrichedDevotee> results = new ArrayList<>();
    private OnSearchResultClickListener listener;

    public interface OnSearchResultClickListener {
        void onSearchResultClick(DevoteeDao.EnrichedDevotee devotee);
    }

    public void setOnSearchResultClickListener(OnSearchResultClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_search_result, parent, false);
        return new ViewHolder(view, listener, () -> results);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void setSearchResults(List<DevoteeDao.EnrichedDevotee> newResults) {
        this.results = (newResults == null) ? new ArrayList<>() : newResults;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, mobileText, statusText;
        interface SearchResultProvider { List<DevoteeDao.EnrichedDevotee> getList(); }

        public ViewHolder(@NonNull View itemView, OnSearchResultClickListener listener, SearchResultProvider provider) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_search_devotee_name);
            mobileText = itemView.findViewById(R.id.text_search_devotee_mobile);
            statusText = itemView.findViewById(R.id.text_search_reg_status);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onSearchResultClick(provider.getList().get(pos));
                }
            });
        }

        void bind(DevoteeDao.EnrichedDevotee result) {
            Context context = itemView.getContext();
            nameText.setText(result.devotee().getFullName());
            mobileText.setText(result.devotee().getMobileE164());

            if (result.isPreRegisteredForCurrentEvent()) {
                statusText.setText("Pre-Reg");
                statusText.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_status_prereg));
                statusText.setVisibility(View.VISIBLE);
            } else {
                statusText.setText("Walk-in");
                statusText.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_status_walkin));
                statusText.setVisibility(View.VISIBLE); // Change to VISIBLE once you have the blue drawable
            }
        }
    }
}