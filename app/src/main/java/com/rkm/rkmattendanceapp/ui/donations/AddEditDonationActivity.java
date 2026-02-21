// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/AddEditDonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.AddEditDevoteeActivity;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class AddEditDonationActivity extends AppCompatActivity {
    
    private static final String TAG = "AddEditDonationActivity";
    public static final String EXTRA_DEVOTEE_ID = "com.rkm.rkmattendanceapp.ui.donations.EXTRA_DEVOTEE_ID";
    public static final String EXTRA_BATCH_ID = "com.rkm.rkmattendanceapp.ui.donations.EXTRA_BATCH_ID";

    private AddEditDonationViewModel viewModel;
    private long currentDevoteeId = -1;
    private long currentBatchId = -1;

    private TextView donorNameText, donorMobileText, donorAddressText, donorEmailText, donorIdLabel, donorIdValue, missingFieldsWarningText;
    private ImageButton editDevoteeButton;
    private LinearLayout donorIdLayout;
    private EditText amountEditText, upiRefEditText;
    private AutoCompleteTextView purposeAutoComplete;
    private RadioGroup paymentMethodRadioGroup;
    private TextInputLayout upiRefLayout;
    
    private ActivityResultLauncher<Intent> editDevoteeLauncher;
    private MenuItem saveDonationMenuItem;
    private Devotee currentDevotee;

    private Button skipDetailsButton; // New Button
    private boolean isDetailsSkipped = false; // New Flag
    private final String[] purposes = {"General Donation","Sadhu Seva", "Math Activities", "Bhandara" };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_donation);
        setTitle("Record Donation");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        Intent intent = getIntent();
        currentDevoteeId = intent.getLongExtra(EXTRA_DEVOTEE_ID, -1);
        currentBatchId = intent.getLongExtra(EXTRA_BATCH_ID, -1);

        if (currentDevoteeId == -1 || currentBatchId == -1) {
            Toast.makeText(this, "Error: Missing Devotee or Batch ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupLauncher();
        bindViews();
        setupPurposeAutoComplete();
        setupListeners();

        viewModel = new ViewModelProvider(this).get(AddEditDonationViewModel.class);
        observeViewModel();
        viewModel.loadDevotee(currentDevoteeId);
    }

    private void setupLauncher() {
        editDevoteeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(this, "Devotee details updated.", Toast.LENGTH_SHORT).show();
                        // Reset flag so we re-check the actual data
                        isDetailsSkipped = false;
                        viewModel.loadDevotee(currentDevoteeId);
                    }
                }
        );
    }

    private void bindViews() {
        donorNameText = findViewById(R.id.text_donor_name);
        donorMobileText = findViewById(R.id.text_donor_mobile);
        donorAddressText = findViewById(R.id.text_donor_address);
        donorEmailText = findViewById(R.id.text_donor_email);
        editDevoteeButton = findViewById(R.id.button_edit_devotee);
        donorIdLayout = findViewById(R.id.layout_donor_id);
        donorIdLabel = findViewById(R.id.text_donor_id_label);
        donorIdValue = findViewById(R.id.text_donor_id_value);
        missingFieldsWarningText = findViewById(R.id.text_missing_fields_warning);
        amountEditText = findViewById(R.id.edit_text_amount);
        upiRefEditText = findViewById(R.id.edit_text_upi_ref);
        purposeAutoComplete = findViewById(R.id.autocomplete_purpose);
        paymentMethodRadioGroup = findViewById(R.id.radio_group_payment_method);
        upiRefLayout = findViewById(R.id.layout_upi_ref);
        skipDetailsButton = findViewById(R.id.button_skip_details); // Bind new button

    }

    private void setupPurposeAutoComplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, purposes);
        purposeAutoComplete.setAdapter(adapter);

        // 1. DO NOT pre-fill text. Keep it clean.
        // purposeAutoComplete.setText(purposes[0], false); <-- REMOVE THIS

        // 2. Set a descriptive hint to inform the user of the default
        TextInputLayout layout = findViewById(R.id.layout_purpose);
        layout.setHint("Towards (Default: " + purposes[0] + ")");

        // 3. FORCE the menu to open when clicked or focused.
        // This solves the "clueless" problem. They tap, they see options immediately.
        purposeAutoComplete.setOnClickListener(v -> purposeAutoComplete.showDropDown());
        purposeAutoComplete.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                purposeAutoComplete.showDropDown();
            }
        });
    }

    private void setupListeners() {
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_cash) {
                upiRefLayout.setVisibility(View.GONE);
            } else {
                upiRefLayout.setVisibility(View.VISIBLE);
                // Dynamic Hint Change
                if (checkedId == R.id.radio_upi) {
                    upiRefLayout.setHint("UPI Reference ID*");
                } else {
                    upiRefLayout.setHint("Cheque Number*");
                }
            }
        });

        editDevoteeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditDevoteeActivity.class);
            intent.putExtra(AddEditDevoteeActivity.EXTRA_DEVOTEE_ID, currentDevoteeId);
            editDevoteeLauncher.launch(intent);
        });

        skipDetailsButton.setOnClickListener(v -> {
            isDetailsSkipped = true;
            validateDevoteeAndSetSaveState();
        });

    }

    private void observeViewModel() {
        viewModel.getDevotee().observe(this, devotee -> {
            if (devotee != null) {
                this.currentDevotee = devotee;
                updateDonorInfo(devotee);
                validateDevoteeAndSetSaveState();
            }
        });
        viewModel.getSaveFinished().observe(this, finished -> {
            if (finished != null && finished) {
                Toast.makeText(this, "Donation recorded successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        viewModel.getErrorMessage().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateDonorInfo(Devotee devotee) {
        donorNameText.setText(devotee.getFullName());
        donorMobileText.setText(devotee.getMobileE164());
        if (isNotBlank(devotee.getAddress())) { donorAddressText.setText(devotee.getAddress()); donorAddressText.setVisibility(View.VISIBLE); } else { donorAddressText.setVisibility(View.GONE); }
        if (isNotBlank(devotee.getEmail())) { donorEmailText.setText(devotee.getEmail()); donorEmailText.setVisibility(View.VISIBLE); } else { donorEmailText.setVisibility(View.GONE); }
        if (isNotBlank(devotee.getPan())) { donorIdLabel.setText("PAN:"); donorIdValue.setText(devotee.getPan()); donorIdLayout.setVisibility(View.VISIBLE); } else if (isNotBlank(devotee.getAadhaar())) { donorIdLabel.setText("Aadhaar:"); donorIdValue.setText(devotee.getAadhaar()); donorIdLayout.setVisibility(View.VISIBLE); } else { donorIdLayout.setVisibility(View.GONE); }
    }

    private void validateDevoteeAndSetSaveState() {
        if (currentDevotee == null || saveDonationMenuItem == null) return;

        // Check if data exists
        boolean isAddressValid = isNotBlank(currentDevotee.getAddress());
        boolean isIdValid = isNotBlank(currentDevotee.getPan()) || isNotBlank(currentDevotee.getAadhaar());
        boolean hasRequiredDetails = isAddressValid && isIdValid;

        // ENABLE SAVE: If details exist OR user explicitly skipped
        boolean enableSave = hasRequiredDetails || isDetailsSkipped;

        saveDonationMenuItem.setEnabled(enableSave);

        // Update Icon Alpha
        Drawable icon = saveDonationMenuItem.getIcon();
        if (icon != null) icon.mutate().setAlpha(enableSave ? 255 : 130);

        // SHOW/HIDE WARNING & SKIP BUTTON
        // If save is enabled, we don't need warnings or skip buttons
        if (enableSave) {
            missingFieldsWarningText.setVisibility(View.GONE);
            skipDetailsButton.setVisibility(View.GONE);
        } else {
            // Data is missing and user hasn't skipped yet
            missingFieldsWarningText.setVisibility(View.VISIBLE);
            skipDetailsButton.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNotBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private void saveDonation() {
        String amountStr = amountEditText.getText().toString().trim();
        String purpose = purposeAutoComplete.getText().toString().trim();
        String refId = upiRefEditText.getText().toString().trim();

        // Determine Payment Method
        int selectedId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        String paymentMethod;
        if (selectedId == R.id.radio_upi) {
            paymentMethod = "UPI";
        } else if (selectedId == R.id.radio_cheque) {
            paymentMethod = "CHEQUE";
        } else {
            paymentMethod = "CASH";
        }

        if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(purpose)) {
            purpose = purposes[0];
        }

        // Validate Ref ID for both UPI and Cheque
        if (!"CASH".equals(paymentMethod) && TextUtils.isEmpty(refId)) {
            String errorMsg = "UPI".equals(paymentMethod) ? "Please enter the UPI Reference ID." : "Please enter the Cheque Number.";
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        // Pass refId only if not Cash
        String finalRef = "CASH".equals(paymentMethod) ? null : refId;

        viewModel.saveDonation(currentDevoteeId, amount, paymentMethod, finalRef, purpose, currentBatchId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_donation_menu, menu);
        saveDonationMenuItem = menu.findItem(R.id.action_save_donation);
        validateDevoteeAndSetSaveState(); 
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_donation) { saveDonation(); return true; }
        else if (itemId == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
