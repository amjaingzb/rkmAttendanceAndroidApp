// In: src/main/java/com/rkm/attendance/db/DevoteeDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.model.Devotee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class DevoteeDao {
    private final SQLiteDatabase db;

    // ... (CounterStats and EnrichedDevotee classes remain unchanged) ...
    public static final class CounterStats {
        private final long totalDevotees;
        private final long totalMappedWhatsAppNumbers;
        private final long registeredDevoteesInWhatsApp;
        private final long devoteesWithAttendance;

        public CounterStats(long totalDevotees, long totalMappedWhatsAppNumbers, long registeredDevoteesInWhatsApp, long devoteesWithAttendance) {
            this.totalDevotees = totalDevotees;
            this.totalMappedWhatsAppNumbers = totalMappedWhatsAppNumbers;
            this.registeredDevoteesInWhatsApp = registeredDevoteesInWhatsApp;
            this.devoteesWithAttendance = devoteesWithAttendance;
        }
        public long totalDevotees() { return totalDevotees; }
        public long totalMappedWhatsAppNumbers() { return totalMappedWhatsAppNumbers; }
        public long registeredDevoteesInWhatsApp() { return registeredDevoteesInWhatsApp; }
        public long devoteesWithAttendance() { return devoteesWithAttendance; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CounterStats that = (CounterStats) o;
            return totalDevotees == that.totalDevotees && totalMappedWhatsAppNumbers == that.totalMappedWhatsAppNumbers && registeredDevoteesInWhatsApp == that.registeredDevoteesInWhatsApp && devoteesWithAttendance == that.devoteesWithAttendance;
        }
        @Override
        public int hashCode() { return Objects.hash(totalDevotees, totalMappedWhatsAppNumbers, registeredDevoteesInWhatsApp, devoteesWithAttendance); }
    }
    public static final class EnrichedDevotee {
        private final Devotee devotee;
        private final Integer whatsAppGroup;
        private final int cumulativeAttendance;
        private final String lastAttendanceDate;
        private final boolean isPreRegisteredForCurrentEvent;

        public EnrichedDevotee(Devotee devotee, Integer whatsAppGroup, int cumulativeAttendance, String lastAttendanceDate, boolean isPreRegistered) {
            this.devotee = devotee;
            this.whatsAppGroup = whatsAppGroup;
            this.cumulativeAttendance = cumulativeAttendance;
            this.lastAttendanceDate = lastAttendanceDate;
            this.isPreRegisteredForCurrentEvent = isPreRegistered;
        }
        public boolean isPreRegisteredForCurrentEvent() { return isPreRegisteredForCurrentEvent; }
        public Devotee devotee() { return devotee; }
        public Integer whatsAppGroup() { return whatsAppGroup; }
        public int cumulativeAttendance() { return cumulativeAttendance; }
        public String lastAttendanceDate() { return lastAttendanceDate; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnrichedDevotee that = (EnrichedDevotee) o;
            return cumulativeAttendance == that.cumulativeAttendance && Objects.equals(devotee, that.devotee) && Objects.equals(whatsAppGroup, that.whatsAppGroup) && Objects.equals(lastAttendanceDate, that.lastAttendanceDate);
        }
        @Override
        public int hashCode() { return Objects.hash(devotee, whatsAppGroup, cumulativeAttendance, lastAttendanceDate, isPreRegisteredForCurrentEvent); }
    }


    public DevoteeDao(SQLiteDatabase db) {
        this.db = db;
    }

    public Devotee fromCursor(Cursor cursor) {
        int idCol = cursor.getColumnIndexOrThrow("devotee_id");
        int fullNameCol = cursor.getColumnIndexOrThrow("full_name");
        int nameNormCol = cursor.getColumnIndexOrThrow("name_norm");
        int mobileCol = cursor.getColumnIndexOrThrow("mobile_e164");
        int addressCol = cursor.getColumnIndexOrThrow("address");
        int ageCol = cursor.getColumnIndexOrThrow("age");
        int emailCol = cursor.getColumnIndexOrThrow("email");
        int genderCol = cursor.getColumnIndexOrThrow("gender");
        int extraJsonCol = cursor.getColumnIndexOrThrow("extra_json");

        return new Devotee(
                cursor.getLong(idCol),
                cursor.getString(fullNameCol),
                cursor.getString(nameNormCol),
                cursor.getString(mobileCol),
                cursor.getString(addressCol),
                cursor.isNull(ageCol) ? null : cursor.getInt(ageCol),
                cursor.getString(emailCol),
                cursor.getString(genderCol),
                cursor.getString(extraJsonCol)
        );
    }

    public Devotee getById(long id) {
        try (Cursor cursor = db.query("devotee", null, "devotee_id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    public void update(Devotee d) {
        ContentValues values = new ContentValues();
        values.put("full_name", d.getFullName());
        values.put("name_norm", normalizeName(d.getFullName()));
        values.put("mobile_e164", normalizePhone(d.getMobileE164()));
        values.put("address", d.getAddress());
        values.put("age", d.getAge());
        values.put("email", d.getEmail());
        values.put("gender", d.getGender());
        values.put("extra_json", d.getExtraJson());
        db.update("devotee", values, "devotee_id = ?", new String[]{String.valueOf(d.getDevoteeId())});
    }

    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int deletedRows = 0;
        db.beginTransaction();
        try {
            for (Long id : ids) {
                deletedRows += db.delete("devotee", "devotee_id = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return deletedRows;
    }

    public List<Devotee> findByMobileAny(String mobile10) {
        List<Devotee> out = new ArrayList<>();
        String sql = "SELECT * FROM devotee " +
                "WHERE mobile_e164 = ? OR (extra_json IS NOT NULL AND extra_json LIKE '%' || ? || '%') " +
                "ORDER BY full_name";
        try (Cursor cursor = db.rawQuery(sql, new String[]{mobile10, mobile10})) {
            while (cursor.moveToNext()) {
                out.add(fromCursor(cursor));
            }
        }
        return out;
    }

    public long resolveOrCreateDevotee(String rawName, String rawMobile,
                                       String address, Integer age, String email, String gender) {
        String nameNorm  = normalizeName(rawName);
        String mobile10  = normalizePhone(rawMobile);
        if (nameNorm == null || nameNorm.trim().isEmpty() || mobile10 == null || mobile10.length() != 10) {
            throw new IllegalArgumentException("resolveOrCreateDevotee: missing/invalid name or mobile");
        }
        List<Devotee> sameMobile = findByMobileAny(mobile10);
        Devotee best = null;
        double bestSim = 0.0;
        for (Devotee cand : sameMobile) {
            if (CsvImporter.normName(rawName).equals(CsvImporter.normName(cand.getFullName()))) {
                best = cand; bestSim = 1.0; break;
            }
        }
        if (best == null) {
            for (Devotee cand : sameMobile) {
                if (CsvImporter.samePersonOnMobileAggressive(rawName, cand.getFullName())) {
                    best = cand; bestSim = 0.97; break;
                }
            }
        }
        if (best == null) {
            for (Devotee cand : sameMobile) {
                if (CsvImporter.isSubsetName(rawName, cand.getFullName()) || CsvImporter.isSubsetName(cand.getFullName(), rawName)) {
                    best = cand; bestSim = 0.95; break;
                }
            }
        }
        if (best == null) {
            for (Devotee cand : sameMobile) {
                double s = CsvImporter.jaroWinklerSim(CsvImporter.normName(rawName), CsvImporter.normName(cand.getFullName()));
                if (s > bestSim) { bestSim = s; best = cand; }
            }
            if (bestSim < 0.92) best = null;
        }
        if (best != null) {
            if (bestSim < 0.999) {
                logFuzzyMerge(mobile10, rawName, best.getFullName(), bestSim);
            }
            return best.getDevoteeId();
        }
        Devotee fresh = new Devotee(null, rawName, nameNorm, mobile10, address, age, email, gender, null);
        return insertAndGetId(fresh);
    }

    private long insertAndGetId(Devotee d) {
        ContentValues values = new ContentValues();
        values.put("full_name", d.getFullName());
        values.put("name_norm", d.getNameNorm());
        values.put("mobile_e164", d.getMobileE164());
        values.put("address", d.getAddress());
        values.put("age", d.getAge());
        values.put("email", d.getEmail());
        values.put("gender", d.getGender());
        values.put("extra_json", d.getExtraJson());
        return db.insertOrThrow("devotee", null, values);
    }

    public List<EnrichedDevotee> searchEnrichedDevotees(String mobileInput, String namePart) {
        String mobileDigits = (mobileInput != null) ? mobileInput.replaceAll("[^0-9]", "") : null;
        boolean useMobile = mobileDigits != null && mobileDigits.length() >= 4;
        boolean useName   = (namePart != null && !namePart.trim().isEmpty() && namePart.trim().length() >= 3);
        if (!useMobile && !useName) { return Collections.emptyList(); }
        String sql = "WITH att_stats AS (" +
                "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
                "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
                "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
                ") " +
                "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date " +
                "FROM devotee d " +
                "LEFT JOIN whatsapp_group_map wgm ON d.mobile_e164 = wgm.phone_number_10 " +
                "LEFT JOIN att_stats ats ON d.devotee_id = ats.devotee_id " +
                "WHERE (? IS NOT NULL AND ( d.mobile_e164 LIKE '%' || ? || '%' OR (d.extra_json IS NOT NULL AND d.extra_json LIKE '%' || ? || '%') )) " +
                "OR (? IS NOT NULL AND d.name_norm LIKE '%' || lower(?) || '%' ) " +
                "ORDER BY d.full_name COLLATE NOCASE";
        String[] args = {
                useMobile ? mobileDigits : null, useMobile ? mobileDigits : null, useMobile ? mobileDigits : null,
                useName ? namePart : null, useName ? namePart : null
        };
        List<EnrichedDevotee> results = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                results.add(enrichedFromCursor(cursor, false));
            }
        }
        return results;
    }

    // NEW / MODIFIED: This is the simple, correct search method for the operator.
    public List<Devotee> searchSimpleDevotees(String query) {
        if (query == null || query.trim().length() < 3) {
            return Collections.emptyList();
        }
        String searchTerm = query.trim().toLowerCase();
        String digitsOnly = searchTerm.replaceAll("[^0-9]", "");

        List<Devotee> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM devotee WHERE ");
        List<String> args = new ArrayList<>();

        // Search by name is always performed
        sql.append("name_norm LIKE ?");
        args.add("%" + searchTerm + "%");

        // If there are digits, also search by mobile
        if (!digitsOnly.isEmpty()) {
            sql.append(" OR mobile_e164 LIKE ?");
            args.add("%" + digitsOnly + "%");
        }
        sql.append(" ORDER BY full_name COLLATE NOCASE");

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                results.add(fromCursor(cursor));
            }
        }
        return results;
    }

    public List<EnrichedDevotee> getAllEnrichedDevotees() {
        String sql = "WITH att_stats AS (" +
                "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
                "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
                "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
                ") " +
                "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date " +
                "FROM devotee d " +
                "LEFT JOIN whatsapp_group_map wgm ON d.mobile_e164 = wgm.phone_number_10 " +
                "LEFT JOIN att_stats ats ON d.devotee_id = ats.devotee_id " +
                "ORDER BY d.full_name COLLATE NOCASE";
        List<EnrichedDevotee> results = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                results.add(enrichedFromCursor(cursor, false));
            }
        }
        return results;
    }

    private EnrichedDevotee enrichedFromCursor(Cursor cursor, boolean isPreRegisteredForEvent) {
        Devotee devotee = fromCursor(cursor);
        int groupCol = cursor.getColumnIndex("group_number");
        Integer group = cursor.isNull(groupCol) ? null : cursor.getInt(groupCol);
        int attendance = cursor.getInt(cursor.getColumnIndexOrThrow("cumulative_attendance"));
        String lastDate = cursor.getString(cursor.getColumnIndexOrThrow("last_attendance_date"));
        return new EnrichedDevotee(devotee, group, attendance, lastDate, isPreRegisteredForEvent);
    }

    public CounterStats getCounterStats() {
        long totalDevotees = simpleCountQuery("SELECT COUNT(devotee_id) FROM devotee");
        long mappedWhatsApp = simpleCountQuery("SELECT COUNT(DISTINCT phone_number_10) FROM whatsapp_group_map");
        long registeredInWhatsApp = simpleCountQuery("SELECT COUNT(devotee_id) FROM devotee WHERE mobile_e164 IN (SELECT phone_number_10 FROM whatsapp_group_map)");
        long devoteesWithAttendance = simpleCountQuery("SELECT COUNT(DISTINCT devotee_id) FROM attendance WHERE cnt > 0");
        return new CounterStats(totalDevotees, mappedWhatsApp, registeredInWhatsApp, devoteesWithAttendance);
    }

    private long simpleCountQuery(String sql) {
        try (SQLiteStatement statement = db.compileStatement(sql)) {
            return statement.simpleQueryForLong();
        }
    }

    public static String normalizeName(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        t = t.replaceAll("[\u2018\u2019\u201A\u201B'\"]", "");
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    public static String normalizePhone(String s) {
        if (s == null) return null;
        String[] parts = s.split("[,/;]+");
        for (String part : parts) {
            String digits = part.replaceAll("[^0-9]", "");
            if (!digits.trim().isEmpty()) {
                return last10(digits);
            }
        }
        return null;
    }

    private static String last10(String digits) {
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    public static List<String> extractAllPhones(String s) {
        if (s == null) return Collections.emptyList();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String[] parts = s.split("[,/;]+");
        for (String part : parts) {
            String digits = part.replaceAll("[^0-9]", "");
            if (!digits.trim().isEmpty()) set.add(last10(digits));
        }
        return new ArrayList<>(set);
    }

    private void logFuzzyMerge(String mobile, String oldName, String newName, double similarity) {
        ContentValues values = new ContentValues();
        values.put("mobile", mobile);
        values.put("old_name", oldName);
        values.put("new_name", newName);
        values.put("similarity", similarity);
        db.insert("fuzzy_merge_log", null, values);
    }
}