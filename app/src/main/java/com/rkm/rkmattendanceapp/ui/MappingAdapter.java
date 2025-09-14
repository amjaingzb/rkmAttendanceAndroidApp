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

    private final Context context;
    private final List<String> csvHeaders;
    private final Map<String, String> guessedMapping;
    private final String[] currentMapping;

    // These are the database fields the user can map to.
    private static final String[] TARGET_FIELDS = {
            "Drop", "full_name", "mobile", "address", "age", "email", "gender"
    };

    public MappingAdapter(Context context, List<String> csvHeaders) {
        this.context = context;
        this.csvHeaders = csvHeaders;
        this.guessedMapping = CsvImporter.guessTargets(csvHeaders);
        this.currentMapping = new String[csvHeaders.size()];
        Arrays.fill(currentMapping, TARGET_FIELDS[0]); // Default all to "Drop"
    }

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
            finalMapping.put(csvHeaders.get(i), currentMapping[i]);
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
            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>(Arrays.asList(TARGET_FIELDS)));
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            targetSpinner.setAdapter(spinnerAdapter);
        }

        void bind(int position) {
            String header = csvHeaders.get(position);
            headerText.setText(header);

            // Set initial selection based on our guess
            String guessedTarget = guessedMapping.get(header);
            int selectionIndex = 0;
            if (guessedTarget != null) {
                for (int i = 0; i < TARGET_FIELDS.length; i++) {
                    if (TARGET_FIELDS[i].equals(guessedTarget)) {
                        selectionIndex = i;
                        break;
                    }
                }
            }
            targetSpinner.setSelection(selectionIndex);
            currentMapping[position] = TARGET_FIELDS[selectionIndex];

            targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    currentMapping[getAdapterPosition()] = TARGET_FIELDS[pos];
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }
}
