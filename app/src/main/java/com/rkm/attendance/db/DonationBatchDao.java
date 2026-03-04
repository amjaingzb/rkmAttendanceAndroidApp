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

    public long insertNewBatch(String localTimestamp) {
        ContentValues values = new ContentValues();
        values.put("status", "ACTIVE");
        values.put("start_ts", localTimestamp);
        return db.insertOrThrow("donation_batches", null, values);
    }

    public DonationBatch findActiveBatch() {
        // Subquery calculates the count of batches on the same date with ID <= current ID
        String sql = "SELECT b.*, " +
                "(SELECT COUNT(*) FROM donation_batches b2 " +
                " WHERE date(b2.start_ts) = date(b.start_ts) AND b2.batch_id <= b.batch_id) as daily_seq " +
                "FROM donation_batches b " +
                "WHERE b.status = 'ACTIVE' " +
                "ORDER BY b.start_ts DESC LIMIT 1";
        
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    public void closeBatch(long batchId, String user, String localTimestamp) {
        ContentValues values = new ContentValues();
        values.put("status", "DEPOSITED");
        values.put("deposited_by", user);
        values.put("end_ts", localTimestamp);
        db.update("donation_batches", values, "batch_id = ?", new String[]{String.valueOf(batchId)});
    }

    public void deleteBatch(long batchId) { db.delete("donation_batches", "batch_id = ?", new String[]{String.valueOf(batchId)}); }

    public DonationBatch fromCursor(Cursor cursor) {
        int seqIdx = cursor.getColumnIndex("daily_seq");
        int sequence = (seqIdx != -1) ? cursor.getInt(seqIdx) : 0;

        return new DonationBatch(
                cursor.getLong(cursor.getColumnIndexOrThrow("batch_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("start_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("end_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("deposited_by")),
                sequence
        );
    }
}
