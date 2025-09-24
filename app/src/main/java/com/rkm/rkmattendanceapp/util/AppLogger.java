// In: app/src/main/java/com/rkm/rkmattendanceapp/util/AppLogger.java
package com.rkm.rkmattendanceapp.util;

import android.util.Log;
import com.rkm.rkmattendanceapp.BuildConfig;

public class AppLogger {

    private static final String APP_TAG = "RKM_APP";

    private static String formatTag(String componentTag) {
        return APP_TAG + ":" + componentTag;
    }

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(formatTag(tag), message);
        }
    }

    public static void w(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.w(formatTag(tag), message);
        }
    }

    public static void w(String tag, String message, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.w(formatTag(tag), message, tr);
        }
    }

    public static void e(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.e(formatTag(tag), message);
        }
    }

    public static void e(String tag, String message, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.e(formatTag(tag), message, tr);
        }
    }
}
