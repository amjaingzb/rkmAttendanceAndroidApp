// In: ui/MarkAttendanceActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private TextView listHeaderTextView;

    private long eventId = -1;
    private ActivityResultLauncher<Intent> addDevoteeLauncher;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // === START OF NEW CODE ===
    // STEP 2.1: Add a variable to hold the fetched invite details.
    private AttendanceRepository.WhatsAppInvite whatsAppInviteDetails;
    // === END OF NEW CODE ===

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
                        if (searchEditText != null) searchEditText.setText("");
                        viewModel.loadEventData(eventId);
                    }
                });

        viewModel = new ViewModelProvider(this).get(MarkAttendanceViewModel.class);
        bindViews();
        setupRecyclerViews();
        setupSearchAndButtons();
        observeViewModel();
        viewModel.loadEventData(eventId);
        
        showPristineState();
    }

    private void setupRecyclerViews() {
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchResultAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        
        searchAdapter.setOnSearchResultClickListener(this::showConfirmationDialog);

        checkedInRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkedInAdapter = new DevoteeListAdapter();
        checkedInRecyclerView.setAdapter(checkedInAdapter);
    }

    private void showConfirmationDialog(DevoteeDao.EnrichedDevotee enrichedDevotee) {
        String title = enrichedDevotee.devotee().getFullName();
        
        String status = enrichedDevotee.getEventStatus() == EventStatus.PRE_REGISTERED ? "Pre-Registered" : "Walk-in";
        String mobile = enrichedDevotee.devotee().getMobileE164();
        String groupStatus = (enrichedDevotee.whatsAppGroup() != null && enrichedDevotee.whatsAppGroup() > 0)
                ? "Group: " + enrichedDevotee.whatsAppGroup()
                : "Not in any group";
        
        String message = "Status: " + status + "\n" +
                         "Mobile: " + mobile + "\n" +
                         "WhatsApp: " + groupStatus;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("Mark as Present", (dialog, which) -> {
                   viewModel.markAttendance(enrichedDevotee.devotee().getDevoteeId());
               })
               .setNegativeButton("Cancel", null);

        if (enrichedDevotee.whatsAppGroup() == null || enrichedDevotee.whatsAppGroup() == 0) {
            // STEP 2.2: Update the button's click listener to call our new method.
            builder.setNeutralButton("Send Invite", (dialog, which) -> {
                dispatchWhatsAppInvite(enrichedDevotee.devotee());
            });
        }
        
        builder.show();
    }

    // === START OF NEW CODE ===
    // STEP 2.3: New method to construct and launch the WhatsApp Intent.
    private void dispatchWhatsAppInvite(Devotee devotee) {
        if (whatsAppInviteDetails == null || TextUtils.isEmpty(whatsAppInviteDetails.link) || TextUtils.isEmpty(whatsAppInviteDetails.messageTemplate)) {
            Toast.makeText(this, "WhatsApp invite details not configured.", Toast.LENGTH_LONG).show();
            return;
        }

        String fullMessage = whatsAppInviteDetails.messageTemplate + whatsAppInviteDetails.link;
        // Prepend "91" country code for the WhatsApp API.
        String phoneForApi = "91" + devotee.getMobileE164();

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneForApi + "&text=" + URLEncoder.encode(fullMessage, "UTF-8");
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            // This should never happen with "UTF-8"
            Toast.makeText(this, "Error preparing invite message.", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show();
        }
    }
    // === END OF NEW CODE ===

    private void observeViewModel() {
        // === START OF NEW CODE ===
        // STEP 2.4: Observe the LiveData and store the invite details.
        viewModel.getWhatsAppInvite().observe(this, invite -> {
            this.whatsAppInviteDetails = invite;
        });
        // === END OF NEW CODE ===

        viewModel.getSearchResults().observe(this, results -> {
            searchProgressBar.setVisibility(View.GONE);
            if (results != null && !results.isEmpty()) {
                listHeaderTextView.setText("Search Results");
                listHeaderTextView.setVisibility(View.VISIBLE);
                noResultsTextView.setVisibility(View.GONE);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                searchAdapter.setSearchResults(results);
            } else {
                searchResultsRecyclerView.setVisibility(View.GONE);
                if (searchEditText.getText().length() >= SEARCH_TRIGGER_LENGTH) {
                    listHeaderTextView.setVisibility(View.GONE);
                    noResultsTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        
        viewModel.getEventDetails().observe(this, event -> { if (event != null) { setTitle(event.getEventName()); if (getSupportActionBar() != null) { getSupportActionBar().setSubtitle(event.getEventDate()); } } });
        viewModel.getEventStats().observe(this, stats -> { if (stats != null) { statPreReg.setText(String.valueOf(stats.preRegistered)); statAttended.setText(String.valueOf(stats.attended)); statSpotReg.setText(String.valueOf(stats.spotRegistered)); statTotal.setText(String.valueOf(stats.total)); } });
        viewModel.getCheckedInList().observe(this, devotees -> { if (devotees != null) { checkedInAdapter.setDevotees(devotees); } });
        viewModel.getErrorMessage().observe(this, error -> { if (error != null && !error.isEmpty()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); } });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.operator_main_menu, menu); return true; }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { if (item.getItemId() == R.id.action_admin_login) { Intent intent = new Intent(this, RoleSelectionActivity.class); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); finish(); return true; } return super.onOptionsItemSelected(item); }
    private void bindViews() { statPreReg = findViewById(R.id.text_stat_pre_reg); statAttended = findViewById(R.id.text_stat_attended); statSpotReg = findViewById(R.id.text_stat_spot_reg); statTotal = findViewById(R.id.text_stat_total); searchInputLayout = findViewById(R.id.text_input_layout_search); searchEditText = findViewById(R.id.edit_text_search_attendee); addNewButton = findViewById(R.id.button_add_new_spot); searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results); checkedInRecyclerView = findViewById(R.id.recycler_view_checked_in); checkedInLayout = findViewById(R.id.layout_checked_in_list); searchProgressBar = findViewById(R.id.progress_bar_search); noResultsTextView = findViewById(R.id.text_no_results); listHeaderTextView = findViewById(R.id.text_list_header); }
    private void setupSearchAndButtons() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String query = s.toString();
                searchHandler.removeCallbacks(searchRunnable);
                if (query.length() >= SEARCH_TRIGGER_LENGTH) {
                    showSearchingState();
                    searchRunnable = () -> viewModel.searchDevotees(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
                } else if (query.length() > 0) {
                    showInsufficientInputState();
                } else {
                    showPristineState();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        addNewButton.setOnClickListener(v -> { Intent intent = new Intent(this, AddEditDevoteeActivity.class); String prefillQuery = searchEditText.getText().toString().trim(); if (!prefillQuery.isEmpty()) { intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery); } intent.putExtra(AddEditDevoteeActivity.EXTRA_IS_ON_SPOT_REG, true); intent.putExtra(AddEditDevoteeActivity.EXTRA_EVENT_ID, eventId); addDevoteeLauncher.launch(intent); });
    }
    private void showPristineState() { searchInputLayout.setHelperText(null); searchProgressBar.setVisibility(View.GONE); noResultsTextView.setVisibility(View.GONE); searchResultsRecyclerView.setVisibility(View.GONE); checkedInLayout.setVisibility(View.VISIBLE); searchAdapter.setSearchResults(new ArrayList<>()); listHeaderTextView.setText("Recently Checked-In"); listHeaderTextView.setVisibility(View.VISIBLE); }
    private void showInsufficientInputState() { searchInputLayout.setHelperText("Enter at least " + SEARCH_TRIGGER_LENGTH + " characters"); searchProgressBar.setVisibility(View.GONE); noResultsTextView.setVisibility(View.GONE); searchResultsRecyclerView.setVisibility(View.GONE); checkedInLayout.setVisibility(View.GONE); searchAdapter.setSearchResults(new ArrayList<>()); listHeaderTextView.setVisibility(View.GONE); }
    private void showSearchingState() { searchInputLayout.setHelperText(null); noResultsTextView.setVisibility(View.GONE); searchResultsRecyclerView.setVisibility(View.GONE); checkedInLayout.setVisibility(View.GONE); searchProgressBar.setVisibility(View.VISIBLE); listHeaderTextView.setVisibility(View.GONE); }
}
