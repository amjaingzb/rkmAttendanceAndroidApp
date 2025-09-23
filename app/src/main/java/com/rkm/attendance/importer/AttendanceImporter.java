// In: src/main/java/com/rkm/attendance/importer/AttendanceImporter.java
package com.rkm.attendance.importer;

import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;

import java.util.Map;

public class AttendanceImporter {

    public static class ParsedAttendanceRow {
        public final Devotee devotee;
        public final int count;

        public ParsedAttendanceRow(Devotee devotee, int count) {
            this.devotee = devotee;
            this.count = count;
        }
    }

    // STEP 4.1: Update parseRow to handle the new fields.
    public ParsedAttendanceRow parseRow(Map<String, String> row, ImportMapping mapping) {
        String fullName = value(row, mapping, "full_name");
        String mobile = value(row, mapping, "mobile");
        if (isBlank(fullName) || isBlank(mobile)) {
            return null; // Mandatory fields are missing
        }

        String address = value(row, mapping, "address");
        Integer age = parseAge(value(row, mapping, "age"));
        String email = value(row, mapping, "email");
        String gender = value(row, mapping, "gender");
        String aadhaar = value(row, mapping, "aadhaar"); // NEW
        String pan = value(row, mapping, "pan");         // NEW
        String nameNorm = DevoteeDao.normalizeName(fullName);
        String mobileNorm = DevoteeDao.normalizePhone(mobile);

        if (isBlank(mobileNorm)) {
            return null;
        }

        Devotee parsedDevotee = new Devotee(null, fullName, nameNorm, mobileNorm, address, age, email, gender, aadhaar, pan, null);

        int count = parseCount(value(row, mapping, "count"));

        return new ParsedAttendanceRow(parsedDevotee, count);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static Integer parseAge(String s) { if (s == null) return null; String d = s.replaceAll("[^0-9]", ""); if (d.isEmpty()) return null; try { int age = Integer.parseInt(d); return (age > 0 && age < 100) ? age : null; } catch (NumberFormatException e) { return null; } }
    private static String headerFor(ImportMapping mapping, String target) { for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) { if (target.equalsIgnoreCase(entry.getValue())) { return entry.getKey(); } } return null; }
    private static String normHeader(String s) { if (s == null) return ""; String x = s.replace("\uFEFF", "").replace('\u00A0', ' '); return x.toLowerCase().replaceAll("\\s+", " ").trim(); }
    private static String value(Map<String, String> row, ImportMapping mapping, String target) { String chosen = headerFor(mapping, target); if (chosen == null) return null; String v = row.get(chosen); if (v != null) return v; String want = normHeader(chosen); for (Map.Entry<String, String> e : row.entrySet()) { if (normHeader(e.getKey()).equals(want)) { return e.getValue(); } } return null; }
    private static int parseCount(String s) { if (s == null) return 0; String t = s.trim(); if (t.isEmpty()) return 0; try { String d = t.replaceAll("[^0-9\\-+]", ""); if (d.isEmpty()) return 0; return Integer.parseInt(d); } catch (NumberFormatException ignore) { return 0; } }
}
