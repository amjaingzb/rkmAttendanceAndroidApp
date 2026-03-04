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
    public DonationReportDao(SQLiteDatabase db) { this.db = db; }

    public List<DailySummary> getDailySummaries() {
        List<DailySummary> summaries = new ArrayList<>();
        String sql = "SELECT date(donation_timestamp) as donation_date, SUM(amount) as total_amount, COUNT(donation_id) as donation_count, COUNT(DISTINCT batch_id) as batch_count FROM donations GROUP BY donation_date ORDER BY donation_date DESC";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                summaries.add(new DailySummary(cursor.getString(0), cursor.getDouble(1), cursor.getInt(2), cursor.getInt(3)));
            }
        }
        return summaries;
    }
    
    public List<FullDonationRecord> getFullDonationRecordsForDate(String date) {
        List<FullDonationRecord> records = new ArrayList<>();
        String sql = "SELECT d.*, dv.*, " +
                     "(SELECT COUNT(*) FROM donation_batches b2 JOIN donation_batches b1 ON date(b1.start_ts) = date(b2.start_ts) WHERE b1.batch_id = d.batch_id AND b2.batch_id <= b1.batch_id) as batch_seq " +
                     "FROM donations d JOIN devotee dv ON d.devotee_id = dv.devotee_id " +
                     "WHERE date(d.donation_timestamp) = ? ORDER BY d.donation_timestamp ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{date})) {
            DonationDao dDao = new DonationDao(db);
            DevoteeDao dvDao = new DevoteeDao(db);
            while(cursor.moveToNext()) {
                records.add(new FullDonationRecord(dDao.fromCursor(cursor), dvDao.fromCursor(cursor)));
            }
        }
        return records;
    }
}
