// In: app/src/main/java/com/rkm/attendance/db/DonationReportDao.java
package com.rkm.attendance.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Donation;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.DailySummary;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.FullDonationRecord;
import java.util.ArrayList;
import java.util.List;

public class DonationReportDao {
    private final SQLiteDatabase db;

    public DonationReportDao(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Executes a complex query to group all donations by date and calculate daily totals.
     */
    public List<DailySummary> getDailySummaries() {
        List<DailySummary> summaries = new ArrayList<>();
        String sql = "SELECT " +
                "    date(donation_timestamp) as donation_date, " +
                "    SUM(amount) as total_amount, " +
                "    COUNT(donation_id) as donation_count, " +
                "    COUNT(DISTINCT batch_id) as batch_count " +
                "FROM donations " +
                "GROUP BY donation_date " +
                "ORDER BY donation_date DESC";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                summaries.add(new DailySummary(
                        cursor.getString(cursor.getColumnIndexOrThrow("donation_date")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("donation_count")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("batch_count"))
                ));
            }
        }
        return summaries;
    }
    
    /**
     * Fetches all donation records for a specific date, joined with devotee info.
     */
    public List<FullDonationRecord> getFullDonationRecordsForDate(String date) {
        List<FullDonationRecord> records = new ArrayList<>();
        // This query joins donations and devotees for a given date.
        String sql = "SELECT d.*, dv.* FROM donations d " +
                     "JOIN devotee dv ON d.devotee_id = dv.devotee_id " +
                     "WHERE date(d.donation_timestamp) = ? " +
                     "ORDER BY d.donation_timestamp ASC";
        
        try (Cursor cursor = db.rawQuery(sql, new String[]{date})) {
            while(cursor.moveToNext()) {
                // We need to create Devotee and Donation objects from the same cursor
                DevoteeDao devoteeDao = new DevoteeDao(db); // Helper to use its fromCursor method
                Donation donation = fromCursor(cursor);
                Devotee devotee = devoteeDao.fromCursor(cursor);
                records.add(new FullDonationRecord(donation, devotee));
            }
        }
        return records;
    }

    // Helper to create a Donation object from a cursor that contains all its fields
    private Donation fromCursor(Cursor cursor) {
        return new Donation(
                cursor.getLong(cursor.getColumnIndexOrThrow("donation_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("devotee_id")),
                cursor.isNull(cursor.getColumnIndexOrThrow("event_id")) ? null : cursor.getLong(cursor.getColumnIndexOrThrow("event_id")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("payment_method")),
                cursor.getString(cursor.getColumnIndexOrThrow("reference_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("purpose")),
                cursor.getString(cursor.getColumnIndexOrThrow("donation_timestamp")),
                cursor.getString(cursor.getColumnIndexOrThrow("created_by_user")),
                cursor.getLong(cursor.getColumnIndexOrThrow("batch_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("receipt_number"))
        );
    }
}
