// In: src/main/java/com/rkm/rkmattendanceapp/ui/AddEditDevoteeActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.R;

public class AddEditDevoteeActivity extends AppCompatActivity {

    public static final String EXTRA_DEVOTEE_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_DEVOTEE_ID";
    public static final String EXTRA_PREFILL_QUERY = "com.rkm.rkmattendanceapp.ui.EXTRA_PREFILL_QUERY";
    public static final String EXTRA_IS_ON_SPOT_REG = "com.rkm.rkmattendanceapp.ui.EXTRA_IS_ON_SPOT_REG";
    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID";
    // NEW: Key for the result data
    public static final String RESULT_EXTRA_NEW_DEVOTEE_ID = "com.rkm.rkmattendanceapp.ui.RESULT_EXTRA_NEW_DEVOTEE_ID";

    public static final long NEW_DEVOTEE_ID = -1;

    private AddEditDevoteeViewModel viewModel;
    private TextInputLayout mobileInputLayout;
    private TextInputEditText nameEditText, mobileEditText, emailEditText, addressEditText, aadhaarEditText, panEditText, ageEditText;
    private AutoCompleteTextView genderAutoComplete;

    private long currentDevoteeId = NEW_DEVOTEE_ID;
    private boolean isOnSpotMode = false;
    private long eventIdForOnSpot = -1;
    private long newDevoteeIdToReturn = -1; // Variable to hold the new ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_devotee);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        bindViews();

        String[] genders = new String[]{"", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        genderAutoComplete.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AddEditDevoteeViewModel.class);

        mobileEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mobileInputLayout.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

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
    
    private void bindViews() {
        mobileInputLayout = findViewById(R.id.text_input_layout_mobile);
        nameEditText = findViewById(R.id.edit_text_name);
        mobileEditText = findViewById(R.id.edit_text_mobile);
        emailEditText = findViewById(R.id.edit_text_email);
        addressEditText = findViewById(R.id.edit_text_address);
        aadhaarEditText = findViewById(R.id.edit_text_aadhaar);
        panEditText = findViewById(R.id.edit_text_pan);
        ageEditText = findViewById(R.id.edit_text_age);
        genderAutoComplete = findViewById(R.id.auto_complete_gender);
    }

    private void observeViewModel() {
        viewModel.getDevotee().observe(this, devotee -> {
            if (devotee != null) {
                nameEditText.setText(devotee.getFullName());
                mobileEditText.setText(devotee.getMobileE164());
                emailEditText.setText(devotee.getEmail());
                addressEditText.setText(devotee.getAddress());
                aadhaarEditText.setText(devotee.getAadhaar());
                panEditText.setText(devotee.getPan());
                if (devotee.getAge() != null) {
                    ageEditText.setText(String.valueOf(devotee.getAge()));
                }
                genderAutoComplete.setText(devotee.getGender(), false);
            }
        });
        
        viewModel.getNewDevoteeId().observe(this, id -> {
            if (id != null) {
                newDevoteeIdToReturn = id;
            }
        });
        
        viewModel.getSaveFinished().observe(this, finished -> {
            if (finished != null && finished) {
                Toast.makeText(this, "Devotee saved", Toast.LENGTH_SHORT).show();
                
                Intent resultIntent = new Intent();
                if (newDevoteeIdToReturn > 0) {
                    resultIntent.putExtra(RESULT_EXTRA_NEW_DEVOTEE_ID, newDevoteeIdToReturn);
                }
                setResult(RESULT_OK, resultIntent);
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
        String mobileRaw = mobileEditText.getText().toString().trim();
        String mobileNorm = DevoteeDao.normalizePhone(mobileRaw);
        
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mobileNorm) || mobileNorm.length() != 10) {
            mobileInputLayout.setError("Mobile number must be 10 digits");
            return;
        }
        mobileInputLayout.setError(null);

        String email = emailEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String aadhaar = aadhaarEditText.getText().toString().trim();
        String pan = panEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String gender = genderAutoComplete.getText().toString().trim();

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
                currentDevoteeId == NEW_DEVOTEE_ID ? null : currentDevoteeId, name, null,
                mobileNorm, address, age, email, gender, aadhaar, pan, null
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
