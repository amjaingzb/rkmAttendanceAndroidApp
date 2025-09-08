// In: src/main/java/com/rkm/attendance/db/WhatsAppGroupDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Manages database operations for the whatsapp_group_map table using Android's SQLite API.
 */
public class WhatsAppGroupDao {
    private final SQLiteDatabase db;

    public WhatsAppGroupDao(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Inserts or replaces a phone-to-group mapping using SQLite's REPLACE strategy.
     */
    public void upsert(String phone10, int groupNumber) {
        ContentValues values = new ContentValues();
        values.put("phone_number_10", phone10);
        values.put("group_number", groupNumber);
        
        // Use SQLiteDatabase.replace, which is equivalent to INSERT OR REPLACE
        db.replace("whatsapp_group_map", null, values);
    }
}