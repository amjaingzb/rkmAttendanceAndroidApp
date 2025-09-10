// In: ui/MarkAttendanceActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.rkm.rkmattendanceapp.R;

public class MarkAttendanceActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID";

    private MarkAttendanceViewModel viewModel;
    private TextView statPreReg, statAttended, statSpotReg, statTotal;
    private TextInputEditText searchEditText;
    private Button addNewButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView checkedInRecyclerView;
    private LinearLayout checkedInLayout;
    private SearchResultAdapter searchAdapter;
    private DevoteeListAdapter checkedInAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);
        if (eventId == -1) {
            Toast.makeText(this, "Error: No Event ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(MarkAttendanceViewModel.class);

        bindViews();
        setupRecyclerViews();
        setupSearch();
        observeViewModel();

        viewModel.loadEventData(eventId);
    }

    private void bindViews() {
        statPreReg = findViewById(R.id.text_stat_pre_reg);
        statAttended = findViewById(R.id.text_stat_attended);
        statSpotReg = findViewById(R.id.text_stat_spot_reg);
        statTotal = findViewById(R.id.text_stat_total);
        searchEditText = findViewById(R.id.edit_text_search_attendee);
        addNewButton = findViewById(R.id.button_add_new_spot);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        checkedInRecyclerView = findViewById(R.id.recycler_view_checked_in);
        checkedInLayout = findViewById(R.id.layout_checked_in_list);
    }

    private void setupRecyclerViews() {
        // Search Results
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchResultAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchAdapter.setOnSearchResultClickListener(devotee -> {
            viewModel.markAttendance(devotee.devotee().getDevoteeId());
            searchEditText.setText(""); // Clear search after marking
        });

        // Checked-in List
        checkedInRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkedInAdapter = new DevoteeListAdapter();
        checkedInRecyclerView.setAdapter(checkedInAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Using a Handler/debounce is better, but this is fine for now
                viewModel.searchDevotees(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getEventDetails().observe(this, event -> {
            if (event != null) {
                setTitle(event.getEventName());
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(event.getEventDate());
                }
            }
        });

        viewModel.getEventStats().observe(this, stats -> {
            if (stats != null) {
                statPreReg.setText(String.valueOf(stats.preRegistered));
                statAttended.setText(String.valueOf(stats.attended));
                statSpotReg.setText(String.valueOf(stats.spotRegistered));
                statTotal.setText(String.valueOf(stats.total));
            }
        });

        viewModel.getCheckedInList().observe(this, devotees -> {
            if (devotees != null) {
                checkedInAdapter.setDevotees(devotees);
            }
        });

        viewModel.getSearchResults().observe(this, results -> {
            if (results != null && !results.isEmpty()) {
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                checkedInLayout.setVisibility(View.GONE);
                searchAdapter.setSearchResults(results);
            } else {
                searchResultsRecyclerView.setVisibility(View.GONE);
                checkedInLayout.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}