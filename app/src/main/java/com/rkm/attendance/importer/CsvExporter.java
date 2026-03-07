package com.rkm.attendance.importer;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.opencsv.CSVWriter;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.ui.donations.DonationRecord;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels;
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

    // Standardized Header Order requested by customer
    private final String[] DONATION_HEADERS = {
            "Timestamp", "Devotee Name", "Mobile", "Address", "Email", "PAN", "Aadhaar",
            "Amount", "Payment Method", "Reference ID", "Purpose", "Receipt Number", "Batch ID"
    };

    public Uri exportFullDonationRecords(Context context, List<DonationReportModels.FullDonationRecord> records, String date, String fileProviderAuthority) throws Exception {
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        
        String fileName = String.format(Locale.US, "donations_for_%s_%s.csv", date, generateTimestamp());
        File file = new File(exportDir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(DONATION_HEADERS);
            for (DonationReportModels.FullDonationRecord record : records) {
                String[] data = {
                    record.donation.donationTimestamp,
                    record.devotee.getFullName(),
                    record.devotee.getMobileE164(),
                    record.devotee.getAddress(),
                    record.devotee.getEmail(),
                    record.devotee.getPan(),
                    record.devotee.getAadhaar(),
                    String.format(Locale.US, "%.2f", record.donation.amount),
                    record.donation.paymentMethod,
                    record.donation.referenceId,
                    record.donation.purpose,
                    record.donation.receiptNumber,
                    String.valueOf(record.donation.batchSequence)
                };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    public Uri exportDonationsForBatch(Context context, long batchId, int dailySequence, List<DonationRecord> donations, String fileProviderAuthority) throws Exception {
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        
        String fileName = String.format(Locale.US, "batch_%d_donations_%s.csv", dailySequence, generateTimestamp());
        File file = new File(exportDir, fileName);
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(DONATION_HEADERS);
            for (DonationRecord record : donations) {
                String[] data = {
                    record.donation.donationTimestamp,
                    record.devoteeName,
                    record.mobile,
                    record.address,
                    record.email,
                    record.pan,
                    record.aadhaar,
                    String.format(Locale.US, "%.2f", record.donation.amount),
                    record.donation.paymentMethod,
                    record.donation.referenceId,
                    record.donation.purpose,
                    record.donation.receiptNumber,
                    String.valueOf(dailySequence)
                };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    public Uri exportDevotees(Context context, List<DevoteeDao.EnrichedDevotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = { "Full Name", "Mobile Number", "Address", "Age", "Email", "Gender", "Aadhaar", "PAN", "WhatsApp Group", "Total Attendance", "Last Attended Date", "Extra JSON" };
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        String fileName = "devotee_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            for (DevoteeDao.EnrichedDevotee enriched : devotees) {
                String[] data = {
                    enriched.devotee().getFullName(), enriched.devotee().getMobileE164(),
                    enriched.devotee().getAddress(), enriched.devotee().getAge() != null ? String.valueOf(enriched.devotee().getAge()) : "",
                    enriched.devotee().getEmail(), enriched.devotee().getGender(),
                    enriched.devotee().getAadhaar(), enriched.devotee().getPan(),
                    enriched.whatsAppGroup() != null ? String.valueOf(enriched.whatsAppGroup()) : "N/A",
                    String.valueOf(enriched.cumulativeAttendance()), enriched.lastAttendanceDate(),
                    enriched.devotee().getExtraJson()
                };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    public Uri exportSimpleDevoteeList(Context context, List<Devotee> devotees, String fileProviderAuthority) throws Exception {
        String[] headers = {"Full Name", "Mobile Number"};
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        String fileName = "event_attendance_export_" + generateTimestamp() + ".csv";
        File file = new File(exportDir, fileName);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            for (Devotee devotee : devotees) {
                String[] data = { devotee.getFullName(), devotee.getMobileE164() };
                writer.writeNext(data);
            }
        }
        return FileProvider.getUriForFile(context, fileProviderAuthority, file);
    }

    // Add these methods to app/src/main/java/com/rkm/attendance/importer/CsvExporter.java

    public Uri exportCashSummary(Context context, List<DonationReportModels.FullDonationRecord> records, String date, String authority) throws Exception {
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        File file = new File(exportDir, "cash_summary_" + date + "_" + generateTimestamp() + ".csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(new String[]{"CASH SUMMARY - " + date});
            writer.writeNext(new String[]{"SNo", "Donor Name", "Amount"});

            double total = 0;
            int sno = 1;
            for (DonationReportModels.FullDonationRecord r : records) {
                if ("CASH".equalsIgnoreCase(r.donation.paymentMethod)) {
                    writer.writeNext(new String[]{String.valueOf(sno++), r.devotee.getFullName(), String.format(Locale.US, "%.2f", r.donation.amount)});
                    total += r.donation.amount;
                }
            }
            writer.writeNext(new String[]{"", "TOTAL", String.format(Locale.US, "%.2f", total)});
        }
        return FileProvider.getUriForFile(context, authority, file);
    }

    public Uri exportChequeSummary(Context context, List<DonationReportModels.FullDonationRecord> records, String date, String authority) throws Exception {
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        File file = new File(exportDir, "cheque_summary_" + date + "_" + generateTimestamp() + ".csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(new String[]{"CHEQUE SUMMARY - " + date});
            writer.writeNext(new String[]{"SNo", "Donor Name", "Amount", "Cheque No"});

            double total = 0;
            int sno = 1;
            for (DonationReportModels.FullDonationRecord r : records) {
                if ("CHEQUE".equalsIgnoreCase(r.donation.paymentMethod)) {
                    writer.writeNext(new String[]{String.valueOf(sno++), r.devotee.getFullName(), String.format(Locale.US, "%.2f", r.donation.amount), r.donation.referenceId});
                    total += r.donation.amount;
                }
            }
            writer.writeNext(new String[]{"", "TOTAL", String.format(Locale.US, "%.2f", total), ""});
        }
        return FileProvider.getUriForFile(context, authority, file);
    }
}
