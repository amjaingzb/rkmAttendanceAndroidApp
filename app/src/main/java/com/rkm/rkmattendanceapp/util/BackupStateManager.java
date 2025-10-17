// In: app/src/main/java/com/rkm/rkmattendanceapp/util/BackupStateManager.java
package com.rkm.rkmattendanceapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class BackupStateManager {

    private static final String PREFS_NAME = "BackupStatePrefs";
    private static final String KEY_IS_DB_DIRTY = "isDbDirty";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Call this whenever a write operation is performed on the database.
     */
    public static void setDbDirty(Context context) {
        getPrefs(context).edit().putBoolean(KEY_IS_DB_DIRTY, true).apply();
        AppLogger.d("BackupStateManager", "Database is now marked as DIRTY.");
    }

    /**
     * Call this after a successful backup or restore.
     */
    public static void clearDbDirtyFlag(Context context) {
        getPrefs(context).edit().putBoolean(KEY_IS_DB_DIRTY, false).commit();
        AppLogger.d("BackupStateManager", "Database is now marked as CLEAN.");
    }

    /**
     * Check this to determine if the database has been modified since the last backup.
     */
    public static boolean isDbDirty(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_DB_DIRTY, false);
    }
}
