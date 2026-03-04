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
    public DonationDao(SQLiteDatabase db) { this.db = db; }

    public static class BatchSummary {
        public final double totalCash, totalUpi, totalCheque;
        public final int donationCount;
        public BatchSummary(double totalCash, double totalUpi, double totalCheque, int donationCount) {
            this.totalCash = totalCash; this.totalUpi = totalUpi; this.totalCheque = totalCheque; this.donationCount = donationCount;
        }
    }

    public DonationReportModels.FullDonationRecord getFullRecordById(long donationId) {
        String sql = "SELECT d.*, dv.*, " +
                "(SELECT COUNT(*) FROM donation_batches b2 JOIN donation_batches b1 ON date(b1.start_ts) = date(b2.start_ts) WHERE b1.batch_id = d.batch_id AND b2.batch_id <= b1.batch_id) as batch_seq " +
                "FROM donations d JOIN devotee dv ON d.devotee_id = dv.devotee_id WHERE d.donation_id = ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(donationId)})) {
            if (cursor.moveToFirst()) {
                return new DonationReportModels.FullDonationRecord(fromCursor(cursor), new DevoteeDao(db).fromCursor(cursor));
            }
        }
        return null;
    }

    public List<DonationRecord> getDonationsForBatch(long batchId) {
        List<DonationRecord> records = new ArrayList<>();
        String sql = "SELECT dn.*, dv.full_name, dv.mobile_e164, dv.address, dv.email, dv.pan, dv.aadhaar, " +
                "(SELECT COUNT(*) FROM donation_batches b2 JOIN donation_batches b1 ON date(b1.start_ts) = date(b2.start_ts) WHERE b1.batch_id = dn.batch_id AND b2.batch_id <= b1.batch_id) as batch_seq " +
                "FROM donations dn JOIN devotee dv ON dn.devotee_id = dv.devotee_id WHERE dn.batch_id = ? ORDER BY dn.donation_timestamp DESC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(batchId)})) {
            while (cursor.moveToNext()) {
                Donation d = fromCursor(cursor);
                records.add(new DonationRecord(d, cursor.getString(cursor.getColumnIndexOrThrow("full_name")), cursor.getString(cursor.getColumnIndexOrThrow("mobile_e164")), cursor.getString(cursor.getColumnIndexOrThrow("address")), cursor.getString(cursor.getColumnIndexOrThrow("email")), cursor.getString(cursor.getColumnIndexOrThrow("pan")), cursor.getString(cursor.getColumnIndexOrThrow("aadhaar"))));
            }
        }
        return records;
    }

    public Donation fromCursor(Cursor cursor) {
        int seqIdx = cursor.getColumnIndex("batch_seq");
        int sequence = (seqIdx != -1) ? cursor.getInt(seqIdx) : 0;
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
                cursor.getString(cursor.getColumnIndexOrThrow("receipt_number")),
                sequence
        );
    }

    public long insert(long devoteeId, Long eventId, double amount, String paymentMethod, String referenceId, String purpose, String createdByUser, long batchId, String receiptNumber) {
        ContentValues values = new ContentValues();
        values.put("devotee_id", devoteeId); if (eventId != null) values.put("event_id", eventId);
        values.put("amount", amount); values.put("payment_method", paymentMethod); values.put("reference_id", referenceId);
        values.put("purpose", purpose); values.put("created_by_user", createdByUser); values.put("batch_id", batchId); values.put("receipt_number", receiptNumber);
        return db.insertOrThrow("donations", null, values);
    }

    public int delete(long donationId) { return db.delete("donations", "donation_id = ?", new String[]{String.valueOf(donationId)}); }
    public BatchSummary getBatchSummary(long batchId) {
        String[] args = new String[]{String.valueOf(batchId)};
        double cash = simpleQueryForDouble("SELECT SUM(amount) FROM donations WHERE batch_id = ? AND payment_method = 'CASH'", args);
        double upi = simpleQueryForDouble("SELECT SUM(amount) FROM donations WHERE batch_id = ? AND payment_method = 'UPI'", args);
        double cheque = simpleQueryForDouble("SELECT SUM(amount) FROM donations WHERE batch_id = ? AND payment_method = 'CHEQUE'", args);
        int count = (int) simpleQueryForLong("SELECT COUNT(*) FROM donations WHERE batch_id = ?", args);
        return new BatchSummary(cash, upi, cheque, count);
    }
    private long simpleQueryForLong(String sql, String[] args) { try (SQLiteStatement s = db.compileStatement(sql)) { if (args != null) for (int i = 0; i < args.length; i++) s.bindString(i + 1, args[i]); return s.simpleQueryForLong(); } }
    private double simpleQueryForDouble(String sql, String[] args) { try (SQLiteStatement s = db.compileStatement(sql)) { if (args != null) for (int i = 0; i < args.length; i++) s.bindString(i + 1, args[i]); try { return s.simpleQueryForLong(); } catch (Exception e) { return 0.0; } } }
}
