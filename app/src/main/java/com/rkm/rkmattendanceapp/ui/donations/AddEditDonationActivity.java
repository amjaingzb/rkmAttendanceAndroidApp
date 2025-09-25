// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/AddEditDonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.AddEditDevoteeActivity;

public class AddEditDonationActivity extends AppCompatActivity {

    public static final String EXTRA_DEVOTEE_ID = "com.rkm.rkmattendanceapp.ui.donations.EXTRA_DEVOTEE_ID";

    private AddEditDonationViewModel viewModel;
    private long currentDevoteeId = -1;

    private TextView donorNameText, donorMobileText;
    private EditText amountEditText, upiRefEditText;
    private AutoCompleteTextView purposeAutoComplete;
    private RadioGroup paymentMethodRadioGroup;
    private TextInputLayout upiRefLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_donation);
        setTitle("Record Donation");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        currentDevoteeId = getIntent().getLongExtra(EXTRA_DEVOTEE_ID, -1);
        if (currentDevoteeId == -1) {
            Toast.makeText(this, "Error: No devotee ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupPurposeAutoComplete();
        setupListeners();

        viewModel = new ViewModelProvider(this).get(AddEditDonationViewModel.class);
        observeViewModel();
        viewModel.loadDevotee(currentDevoteeId);
    }

    private void bindViews() {
        donorNameText = findViewById(R.id.text_donor_name);
        donorMobileText = findViewById(R.id.text_donor_mobile);
        amountEditText = findViewById(R.id.edit_text_amount);
        upiRefEditText = findViewById(R.id.edit_text_upi_ref);
        purposeAutoComplete = findViewById(R.id.autocomplete_purpose);
        paymentMethodRadioGroup = findViewById(R.id.radio_group_payment_method);
        upiRefLayout = findViewById(R.id.layout_upi_ref);
    }

    private void setupPurposeAutoComplete() {
        String[] purposes = {"Sadhu Seva", "Math Activities", "General Donation", "Bhandara"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, purposes);
        purposeAutoComplete.setAdapter(adapter);
    }

    private void setupListeners() {
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_upi) {
                upiRefLayout.setVisibility(View.VISIBLE);
            } else {
                upiRefLayout.setVisibility(View.GONE);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getDevotee().observe(this, this::updateDonorInfo);
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
        if (devotee != null) {
            donorNameText.setText(devotee.getFullName());
            donorMobileText.setText(devotee.getMobileE164());
        }
    }

    private void saveDonation() {
        String amountStr = amountEditText.getText().toString().trim();
        String purpose = purposeAutoComplete.getText().toString().trim();
        String paymentMethod = paymentMethodRadioGroup.getCheckedRadioButtonId() == R.id.radio_upi ? "UPI" : "CASH";
        String upiRef = upiRefEditText.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(purpose)) {
            Toast.makeText(this, "Please specify the purpose of the donation.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("UPI".equals(paymentMethod) && TextUtils.isEmpty(upiRef)) {
            Toast.makeText(this, "Please enter the UPI Reference ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        viewModel.saveDonation(currentDevoteeId, amount, paymentMethod, upiRef, purpose);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_donation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_donation) {
            saveDonation();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
