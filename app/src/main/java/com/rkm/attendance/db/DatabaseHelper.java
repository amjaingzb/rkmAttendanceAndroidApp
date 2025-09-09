// In: src/main/java/com/rkm/attendance/db/DatabaseHelper.java
package com.rkm.attendance.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "devotees.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // This is called only when the database is created for the first time.
        // We use "IF NOT EXISTS" to make this operation safe even if called unexpectedly.

        // 1. Devotee Table
        db.execSQL("CREATE TABLE IF NOT EXISTS devotee (\n" +
                "  devotee_id   INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  full_name    TEXT NOT NULL,\n" +
                "  name_norm    TEXT NOT NULL,\n" +
                "  mobile_e164  TEXT NOT NULL,\n" +
                "  address      TEXT,\n" +
                "  age          INTEGER,\n" +
                "  email        TEXT,\n" +
                "  gender       TEXT,\n" +
                "  extra_json   TEXT,\n" +
                "  created_at   TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                "  updated_at   TEXT DEFAULT CURRENT_TIMESTAMP\n" +
                ")");

        // 2. Devotee Indexes and other related tables
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ux_devotee_phone_name ON devotee (mobile_e164, name_norm)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_mobile ON devotee (mobile_e164)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_name ON devotee (name_norm)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_email ON devotee (email)");

        db.execSQL("CREATE TABLE IF NOT EXISTS fuzzy_merge_log ( " +
                "log_id     INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mobile     TEXT NOT NULL, " +
                "old_name   TEXT, " +
                "new_name   TEXT, " +
                "similarity REAL, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP " +
                ")");

        // 3. Events Table
        db.execSQL("CREATE TABLE IF NOT EXISTS event (\n" +
                "  event_id    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  event_code  TEXT UNIQUE,\n" +
                "  event_name  TEXT NOT NULL,\n" +
                "  event_date  TEXT,\n" +
                "  remark      TEXT\n" +
                ")");

        // 4. Attendance Table (Corrected Schema)
        db.execSQL("CREATE TABLE IF NOT EXISTS attendance (\n" +
                "  attendance_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  event_id      INTEGER NOT NULL,\n" +
                "  devotee_id    INTEGER,\n" +
                "  reg_type      TEXT NOT NULL, -- 'PRE_REG' or 'SPOT_REG'\n" +
                "  cnt           INTEGER NOT NULL DEFAULT 0,\n" +
                "  remark        TEXT,\n" +
                "  created_at    TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                "  updated_at    TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                "  FOREIGN KEY (event_id)   REFERENCES event(event_id)   ON DELETE CASCADE,\n" +
                "  FOREIGN KEY (devotee_id) REFERENCES devotee(devotee_id) ON DELETE RESTRICT\n" +
                ")");

        // 5. WhatsApp Group Map Table
        db.execSQL("CREATE TABLE IF NOT EXISTS whatsapp_group_map (\n" +
                "  phone_number_10 TEXT PRIMARY KEY NOT NULL,\n" +
                "  group_number    INTEGER NOT NULL\n" +
                ")");

        // 6. Attendance Indexes
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ux_attendance_ev_dev ON attendance(event_id, devotee_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_attendance_event ON attendance(event_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_attendance_devotee ON attendance(devotee_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_whatsapp_group_num ON whatsapp_group_map(group_number)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This is the key: Check if we are upgrading from a desktop DB.
        if (oldVersion == 0) {
            // This is a database file from the desktop. The schema should
            // already match what onCreate would do. We just need to let the
            // helper know the upgrade is "done" without wiping any data.
            // We do nothing here. The helper will automatically set the
            // version number to 1 in the file after this method returns.
            return;
        }

        // This is the original "destructive" upgrade path for any other
        // future upgrades during development (e.g., from v1 to v2).
        db.execSQL("DROP TABLE IF EXISTS attendance");
        db.execSQL("DROP TABLE IF EXISTS event");
        db.execSQL("DROP TABLE IF EXISTS devotee");
        db.execSQL("DROP TABLE IF EXISTS fuzzy_merge_log");
        db.execSQL("DROP TABLE IF EXISTS whatsapp_group_map");
        onCreate(db);
    }
    
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
    }
}