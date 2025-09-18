// In: src/main/java/com/rkm/attendance/db/DevoteeDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.ui.EventStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DevoteeDao {
    private final SQLiteDatabase db;

    public DevoteeDao(SQLiteDatabase db) {
        this.db = db;
    }
    
    // --- START OF FIX (WhatsApp Icon) ---
    private static final String ENRICHED_DEVOTEE_SQL =
            "WITH att_stats AS (" +
            "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
            "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
            "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
            ") " +
            "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date " +
            "FROM devotee d " +
            "LEFT JOIN whatsapp_group_map wgm ON d.mobile_e164 = wgm.phone_number_10 " +
            "LEFT JOIN att_stats ats ON d.devotee_id = ats.devotee_id ";

    public List<EnrichedDevotee> getAllEnrichedDevotees() {
        String sql = ENRICHED_DEVOTEE_SQL + "ORDER BY d.full_name COLLATE NOCASE";
        List<EnrichedDevotee> results = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                results.add(enrichedFromCursor(cursor, EventStatus.WALK_IN));
            }
        }
        return results;
    }
    
    public Integer getWhatsAppGroup(String mobileE164) {
        if (mobileE164 == null) return null;
        try (Cursor cursor = db.query("whatsapp_group_map", new String[]{"group_number"}, "phone_number_10 = ?", new String[]{mobileE164}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return null;
    }
    // --- END OF FIX (WhatsApp Icon) ---

    public void update(Devotee d) {
        String sql = "UPDATE devotee SET " +
                "full_name = ?, " + "name_norm = ?, " + "mobile_e164 = ?, " +
                "address = ?, " + "age = ?, " + "email = ?, " + "gender = ?, " +
                "extra_json = ?, " + "updated_at = strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime') " +
                "WHERE devotee_id = ?";
        Object[] args = new Object[]{
                d.getFullName(), normalizeName(d.getFullName()), normalizePhone(d.getMobileE164()),
                d.getAddress(), d.getAge(), d.getEmail(), d.getGender(), d.getExtraJson(), d.getDevoteeId()
        };
        db.execSQL(sql, args);
    }
    
    public Devotee findByKey(String mobile10, String nameNorm) {
        String sql = "SELECT * FROM devotee WHERE mobile_e164 = ? AND name_norm = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(sql, new String[]{mobile10, nameNorm})) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    // ... All other methods are unchanged ...
    public Devotee fromCursor(Cursor cursor) { int idCol = cursor.getColumnIndexOrThrow("devotee_id"); int fullNameCol = cursor.getColumnIndexOrThrow("full_name"); int nameNormCol = cursor.getColumnIndexOrThrow("name_norm"); int mobileCol = cursor.getColumnIndexOrThrow("mobile_e164"); int addressCol = cursor.getColumnIndexOrThrow("address"); int ageCol = cursor.getColumnIndexOrThrow("age"); int emailCol = cursor.getColumnIndexOrThrow("email"); int genderCol = cursor.getColumnIndexOrThrow("gender"); int extraJsonCol = cursor.getColumnIndexOrThrow("extra_json"); return new Devotee( cursor.getLong(idCol), cursor.getString(fullNameCol), cursor.getString(nameNormCol), cursor.getString(mobileCol), cursor.getString(addressCol), cursor.isNull(ageCol) ? null : cursor.getInt(ageCol), cursor.getString(emailCol), cursor.getString(genderCol), cursor.getString(extraJsonCol) ); }
    public Devotee getById(long id) { try (Cursor cursor = db.query("devotee", null, "devotee_id = ?", new String[]{String.valueOf(id)}, null, null, null)) { return cursor.moveToFirst() ? fromCursor(cursor) : null; } }
    public int deleteByIds(List<Long> ids) { if (ids == null || ids.isEmpty()) return 0; int deletedRows = 0; db.beginTransaction(); try { for (Long id : ids) { deletedRows += db.delete("devotee", "devotee_id = ?", new String[]{String.valueOf(id)}); } db.setTransactionSuccessful(); } finally { db.endTransaction(); } return deletedRows; }
    public List<Devotee> findByMobileAny(String mobile10) { List<Devotee> out = new ArrayList<>(); String sql = "SELECT * FROM devotee " + "WHERE mobile_e164 = ? OR (extra_json IS NOT NULL AND extra_json LIKE '%' || ? || '%') " + "ORDER BY full_name"; try (Cursor cursor = db.rawQuery(sql, new String[]{mobile10, mobile10})) { while (cursor.moveToNext()) { out.add(fromCursor(cursor)); } } return out; }
    public long resolveOrCreateDevotee(String rawName, String rawMobile, String address, Integer age, String email, String gender) { String nameNorm  = normalizeName(rawName); String mobile10  = normalizePhone(rawMobile); if (nameNorm == null || nameNorm.trim().isEmpty() || mobile10 == null || mobile10.length() != 10) { throw new IllegalArgumentException("resolveOrCreateDevotee: missing/invalid name or mobile"); } List<Devotee> sameMobile = findByMobileAny(mobile10); Devotee best = null; double bestSim = 0.0; for (Devotee cand : sameMobile) { if (normalizeName(rawName).equals(normalizeName(cand.getFullName()))) { best = cand; bestSim = 1.0; break; } } if (best == null) { for (Devotee cand : sameMobile) { if (samePersonOnMobileAggressive(rawName, cand.getFullName())) { best = cand; bestSim = 0.97; break; } } } if (best == null) { for (Devotee cand : sameMobile) { if (isSubsetName(rawName, cand.getFullName()) || isSubsetName(cand.getFullName(), rawName)) { best = cand; bestSim = 0.95; break; } } } if (best == null) { for (Devotee cand : sameMobile) { double s = jaroWinklerSim(normalizeName(rawName), normalizeName(cand.getFullName())); if (s > bestSim) { bestSim = s; best = cand; } } if (bestSim < 0.92) best = null; } if (best != null) { if (bestSim < 0.999) { logFuzzyMerge(mobile10, rawName, best.getFullName(), bestSim); } return best.getDevoteeId(); } Devotee fresh = new Devotee(null, rawName, nameNorm, mobile10, address, age, email, gender, null); return insertAndGetId(fresh); }
    private long insertAndGetId(Devotee d) { ContentValues values = new ContentValues(); values.put("full_name", d.getFullName()); values.put("name_norm", d.getNameNorm()); values.put("mobile_e164", d.getMobileE164()); values.put("address", d.getAddress()); values.put("age", d.getAge()); values.put("email", d.getEmail()); values.put("gender", d.getGender()); values.put("extra_json", d.getExtraJson()); return db.insertOrThrow("devotee", null, values); }
    public static class EnrichedDevotee { private final Devotee devotee; private final Integer whatsAppGroup; private final int cumulativeAttendance; private final String lastAttendanceDate; private final EventStatus eventStatus; public EnrichedDevotee(Devotee d, Integer wg, int ca, String lad, EventStatus es) { this.devotee = d; this.whatsAppGroup = wg; this.cumulativeAttendance = ca; this.lastAttendanceDate = lad; this.eventStatus = es; } public Devotee devotee() { return devotee; } public Integer whatsAppGroup() { return whatsAppGroup; } public int cumulativeAttendance() { return cumulativeAttendance; } public String lastAttendanceDate() { return lastAttendanceDate; } public EventStatus getEventStatus() { return eventStatus; } }
    public List<Devotee> searchSimpleDevotees(String query) { if (query == null || query.trim().length() < 3) { return Collections.emptyList(); } String s = query.trim().toLowerCase(); String d = s.replaceAll("[^0-9]", ""); List<Devotee> results = new ArrayList<>(); StringBuilder sql = new StringBuilder("SELECT * FROM devotee WHERE "); List<String> args = new ArrayList<>(); sql.append("name_norm LIKE ?"); args.add("%" + s + "%"); if (!d.isEmpty()) { sql.append(" OR mobile_e164 LIKE ?"); args.add("%" + d + "%"); } sql.append(" ORDER BY full_name COLLATE NOCASE"); try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) { while (cursor.moveToNext()) { results.add(fromCursor(cursor)); } } return results; }
    private EnrichedDevotee enrichedFromCursor(Cursor cursor, EventStatus eventStatus) { Devotee d = fromCursor(cursor); int gCol = cursor.getColumnIndex("group_number"); Integer g = cursor.isNull(gCol) ? null : cursor.getInt(gCol); int a = cursor.getInt(cursor.getColumnIndexOrThrow("cumulative_attendance")); String ld = cursor.getString(cursor.getColumnIndexOrThrow("last_attendance_date")); return new EnrichedDevotee(d, g, a, ld, eventStatus); }
    public static final class CounterStats { private final long t, m, r, d; public CounterStats(long t, long m, long r, long d) { this.t = t; this.m = m; this.r = r; this.d = d; } public long totalDevotees() { return t; } public long totalMappedWhatsAppNumbers() { return m; } public long registeredDevoteesInWhatsApp() { return r; } public long devoteesWithAttendance() { return d; } }
    public CounterStats getCounterStats() { return new CounterStats(sC("SELECT COUNT(devotee_id) FROM devotee"), sC("SELECT COUNT(DISTINCT phone_number_10) FROM whatsapp_group_map"), sC("SELECT COUNT(devotee_id) FROM devotee WHERE mobile_e164 IN (SELECT phone_number_10 FROM whatsapp_group_map)"), sC("SELECT COUNT(DISTINCT devotee_id) FROM attendance WHERE cnt > 0")); }
    private long sC(String sql) { try (SQLiteStatement s = db.compileStatement(sql)) { return s.simpleQueryForLong(); } }
    public static String normalizeName(String s) { if (s == null) return null; String t = s.trim().toLowerCase(); t = t.replaceAll("[\\u2018\\u2019\\u201A\\u201B'\"]", ""); t = t.replaceAll("\\s+", " "); return t; }
    public static String normalizePhone(String s) { if (s == null) return null; String[] parts = s.split("[,/;]+"); for (String part : parts) { String d = part.replaceAll("[^0-9]", ""); if (!d.trim().isEmpty()) { return last10(d); } } return null; }
    private static String last10(String d) { return d.length() > 10 ? d.substring(d.length() - 10) : d; }
    public static List<String> extractAllPhones(String s) { if (s == null) return Collections.emptyList(); LinkedHashSet<String> set = new LinkedHashSet<>(); String[] parts = s.split("[,/;]+"); for (String p : parts) { String d = p.replaceAll("[^0-9]", ""); if (!d.trim().isEmpty()) set.add(last10(d)); } return new ArrayList<>(set); }
    private void logFuzzyMerge(String m, String o, String n, double s) { ContentValues v = new ContentValues(); v.put("mobile", m); v.put("old_name", o); v.put("new_name", n); v.put("similarity", s); db.insert("fuzzy_merge_log", null, v); }
    private static String compact(String s) { if (s == null) return ""; return s.toLowerCase().replaceAll("[^a-z0-9]", ""); }
    private static List<String> tokens(String s) { if (s == null || s.trim().isEmpty()) { return new ArrayList<>(); } return Arrays.stream(s.toLowerCase().replaceAll("[^a-z ]"," ").trim().split("\\s+")).filter(t -> t != null && !t.trim().isEmpty()).collect(Collectors.toList()); }
    private static boolean isInitial(String t) { return t.length()==1 || (t.length()==2 && t.charAt(1)=='.'); }
    private static Set<String> tokensNoInitials(String s) { return tokens(s).stream().filter(t -> !isInitial(t)).collect(Collectors.toCollection(LinkedHashSet::new)); }
    public static boolean samePersonOnMobileAggressive(String a, String b) { String ca = compact(a), cb = compact(b); if (!ca.isEmpty() && ca.equals(cb)) return true; Set<String> sa = tokensNoInitials(a), sb = tokensNoInitials(b); if (!sa.isEmpty() && sa.equals(sb)) return true; double sim = jaroWinklerSim(ca, cb); return sim >= 0.90; }
    public static boolean isSubsetName(String shorter, String longer) { if (shorter == null || longer == null) return false; String s = shorter.toLowerCase().trim(); String l = longer.toLowerCase().trim(); Set<String> sTokens = new HashSet<>(Arrays.asList(s.split("\\s+"))); Set<String> lTokens = new HashSet<>(Arrays.asList(l.split("\\s+"))); return lTokens.containsAll(sTokens); }
    public static double jaroWinklerSim(String s1, String s2) { if (s1 == null || s2 == null) return 0.0; if (s1.equals(s2)) return 1.0; int len1 = s1.length(), len2 = s2.length(); if (len1 == 0 || len2 == 0) return 0.0; int md = Math.max(len1, len2) / 2 - 1; boolean[] s1m = new boolean[len1]; boolean[] s2m = new boolean[len2]; int m = 0; for (int i = 0; i < len1; i++) { int st = Math.max(0, i - md); int e = Math.min(i + md + 1, len2); for (int j = st; j < e; j++) { if (s2m[j]) continue; if (s1.charAt(i) != s2.charAt(j)) continue; s1m[i] = true; s2m[j] = true; m++; break; } } if (m == 0) return 0.0; int t = 0; int k = 0; for (int i = 0; i < len1; i++) { if (!s1m[i]) continue; while (!s2m[k]) k++; if (s1.charAt(i) != s2.charAt(k)) t++; k++; } double j = (( (double)m / len1) + ((double)m / len2) + ((m - t / 2.0) / m)) / 3.0; int p = 0; for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) { if (s1.charAt(i) == s2.charAt(i)) p++; else break; } return j + 0.1 * p * (1.0 - j); }
}
