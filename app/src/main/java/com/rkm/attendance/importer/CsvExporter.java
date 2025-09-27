// In: src/main/java/com/rkm/attendance/importer/CsvExporter.java
package com.rkm.attendance.importer;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.opencsv.CSVWriter;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.ui.donations.DonationRecord;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    private static final String TAG = "CsvExporter";
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private String generateTimestamp() {
        return FILENAME_FORMATTER.format(LocalDateTime.now());
    }

    // THIS IS THE NEW, CORRECT METHOD
    public Uri exportDonationsForBatch(Context context, long batchId, List<DonationRecord> donations, String fileProviderAuthority) throws Exception {
        String[] headers = {
                "Donation ID", "Receipt Number", "Devotee Name", "Amount", "Payment Method",
                "Reference ID", "Purpose", "Timestamp"
        };

        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String fileName = String.format(Locale.US, "batch_%d_donations_%s.csv", batchId, generateTimestamp());
        File file = new File(exportDir, fileName);

        AppLogger.d(TAG, "Creating batch donation export file at: " + file.getAbsolutePath());

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);

            for (DonationRecord record : donations) {
                String[] data = {
                        String.valueOf(record.donation.donationId),
                        record.donation.receiptNumber,
                        record.devoteeName,
                        String.valueOf(record.donation.amount),
                        record.donation.paymentMethod,
                        record.donation.referenceId,
                        record.donation.purpose,
                        record.donation.donationTimestamp
                };
                writer.writeNext(data);
            }
            AppLogger.d(TAG, "Successfully wrote " + donations.size() + " donation records to CSV.");
        }

        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    // THIS IS THE ORIGINAL, CORRECTED METHOD
    public Uri exportDevotees(Context context, List<DevoteeDao.EnrichedDevotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = {
                "Full Name", "Mobile Number", "Address", "Age", "Email", "Gender",
                "Aadhaar", "PAN", "WhatsApp Group", "Total Attendance",
                "Last Attended Date", "Extra JSON"
        };
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String fileName = "devotee_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            for (DevoteeDao.EnrichedDevotee enriched : devotees) {
                String[] data = {
                        enriched.devotee().getFullName(),
                        enriched.devotee().getMobileE164(),
                        enriched.devotee().getAddress(),
                        enriched.devotee().getAge() != null ? String.valueOf(enriched.devotee().getAge()) : "",
                        enriched.devotee().getEmail(),
                        enriched.devotee().getGender(),
                        enriched.devotee().getAadhaar(),
                        enriched.devotee().getPan(),
                        enriched.whatsAppGroup() != null ? String.valueOf(enriched.whatsAppGroup()) : "N/A",
                        String.valueOf(enriched.cumulativeAttendance()),
                        enriched.lastAttendanceDate(),
                        enriched.devotee().getExtraJson()
                };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    // THIS IS THE ORIGINAL, CORRECTED METHOD
    public Uri exportSimpleDevoteeList(Context context, List<Devotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = {"Full Name", "Mobile Number"};
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String fileName = "event_attendance_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            for (Devotee devotee : devotees) {
                String[] data = {
                        devotee.getFullName(),
                        devotee.getMobileE164()
                };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }
}