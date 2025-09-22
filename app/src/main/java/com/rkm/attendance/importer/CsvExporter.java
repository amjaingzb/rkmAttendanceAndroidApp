// In: src/main/java/com/rkm/attendance/importer/CsvExporter.java
package com.rkm.attendance.importer;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.opencsv.CSVWriter;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class CsvExporter {

    private static final String TAG = "CsvExporter";

    /**
     * Exports the full list of enriched devotees to a temporary CSV file and returns its shareable URI.
     * @param context The application context.
     * @param devotees The list of devotees to export.
     * @param fileProviderAuthority The authority string for the FileProvider.
     * @return A content URI for the generated file, suitable for sharing.
     * @throws Exception if the file cannot be created or written.
     */
    public Uri exportDevotees(Context context, List<DevoteeDao.EnrichedDevotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = {
                "Devotee ID", "Full Name", "Mobile Number", "Address", "Age",
                "Email", "Gender", "WhatsApp Group", "Total Attendance",
                "Last Attended Date", "Extra JSON"
        };

        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, "devotee_export.csv");

        AppLogger.d(TAG, "Creating export file at: " + file.getAbsolutePath());

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);

            for (DevoteeDao.EnrichedDevotee enriched : devotees) {
                String[] data = {
                    String.valueOf(enriched.devotee().getDevoteeId()),
                    enriched.devotee().getFullName(),
                    enriched.devotee().getMobileE164(),
                    enriched.devotee().getAddress(),
                    enriched.devotee().getAge() != null ? String.valueOf(enriched.devotee().getAge()) : "",
                    enriched.devotee().getEmail(),
                    enriched.devotee().getGender(),
                    enriched.whatsAppGroup() != null ? String.valueOf(enriched.whatsAppGroup()) : "N/A",
                    String.valueOf(enriched.cumulativeAttendance()),
                    enriched.lastAttendanceDate(),
                    enriched.devotee().getExtraJson()
                };
                writer.writeNext(data);
            }
            AppLogger.d(TAG, "Successfully wrote " + devotees.size() + " records to CSV.");
        }

        // Use the FileProvider to get a secure, shareable content URI
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }
}
