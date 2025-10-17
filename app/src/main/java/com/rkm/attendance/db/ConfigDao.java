// In: app/src/main/java/com/rkm/attendance/db/ConfigDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rkm.attendance.model.ConfigItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigDao {
    private final SQLiteDatabase db;

    public static final String KEY_SUPER_ADMIN_PIN = "SUPER_ADMIN_PIN";
    public static final String KEY_EVENT_COORDINATOR_PIN = "EVENT_COORDINATOR_PIN";
    public static final String KEY_DONATION_COLLECTOR_PIN = "DONATION_COLLECTOR_PIN";
    public static final String KEY_WHATSAPP_INVITE_LINK = "WHATSAPP_INVITE_LINK";
    public static final String KEY_WHATSAPP_INVITE_MESSAGE = "WHATSAPP_INVITE_MESSAGE";
    public static final String KEY_OFFICE_EMAIL = "OFFICE_EMAIL";

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

    // ANNOTATION: New method to get all settings for the UI
    public List<ConfigItem> getAllEditableConfigs() {
        List<ConfigItem> items = new ArrayList<>();
        // Define which keys are editable and what their display names are
        List<String> editableKeys = Arrays.asList(
            KEY_SUPER_ADMIN_PIN, KEY_EVENT_COORDINATOR_PIN, KEY_DONATION_COLLECTOR_PIN,
            KEY_OFFICE_EMAIL, KEY_WHATSAPP_INVITE_LINK, KEY_WHATSAPP_INVITE_MESSAGE
        );
        String inClause = "('" + String.join("','", editableKeys) + "')";
        String sql = "SELECT * FROM app_config WHERE config_key IN " + inClause;

        try(Cursor cursor = db.rawQuery(sql, null)) {
            while(cursor.moveToNext()) {
                String key = cursor.getString(cursor.getColumnIndexOrThrow("config_key"));
                String value = cursor.getString(cursor.getColumnIndexOrThrow("config_value"));
                items.add(new ConfigItem(key, getDisplayNameForKey(key), value, isProtectedKey(key)));
            }
        }
        return items;
    }

    // ANNOTATION: New method to update a setting value
    public void updateValue(String key, String value) {
        ContentValues values = new ContentValues();
        values.put("config_value", value);
        db.update("app_config", values, "config_key = ?", new String[]{key});
    }

    private String getDisplayNameForKey(String key) {
        switch (key) {
            case KEY_SUPER_ADMIN_PIN: return "Super Admin PIN";
            case KEY_EVENT_COORDINATOR_PIN: return "Event Coordinator PIN";
            case KEY_DONATION_COLLECTOR_PIN: return "Donation Collector PIN";
            case KEY_OFFICE_EMAIL: return "Office Email Address";
            case KEY_WHATSAPP_INVITE_LINK: return "WhatsApp Invite Link";
            case KEY_WHATSAPP_INVITE_MESSAGE: return "WhatsApp Invite Message";
            default: return key;
        }
    }

    private boolean isProtectedKey(String key) {
        return key.endsWith("_PIN");
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
