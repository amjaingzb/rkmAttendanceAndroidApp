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

    public interface MappingChangeListener { void onMappingChanged(); }

    private final List<String> csvHeaders;
    private final String[] currentMappingValues;
    private final MappingChangeListener changeListener;
    private final String[] targetValues;
    private final String[] targetLabels;

    public MappingAdapter(Context context, List<String> csvHeaders, MappingChangeListener listener, Privilege privilege, ImportType importType) {
        this.csvHeaders = csvHeaders;
        this.changeListener = listener;
        this.currentMappingValues = new String[csvHeaders.size()];

        List<String> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // STEP 5.1: Add the new fields to the list of available mapping targets.
        switch (importType) {
            case WHATSAPP:
                values.addAll(Arrays.asList("DROP", "phone", "whatsAppGroupId"));
                labels.addAll(Arrays.asList("Drop", "Phone Number", "WhatsApp Group ID"));
                break;
            default: // DEVOTEE and ATTENDANCE
                values.addAll(Arrays.asList("DROP", "RETAIN", "full_name", "mobile", "address", "age", "email", "gender", "aadhaar", "pan"));
                labels.addAll(Arrays.asList("Drop", "Retain", "Full Name", "Mobile Number", "Address", "Age", "Email", "Gender", "Aadhaar Number", "PAN Number"));
                if (privilege == Privilege.SUPER_ADMIN && importType == ImportType.ATTENDANCE) {
                    values.add("count");
                    labels.add("Count (Attendance)");
                }
                break;
        }

        this.targetValues = values.toArray(new String[0]);
        this.targetLabels = labels.toArray(new String[0]);

        Map<String, String> guessedMapping = CsvImporter.guessTargets(csvHeaders, importType);
        
        for (int i = 0; i < csvHeaders.size(); i++) {
            String guessedTarget = guessedMapping.get(csvHeaders.get(i));
            if (guessedTarget != null && values.contains(guessedTarget)) {
                currentMappingValues[i] = guessedTarget;
            } else {
                currentMappingValues[i] = targetValues[0]; // Default to DROP
            }
        }
    }

    public void toggleDropRetain(boolean retainAll) {
        for (int i = 0; i < currentMappingValues.length; i++) {
            if (retainAll) {
                if ("DROP".equals(currentMappingValues[i])) currentMappingValues[i] = "RETAIN";
            } else {
                if ("RETAIN".equals(currentMappingValues[i])) currentMappingValues[i] = "DROP";
            }
        }
        notifyDataSetChanged();
        if (changeListener != null) changeListener.onMappingChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_mapping, parent, false); return new ViewHolder(view); }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { holder.bind(position); }
    @Override
    public int getItemCount() { return csvHeaders.size(); }
    public ImportMapping getFinalMapping() { ImportMapping finalMapping = new ImportMapping(); for (int i = 0; i < csvHeaders.size(); i++) { finalMapping.put(csvHeaders.get(i), currentMappingValues[i]); } return finalMapping; }
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        Spinner targetSpinner;
        ArrayAdapter<String> spinnerAdapter;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.text_csv_header);
            targetSpinner = itemView.findViewById(R.id.spinner_target_field);
            spinnerAdapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(Arrays.asList(targetLabels)));
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            targetSpinner.setAdapter(spinnerAdapter);
        }
        void bind(int position) {
            headerText.setText(csvHeaders.get(position));
            String currentSelectionValue = currentMappingValues[position];
            int selectionIndex = 0;
            for (int i = 0; i < targetValues.length; i++) { if (targetValues[i].equals(currentSelectionValue)) { selectionIndex = i; break; } }
            targetSpinner.setSelection(selectionIndex, false);
            targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    currentMappingValues[getAdapterPosition()] = targetValues[pos];
                    if (changeListener != null) changeListener.onMappingChanged();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }
}
