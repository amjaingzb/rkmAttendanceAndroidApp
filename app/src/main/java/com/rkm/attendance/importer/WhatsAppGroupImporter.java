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
import java.util.Arrays;
import java.util.LinkedHashMap;
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

    public Stats importCsv(InputStream inputStream, ImportMapping mapping) throws Exception {
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new InputStreamReader(inputStream))) {
            return doImport(reader, mapping);
        }
    }

    public Stats importCsv(File csvFile, ImportMapping mapping) throws Exception {
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csvFile))) {
            return doImport(reader, mapping);
        }
    }

    // --- START OF FIX #2 ---
    // This method now correctly RETURNS the stats object.
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
    // --- END OF FIX #2 ---

    // --- START OF FIX #1 ---
    // Added "phonenumber" to the synonym list for better guessing.
    // NOTE: This is a static helper method, but placing it here keeps it co-located with the importer.
    public static Map<String, String> guessTargets(Map<String, String> mapping) {
        Map<String, String> out = new LinkedHashMap<>();
        java.util.function.Function<String,String> norm = s -> s==null?"":s.toLowerCase().replaceAll("[^a-z0-9]","");

        Map<String,String> syn = new LinkedHashMap<>();
        for (String s : Arrays.asList("mobile", "mobileno", "mobilenumber", "phone", "phoneno", "phonenumber", "contact", "contactno", "contactnumber"))
            syn.put(norm.apply(s), "phone");
        for (String s : Arrays.asList("group", "groupid", "whatsappgroup", "whatsappgroupid"))
            syn.put(norm.apply(s), "whatsAppGroupId");
            
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String k = norm.apply(entry.getKey());
            String mapped = syn.get(k);
            if (mapped != null) out.put(entry.getKey(), mapped);
        }
        return out;
    }
    // --- END OF FIX #1 ---

    private static String value(Map<String, String> row, ImportMapping mapping, String target) {
        for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) {
            if (target.equalsIgnoreCase(entry.getValue())) {
                return row.get(entry.getKey());
            }
        }
        return null;
    }
}
