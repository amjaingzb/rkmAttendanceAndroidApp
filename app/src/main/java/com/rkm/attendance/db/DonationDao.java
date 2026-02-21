package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.rkm.attendance.model.Donation;
import com.rkm.rkmattendanceapp.ui.donations.DonationRecord;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels;
import java.util.ArrayList;
import java.util.List;

public class DonationDao {
    private final SQLiteDatabase db;

    public DonationDao(SQLiteDatabase db) {
        this.db = db;
    }

    public static class BatchSummary {
        public final double totalCash;
        public final double totalUpi;
        public final int donationCount;

        public BatchSummary(double totalCash, double totalUpi, int donationCount) {
            this.totalCash = totalCash;
            this.totalUpi = totalUpi;
            this.donationCount = donationCount;
        }
    }

    // --- RECEIPT GENERATION METHOD (Ensure this appears only once) ---
    public DonationReportModels.FullDonationRecord getFullRecordById(long donationId) {
        String sql = "SELECT d.*, dv.* FROM donations d " +
                "JOIN devotee dv ON d.devotee_id = dv.devotee_id " +
                "WHERE d.donation_id = ?";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(donationId)})) {
            if (cursor.moveToFirst()) {
                DevoteeDao devoteeDao = new DevoteeDao(db);
                Donation donation = fromCursor(cursor);
                com.rkm.attendance.model.Devotee devotee = devoteeDao.fromCursor(cursor);
                return new DonationReportModels.FullDonationRecord(donation, devotee);
            }
        }
        return null;
    }
    // ---------------------------------------------------------------

    public long insert(long devoteeId, Long eventId, double amount, String paymentMethod, String referenceId, String purpose, String createdByUser, long batchId, String receiptNumber) {
        ContentValues values = new ContentValues();
        values.put("devotee_id", devoteeId);
        if (eventId != null) {
            values.put("event_id", eventId);
        }
        values.put("amount", amount);
        values.put("payment_method", paymentMethod);
        values.put("reference_id", referenceId);
        values.put("purpose", purpose);
        values.put("created_by_user", createdByUser);
        values.put("batch_id", batchId);
        values.put("receipt_number", receiptNumber);
        return db.insertOrThrow("donations", null, values);
    }

    public int delete(long donationId) {
        return db.delete("donations", "donation_id = ?", new String[]{String.valueOf(donationId)});
    }

    public List<DonationRecord> getDonationsForBatch(long batchId) {
        List<DonationRecord> records = new ArrayList<>();
        String sql = "SELECT dn.*, dv.full_name " +
                "FROM donations dn " +
                "JOIN devotee dv ON dn.devotee_id = dv.devotee_id " +
                "WHERE dn.batch_id = ? " +
                "ORDER BY dn.donation_timestamp DESC";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(batchId)})) {
            while (cursor.moveToNext()) {
                records.add(recordFromCursor(cursor));
            }
        }
        return records;
    }

    public BatchSummary getBatchSummary(long batchId) {
        String[] args = new String[]{String.valueOf(batchId)};
        double totalCash = simpleQueryForDouble("SELECT SUM(amount) FROM donations WHERE batch_id = ? AND payment_method = 'CASH'", args);
        double totalUpi = simpleQueryForDouble("SELECT SUM(amount) FROM donations WHERE batch_id = ? AND payment_method = 'UPI'", args);
        int count = (int) simpleQueryForLong("SELECT COUNT(*) FROM donations WHERE batch_id = ?", args);
        return new BatchSummary(totalCash, totalUpi, count);
    }

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

    private DonationRecord recordFromCursor(Cursor cursor) {
        Donation donation = fromCursor(cursor);
        String devoteeName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
        return new DonationRecord(donation, devoteeName);
    }

    private long simpleQueryForLong(String sql, String[] args) {
        try (SQLiteStatement statement = db.compileStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    statement.bindString(i + 1, args[i]);
                }
            }
            return statement.simpleQueryForLong();
        }
    }

    private double simpleQueryForDouble(String sql, String[] args) {
        try (SQLiteStatement statement = db.compileStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    statement.bindString(i + 1, args[i]);
                }
            }
            try {
                return statement.simpleQueryForLong();
            } catch (android.database.sqlite.SQLiteDoneException e) {
                return 0.0;
            }
        }
    }
}