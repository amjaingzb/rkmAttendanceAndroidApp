// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDevoteeActivity.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportDevoteeActivity extends AppCompatActivity {
    private static final String TAG = "ReportDevoteeActivity";
    private ReportDevoteeViewModel viewModel;
    private ReportDevoteeAdapter adapter;
    private ProgressBar progressBar;
    private TextView noDevoteesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_devotee);
        setTitle(R.string.title_activity_report_devotee);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progress_bar);
        noDevoteesText = findViewById(R.id.text_no_devotees);
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(ReportDevoteeViewModel.class);
        observeViewModel();
        
        viewModel.loadDevoteeActivity();
    }
    
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_report_devotees);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportDevoteeAdapter();
        recyclerView.setAdapter(adapter);
    }
    
    private void observeViewModel() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.getDevoteeList().observe(this, devotees -> {
            progressBar.setVisibility(View.GONE);
            if (devotees == null || devotees.isEmpty()) {
                noDevoteesText.setVisibility(View.VISIBLE);
            } else {
                noDevoteesText.setVisibility(View.GONE);
                adapter.setDevotees(devotees);
            }
        });

        viewModel.getShareableFileUri().observe(this, uri -> {
            if (uri != null) {
                AppLogger.d(TAG, "Received shareable URI for devotee activity report: " + uri);
                shareCsvFile(uri);
                viewModel.onShareIntentHandled();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            progressBar.setVisibility(View.GONE);
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void shareCsvFile(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_export_title)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // --- START OF FIX ---
        // Use the new, generic export_menu.xml file
        getMenuInflater().inflate(R.menu.export_menu, menu);
        // --- END OF FIX ---
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_export_csv) {
            AppLogger.d(TAG, "Export CSV menu item clicked.");
            Toast.makeText(this, "Generating export file...", Toast.LENGTH_SHORT).show();
            viewModel.startDevoteeActivityExport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
