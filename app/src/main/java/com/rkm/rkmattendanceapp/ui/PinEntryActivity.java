// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/PinEntryActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.donations.DonationActivity;

public class PinEntryActivity extends AppCompatActivity {

    public static final String EXTRA_PRIVILEGE = "com.rkm.rkmattendanceapp.ui.EXTRA_PRIVILEGE";

    private AttendanceRepository repository;
    private Privilege currentPrivilege;
    private EditText pin1, pin2, pin3, pin4;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_entry);

        repository = ((AttendanceApplication) getApplication()).repository;

        currentPrivilege = (Privilege) getIntent().getSerializableExtra(EXTRA_PRIVILEGE);
        if (currentPrivilege == null) {
            Toast.makeText(this, "Error: No role specified.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.text_pin_entry_title);
        submitButton = findViewById(R.id.button_submit_pin);
        pin1 = findViewById(R.id.pin_digit_1);
        pin2 = findViewById(R.id.pin_digit_2);
        pin3 = findViewById(R.id.pin_digit_3);
        pin4 = findViewById(R.id.pin_digit_4);

        switch (currentPrivilege) {
            case SUPER_ADMIN:
                title.setText("Enter Super Admin PIN");
                break;
            case EVENT_COORDINATOR:
                title.setText("Enter Event Coordinator PIN");
                break;
            case DONATION_COLLECTOR:
                title.setText("Enter Donation Collector PIN");
                break;
        }

        setupPinTextWatchers();

        submitButton.setOnClickListener(v -> {
            String pin = getPinFromInput();
            if (pin.length() == 4) {
                verifyPin(pin);
            }
        });
    }

    private void setupPinTextWatchers() {
        pin1.addTextChangedListener(new PinTextWatcher(pin1, pin2));
        pin2.addTextChangedListener(new PinTextWatcher(pin2, pin3));
        pin3.addTextChangedListener(new PinTextWatcher(pin3, pin4));
        pin4.addTextChangedListener(new PinTextWatcher(pin4, null));
    }

    private String getPinFromInput() {
        return pin1.getText().toString() +
                pin2.getText().toString() +
                pin3.getText().toString() +
                pin4.getText().toString();
    }

    private void verifyPin(String pin) {
        new Thread(() -> {
            boolean isCorrect;
            switch (currentPrivilege) {
                case SUPER_ADMIN:
                    isCorrect = repository.checkSuperAdminPin(pin);
                    break;
                case EVENT_COORDINATOR:
                    isCorrect = repository.checkEventCoordinatorPin(pin);
                    break;
                case DONATION_COLLECTOR:
                    isCorrect = repository.checkDonationCollectorPin(pin);
                    break;
                default:
                    isCorrect = false;
                    break;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isCorrect) {
                    onPinSuccess();
                } else {
                    onPinFailure();
                }
            });
        }).start();
    }

    private void onPinSuccess() {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
        Intent intent;
        if (currentPrivilege == Privilege.DONATION_COLLECTOR) {
            intent = new Intent(this, DonationActivity.class);
        } else {
            intent = new Intent(this, AdminMainActivity.class);
            intent.putExtra(AdminMainActivity.EXTRA_PRIVILEGE, currentPrivilege);
        }
        startActivity(intent);
        finish();
    }

    private void onPinFailure() {
        Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
        pin1.setText("");
        pin2.setText("");
        pin3.setText("");
        pin4.setText("");
        pin1.requestFocus();
        submitButton.setEnabled(false);
    }

    private class PinTextWatcher implements TextWatcher {
        private final View currentView;
        private final View nextView;

        PinTextWatcher(View current, View next) {
            this.currentView = current;
            this.nextView = next;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1) {
                if (nextView != null) {
                    nextView.requestFocus();
                }
            }
            submitButton.setEnabled(getPinFromInput().length() == 4);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
