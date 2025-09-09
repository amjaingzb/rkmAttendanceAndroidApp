// In: src/main/java/com/rkm/rkmattendanceapp/AttendanceApplication.java
package com.rkm.rkmattendanceapp;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DatabaseHelper;

public class AttendanceApplication extends Application {

    // This will be our single, app-wide instance of the repository.
    public AttendanceRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Create an instance of our Android-specific database helper.
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        
        // 2. Get a writable database connection. This will trigger the
        //    onCreate method in DatabaseHelper the first time the app runs.
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // 3. Create the single repository instance for the entire app,
        //    passing it the database connection.
        repository = new AttendanceRepository(database);
    }
}