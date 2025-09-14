// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/MappingActivity.java
package com.rkm.rkmattendanceapp.ui;

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
import java.util.Map;
import java.util.Set;

public class MappingActivity extends AppCompatActivity implements MappingAdapter.MappingChangeListener {

    public static final String EXTRA_FILE_URI = "com.rkm.rkmattendanceapp.ui.EXTRA_FILE_URI";
    public static final String EXTRA_CSV_HEADERS = "com.rkm.rkmattendanceapp.ui.EXTRA_CSV_HEADERS";

    private MappingViewModel viewModel;
    private MappingAdapter adapter;
    private Uri fileUri;
    private ProgressBar progressBar;
    private SwitchMaterial retainDroppedSwitch;
    private MenuItem startImportMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);
        setTitle(R.string.mapping_activity_title);
        
        // Enable the back arrow in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fileUri = getIntent().getParcelableExtra(EXTRA_FILE_URI);
        ArrayList<String> headers = getIntent().getStringArrayListExtra(EXTRA_CSV_HEADERS);

        if (fileUri == null || headers == null || headers.isEmpty()) {
            Toast.makeText(this, "Error: Invalid file or headers.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progress_bar_import);
        retainDroppedSwitch = findViewById(R.id.switch_unmapped_to_extras);
        viewModel = new ViewModelProvider(this).get(MappingViewModel.class);
        setupRecyclerView(headers);
        observeViewModel();
        
        retainDroppedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.toggleDropRetain(isChecked);
            }
        });
    }

    private void setupRecyclerView(ArrayList<String> headers) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_mapping);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MappingAdapter(this, headers, this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (startImportMenuItem != null) {
                startImportMenuItem.setEnabled(!isLoading);
            }
            retainDroppedSwitch.setEnabled(!isLoading);
        });

        viewModel.getImportStats().observe(this, stats -> {
            if (stats != null) {
                showSuccessDialog(stats);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorDialog(error);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mapping_menu, menu);
        startImportMenuItem = menu.findItem(R.id.action_start_import);
        validateMapping();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        // --- START OF FIX #2 ---
        if (itemId == android.R.id.home) {
            // Treat the "Up" button exactly like the system back button
            finish();
            return true;
        } 
        // --- END OF FIX #2 ---
        else if (itemId == R.id.action_start_import) {
            if (validateMapping()) {
                ImportMapping finalMapping = adapter.getFinalMapping();
                viewModel.startImport(fileUri, finalMapping);
            } else {
                 Toast.makeText(this, "Please map columns for 'Full Name' and 'Mobile Number'", Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onMappingChanged() {
        validateMapping();
    }

    private boolean validateMapping() {
        if (adapter == null || startImportMenuItem == null) {
            return false;
        }
        ImportMapping currentMapping = adapter.getFinalMapping();
        Set<String> mappedTargets = new HashSet<>(currentMapping.asMap().values());
        boolean isNameMapped = mappedTargets.contains("full_name");
        boolean isMobileMapped = mappedTargets.contains("mobile");
        if (isNameMapped && isMobileMapped) {
            startImportMenuItem.setEnabled(true);
            return true;
        } else {
            startImportMenuItem.setEnabled(false);
            return false;
        }
    }
    
    private void showSuccessDialog(CsvImporter.ImportStats stats) {
        String message = getString(R.string.import_stats_message,
                stats.processed, stats.inserted, stats.updatedChanged, stats.skipped);
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_success_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_error_title)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
