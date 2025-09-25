// In: app/src/main/java/com/rkm/attendance/db/DonationDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rkm.attendance.model.Donation;
import com.rkm.rkmattendanceapp.ui.donations.DonationRecord;

import java.util.ArrayList;
import java.util.List;

public class DonationDao {
    private final SQLiteDatabase db;

    public DonationDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(long devoteeId, Long eventId, double amount, String paymentMethod, String referenceId, String purpose, String createdByUser) {
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
        return db.insertOrThrow("donations", null, values);
    }

    public int delete(long donationId) {
        return db.delete("donations", "donation_id = ?", new String[]{String.valueOf(donationId)});
    }

    public List<DonationRecord> getTodaysDonations() {
        List<DonationRecord> records = new ArrayList<>();
        // Query to get donations made "today" based on the device's local time.
        String sql = "SELECT dn.*, dv.full_name " +
                     "FROM donations dn " +
                     "JOIN devotee dv ON dn.devotee_id = dv.devotee_id " +
                     "WHERE date(dn.donation_timestamp) = date('now', 'localtime') " +
                     "ORDER BY dn.donation_timestamp DESC";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                records.add(recordFromCursor(cursor));
            }
        }
        return records;
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
                cursor.getString(cursor.getColumnIndexOrThrow("created_by_user"))
        );
    }

    private DonationRecord recordFromCursor(Cursor cursor) {
        Donation donation = fromCursor(cursor);
        String devoteeName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
        return new DonationRecord(donation, devoteeName);
    }
}
