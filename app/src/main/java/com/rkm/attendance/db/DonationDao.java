// In: app/src/main/java/com/rkm/attendance/db/DonationDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class DonationDao {
    private final SQLiteDatabase db;

    public DonationDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(long devoteeId, Double eventId, double amount, String paymentMethod, String referenceId, String purpose, String createdByUser) {
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
}
