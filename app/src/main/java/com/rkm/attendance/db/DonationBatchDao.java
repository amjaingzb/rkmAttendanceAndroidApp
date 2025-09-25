// In: app/src/main/java/com/rkm/attendance/db/DonationBatchDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.rkm.attendance.model.DonationBatch;

public class DonationBatchDao {
    private final SQLiteDatabase db;

    public DonationBatchDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertNewBatch() {
        ContentValues values = new ContentValues();
        values.put("status", "ACTIVE");
        return db.insertOrThrow("donation_batches", null, values);
    }

    public DonationBatch findActiveBatch() {
        String sql = "SELECT * FROM donation_batches WHERE status = 'ACTIVE' ORDER BY start_ts DESC LIMIT 1";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    public void closeBatch(long batchId, String user) {
        ContentValues values = new ContentValues();
        values.put("status", "DEPOSITED");
        values.put("deposited_by", user);
        values.put("end_ts", "strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')");
        db.update("donation_batches", values, "batch_id = ?", new String[]{String.valueOf(batchId)});
    }

    public DonationBatch fromCursor(Cursor cursor) {
        return new DonationBatch(
                cursor.getLong(cursor.getColumnIndexOrThrow("batch_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("start_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("end_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("deposited_by"))
        );
    }
}
