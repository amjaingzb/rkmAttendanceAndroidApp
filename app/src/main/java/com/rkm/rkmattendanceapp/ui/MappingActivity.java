// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/MappingActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.rkmattendanceapp.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MappingActivity extends AppCompatActivity implements MappingAdapter.MappingChangeListener {
    
    public static final String EXTRA_FILE_URI = "com.rkm.rkmattendanceapp.ui.EXTRA_FILE_URI";
    public static final String EXTRA_CSV_HEADERS = "com.rkm.rkmattendanceapp.ui.EXTRA_CSV_HEADERS";
    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID";
    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";
    public static final String EXTRA_IMPORT_TYPE = "com.rkm.rkmattendanceapp.ui.EXTRA_IMPORT_TYPE";

    private MappingViewModel viewModel;
    private MappingAdapter adapter;
    private SwitchMaterial retainDroppedSwitch;
    private MenuItem startImportMenuItem;
    
    private Uri fileUri;
    private long eventId = -1;
    private Privilege privilege = Privilege.EVENT_COORDINATOR;
    private ImportType importType = ImportType.DEVOTEE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);
        
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
        ArrayList<String> headers = intent.getStringArrayListExtra(EXTRA_CSV_HEADERS);
        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1);
        if (intent.hasExtra(EXTRA_PRIVILEGE)) privilege = (Privilege) intent.getSerializableExtra(EXTRA_PRIVILEGE);
        if (intent.hasExtra(EXTRA_IMPORT_TYPE)) importType = (ImportType) intent.getSerializableExtra(EXTRA_IMPORT_TYPE);

        retainDroppedSwitch = findViewById(R.id.switch_unmapped_to_extras);
        
        // --- START OF FIX ---
        // Set a contextual title and hide irrelevant UI for WhatsApp import
        switch(importType) {
            case WHATSAPP:
                setTitle("Map WhatsApp Columns");
                retainDroppedSwitch.setVisibility(View.GONE); // Hide the switch
                break;
            case ATTENDANCE:
                setTitle("Map Attendance Columns");
                retainDroppedSwitch.setVisibility(View.VISIBLE);
                break;
            default: // DEVOTEE
                setTitle(R.string.mapping_activity_title);
                retainDroppedSwitch.setVisibility(View.VISIBLE);
        }
        // --- END OF FIX ---

        if (fileUri == null || headers == null || headers.isEmpty()) {
            Toast.makeText(this, "Error: Invalid file or headers.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(MappingViewModel.class);
        setupRecyclerView(headers);
        observeViewModel();
        
        retainDroppedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) adapter.toggleDropRetain(isChecked);
        });
    }

    private void setupRecyclerView(ArrayList<String> headers) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_mapping);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MappingAdapter(this, headers, this, privilege, importType);
        recyclerView.setAdapter(adapter);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } 
        else if (itemId == R.id.action_start_import) {
            if (validateMapping()) {
                switch(importType) {
                    case WHATSAPP:
                        viewModel.startWhatsAppImport(fileUri, adapter.getFinalMapping());
                        break;
                    case ATTENDANCE:
                        viewModel.startAttendanceImport(fileUri, adapter.getFinalMapping(), eventId);
                        break;
                    default:
                        viewModel.startDevoteeImport(fileUri, adapter.getFinalMapping());
                        break;
                }
            } else {
                 Toast.makeText(this, "Please map all required columns", Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private boolean validateMapping() {
        if (adapter == null || startImportMenuItem == null) return false;
        Set<String> mappedTargets = new HashSet<>(adapter.getFinalMapping().asMap().values());
        boolean isValid;
        switch(importType) {
            case WHATSAPP:
                isValid = mappedTargets.contains("phone") && mappedTargets.contains("whatsAppGroupId");
                break;
            default:
                isValid = mappedTargets.contains("full_name") && mappedTargets.contains("mobile");
                break;
        }
        startImportMenuItem.setEnabled(isValid);
        return isValid;
    }

    private void observeViewModel() { ProgressBar progressBar = findViewById(R.id.progress_bar_import); viewModel.getIsLoading().observe(this, isLoading -> { progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE); if (startImportMenuItem != null) startImportMenuItem.setEnabled(!isLoading); retainDroppedSwitch.setEnabled(!isLoading); }); viewModel.getImportStats().observe(this, this::showSuccessDialog); viewModel.getErrorMessage().observe(this, this::showErrorDialog); }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.mapping_menu, menu); startImportMenuItem = menu.findItem(R.id.action_start_import); validateMapping(); return true; }
    @Override
    public void onMappingChanged() { validateMapping(); }
    private void showSuccessDialog(CsvImporter.ImportStats stats) { if (stats == null) return; String message = getString(R.string.import_stats_message, stats.processed, stats.inserted, stats.updatedChanged, stats.skipped); new AlertDialog.Builder(this) .setTitle(R.string.import_success_title) .setMessage(message) .setPositiveButton(android.R.string.ok, (d, w) -> { setResult(RESULT_OK); finish(); }) .setCancelable(false).show(); }
    private void showErrorDialog(String errorMessage) { if (errorMessage == null || errorMessage.isEmpty()) return; new AlertDialog.Builder(this) .setTitle(R.string.import_error_title) .setMessage(errorMessage) .setPositiveButton(android.R.string.ok, null).show(); }
}
