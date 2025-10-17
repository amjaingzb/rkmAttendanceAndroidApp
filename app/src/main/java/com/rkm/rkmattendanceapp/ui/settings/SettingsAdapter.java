// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/settings/SettingsAdapter.java
package com.rkm.rkmattendanceapp.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rkm.attendance.model.ConfigItem;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

    public interface OnSettingClickListener {
        void onSettingClick(ConfigItem item);
    }

    private List<ConfigItem> configItems = new ArrayList<>();
    private OnSettingClickListener listener;

    public void setOnSettingClickListener(OnSettingClickListener listener) {
        this.listener = listener;
    }

    public void setConfigItems(List<ConfigItem> items) {
        this.configItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfigItem item = configItems.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSettingClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return configItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText, valueText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_setting_name);
            valueText = itemView.findViewById(R.id.text_setting_value);
        }

        void bind(ConfigItem item) {
            nameText.setText(item.displayName);
            if (item.isProtected) {
                valueText.setText("****");
            } else {
                valueText.setText(item.value);
            }
        }
    }
}
