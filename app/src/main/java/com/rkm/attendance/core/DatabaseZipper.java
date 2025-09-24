// In: app/src/main/java/com/rkm/attendance/core/DatabaseZipper.java
package com.rkm.attendance.core;

import android.content.Context;

import com.rkm.rkmattendanceapp.util.AppLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DatabaseZipper {

    private static final String TAG = "DatabaseZipper";

    public static void zipDatabase(Context context, OutputStream destinationStream) throws Exception {
        File dbFile = context.getDatabasePath("devotees.db");
        File dbWalFile = new File(dbFile.getParent(), "devotees.db-wal");
        File dbShmFile = new File(dbFile.getParent(), "devotees.db-shm");
        List<File> filesToZip = Arrays.asList(dbFile, dbWalFile, dbShmFile);

        try (ZipOutputStream zos = new ZipOutputStream(destinationStream)) {
            for (File file : filesToZip) {
                if (file.exists()) {
                    AppLogger.d(TAG, "Zipping file: " + file.getName());
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                } else {
                    AppLogger.w(TAG, "File not found for zipping: " + file.getName());
                }
            }
        }
    }

    public static void unzipDatabase(Context context, InputStream sourceStream) throws Exception {
        File dbPath = context.getDatabasePath("devotees.db").getParentFile();
        deleteOldDatabaseFiles(context);

        try (ZipInputStream zis = new ZipInputStream(sourceStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(dbPath, zipEntry.getName());
                AppLogger.d(TAG, "Unzipping to: " + newFile.getAbsolutePath());
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }
    
    // NEW: Method to restore from a single .db file
    public static void restoreSingleDbFile(Context context, InputStream sourceStream) throws Exception {
        deleteOldDatabaseFiles(context);
        File dbFile = context.getDatabasePath("devotees.db");
        AppLogger.d(TAG, "Copying single .db file to: " + dbFile.getAbsolutePath());
        try (OutputStream fos = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = sourceStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    // NEW: Centralized helper to delete old files
    private static void deleteOldDatabaseFiles(Context context) {
        AppLogger.w(TAG, "Deleting old database files before restore...");
        File dbPath = context.getDatabasePath("devotees.db").getParentFile();
        File oldDb = context.getDatabasePath("devotees.db");
        if (oldDb.exists()) oldDb.delete();
        File oldWal = new File(dbPath, "devotees.db-wal");
        if (oldWal.exists()) oldWal.delete();
        File oldShm = new File(dbPath, "devotees.db-shm");
        if (oldShm.exists()) oldShm.delete();
        AppLogger.d(TAG, "Old database files deleted.");
    }
}
