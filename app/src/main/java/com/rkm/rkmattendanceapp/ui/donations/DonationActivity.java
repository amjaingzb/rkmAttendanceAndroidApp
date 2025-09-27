// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.app.Activity;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.importer.CsvExporter;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.AboutActivity;
import com.rkm.rkmattendanceapp.ui.AddEditDevoteeActivity;
import com.rkm.rkmattendanceapp.ui.DevoteeListAdapter;
import com.rkm.rkmattendanceapp.ui.RoleSelectionActivity;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class DonationActivity extends AppCompatActivity {

    private static final String TAG = "DonationActivity";
    private static final int SEARCH_TRIGGER_LENGTH = 3;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 300;

    private DonationViewModel viewModel;
    private TextInputEditText searchEditText;
    private Button addNewButton, depositButton, startNewBatchButton, logoutButton;
    private RecyclerView searchResultsRecyclerView, donationsRecyclerView;
    private DevoteeListAdapter searchAdapter;
    private DonationListAdapter donationAdapter;
    private ProgressBar searchProgressBar;
    private TextView noResultsTextView, listHeaderTextView, batchClosedMessageText;
    private CardView batchSummaryCard;
    private TextView batchTitleText, batchStartTimeText, batchCashText, batchUpiText, batchTotalText;
    private LinearLayout batchClosedLayout, searchControlsLayout;
    
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private ActivityResultLauncher<Intent> addDevoteeLauncher, addDonationLauncher,emailLauncher;
    
    private Long activeBatchId = null;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        setTitle("Record Donations");
        viewModel = new ViewModelProvider(this).get(DonationViewModel.class);
        setupLaunchers();
        bindViews();
        setupRecyclerViews();
        setupClickListeners();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchResultsRecyclerView.getVisibility() == View.VISIBLE) {
                    searchEditText.setText("");
                } else {
                    logoutAndReturnToRoleSelection();
                }
            }
        });
        getSupportFragmentManager().setFragmentResultListener(DonationActionsBottomSheetFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            String action = bundle.getString(DonationActionsBottomSheetFragment.KEY_ACTION);
            long donationId = bundle.getLong(DonationActionsBottomSheetFragment.KEY_DONATION_ID);
            if ("DELETE".equals(action)) {
                new AlertDialog.Builder(this)
                    .setTitle("Delete Donation")
                    .setMessage("Are you sure you want to delete this donation record?")
                    .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteDonation(donationId))
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
        observeViewModel();
        viewModel.loadOrRefreshActiveBatch();
    }

    private void setupLaunchers() {
        addDonationLauncher = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) viewModel.loadOrRefreshActiveBatch();
            searchEditText.setText("");
        });
        addDevoteeLauncher = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                String newPhone = result.getData().getStringExtra(AddEditDevoteeActivity.RESULT_EXTRA_NEW_DEVOTEE_PHONE);
                if (newPhone != null && !newPhone.isEmpty()) { searchEditText.setText(newPhone); searchEditText.selectAll(); }
                else { searchEditText.setText(""); }
            } else { searchEditText.setText(""); }
        });
        emailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // This code runs regardless of whether the email was sent or cancelled.
                    // The important thing is that the user's "email sending" task is complete.
                    AppLogger.d(TAG, "Returned from email client. Closing batch.");
                    viewModel.closeActiveBatch();
                }
        );
    }

    private void bindViews() {
        searchEditText = findViewById(R.id.edit_text_search_devotee);
        addNewButton = findViewById(R.id.button_add_new_devotee);
        depositButton = findViewById(R.id.button_deposit_batch);
        startNewBatchButton = findViewById(R.id.button_start_new_batch);
        logoutButton = findViewById(R.id.button_logout);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        donationsRecyclerView = findViewById(R.id.recycler_view_donations);
        searchProgressBar = findViewById(R.id.progress_bar_search);
        noResultsTextView = findViewById(R.id.text_no_results);
        listHeaderTextView = findViewById(R.id.text_list_header);
        batchSummaryCard = findViewById(R.id.card_batch_summary);
        batchTitleText = findViewById(R.id.text_batch_title);
        batchStartTimeText = findViewById(R.id.text_batch_start_time);
        batchCashText = findViewById(R.id.text_batch_cash);
        batchUpiText = findViewById(R.id.text_batch_upi);
        batchTotalText = findViewById(R.id.text_batch_total);
        batchClosedLayout = findViewById(R.id.layout_batch_closed);
        batchClosedMessageText = findViewById(R.id.text_batch_closed_message);
        searchControlsLayout = findViewById(R.id.layout_search_controls);
    }

    private void setupRecyclerViews() {
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new DevoteeListAdapter();
        searchAdapter.setOnDevoteeClickListener(devotee -> {
            if (activeBatchId == null) { Toast.makeText(this, "Error: No active batch found.", Toast.LENGTH_SHORT).show(); return; }
            Intent intent = new Intent(this, AddEditDonationActivity.class);
            intent.putExtra(AddEditDonationActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
            intent.putExtra(AddEditDonationActivity.EXTRA_BATCH_ID, activeBatchId);
            addDonationLauncher.launch(intent);
        });
        searchResultsRecyclerView.setAdapter(searchAdapter);
        donationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        donationAdapter = new DonationListAdapter();
        donationAdapter.setOnDonationClickListener(record -> DonationActionsBottomSheetFragment.newInstance(record.donation.donationId).show(getSupportFragmentManager(), DonationActionsBottomSheetFragment.TAG));
        donationsRecyclerView.setAdapter(donationAdapter);
    }

    private void setupClickListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String query = s.toString();
                searchHandler.removeCallbacks(searchRunnable);
                if (query.length() >= SEARCH_TRIGGER_LENGTH) { showSearchingState(); searchRunnable = () -> viewModel.searchDevotees(query); searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS); }
                else if (query.length() > 0) { showInsufficientInputState(); }
                else { showActiveBatchState(); }
            }
            public void afterTextChanged(Editable s) {}
        });
        addNewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditDevoteeActivity.class);
            String prefillQuery = searchEditText.getText().toString().trim();
            if (!prefillQuery.isEmpty()) { intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery); }
            addDevoteeLauncher.launch(intent);
        });
        depositButton.setOnClickListener(v -> showDepositConfirmation());
        startNewBatchButton.setOnClickListener(v -> viewModel.startNewBatch());
        logoutButton.setOnClickListener(v -> logoutAndReturnToRoleSelection());
    }

    private void observeViewModel() {
        viewModel.getActiveBatchData().observe(this, data -> {
            if (data != null) {
                showActiveBatchState();
                updateBatchUi(data);
            }
        });
        viewModel.getSearchResults().observe(this, this::updateSearchResultsUi);
        viewModel.getBatchClosedEvent().observe(this, closed -> {
            if (closed != null && closed) {
                showBatchClosedState();
            }
        });
        viewModel.getErrorMessage().observe(this, error -> { if (error != null && !error.isEmpty()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); } });
    }

    private void updateBatchUi(AttendanceRepository.ActiveBatchData data) {
        this.activeBatchId = data.batch.batchId;
        batchTitleText.setText(getString(R.string.batch_title_format, data.batch.batchId));
        LocalDateTime startTime = LocalDateTime.parse(data.batch.startTs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        batchStartTimeText.setText(getString(R.string.batch_start_time_format, timeFormatter.format(startTime)));
        batchCashText.setText(currencyFormatter.format(data.summary.totalCash));
        batchUpiText.setText(currencyFormatter.format(data.summary.totalUpi));
        batchTotalText.setText(currencyFormatter.format(data.summary.totalCash + data.summary.totalUpi));
        donationAdapter.setDonations(data.donations);
        listHeaderTextView.setVisibility(data.donations.isEmpty() ? View.GONE : View.VISIBLE);
        listHeaderTextView.setText("Donations in this Batch (" + data.donations.size() + ")");
    }
    
    private void updateSearchResultsUi(java.util.List<Devotee> results) {
        searchProgressBar.setVisibility(View.GONE);
        if (results != null && !results.isEmpty()) {
            listHeaderTextView.setText("Search Results");
            noResultsTextView.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            searchAdapter.setDevotees(results);
        } else {
            searchResultsRecyclerView.setVisibility(View.GONE);
            if (searchEditText.getText().length() >= SEARCH_TRIGGER_LENGTH) { listHeaderTextView.setVisibility(View.GONE); noResultsTextView.setVisibility(View.VISIBLE); }
        }
    }

    private void showActiveBatchState() {
        batchSummaryCard.setVisibility(View.VISIBLE);
        donationsRecyclerView.setVisibility(View.VISIBLE);
        searchControlsLayout.setVisibility(View.VISIBLE);
        batchClosedLayout.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
    }
    
    private void showBatchClosedState() {
        batchSummaryCard.setVisibility(View.GONE);
        donationsRecyclerView.setVisibility(View.GONE);
        listHeaderTextView.setVisibility(View.GONE);
        searchControlsLayout.setVisibility(View.GONE);
        batchClosedLayout.setVisibility(View.VISIBLE);
        if (activeBatchId != null) {
            batchClosedMessageText.setText(String.format(Locale.US, "Batch #%d successfully closed.\nThank you.", activeBatchId));
        } else {
            batchClosedMessageText.setText("Batch successfully closed.\nThank you.");
        }
    }

    private void showInsufficientInputState() {
        searchProgressBar.setVisibility(View.GONE); noResultsTextView.setVisibility(View.GONE); searchResultsRecyclerView.setVisibility(View.GONE);
        donationsRecyclerView.setVisibility(View.GONE); listHeaderTextView.setVisibility(View.GONE);
    }
    private void showSearchingState() {
        noResultsTextView.setVisibility(View.GONE); searchResultsRecyclerView.setVisibility(View.GONE);
        donationsRecyclerView.setVisibility(View.GONE); searchProgressBar.setVisibility(View.VISIBLE); listHeaderTextView.setVisibility(View.GONE);
    }
    
    private void showDepositConfirmation() {
        String message = "Please deposit the cash in the office. A summary email will automatically be sent.";
        new AlertDialog.Builder(this)
            .setTitle("Close Batch")
            .setMessage(message)
            .setPositiveButton("OK", (dialog, which) -> {
                sendSummaryEmailWithAttachment();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendSummaryEmailWithAttachment() {
        AttendanceRepository.ActiveBatchData data = viewModel.getActiveBatchData().getValue();
        if (data == null) { Toast.makeText(this, "Could not send email: no active batch data found.", Toast.LENGTH_LONG).show(); return; }
        String officeEmail = ((AttendanceApplication) getApplication()).repository.getOfficeEmail();
        if (officeEmail == null || officeEmail.isEmpty()) { Toast.makeText(this, "Error: Office email has not been configured.", Toast.LENGTH_LONG).show(); return; }

        try {
            CsvExporter exporter = new CsvExporter();
            String authority = getApplication().getPackageName() + ".fileprovider";
            Uri csvUri = exporter.exportDonationsForBatch(this, data.batch.batchId, data.donations, authority);

            String today = DateTimeFormatter.ofPattern("dd MMM, yyyy").format(LocalDateTime.now());
            String subject = String.format("Donation Collection Summary: Batch #%d (%s)", data.batch.batchId, today);
            String body = "Donation Collection Summary\n" + "-----------------------------------\n" + "Batch ID: " + data.batch.batchId + "\n" + "Date: " + today + "\n" + "Collection Period: " + timeFormatter.format(LocalDateTime.parse(data.batch.startTs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) + " - " + timeFormatter.format(LocalDateTime.now()) + "\n" + "Collected By: Donation Collector\n" + "-----------------------------------\n" + "Total Donations: " + data.summary.donationCount + "\n" + "Cash Collected: " + currencyFormatter.format(data.summary.totalCash) + "\n" + "UPI Collected: " + currencyFormatter.format(data.summary.totalUpi) + "\n" + "-----------------------------------\n" + "Grand Total: " + currencyFormatter.format(data.summary.totalCash + data.summary.totalUpi) + "\n" + "-----------------------------------\n\n" + "Detailed transaction list is attached.\n\n" + "This is an auto-generated email from the SevaConnect Halasuru app.";

            // --- START OF DEFINITIVE FIX ---
            // Use the robust ACTION_SEND intent, which is designed for attachments.
            Intent intent = new Intent(Intent.ACTION_SEND);

            // Set the MIME type for an email
            intent.setType("message/rfc822");

            // Set email details
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{officeEmail});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);

            // Attach the file
            intent.putExtra(Intent.EXTRA_STREAM, csvUri);

            // Grant permission for the receiving app to read the file
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Target Gmail directly to avoid the share chooser
            intent.setPackage("com.google.android.gm");

            // Use the launcher to close the batch when the user returns
            emailLauncher.launch(intent);
            // --- END OF DEFINITIVE FIX ---

        } catch (android.content.ActivityNotFoundException ex) {
            // This will now catch the case where Gmail is not installed.
            Toast.makeText(this, "Gmail is not installed. Could not send email.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to generate or send batch summary email.", e);
            Toast.makeText(this, "Error creating summary attachment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.donation_main_menu, menu); return true; }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { int itemId = item.getItemId(); if (itemId == R.id.action_switch_role) { logoutAndReturnToRoleSelection(); return true; } else if (itemId == R.id.action_about) { startActivity(new Intent(this, AboutActivity.class)); return true; } return super.onOptionsItemSelected(item); }
    private void logoutAndReturnToRoleSelection() { Intent intent = new Intent(this, RoleSelectionActivity.class); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); finish(); }
}
