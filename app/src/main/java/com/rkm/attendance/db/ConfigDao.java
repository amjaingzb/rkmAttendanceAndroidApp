// In: app/src/main/java/com/rkm/attendance/db/ConfigDao.java
package com.rkm.attendance.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ConfigDao {
    private final SQLiteDatabase db;

    public static final String KEY_SUPER_ADMIN_PIN = "SUPER_ADMIN_PIN";
    public static final String KEY_EVENT_COORDINATOR_PIN = "EVENT_COORDINATOR_PIN";
    public static final String KEY_DONATION_COLLECTOR_PIN = "DONATION_COLLECTOR_PIN";
    public static final String KEY_WHATSAPP_INVITE_LINK = "WHATSAPP_INVITE_LINK";
    public static final String KEY_WHATSAPP_INVITE_MESSAGE = "WHATSAPP_INVITE_MESSAGE";
    public static final String KEY_OFFICE_EMAIL = "OFFICE_EMAIL"; // NEW

    public ConfigDao(SQLiteDatabase db) {
        this.db = db;
    }

    public String getValue(String key) {
        String[] columns = {"config_value"};
        String selection = "config_key = ?";
        String[] selectionArgs = {key};

        try (Cursor cursor = db.query("app_config", columns, selection, selectionArgs, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("config_value"));
            }
            return null;
        }
    }

    public boolean checkSuperAdminPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) return false;
        String storedPin = getValue(KEY_SUPER_ADMIN_PIN);
        return pin.equals(storedPin);
    }

    public boolean checkEventCoordinatorPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) return false;
        String storedPin = getValue(KEY_EVENT_COORDINATOR_PIN);
        return pin.equals(storedPin);
    }

    public boolean checkDonationCollectorPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) return false;
        String storedPin = getValue(KEY_DONATION_COLLECTOR_PIN);
        return pin.equals(storedPin);
    }
}
