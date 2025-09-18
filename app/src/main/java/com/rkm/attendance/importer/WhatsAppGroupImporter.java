// In: src/main/java/com/rkm/attendance/importer/WhatsAppGroupImporter.java
package com.rkm.attendance.importer;

import android.database.sqlite.SQLiteDatabase;
import com.opencsv.CSVReaderHeaderAware;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.db.WhatsAppGroupDao;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class WhatsAppGroupImporter {
    private final SQLiteDatabase db;
    private final WhatsAppGroupDao dao;

    public WhatsAppGroupImporter(SQLiteDatabase db) {
        this.db = db;
        this.dao = new WhatsAppGroupDao(db);
    }

    public static class Stats {
        public int processed, insertedOrUpdated, skipped;
    }

    // --- START OF FIX ---
    // This new method handles importing from an InputStream, which is required by the repository.
    public Stats importCsv(InputStream inputStream, ImportMapping mapping) throws Exception {
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new InputStreamReader(inputStream))) {
            return doImport(reader, mapping);
        }
    }
    // --- END OF FIX ---

    // This is the original method that works with a File object
    public Stats importCsv(File csvFile, ImportMapping mapping) throws Exception {
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csvFile))) {
            return doImport(reader, mapping);
        }
    }

    // This private method contains the actual import logic, shared by both public methods.
    private Stats doImport(CSVReaderHeaderAware reader, ImportMapping mapping) throws Exception {
        Stats st = new Stats();
        db.beginTransaction();
        try {
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                st.processed++;

                String rawPhone = value(row, mapping, "phone");
                String rawGroupId = value(row, mapping, "whatsAppGroupId");

                String phone10 = DevoteeDao.normalizePhone(rawPhone);
                Integer groupId = null;
                try {
                    if (rawGroupId != null && !rawGroupId.trim().isEmpty()) {
                        groupId = Integer.parseInt(rawGroupId.trim());
                    }
                } catch (NumberFormatException e) {
                    // ignore, groupId will remain null and the row will be skipped
                }

                if (phone10 == null || phone10.length() != 10 || groupId == null) {
                    st.skipped++;
                    continue;
                }

                dao.upsert(phone10, groupId);
                st.insertedOrUpdated++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return st;
    }

    private static String value(Map<String, String> row, ImportMapping mapping, String target) {
        for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) {
            if (target.equalsIgnoreCase(entry.getValue())) {
                return row.get(entry.getKey());
            }
        }
        return null;
    }
}
