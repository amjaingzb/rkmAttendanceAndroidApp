// In: ui/MarkAttendanceActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rkm.rkmattendanceapp.R;

import java.util.ArrayList;

public class MarkAttendanceActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID";
    private static final int SEARCH_TRIGGER_LENGTH = 3;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 300;

    private MarkAttendanceViewModel viewModel;
    private TextView statPreReg, statAttended, statSpotReg, statTotal;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private Button addNewButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView checkedInRecyclerView;
    private LinearLayout checkedInLayout;
    private SearchResultAdapter searchAdapter;
    private DevoteeListAdapter checkedInAdapter;
    private ProgressBar searchProgressBar;
    private TextView noResultsTextView;

    private long eventId = -1;
    private ActivityResultLauncher<Intent> addDevoteeLauncher;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // NEW: The flag to control the observer during a search.
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);
        if (eventId == -1) {
            Toast.makeText(this, "Error: No Event ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        addDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (searchEditText != null) {
                            searchEditText.setText("");
                        }
                        viewModel.loadEventData(eventId);
                    }
                }
        );

        viewModel = new ViewModelProvider(this).get(MarkAttendanceViewModel.class);

        bindViews();
        setupRecyclerViews();
        setupSearchAndButtons();
        observeViewModel();

        viewModel.loadEventData(eventId);
    }

    private void bindViews() {
        statPreReg = findViewById(R.id.text_stat_pre_reg);
        statAttended = findViewById(R.id.text_stat_attended);
        statSpotReg = findViewById(R.id.text_stat_spot_reg);
        statTotal = findViewById(R.id.text_stat_total);
        searchInputLayout = findViewById(R.id.text_input_layout_search);
        searchEditText = findViewById(R.id.edit_text_search_attendee);
        addNewButton = findViewById(R.id.button_add_new_spot);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        checkedInRecyclerView = findViewById(R.id.recycler_view_checked_in);
        checkedInLayout = findViewById(R.id.layout_checked_in_list);
        searchProgressBar = findViewById(R.id.progress_bar_search);
        noResultsTextView = findViewById(R.id.text_no_results);
    }

    private void setupRecyclerViews() {
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchResultAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchAdapter.setOnSearchResultClickListener(devotee -> {
            viewModel.markAttendance(devotee.devotee().getDevoteeId());
            searchEditText.setText("");
        });

        checkedInRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkedInAdapter = new DevoteeListAdapter();
        checkedInRecyclerView.setAdapter(checkedInAdapter);
    }

    private void setupSearchAndButtons() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String query = s.toString();
                searchHandler.removeCallbacks(searchRunnable);

                if (query.length() >= SEARCH_TRIGGER_LENGTH) {
                    // MODIFIED: Set the searching flag to true
                    isSearching = true;
                    showSearchingState();
                    searchRunnable = () -> viewModel.searchDevotees(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
                } else if (query.length() > 0) {
                    isSearching = false; // Not searching if input is insufficient
                    showInsufficientInputState();
                } else {
                    isSearching = false; // Not searching if input is empty
                    showPristineState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        addNewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditDevoteeActivity.class);
            String prefillQuery = searchEditText.getText().toString().trim();
            if (!prefillQuery.isEmpty()) {
                intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery);
            }
            intent.putExtra(AddEditDevoteeActivity.EXTRA_IS_ON_SPOT_REG, true);
            intent.putExtra(AddEditDevoteeActivity.EXTRA_EVENT_ID, eventId);
            addDevoteeLauncher.launch(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getEventDetails().observe(this, event -> { /* ... unchanged ... */ });
        viewModel.getEventStats().observe(this, stats -> { /* ... unchanged ... */ });
        viewModel.getCheckedInList().observe(this, devotees -> { /* ... unchanged ... */ });
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
            // MODIFIED: The observer is now controlled by the isSearching flag.
            if (!isSearching) {
                // If we are not in a searching state, we shouldn't process any lingering results.
                // This can happen if the user deletes text quickly.
                return;
            }

            // We have received results, so the search is over.
            isSearching = false;
            searchProgressBar.setVisibility(View.GONE);

            if (results != null && !results.isEmpty()) {
                noResultsTextView.setVisibility(View.GONE);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                searchAdapter.setSearchResults(results);
            } else {
                searchResultsRecyclerView.setVisibility(View.GONE);
                noResultsTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> { /* ... unchanged ... */ });
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPristineState() {
        searchInputLayout.setHelperText(null);
        searchProgressBar.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        checkedInLayout.setVisibility(View.VISIBLE);
        // Clear the adapter directly for an instant UI update
        searchAdapter.setSearchResults(new ArrayList<>());
    }

    private void showInsufficientInputState() {
        searchInputLayout.setHelperText("Enter at least " + SEARCH_TRIGGER_LENGTH + " characters");
        searchProgressBar.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        checkedInLayout.setVisibility(View.GONE);
        searchAdapter.setSearchResults(new ArrayList<>());
    }

    private void showSearchingState() {
        searchInputLayout.setHelperText(null);
        noResultsTextView.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        checkedInLayout.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.VISIBLE);
    }
}