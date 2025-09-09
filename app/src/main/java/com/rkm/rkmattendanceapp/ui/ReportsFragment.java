package com.rkm.rkmattendanceapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.R;

public class ReportsFragment extends Fragment {

    private ReportsViewModel reportsViewModel;
    private TextView totalDevoteesText, totalWhatsappText, devoteesInWhatsappText, devoteesWithAttendanceText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        totalDevoteesText = view.findViewById(R.id.text_total_devotees);
        totalWhatsappText = view.findViewById(R.id.text_total_whatsapp);
        devoteesInWhatsappText = view.findViewById(R.id.text_devotees_in_whatsapp);
        devoteesWithAttendanceText = view.findViewById(R.id.text_devotees_with_attendance);

        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        // Observe the stats
        reportsViewModel.getCounterStats().observe(getViewLifecycleOwner(), this::updateStatsUI);

        // Observe for errors
        reportsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // Load the data
        reportsViewModel.loadStats();
    }

    private void updateStatsUI(DevoteeDao.CounterStats stats) {
        if (stats == null) return;
        totalDevoteesText.setText(String.valueOf(stats.totalDevotees()));
        totalWhatsappText.setText(String.valueOf(stats.totalMappedWhatsAppNumbers()));
        devoteesInWhatsappText.setText(String.valueOf(stats.registeredDevoteesInWhatsApp()));
        devoteesWithAttendanceText.setText(String.valueOf(stats.devoteesWithAttendance()));
    }
}