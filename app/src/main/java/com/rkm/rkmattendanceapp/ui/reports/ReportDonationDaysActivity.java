// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDonationDaysActivity.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.content.Intent;
import android.icu.text.NumberFormat;
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

import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.DailySummary;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReportDonationDaysActivity extends AppCompatActivity implements ReportDonationDaysAdapter.OnDayReportClickListener {
    private static final String TAG = "ReportDonationDays";
    private ReportDonationViewModel viewModel;
    private ReportDonationDaysAdapter adapter;
    private ProgressBar progressBar;
    private TextView noDonationsText;

    // ANNOTATION: New state variables to manage user actions
    private enum ActionType { EMAIL, SHARE }
    private ActionType requestedAction;
    private DailySummary selectedSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_donation_days);
        setTitle("Donations by Day");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        viewModel.getIsLoading().observe(this, isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
        viewModel.getDailySummaries().observe(this, summaries -> {
            if (summaries == null || summaries.isEmpty()) { noDonationsText.setVisibility(View.VISIBLE); }
            else { noDonationsText.setVisibility(View.GONE); adapter.setSummaries(summaries); }
        });

        // ANNOTATION: This observer now handles both email and share actions
        viewModel.getShareableFileUri().observe(this, uri -> {
            if (uri != null) {
                if (requestedAction == ActionType.EMAIL && selectedSummary != null) {
                    emailCsvFile(uri, selectedSummary);
                } else {
                    shareCsvFile(uri);
                }
                // Reset state
                requestedAction = null;
                selectedSummary = null;
                viewModel.onShareIntentHandled();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> { if (error != null && !error.isEmpty()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); } });
    }

    private void shareCsvFile(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Daily Report Via"));
    }
    
    // ANNOTATION: New method to compose and send the summary email with attachment
    private void emailCsvFile(Uri uri, DailySummary summary) {
        String officeEmail = ((AttendanceApplication) getApplication()).repository.getOfficeEmail();
        if (officeEmail == null || officeEmail.isEmpty()) {
            Toast.makeText(this, "Error: Office email has not been configured.", Toast.LENGTH_LONG).show();
            return;
        }

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
        DateTimeFormatter subjectFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.US);
        LocalDate date = LocalDate.parse(summary.date, inputFormatter);
        String subject = "Donation Report: " + subjectFormatter.format(date);
        
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String body = "Daily Donation Summary for " + subjectFormatter.format(date) + "\n" +
                      "-----------------------------------\n" +
                      "Total Amount: " + currencyFormatter.format(summary.totalAmount) + "\n" +
                      "Total Donations: " + summary.donationCount + "\n" +
                      "Number of Batches: " + summary.batchCount + "\n" +
                      "-----------------------------------\n\n" +
                      "The detailed transaction list is attached.";

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{officeEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Email Daily Report Via"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEmailClick(DailySummary summary) {
        // ANNOTATION: Set state and trigger export
        this.requestedAction = ActionType.EMAIL;
        this.selectedSummary = summary;
        Toast.makeText(this, "Generating email report for " + summary.date + "...", Toast.LENGTH_SHORT).show();
        viewModel.exportDonationsForDay(summary.date);
    }

    @Override
    public void onShareClick(DailySummary summary) {
        // ANNOTATION: Set state and trigger export
        this.requestedAction = ActionType.SHARE;
        this.selectedSummary = summary;
        Toast.makeText(this, "Generating export for " + summary.date + "...", Toast.LENGTH_SHORT).show();
        viewModel.exportDonationsForDay(summary.date);
    }
}
