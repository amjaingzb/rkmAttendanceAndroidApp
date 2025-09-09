// In: src/main/java/com/rkm/attendance/importer/AttendanceImporter.java
package com.rkm.attendance.importer;

import android.database.sqlite.SQLiteDatabase;
import com.opencsv.CSVReaderHeaderAware;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.db.EventDao;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

public class AttendanceImporter {
    private final SQLiteDatabase db;
    private final DevoteeDao devoteeDao;
    private final EventDao eventDao;

    public AttendanceImporter(SQLiteDatabase db) {
        this.db = db;
        this.devoteeDao = new DevoteeDao(db);
        this.eventDao = new EventDao(db);
    }

    public static class Stats {
        public int processed, insertedOrAdded, skipped;
    }

    public Stats importForEvent(long eventId, File csvFile, ImportMapping mapping) throws Exception {
        Stats st = new Stats();
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csvFile))) {
            Map<String, String> row;
            db.beginTransaction();
            try {
                while ((row = reader.readMap()) != null) {
                    st.processed++;

                    String rawName = val(row, mapping, "full_name");
                    String rawMobile = val(row, mapping, "mobile");
                    String rawCount = val(row, mapping, "count");

                    String mobile10 = DevoteeDao.normalizePhone(rawMobile);
                    if (mobile10 == null || mobile10.length() != 10 || rawName == null || rawName.trim().isEmpty()) {
                        st.skipped++;
                        continue;
                    }

                    long devoteeId = devoteeDao.resolveOrCreateDevotee(rawName, mobile10, null, null, null, null);

                    int count = parseCount(rawCount);
                    String regType = (count > 0) ? "SPOT_REG" : "PRE_REG";

                    eventDao.upsertAttendance(eventId, devoteeId, regType, count, null);
                    st.insertedOrAdded++;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return st;
    }

    // This method logically belongs in EventDao, but we can put it here to keep the importer working
    // It's a direct translation of the old EventDao method.
    private void upsertAttendanceAddCount(long eventId, long devoteeId, Integer cnt, String remark) {
        EventDao.AttendanceInfo existing = eventDao.findAttendance(eventId, devoteeId);
        if (existing != null) {
            int newCount = existing.cnt + (cnt != null ? cnt : 0);
            eventDao.updateAttendanceCount(eventId, devoteeId, newCount);
        } else {
            eventDao.insertAttendanceWithCount(eventId, devoteeId, (cnt != null ? cnt : 0), remark);
        }
    }


    // ----- Pure Java helpers (no changes) -----
    private static String val(Map<String, String> row, ImportMapping mapping, String target) {
        String chosen = headerFor(mapping, target);
        if (chosen == null) return null;
        String v = row.get(chosen);
        if (v != null) return v;

        // Fallback for keys that might have weird characters (like BOM)
        String want = normHeader(chosen);
        // Replace 'var' with the explicit type: Map.Entry<String, String>
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (normHeader(entry.getKey()).equals(want)) {
                return entry.getValue();
            }
        }
        return null;
    }


    private static String headerFor(ImportMapping m, String target) {
        // Replace 'var' with the explicit type: Map.Entry<String, String>
        for (Map.Entry<String, String> entry : m.asMap().entrySet()) {
            if (target.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }


    private static String normHeader(String s) {
        if (s == null) return "";
        String x = s.replace("\uFEFF", "").replace('\u00A0', ' ');
        return x.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static int parseCount(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        try {
            String d = t.replaceAll("[^0-9\\-+]", "");
            if (d.isEmpty()) return 0;
            return Integer.parseInt(d);
        } catch (NumberFormatException ignore) {
            return 0; // Default to 0 if parsing fails
        }
    }
}