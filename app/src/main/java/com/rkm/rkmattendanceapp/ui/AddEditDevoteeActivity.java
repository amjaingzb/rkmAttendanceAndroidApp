// In: src/main/java/com/rkm/rkmattendanceapp/ui/AddEditDevoteeActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

public class AddEditDevoteeActivity extends AppCompatActivity {

    // Define ALL the keys this Activity accepts
    public static final String EXTRA_DEVOTEE_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_DEVOTEE_ID";
    public static final String EXTRA_PREFILL_QUERY = "com.rkm.rkmattendanceapp.ui.EXTRA_PREFILL_QUERY";
    public static final String EXTRA_IS_ON_SPOT_REG = "com.rkm.rkmattendanceapp.ui.EXTRA_IS_ON_SPOT_REG";
    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID"; // The key for the event ID

    public static final long NEW_DEVOTEE_ID = -1;

    private AddEditDevoteeViewModel viewModel;

    private TextInputEditText nameEditText;
    private TextInputEditText mobileEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText addressEditText;
    private TextInputEditText ageEditText;
    private AutoCompleteTextView genderAutoComplete;

    private long currentDevoteeId = NEW_DEVOTEE_ID;

    private boolean isOnSpotMode = false;
    private long eventIdForOnSpot = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_devotee);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        nameEditText = findViewById(R.id.edit_text_name);
        mobileEditText = findViewById(R.id.edit_text_mobile);
        emailEditText = findViewById(R.id.edit_text_email);
        addressEditText = findViewById(R.id.edit_text_address);
        ageEditText = findViewById(R.id.edit_text_age);
        genderAutoComplete = findViewById(R.id.auto_complete_gender);

        String[] genders = new String[]{"", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        genderAutoComplete.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AddEditDevoteeViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_IS_ON_SPOT_REG)) {
            isOnSpotMode = intent.getBooleanExtra(EXTRA_IS_ON_SPOT_REG, false);
            eventIdForOnSpot = intent.getLongExtra(EXTRA_EVENT_ID, -1);
        }
        if (intent.hasExtra(EXTRA_DEVOTEE_ID)) {
            currentDevoteeId = intent.getLongExtra(EXTRA_DEVOTEE_ID, NEW_DEVOTEE_ID);
        }

        if (currentDevoteeId == NEW_DEVOTEE_ID) {
            setTitle("New Devotee");
            if (intent.hasExtra(EXTRA_PREFILL_QUERY)) {
                String query = intent.getStringExtra(EXTRA_PREFILL_QUERY);
                if (query != null && query.matches(".*\\d.*")) {
                    mobileEditText.setText(query);
                } else {
                    nameEditText.setText(query);
                }
            }
        } else {
            setTitle("Edit Devotee");
            viewModel.loadDevotee(currentDevoteeId);
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getDevotee().observe(this, devotee -> {
            if (devotee != null) {
                nameEditText.setText(devotee.getFullName());
                mobileEditText.setText(devotee.getMobileE164());
                emailEditText.setText(devotee.getEmail());
                addressEditText.setText(devotee.getAddress());
                if (devotee.getAge() != null) {
                    ageEditText.setText(String.valueOf(devotee.getAge()));
                }
                genderAutoComplete.setText(devotee.getGender(), false);
            }
        });

        viewModel.getSaveFinished().observe(this, finished -> {
            if (finished != null && finished) {
                Toast.makeText(this, "Devotee saved", Toast.LENGTH_SHORT).show();
                // Set the result to OK so the calling fragment knows we saved.
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveDevotee() {
        String name = nameEditText.getText().toString().trim();
        String mobile = mobileEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String gender = genderAutoComplete.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mobile)) {
            Toast.makeText(this, "Please enter a name and mobile number", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer age = null;
        if (!ageStr.isEmpty()) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Devotee devoteeToSave = new Devotee(
                currentDevoteeId == NEW_DEVOTEE_ID ? null : currentDevoteeId,
                name,
                null,
                mobile,
                address,
                age,
                email,
                gender,
                null
        );

        viewModel.saveDevotee(devoteeToSave, isOnSpotMode, eventIdForOnSpot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_devotee_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_devotee) {
            saveDevotee();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}