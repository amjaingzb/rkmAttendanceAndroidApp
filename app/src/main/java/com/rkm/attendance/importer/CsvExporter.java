// In: src/main/java/com/rkm/attendance/importer/CsvExporter.java
package com.rkm.attendance.importer;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.opencsv.CSVWriter;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExporter {

    private static final String TAG = "CsvExporter";

    // === START OF FILENAME CHANGE ===
    // Create a formatter that's safe for filenames (e.g., "20250923_184530")
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private String generateTimestamp() {
        return FILENAME_FORMATTER.format(LocalDateTime.now());
    }
    // === END OF FILENAME CHANGE ===

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
        
        // === START OF FILENAME CHANGE ===
        // Construct a unique filename using the timestamp.
        String fileName = "devotee_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);
        // === END OF FILENAME CHANGE ===

        AppLogger.d(TAG, "Creating export file at: " + file.getAbsolutePath());

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
            AppLogger.d(TAG, "Successfully wrote " + devotees.size() + " records to CSV.");
        }

        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    public Uri exportSimpleDevoteeList(Context context, List<Devotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = {"Full Name", "Mobile Number"};

        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // === START OF FILENAME CHANGE ===
        // Construct a unique filename using the timestamp.
        String fileName = "event_attendance_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);
        // === END OF FILENAME CHANGE ===

        AppLogger.d(TAG, "Creating event attendance export at: " + file.getAbsolutePath());

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            for (Devotee devotee : devotees) {
                String[] data = {
                    devotee.getFullName(),
                    devotee.getMobileE164()
                };
                writer.writeNext(data);
            }
            AppLogger.d(TAG, "Successfully wrote " + devotees.size() + " attendees to CSV.");
        }

        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }
}
