// In: src/main/java/com/rkm/attendance/db/DatabaseHelper.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "devotees.db";
    private static final int DATABASE_VERSION = 4; // UPDATED: Database version incremented
    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        AppLogger.d(TAG, "onCreate: Creating new database schema for version " + DATABASE_VERSION);
        // This method should contain the complete schema for the LATEST version.
        db.execSQL("CREATE TABLE IF NOT EXISTS devotee (\n" +
                "  devotee_id   INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  full_name    TEXT NOT NULL,\n" +
                "  name_norm    TEXT NOT NULL,\n" +
                "  mobile_e164  TEXT NOT NULL,\n" +
                "  address      TEXT,\n" +
                "  age          INTEGER,\n" +
                "  email        TEXT,\n" +
                "  gender       TEXT,\n" +
                "  aadhaar      TEXT,\n" +
                "  pan          TEXT,\n" +
                "  extra_json   TEXT,\n" +
                "  created_at   TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                "  updated_at   TEXT DEFAULT CURRENT_TIMESTAMP\n" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ux_devotee_phone_name ON devotee (mobile_e164, name_norm)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_mobile ON devotee (mobile_e164)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_name ON devotee (name_norm)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_devotee_email ON devotee (email)");
        db.execSQL("CREATE TABLE IF NOT EXISTS fuzzy_merge_log ( log_id INTEGER PRIMARY KEY AUTOINCREMENT, mobile TEXT NOT NULL, old_name TEXT, new_name TEXT, similarity REAL, created_at TEXT DEFAULT CURRENT_TIMESTAMP )");
        db.execSQL("CREATE TABLE IF NOT EXISTS event (\n" +
                "  event_id    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  event_code  TEXT UNIQUE,\n" +
                "  event_name  TEXT NOT NULL,\n" +
                "  event_date  TEXT,\n" +
                "  active_from_ts  TEXT,\n" +
                "  active_until_ts TEXT,\n" +
                "  remark      TEXT\n" +
                ")");
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
        db.execSQL("CREATE TABLE IF NOT EXISTS whatsapp_group_map (\n" +
                "  phone_number_10 TEXT PRIMARY KEY NOT NULL,\n" +
                "  group_number    INTEGER NOT NULL\n" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ux_attendance_ev_dev ON attendance(event_id, devotee_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_attendance_event ON attendance(event_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_attendance_devotee ON attendance(devotee_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS ix_whatsapp_group_num ON whatsapp_group_map(group_number)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_config (\n" +
                "  config_key   TEXT PRIMARY KEY NOT NULL,\n" +
                "  config_value TEXT\n" +
                ")");
                
        // NEW in v4
        db.execSQL("CREATE TABLE IF NOT EXISTS donation_batches (\n" +
                "    batch_id        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    start_ts        TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    end_ts          TEXT,\n" +
                "    status          TEXT NOT NULL, -- 'ACTIVE' or 'DEPOSITED'\n" +
                "    deposited_by    TEXT\n" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS donations (\n" +
                "    donation_id         INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    devotee_id          INTEGER NOT NULL,\n" +
                "    event_id            INTEGER,\n" +
                "    amount              REAL NOT NULL,\n" +
                "    payment_method      TEXT NOT NULL,\n" +
                "    reference_id        TEXT,\n" +
                "    purpose             TEXT NOT NULL,\n" +
                "    donation_timestamp  TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                "    created_by_user     TEXT,\n" +
                "    is_receipt_sent     INTEGER DEFAULT 0,\n" +
                "    batch_id            INTEGER NOT NULL,\n" + // NEW in v4
                "    receipt_number      TEXT UNIQUE,\n" + // NEW in v4
                "    FOREIGN KEY (devotee_id) REFERENCES devotee(devotee_id) ON DELETE RESTRICT,\n" +
                "    FOREIGN KEY (event_id)   REFERENCES event(event_id)   ON DELETE SET NULL,\n" +
                "    FOREIGN KEY (batch_id)   REFERENCES donation_batches(batch_id) ON DELETE RESTRICT\n" +
                ")");

        // Insert all default values for a fresh install
        insertDefaultPin(db, ConfigDao.KEY_SUPER_ADMIN_PIN, "2222");
        insertDefaultPin(db, ConfigDao.KEY_EVENT_COORDINATOR_PIN, "1111");
        insertDefaultPin(db, ConfigDao.KEY_DONATION_COLLECTOR_PIN, "3333");
        insertDefaultPin(db, ConfigDao.KEY_WHATSAPP_INVITE_LINK, "https://chat.whatsapp.com/YOUR_INVITE_CODE_HERE");
        insertDefaultPin(db, ConfigDao.KEY_WHATSAPP_INVITE_MESSAGE, "Hello! You are invited to join our RKM group for updates. Please join using this link: ");

    }

    private void insertDefaultPin(SQLiteDatabase db, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("config_key", key);
        values.put("config_value", value);
        db.insertWithOnConflict("app_config", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        AppLogger.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 2) {
            AppLogger.d(TAG, "Applying schema changes for version 2...");
            db.execSQL("ALTER TABLE devotee ADD COLUMN aadhaar TEXT");
            db.execSQL("ALTER TABLE devotee ADD COLUMN pan TEXT");
            AppLogger.d(TAG, "Successfully added aadhaar and pan columns to devotee table.");
        }
        if (oldVersion < 3) {
            AppLogger.d(TAG, "Applying schema changes for version 3...");
            db.execSQL("CREATE TABLE IF NOT EXISTS donations (\n" +
                    "    donation_id         INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    devotee_id          INTEGER NOT NULL,\n" +
                    "    event_id            INTEGER,\n" +
                    "    amount              REAL NOT NULL,\n" +
                    "    payment_method      TEXT NOT NULL,\n" +
                    "    reference_id        TEXT,\n" +
                    "    purpose             TEXT NOT NULL,\n" +
                    "    donation_timestamp  TEXT DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    created_by_user     TEXT,\n" +
                    "    is_receipt_sent     INTEGER DEFAULT 0,\n" +
                    "    FOREIGN KEY (devotee_id) REFERENCES devotee(devotee_id) ON DELETE RESTRICT,\n" +
                    "    FOREIGN KEY (event_id)   REFERENCES event(event_id)   ON DELETE SET NULL\n" +
                    ")");
            insertDefaultPin(db, ConfigDao.KEY_DONATION_COLLECTOR_PIN, "3333");
            AppLogger.d(TAG, "Successfully added donations table and default PIN.");
        }
        if (oldVersion < 4) {
             AppLogger.d(TAG, "Applying schema changes for version 4...");
             db.execSQL("CREATE TABLE IF NOT EXISTS donation_batches (\n" +
                "    batch_id        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    start_ts        TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    end_ts          TEXT,\n" +
                "    status          TEXT NOT NULL, -- 'ACTIVE' or 'DEPOSITED'\n" +
                "    deposited_by    TEXT\n" +
                ")");
             db.execSQL("ALTER TABLE donations ADD COLUMN batch_id INTEGER NOT NULL DEFAULT 0");
             db.execSQL("ALTER TABLE donations ADD COLUMN receipt_number TEXT UNIQUE");
             AppLogger.d(TAG, "Successfully added donation_batches table and updated donations table.");
        }
    }
    
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
