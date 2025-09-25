// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.AboutActivity;
import com.rkm.rkmattendanceapp.ui.AddEditDevoteeActivity;
import com.rkm.rkmattendanceapp.ui.DevoteeListAdapter;
import com.rkm.rkmattendanceapp.ui.RoleSelectionActivity;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.ArrayList;

public class DonationActivity extends AppCompatActivity {

    private static final String TAG = "DonationActivity";
    private static final int SEARCH_TRIGGER_LENGTH = 3;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 300;

    private DonationViewModel viewModel;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private Button addNewButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView donationsRecyclerView;
    private View donationsLayout;
    private DevoteeListAdapter searchAdapter;
    private DonationListAdapter donationAdapter;
    private ProgressBar searchProgressBar;
    private TextView noResultsTextView;
    private TextView noDonationsTextView;
    private TextView listHeaderTextView;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private ActivityResultLauncher<Intent> addDevoteeLauncher;
    private ActivityResultLauncher<Intent> addDonationLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        setTitle("Record Donations");

        viewModel = new ViewModelProvider(this).get(DonationViewModel.class);

        setupLaunchers();
        bindViews();
        setupRecyclerViews();
        setupSearchAndButtons();
        observeViewModel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchResultsRecyclerView.getVisibility() == View.VISIBLE) {
                    searchEditText.setText("");
                    showPristineState();
                } else {
                    logoutAndReturnToRoleSelection();
                }
            }
        });

        // --- START OF CRASH FIX ---
        // An Activity must use getSupportFragmentManager(), not getParentFragmentManager().
        getSupportFragmentManager().setFragmentResultListener(DonationActionsBottomSheetFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            String action = bundle.getString(DonationActionsBottomSheetFragment.KEY_ACTION);
            long donationId = bundle.getLong(DonationActionsBottomSheetFragment.KEY_DONATION_ID);
            handleDonationAction(action, donationId);
        });
        // --- END OF CRASH FIX ---

        viewModel.loadTodaysDonations();
    }

    private void handleDonationAction(String action, long donationId) {
        if (action == null) return;
        switch (action) {
            case "DELETE":
                new AlertDialog.Builder(this)
                        .setTitle("Delete Donation")
                        .setMessage("Are you sure you want to delete this donation record? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteDonation(donationId))
                        .setNegativeButton("Cancel", null)
                        .show();
                break;
            case "EDIT":
                Toast.makeText(this, "Edit functionality will be added later.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupLaunchers() {
        addDonationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        viewModel.loadTodaysDonations();
                    }
                    searchEditText.setText("");
                }
        );

        addDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String newPhone = result.getData().getStringExtra(AddEditDevoteeActivity.RESULT_EXTRA_NEW_DEVOTEE_PHONE);
                        if (newPhone != null && !newPhone.isEmpty()) {
                            searchEditText.setText(newPhone);
                            searchEditText.selectAll();
                        } else {
                            searchEditText.setText("");
                        }
                    } else {
                        searchEditText.setText("");
                    }
                });
    }

    private void bindViews() {
        searchInputLayout = findViewById(R.id.text_input_layout_search);
        searchEditText = findViewById(R.id.edit_text_search_devotee);
        addNewButton = findViewById(R.id.button_add_new_devotee);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        donationsRecyclerView = findViewById(R.id.recycler_view_donations);
        donationsLayout = findViewById(R.id.layout_todays_donations);
        searchProgressBar = findViewById(R.id.progress_bar_search);
        noResultsTextView = findViewById(R.id.text_no_results);
        noDonationsTextView = findViewById(R.id.text_no_donations_today);
        listHeaderTextView = findViewById(R.id.text_list_header);
    }

    private void setupRecyclerViews() {
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new DevoteeListAdapter();
        searchAdapter.setOnDevoteeClickListener(devotee -> {
            Intent intent = new Intent(this, AddEditDonationActivity.class);
            intent.putExtra(AddEditDonationActivity.EXTRA_DEVOTEE_ID, devotee.getDevoteeId());
            addDonationLauncher.launch(intent);
        });
        searchResultsRecyclerView.setAdapter(searchAdapter);

        donationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        donationAdapter = new DonationListAdapter();
        donationAdapter.setOnDonationClickListener(record -> {
            DonationActionsBottomSheetFragment.newInstance(record.donation.donationId)
                    .show(getSupportFragmentManager(), DonationActionsBottomSheetFragment.TAG);
        });
        donationsRecyclerView.setAdapter(donationAdapter);
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

        addNewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditDevoteeActivity.class);
            String prefillQuery = searchEditText.getText().toString().trim();
            if (!prefillQuery.isEmpty()) {
                intent.putExtra(AddEditDevoteeActivity.EXTRA_PREFILL_QUERY, prefillQuery);
            }
            addDevoteeLauncher.launch(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getTodaysDonations().observe(this, donations -> {
            donationAdapter.setDonations(donations);
            if (donations == null || donations.isEmpty()) {
                noDonationsTextView.setVisibility(View.VISIBLE);
                donationsRecyclerView.setVisibility(View.GONE);
                listHeaderTextView.setVisibility(View.GONE);
            } else {
                noDonationsTextView.setVisibility(View.GONE);
                donationsRecyclerView.setVisibility(View.VISIBLE);
                listHeaderTextView.setText("Donations Received Today");
                listHeaderTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSearchResults().observe(this, results -> {
            searchProgressBar.setVisibility(View.GONE);
            if (results != null && !results.isEmpty()) {
                listHeaderTextView.setText("Search Results");
                listHeaderTextView.setVisibility(View.VISIBLE);
                noResultsTextView.setVisibility(View.GONE);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                searchAdapter.setDevotees(results);
            } else {
                searchResultsRecyclerView.setVisibility(View.GONE);
                if (searchEditText.getText().length() >= SEARCH_TRIGGER_LENGTH) {
                    listHeaderTextView.setVisibility(View.GONE);
                    noResultsTextView.setVisibility(View.VISIBLE);
                }
            }
        });

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
        searchAdapter.setDevotees(new ArrayList<>());
        
        donationsLayout.setVisibility(View.VISIBLE);
        viewModel.loadTodaysDonations();
    }

    private void showInsufficientInputState() {
        searchInputLayout.setHelperText("Enter at least " + SEARCH_TRIGGER_LENGTH + " characters");
        searchProgressBar.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        searchAdapter.setDevotees(new ArrayList<>());
        donationsLayout.setVisibility(View.GONE);
        listHeaderTextView.setVisibility(View.GONE);
    }

    private void showSearchingState() {
        searchInputLayout.setHelperText(null);
        noResultsTextView.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        donationsLayout.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.VISIBLE);
        listHeaderTextView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.donation_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_switch_role) {
            logoutAndReturnToRoleSelection();
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutAndReturnToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
