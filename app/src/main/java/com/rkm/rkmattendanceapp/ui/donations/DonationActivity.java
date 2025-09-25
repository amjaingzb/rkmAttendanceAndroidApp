// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationActivity.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rkm.rkmattendanceapp.R;

public class DonationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        setTitle("Record Donations");

        // In Phase 1, this activity is just a placeholder to complete the login flow.
        // In Phase 2, we will add the search logic and RecyclerView here.
        // For now, we can show a toast to confirm we've arrived.
        Toast.makeText(this, "Donation Collector Mode", Toast.LENGTH_SHORT).show();
    }
}
