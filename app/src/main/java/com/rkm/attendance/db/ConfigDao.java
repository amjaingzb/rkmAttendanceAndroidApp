// In: app/src/main/java/com/rkm/attendance/db/ConfigDao.java
package com.rkm.attendance.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ConfigDao {
    private final SQLiteDatabase db;

    // --- NEW: Define constants for the PIN keys ---
    public static final String KEY_SUPER_ADMIN_PIN = "SUPER_ADMIN_PIN";
    public static final String KEY_EVENT_COORDINATOR_PIN = "EVENT_COORDINATOR_PIN";

    public ConfigDao(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Retrieves a configuration value from the database.
     * @param key The key of the config value to retrieve.
     * @return The string value, or null if the key doesn't exist.
     */
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

    /**
     * Checks if the provided PIN matches the one stored for the Super Admin.
     * @param pin The PIN to check.
     * @return true if the PIN is correct, false otherwise.
     */
    public boolean checkSuperAdminPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            return false;
        }
        String storedPin = getValue(KEY_SUPER_ADMIN_PIN);
        return pin.equals(storedPin);
    }

    /**
     * Checks if the provided PIN matches the one stored for the Event Coordinator.
     * @param pin The PIN to check.
     * @return true if the PIN is correct, false otherwise.
     */
    public boolean checkEventCoordinatorPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            return false;
        }
        String storedPin = getValue(KEY_EVENT_COORDINATOR_PIN);
        return pin.equals(storedPin);
    }
}