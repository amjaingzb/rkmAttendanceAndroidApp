// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/RoleSelectionActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.donations.DonationActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.button_donation_collector).setOnClickListener(v -> {
            launchPinEntry(Privilege.DONATION_COLLECTOR);
        });

        findViewById(R.id.button_event_coordinator).setOnClickListener(v -> {
            launchPinEntry(Privilege.EVENT_COORDINATOR);
        });

        findViewById(R.id.button_super_admin).setOnClickListener(v -> {
            launchPinEntry(Privilege.SUPER_ADMIN);
        });
    }

    private void launchPinEntry(Privilege role) {
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.putExtra(PinEntryActivity.EXTRA_PRIVILEGE, role);
        startActivity(intent);
        // We finish this activity so the user can't come back to it with the back button
        // after logging in.
        finish();
    }
}
