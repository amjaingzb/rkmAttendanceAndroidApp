// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportEventListActivity.java
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

import com.rkm.attendance.db.EventDao;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportEventListActivity extends AppCompatActivity {

    private static final String TAG = "ReportEventListActivity";
    private ReportEventListViewModel viewModel;
    private ReportEventListAdapter adapter;
    private ProgressBar progressBar;
    private TextView noEventsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_event_list);
        setTitle(R.string.title_activity_report_event_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progress_bar);
        noEventsText = findViewById(R.id.text_no_events);
        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(ReportEventListViewModel.class);
        observeViewModel();
        
        viewModel.loadEventsWithAttendance();
    }
    
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_report_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportEventListAdapter();
        adapter.setOnEventReportClickListener(event -> {
            AppLogger.d(TAG, "Export clicked for event: " + event.eventName);
            Toast.makeText(this, "Generating export for " + event.eventName + "...", Toast.LENGTH_SHORT).show();
            viewModel.startAttendanceExport(event.eventId);
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void observeViewModel() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.getEventList().observe(this, events -> {
            progressBar.setVisibility(View.GONE);
            if (events == null || events.isEmpty()) {
                noEventsText.setVisibility(View.VISIBLE);
            } else {
                noEventsText.setVisibility(View.GONE);
                adapter.setEvents(events);
            }
        });

        viewModel.getShareableFileUri().observe(this, uri -> {
            if (uri != null) {
                AppLogger.d(TAG, "Received shareable URI for attendance: " + uri);
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
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_attendance_title)));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
