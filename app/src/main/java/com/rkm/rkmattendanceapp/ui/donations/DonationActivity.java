// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.AboutActivity;
import com.rkm.rkmattendanceapp.ui.RoleSelectionActivity;

public class DonationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        setTitle("Record Donations");

        // CORRECTED: No "Up" arrow. The back navigation is handled by the menu and system back button.
        // if (getSupportActionBar() != null) {
        //     getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // }

        // In Phase 1, this activity is just a placeholder to complete the login flow.
        Toast.makeText(this, "Donation Collector Mode", Toast.LENGTH_SHORT).show();
        
        // --- START OF SYSTEM BACK BUTTON FIX ---
        // This is the modern, correct way to handle the system back button.
        // It ensures that pressing back performs our custom "logout" action.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                logoutAndReturnToRoleSelection();
            }
        });
        // --- END OF SYSTEM BACK BUTTON FIX ---
    }

    // --- START OF MENU FIX ---
    // Inflate the new 3-dot menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.donation_main_menu, menu);
        return true;
    }

    // Handle clicks on the 3-dot menu items
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
        // This handles the legacy android.R.id.home case, though we don't use it now.
        return super.onOptionsItemSelected(item);
    }
    // --- END OF MENU FIX ---

    // --- START OF CENTRALIZED LOGOUT LOGIC ---
    // A helper method to avoid duplicating code.
    private void logoutAndReturnToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    // --- END OF CENTRALIZED LOGOUT LOGIC ---
}
