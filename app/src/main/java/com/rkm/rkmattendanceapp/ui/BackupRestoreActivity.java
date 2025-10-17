// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/BackupRestoreActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.rkm.attendance.core.DatabaseZipper;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;
import com.rkm.rkmattendanceapp.util.BackupStateManager; // ANNOTATION: New import


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupRestoreActivity extends AppCompatActivity {

    private static final String TAG = "BackupRestoreActivity";
    private static final String PREFS_NAME = "BackupPrefs";
    private static final String KEY_LAST_BACKUP_TIMESTAMP = "lastBackupTimestamp";

    private ActivityResultLauncher<String[]> restoreDbLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
        setTitle("Backup & Restore");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button exportButton = findViewById(R.id.button_export_db);
        Button restoreButton = findViewById(R.id.button_restore_db);

        setupLaunchers();

        exportButton.setOnClickListener(v -> performExportAndShare());

        restoreButton.setOnClickListener(v -> {
            // === START OF FILE PICKER FIX ===
            // Use the generic "*/*" MIME type to allow the user to select ANY file.
            // Our logic will then validate the extension.
            restoreDbLauncher.launch(new String[]{"*/*"});
            // === END OF FILE PICKER FIX ===
        });
    }

    private void performExportAndShare() {
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String fileName = "SevaConnect_Backup_" + timestamp + ".zip";
            File cachePath = new File(getCacheDir(), "exports");
            cachePath.mkdirs();
            File file = new File(cachePath, fileName);

            try (OutputStream os = new FileOutputStream(file)) {
                DatabaseZipper.zipDatabase(getApplicationContext(), os);
            }

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Backup Via"));

            saveLastBackupTimestamp(getApplicationContext());
            BackupStateManager.clearDbDirtyFlag(getApplicationContext());
            setResult(Activity.RESULT_OK);

        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to export and share database", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void setupLaunchers() {
        restoreDbLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Restore")
                        .setMessage("Restoring from this file will overwrite ALL current data in the app. This action cannot be undone. Are you sure you want to continue?")
                        .setPositiveButton("Restore", (dialog, which) -> {
                            performRestore(uri);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void performRestore(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            String fileName = getFileName(uri);
            if (fileName != null && (fileName.toLowerCase().endsWith(".db") || fileName.toLowerCase().endsWith(".zip"))) {
                
                ((AttendanceApplication) getApplication()).repository = null;

                if (fileName.toLowerCase().endsWith(".db")) {
                    AppLogger.d(TAG, "Performing restore from a single .db file.");
                    DatabaseZipper.restoreSingleDbFile(getApplicationContext(), is);
                } else {
                    AppLogger.d(TAG, "Performing restore from a .zip file.");
                    DatabaseZipper.unzipDatabase(getApplicationContext(), is);
                }

                // ANNOTATION: Clear the dirty flag after successful restore
                BackupStateManager.clearDbDirtyFlag(getApplicationContext());
                saveLastBackupTimestamp(getApplicationContext());
                Toast.makeText(this, "Restore successful! App will now restart.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, LauncherActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Runtime.getRuntime().exit(0);

            } else {
                Toast.makeText(this, "Invalid file type. Please select a .zip or .db file.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to restore database", e);
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static void saveLastBackupTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, Instant.now().getEpochSecond()).apply();
    }

    public static long getLastBackupTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
