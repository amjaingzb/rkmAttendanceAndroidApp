// In: src/main/java/com/rkm/rkmattendanceapp/ui/ReportsFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
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

import com.google.android.material.card.MaterialCardView;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.ui.reports.ReportDevoteeActivity;
import com.rkm.rkmattendanceapp.ui.reports.ReportDonationDaysActivity;
import com.rkm.rkmattendanceapp.ui.reports.ReportEventListActivity;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportsFragment extends Fragment {

    private static final String TAG = "ReportsFragment";
    private ReportsViewModel reportsViewModel;
    private TextView totalDevoteesText, totalWhatsappText, devoteesInWhatsappText, devoteesWithAttendanceText;
    
    private MaterialCardView donationsReportCard, attendanceByEventCard, devoteeActivityCard;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        bindViews(view);
        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);
        
        setupClickListeners();
        observeViewModel();

        reportsViewModel.loadStats();
    }

    private void bindViews(View view) {
        totalDevoteesText = view.findViewById(R.id.text_total_devotees);
        totalWhatsappText = view.findViewById(R.id.text_total_whatsapp);
        devoteesInWhatsappText = view.findViewById(R.id.text_devotees_in_whatsapp);
        devoteesWithAttendanceText = view.findViewById(R.id.text_devotees_with_attendance);
        donationsReportCard = view.findViewById(R.id.card_donations_report);
        attendanceByEventCard = view.findViewById(R.id.card_attendance_by_event);
        devoteeActivityCard = view.findViewById(R.id.card_devotee_activity);
    }

    private void setupClickListeners() {
        donationsReportCard.setOnClickListener(v -> {
            AppLogger.d(TAG, "Donations Report card clicked.");
            Intent intent = new Intent(getActivity(), ReportDonationDaysActivity.class);
            startActivity(intent);
        });

        attendanceByEventCard.setOnClickListener(v -> {
            AppLogger.d(TAG, "Attendance by Event card clicked.");
            Intent intent = new Intent(getActivity(), ReportEventListActivity.class);
            startActivity(intent);
        });
        
        devoteeActivityCard.setOnClickListener(v -> {
            AppLogger.d(TAG, "Devotee Activity card clicked.");
            Intent intent = new Intent(getActivity(), ReportDevoteeActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        reportsViewModel.getCounterStats().observe(getViewLifecycleOwner(), this::updateStatsUI);

        reportsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateStatsUI(DevoteeDao.CounterStats stats) {
        if (stats == null) return;
        totalDevoteesText.setText(String.valueOf(stats.totalDevotees()));
        totalWhatsappText.setText(String.valueOf(stats.totalMappedWhatsAppNumbers()));
        devoteesInWhatsappText.setText(String.valueOf(stats.registeredDevoteesInWhatsApp()));
        devoteesWithAttendanceText.setText(String.valueOf(stats.devoteesWithAttendance()));
    }
}
