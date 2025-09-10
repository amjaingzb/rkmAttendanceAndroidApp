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

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        AttendanceRepository repository = ((AttendanceApplication) getApplication()).repository;

        // Use a short delay to make the splash screen visible for a moment
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check for the active event on a background thread
            new Thread(() -> {
                Event activeEvent = repository.getActiveEvent();

                // All UI changes must be posted back to the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (activeEvent != null) {
                        // Active event found, eventually go to Operator Mode
                        launchMarkAttendance(activeEvent.getEventId());
                    } else {
                        // No active event, go to Admin Mode
                        launchAdmin();
                    }
                });
            }).start();
        }, 500); // 500ms delay
    }

    private void launchMarkAttendance(long eventId) {
        // THIS IS THE FINAL VERSION
        Intent intent = new Intent(this, MarkAttendanceActivity.class);
        intent.putExtra(MarkAttendanceActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
    }

    private void launchAdmin() {
        Intent intent = new Intent(this, AdminMainActivity.class);
        startActivity(intent);
        finish();
    }
}