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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MarkAttendanceActivity extends AppCompatActivity {
    private static final String TAG = "MarkAttendanceActivity";
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
    private AttendanceRepository.WhatsAppInvite whatsAppInviteDetails;

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

        // === START OF RACE CONDITION FIX ===
        addDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    boolean wasNewDevoteeAdded = false;
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        long newDevoteeId = result.getData().getLongExtra(AddEditDevoteeActivity.RESULT_EXTRA_NEW_DEVOTEE_ID, -1);
                        if (newDevoteeId > 0) {
                            wasNewDevoteeAdded = true;
                            viewModel.loadNewlyAddedDevotee(newDevoteeId);
                        }
                    }

                    // Only do a full refresh if we DIDN'T just add a new devotee.
                    // If we did, we let the auto-dialog logic take over.
                    if (!wasNewDevoteeAdded) {
                        if (searchEditText != null) searchEditText.setText("");
                        viewModel.loadEventData(eventId);
                    }
                });
        // === END OF RACE CONDITION FIX ===

        viewModel = new ViewModelProvider(this).get(MarkAttendanceViewModel.class);
        bindViews();
        setupRecyclerViews();
        setupSearchAndButtons();
        observeViewModel();
        viewModel.loadEventData(eventId);
        
        showPristineState();
    }

    private void applySubtitleFix() {
        try {
            Toolbar toolbar = findToolbar(getWindow().getDecorView());
            if (toolbar != null && getSupportActionBar() != null) {
                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    View child = toolbar.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        CharSequence subtitle = getSupportActionBar().getSubtitle();
                        if (subtitle != null && subtitle.equals(tv.getText())) {
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Hardcoded small size
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to apply subtitle bandaid fix.", e);
        }
    }

    private Toolbar findToolbar(View view) {
        if (view instanceof Toolbar) return (Toolbar) view;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                Toolbar found = findToolbar(viewGroup.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_devotee_confirmation, null);
        TextView statusText = dialogView.findViewById(R.id.dialog_text_status);
        TextView mobileText = dialogView.findViewById(R.id.dialog_text_mobile);
        TextView whatsappText = dialogView.findViewById(R.id.dialog_text_whatsapp);
        ImageView whatsappIcon = dialogView.findViewById(R.id.dialog_icon_whatsapp);
        Button primaryButton = dialogView.findViewById(R.id.dialog_button_primary);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_button_secondary);
        Button negativeButton = dialogView.findViewById(R.id.dialog_button_negative);
        if (enrichedDevotee.getEventStatus() == EventStatus.PRE_REGISTERED) {
            statusText.setText("Pre-Registered");
            statusText.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_status_prereg));
        } else {
            statusText.setText("Walk-in");
            statusText.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_status_walkin));
        }
        mobileText.setText(enrichedDevotee.devotee().getMobileE164());
        if (enrichedDevotee.whatsAppGroup() != null && enrichedDevotee.whatsAppGroup() > 0) {
            whatsappText.setText("Group: " + enrichedDevotee.whatsAppGroup());
            whatsappIcon.setImageResource(R.drawable.ic_whatsapp_green);
        } else {
            whatsappText.setText("Not in any group");
            whatsappIcon.setImageResource(R.drawable.ic_whatsapp_gray);
        }
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(enrichedDevotee.devotee().getFullName())
                .setView(dialogView)
                .create();
        if (enrichedDevotee.whatsAppGroup() == null || enrichedDevotee.whatsAppGroup() == 0) {
            primaryButton.setText("Send Invite & Mark Present");
            primaryButton.setOnClickListener(v -> {
                viewModel.markAttendance(enrichedDevotee.devotee().getDevoteeId());
                dispatchWhatsAppInvite(enrichedDevotee.devotee());
                dialog.dismiss();
            });
            secondaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText("Mark as Present (No Invite)");
            secondaryButton.setOnClickListener(v -> {
                viewModel.markAttendance(enrichedDevotee.devotee().getDevoteeId());
                dialog.dismiss();
            });
        } else {
            primaryButton.setText("Mark as Present");
            primaryButton.setOnClickListener(v -> {
                viewModel.markAttendance(enrichedDevotee.devotee().getDevoteeId());
                dialog.dismiss();
            });
            secondaryButton.setVisibility(View.GONE);
        }
        negativeButton.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();
    }

    private void dispatchWhatsAppInvite(Devotee devotee) {
        if (whatsAppInviteDetails == null || TextUtils.isEmpty(whatsAppInviteDetails.link) || TextUtils.isEmpty(whatsAppInviteDetails.messageTemplate)) {
            Toast.makeText(this, "WhatsApp invite details not configured.", Toast.LENGTH_LONG).show();
            return;
        }
        String fullMessage = whatsAppInviteDetails.messageTemplate + whatsAppInviteDetails.link;
        String phoneForApi = "91" + devotee.getMobileE164();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneForApi + "&text=" + URLEncoder.encode(fullMessage, "UTF-8");
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(this, "Error preparing invite message.", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show();
        }
    }

    private void observeViewModel() {
        viewModel.getWhatsAppInvite().observe(this, invite -> this.whatsAppInviteDetails = invite);
        viewModel.getNewlyAddedDevotee().observe(this, devotee -> {
            if (devotee != null) {
                if (devotee.whatsAppGroup() == null || devotee.whatsAppGroup() == 0) {
                    showConfirmationDialog(devotee);
                }
                viewModel.onDialogShown();
            }
        });
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
        viewModel.getEventDetails().observe(this, event -> {
            if (event != null) {
                setTitle(event.getEventName());
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setSubtitle(event.getEventDate());
                    applySubtitleFix();
                }
            }
        });
        viewModel.getEventStats().observe(this, stats -> { if (stats != null) { statPreReg.setText(String.valueOf(stats.preRegistered)); statAttended.setText(String.valueOf(stats.attended)); statSpotReg.setText(String.valueOf(stats.spotRegistered)); statTotal.setText(String.valueOf(stats.total)); } });
        viewModel.getCheckedInList().observe(this, devotees -> { if (devotees != null) { checkedInAdapter.setDevotees(devotees); } });
        viewModel.getErrorMessage().observe(this, error -> { if (error != null && !error.isEmpty()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); } });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.operator_main_menu, menu); return true; }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_admin_login) {
            startActivity(new Intent(this, RoleSelectionActivity.class));
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        listHeaderTextView = findViewById(R.id.text_list_header);
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
