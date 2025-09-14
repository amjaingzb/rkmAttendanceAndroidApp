// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/MappingAdapter.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.rkmattendanceapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MappingAdapter extends RecyclerView.Adapter<MappingAdapter.ViewHolder> {

    public interface MappingChangeListener {
        void onMappingChanged();
    }

    private static final String[] TARGET_VALUES = {
            "DROP", "RETAIN", "full_name", "mobile", "address", "age", "email", "gender"
    };
    private static final String[] TARGET_LABELS = {
            "Drop (Ignore this column)", "Retain (Save in extras)", "Full Name",
            "Mobile Number", "Address", "Age", "Email", "Gender"
    };

    private final List<String> csvHeaders;
    private final String[] currentMappingValues;
    private final MappingChangeListener changeListener;

    public MappingAdapter(Context context, List<String> csvHeaders, MappingChangeListener listener) {
        this.csvHeaders = csvHeaders;
        this.changeListener = listener;
        this.currentMappingValues = new String[csvHeaders.size()];
        Map<String, String> guessedMapping = CsvImporter.guessTargets(csvHeaders);
        for (int i = 0; i < csvHeaders.size(); i++) {
            String header = csvHeaders.get(i);
            String guessedTarget = guessedMapping.get(header);
            if (guessedTarget != null) {
                currentMappingValues[i] = guessedTarget;
            } else {
                currentMappingValues[i] = TARGET_VALUES[0]; // Default to "DROP"
            }
        }
    }

    // --- START OF FIX #2 ---
    /**
     * Toggles all mappings between DROP and RETAIN based on the switch state.
     * @param retainAll true to change all DROPs to RETAIN, false to change all RETAINs to DROP.
     */
    public void toggleDropRetain(boolean retainAll) {
        if (retainAll) {
            for (int i = 0; i < currentMappingValues.length; i++) {
                if ("DROP".equals(currentMappingValues[i])) {
                    currentMappingValues[i] = "RETAIN";
                }
            }
        } else {
            for (int i = 0; i < currentMappingValues.length; i++) {
                if ("RETAIN".equals(currentMappingValues[i])) {
                    currentMappingValues[i] = "DROP";
                }
            }
        }
        notifyDataSetChanged(); // This is crucial to update the UI
        if (changeListener != null) {
            changeListener.onMappingChanged(); // Re-validate after the toggle
        }
    }
    // --- END OF FIX #2 ---

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_mapping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return csvHeaders.size();
    }

    public ImportMapping getFinalMapping() {
        ImportMapping finalMapping = new ImportMapping();
        for (int i = 0; i < csvHeaders.size(); i++) {
            finalMapping.put(csvHeaders.get(i), currentMappingValues[i]);
        }
        return finalMapping;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        Spinner targetSpinner;
        ArrayAdapter<String> spinnerAdapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.text_csv_header);
            targetSpinner = itemView.findViewById(R.id.spinner_target_field);
            spinnerAdapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(Arrays.asList(TARGET_LABELS)));
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            targetSpinner.setAdapter(spinnerAdapter);
        }

        void bind(int position) {
            String header = csvHeaders.get(position);
            headerText.setText(header);
            String currentSelectionValue = currentMappingValues[position];
            int selectionIndex = 0;
            for (int i = 0; i < TARGET_VALUES.length; i++) {
                if (TARGET_VALUES[i].equals(currentSelectionValue)) {
                    selectionIndex = i;
                    break;
                }
            }
            targetSpinner.setSelection(selectionIndex, false);
            targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    currentMappingValues[getAdapterPosition()] = TARGET_VALUES[pos];
                    if (changeListener != null) {
                        changeListener.onMappingChanged();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }
}
