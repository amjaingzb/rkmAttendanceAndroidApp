// In: src/main/java/com/rkm/rkmattendanceapp/ui/LauncherActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;

// This activity has no visible UI for long, so we can use a translucent theme.
// It will decide which real activity to launch.
public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Get the global repository instance
        AttendanceRepository repository = ((AttendanceApplication) getApplication()).repository;

        // We need to check for the active event on a background thread
        new Thread(() -> {
            Event activeEvent = repository.getActiveEvent();

            // All UI changes must be posted back to the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activeEvent != null) {
                    // Active event found, go to Operator Mode
                    launchMarkAttendance(activeEvent.getEventId());
                } else {
                    // No active event, go to Admin Mode
                    launchAdmin();
                }
            });
        }).start();
    }

    private void launchMarkAttendance(long eventId) {
        Intent intent = new Intent(this, MarkAttendanceActivity.class);
        // We will need to create MarkAttendanceActivity next.
        // For now, let's just use the Admin activity as a placeholder to avoid errors.
        // TODO: Change this to MarkAttendanceActivity.class when it exists
        // intent.putExtra(MarkAttendanceActivity.EXTRA_EVENT_ID, eventId);

        // TEMPORARY: Launch Admin until Operator screen is built
        launchAdmin();
    }

    private void launchAdmin() {
        Intent intent = new Intent(this, AdminMainActivity.class);
        startActivity(intent);
        // Finish this launcher activity so the user can't press "Back" to get to it.
        finish();
    }
}