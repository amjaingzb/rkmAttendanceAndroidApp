// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDonationDaysActivity.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.DailySummary;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportDonationDaysActivity extends AppCompatActivity implements ReportDonationDaysAdapter.OnDayReportClickListener {
    private static final String TAG = "ReportDonationDays";
    private ReportDonationViewModel viewModel;
    private ReportDonationDaysAdapter adapter;
    private ProgressBar progressBar;
    private TextView noDonationsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_donation_days);
        setTitle("Donations by Day");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(ReportDonationViewModel.class);
        observeViewModel();
        
        viewModel.loadDailySummaries();
    }
    
    private void bindViews() {
        progressBar = findViewById(R.id.progress_bar);
        noDonationsText = findViewById(R.id.text_no_donations);
    }
    
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_donation_days);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportDonationDaysAdapter();
        adapter.setOnDayReportClickListener(this);
        recyclerView.setAdapter(adapter);
    }
    
    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getDailySummaries().observe(this, summaries -> {
            if (summaries == null || summaries.isEmpty()) {
                noDonationsText.setVisibility(View.VISIBLE);
            } else {
                noDonationsText.setVisibility(View.GONE);
                adapter.setSummaries(summaries);
            }
        });

        viewModel.getShareableFileUri().observe(this, uri -> {
            if (uri != null) {
                AppLogger.d(TAG, "Received shareable URI for daily donations: " + uri);
                shareCsvFile(uri);
                viewModel.onShareIntentHandled();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
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
        startActivity(Intent.createChooser(shareIntent, "Share Daily Report Via"));
    }
    
    private void emailCsvFile(Uri uri, String subject) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // This will still use the chooser, but it will be filtered for email apps
        startActivity(Intent.createChooser(emailIntent, "Email Daily Report Via"));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEmailClick(DailySummary summary) {
        // We will implement this after the CSV export is confirmed to work.
        // For now, it will just export the file.
        Toast.makeText(this, "Email feature will be enabled later. Sharing CSV now.", Toast.LENGTH_SHORT).show();
        onShareClick(summary);
    }

    @Override
    public void onShareClick(DailySummary summary) {
        Toast.makeText(this, "Generating export for " + summary.date + "...", Toast.LENGTH_SHORT).show();
        viewModel.exportDonationsForDay(summary.date);
    }
}
