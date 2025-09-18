// In: app/src/main/java/com/rkm/rkmattendanceapp/util/AppLogger.java
package com.rkm.rkmattendanceapp.util;

import android.util.Log;

/**
 * A centralized logging utility for the application.
 * Provides simple methods to log messages and exceptions, which can be
 * globally controlled by the DEBUG flag and filtered by a global APP_TAG.
 */
public class AppLogger {

    /**
     * A single, global tag for all application logs.
     * This allows for easy filtering in Logcat (e.g., "Logcat -s RKM_APP:*").
     */
    private static final String APP_TAG = "RKM_APP";

    /**
     * Master switch for all debug-level logs. Set to 'false' for production releases
     * to disable all verbose logging automatically. Warnings and Errors will still be logged.
     */
    public static final boolean DEBUG = true;

    /**
     * Formats the final log tag. Example: "RKM_APP:MyActivity"
     * @param componentTag The tag for the specific component (e.g., class name).
     * @return A formatted tag string.
     */
    private static String formatTag(String componentTag) {
        return APP_TAG + ":" + componentTag;
    }

    /**
     * Logs a debug message. This will only be logged if AppLogger.DEBUG is true.
     * @param tag The tag, typically the class name.
     * @param message The message to log.
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(formatTag(tag), message);
        }
    }

    /**
     * Logs a warning message. This will always be logged, regardless of the DEBUG flag.
     * @param tag The tag, typically the class name.
     * @param message The message to log.
     */
    public static void w(String tag, String message) {
        Log.w(formatTag(tag), message);
    }

    /**
     * Logs a warning message with an exception. This will always be logged.
     * @param tag The tag, typically the class name.
     * @param message The message to log.
     * @param tr The exception to log.
     */
    public static void w(String tag, String message, Throwable tr) {
        Log.w(formatTag(tag), message, tr);
    }

    /**
     * Logs an error message. This will always be logged.
     * @param tag The tag, typically the class name.
     * @param message The message to log.
     */
    public static void e(String tag, String message) {
        Log.e(formatTag(tag), message);
    }

    /**
     * Logs an error message with an exception. This will always be logged.
     * @param tag The tag, typically the class name.
     * @param message The message to log.
     * @param tr The exception to log.
     */
    public static void e(String tag, String message, Throwable tr) {
        Log.e(formatTag(tag), message, tr);
    }
}
