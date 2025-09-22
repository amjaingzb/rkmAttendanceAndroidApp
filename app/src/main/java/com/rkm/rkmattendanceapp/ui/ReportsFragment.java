// In: src/main/java/com/rkm/rkmattendanceapp/ui/ReportsFragment.java
package com.rkm.rkmattendanceapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportsFragment extends Fragment {

    private static final String TAG = "ReportsFragment";
    private ReportsViewModel reportsViewModel;
    private TextView totalDevoteesText, totalWhatsappText, devoteesInWhatsappText, devoteesWithAttendanceText;
    private TextView exportSubtitleText;
    private MaterialCardView exportCard;

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
        exportSubtitleText = view.findViewById(R.id.text_export_devotee_subtitle);
        exportCard = view.findViewById(R.id.card_export_devotee_list);
    }

    private void setupClickListeners() {
        exportCard.setOnClickListener(v -> {
            AppLogger.d(TAG, "Export devotee list card clicked.");
            Toast.makeText(getContext(), "Generating export file...", Toast.LENGTH_SHORT).show();
            reportsViewModel.startDevoteeExport();
        });
    }

    private void observeViewModel() {
        reportsViewModel.getCounterStats().observe(getViewLifecycleOwner(), this::updateStatsUI);

        reportsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        reportsViewModel.getShareableFileUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                AppLogger.d(TAG, "Received shareable URI: " + uri);
                shareCsvFile(uri);
                reportsViewModel.onShareIntentHandled(); // Reset the event
            }
        });
    }

    private void updateStatsUI(DevoteeDao.CounterStats stats) {
        if (stats == null) return;
        totalDevoteesText.setText(String.valueOf(stats.totalDevotees()));
        totalWhatsappText.setText(String.valueOf(stats.totalMappedWhatsAppNumbers()));
        devoteesInWhatsappText.setText(String.valueOf(stats.registeredDevoteesInWhatsApp()));
        devoteesWithAttendanceText.setText(String.valueOf(stats.devoteesWithAttendance()));

        // Update the dynamic label on the export card
        long total = stats.totalDevotees();
        if (total == 0) {
            exportSubtitleText.setText(R.string.report_export_devotee_subtitle_zero);
            exportCard.setEnabled(false);
            exportCard.setAlpha(0.5f);
        } else if (total == 1) {
            exportSubtitleText.setText(R.string.report_export_devotee_subtitle_one);
            exportCard.setEnabled(true);
            exportCard.setAlpha(1.0f);
        } else {
            exportSubtitleText.setText(getString(R.string.report_export_devotee_subtitle_many, total));
            exportCard.setEnabled(true);
            exportCard.setAlpha(1.0f);
        }
    }
    
    private void shareCsvFile(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_export_title)));
    }
}
