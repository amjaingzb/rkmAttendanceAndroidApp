// In: src/main/java/com/rkm/attendance/importer/CsvImporter.java
package com.rkm.attendance.importer;

import android.database.sqlite.SQLiteDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderHeaderAware;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CsvImporter {
    private final SQLiteDatabase db;
    private final DevoteeDao dao;
    private final ObjectMapper om = new ObjectMapper();

    public static class ImportStats {
        public int processed, inserted, updatedChanged, updatedNoChange, skipped;
    }

    private boolean includeUnmappedAsExtras = false;

    public void setIncludeUnmappedAsExtras(boolean v) {
        this.includeUnmappedAsExtras = v;
    }

    public CsvImporter(SQLiteDatabase db) {
        this.db = db;
        this.dao = new DevoteeDao(db);
    }

    public ImportStats importCsv(File file, ImportMapping mapping) throws Exception {
        ImportStats stats = new ImportStats();

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(file))) {
            Map<String, String> row;
            db.beginTransaction();
            try {
                while ((row = reader.readMap()) != null) {
                    stats.processed++;

                    Devotee d = toDevotee(row, mapping);
                    if (d == null) {
                        stats.skipped++;
                        continue;
                    }
                    
                    Devotee existing = getDevoteeByKey(d.getMobileE164(), d.getNameNorm());
                    
                    dao.resolveOrCreateDevotee(
                        d.getFullName(), d.getMobileE164(), d.getAddress(),
                        d.getAge(), d.getEmail(), d.getGender()
                    );
                    
                    if (existing == null) {
                        stats.inserted++;
                    } else {
                        stats.updatedChanged++;
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return stats;
    }
    
    private Devotee getDevoteeByKey(String mobile10, String nameNorm) {
        String sql = "SELECT * FROM devotee WHERE mobile_e164 = ? AND name_norm = ?";
        try (android.database.Cursor cursor = db.rawQuery(sql, new String[]{mobile10, nameNorm})) {
            if (cursor.moveToFirst()) {
                return dao.fromCursor(cursor);
            }
            return null;
        }
    }

    private Devotee toDevotee(Map<String, String> row, ImportMapping mapping) throws Exception {
        String fullName = value(row, mapping, "full_name");
        String mobile = value(row, mapping, "mobile");
        if (isBlank(fullName) || isBlank(mobile)) {
            return null;
        }

        String address = value(row, mapping, "address");
        Integer age = parseAge(value(row, mapping, "age"));
        String email = value(row, mapping, "email");
        String gender = value(row, mapping, "gender");

        String nameNorm = DevoteeDao.normalizeName(fullName);
        String mobileNorm = DevoteeDao.normalizePhone(mobile);

        if (isBlank(mobileNorm)) {
            return null;
        }

        List<String> allPhones = DevoteeDao.extractAllPhones(mobile);
        List<String> others = new ArrayList<>();
        for (String p : allPhones) {
            if (!p.equals(mobileNorm)) others.add(p);
        }

        Map<String, Object> extras = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : row.entrySet()) {
            String header = e.getKey();
            String choice = mapping.targetFor(header);

            if ("RETAIN".equalsIgnoreCase(choice)) {
                extras.put(header, e.getValue());
                continue;
            }
            if ("DROP".equalsIgnoreCase(choice)) {
                continue;
            }
            if (choice == null || choice.trim().isEmpty()) {
                if (includeUnmappedAsExtras) {
                    extras.put(header, e.getValue());
                }
            }
        }

        if (!others.isEmpty()) {
            extras.put("otherPhones", others);
        }
        String extraJson = extras.isEmpty() ? null : om.writeValueAsString(extras);

        return new Devotee(null, fullName, nameNorm, mobileNorm, address, age, email, gender, extraJson);
    }

    // --- Start of Pure Java Helper Methods ---

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    public static Map<String, String> guessTargets(List<String> headers) {
        Map<String, String> out = new LinkedHashMap<>();
        java.util.function.Function<String,String> norm = s -> s==null?"":s.toLowerCase().replaceAll("[^a-z0-9]","");

        Map<String,String> syn = new LinkedHashMap<>();
        for (String s : Arrays.asList("name","fullname","membername","devotee","person","contactname"))
            syn.put(norm.apply(s), "full_name");
        for (String s : Arrays.asList("mobile","mobileno","mobilenumber", "phone","phoneno","phonenumber", "contact","contactno","contactnumber"))
            syn.put(norm.apply(s), "mobile");
        for (String s : Arrays.asList("address","addr","residence","location"))
            syn.put(norm.apply(s), "address");
        for (String s : Arrays.asList("age","years"))
            syn.put(norm.apply(s), "age");
        for (String s : Arrays.asList("email","e-mail","emailid","emailaddress","mail"))
            syn.put(norm.apply(s), "email");
        for (String s : Arrays.asList("gender","sex"))
            syn.put(norm.apply(s), "gender");

        for (String h : headers) {
            String k = norm.apply(h);
            String mapped = syn.get(k);
            if (mapped == null) {
                if (k.contains("mobile")||k.contains("phone")||k.contains("contact")) mapped="mobile";
                else if (k.contains("name")) mapped="full_name";
            }
            if (mapped != null) out.put(h, mapped);
        }
        return out;
    }

    private static String compact(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static List<String> tokens(String s) {
        if (s == null || s.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(s.toLowerCase()
                        .replaceAll("[^a-z ]"," ")
                        .trim()
                        .split("\\s+"))
                .filter(t -> t != null && !t.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean isInitial(String t) {
        return t.length()==1 || (t.length()==2 && t.charAt(1)=='.');
    }

    // ========== CORRECTED METHOD ==========
    private static Set<String> tokensNoInitials(String s) {
        // The original KeySetView is not available. This is the Java 8 equivalent.
        return tokens(s).stream()
                .filter(t -> !isInitial(t))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean samePersonOnMobileAggressive(String a, String b) {
        String ca = compact(a), cb = compact(b);
        if (!ca.isEmpty() && ca.equals(cb)) return true;
        Set<String> sa = tokensNoInitials(a), sb = tokensNoInitials(b);
        if (!sa.isEmpty() && sa.equals(sb)) return true;
        double sim = jaroWinklerSim(ca, cb);
        return sim >= 0.90;
    }

    private static Integer parseAge(String s) {
        if (s == null) return null;
        String d = s.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return null;
        try {
            int age = Integer.parseInt(d);
            return (age > 0 && age < 100) ? age : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String normName(String s) {
        if (s == null) return "";
        String t = s.toLowerCase().replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", " ").trim();
        String[] parts = t.split(" ");
        Arrays.sort(parts);
        // Use a StringBuilder for older Android compatibility, it's safer than String.join
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
    
    public static double jaroWinklerSim(String s1, String s2) {
        // This method is pure math and standard Java, no changes needed.
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length(), len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];
        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) return 0.0;
        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }
        double m = matches;
        double jaro = ((m / len1) + (m / len2) + ((m - transpositions / 2.0) / m)) / 3.0;
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + 0.1 * prefix * (1.0 - jaro);
    }
    
    public static boolean isFuzzySamePerson(String incomingName, String existingName) {
        String A = normName(incomingName);
        String B = normName(existingName);
        if (A.isEmpty() || B.isEmpty()) return false;
        double sim = jaroWinklerSim(A, B);
        String[] at = A.split(" ");
        String[] bt = B.split(" ");
        boolean tokenGuard = at.length==1 || bt.length==1 || at[0].equals(bt[0]) || at[at.length-1].equals(bt[bt.length-1]);
        return sim >= 0.92 && tokenGuard;
    }

    public static boolean isSubsetName(String shorter, String longer) {
        if (shorter == null || longer == null) return false;
        String s = shorter.toLowerCase().trim();
        String l = longer.toLowerCase().trim();
        Set<String> sTokens = new HashSet<>(Arrays.asList(s.split("\\s+")));
        Set<String> lTokens = new HashSet<>(Arrays.asList(l.split("\\s+")));
        return lTokens.containsAll(sTokens);
    }

    // ========== ADDED MISSING HELPER METHODS ==========
    private static String normHeader(String s) {
        if (s == null) return "";
        // BOM and NBSP characters
        String x = s.replace("\uFEFF", "").replace('\u00A0', ' ');
        return x.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static String headerFor(ImportMapping mapping, String target) {
        for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) {
            if (target.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String value(Map<String, String> row, ImportMapping mapping, String target) {
        String chosen = headerFor(mapping, target);
        if (chosen == null) return null;
        String v = row.get(chosen);
        if (v != null) return v;
        String want = normHeader(chosen);
        for (Map.Entry<String, String> e : row.entrySet()) {
            if (normHeader(e.getKey()).equals(want)) {
                return e.getValue();
            }
        }
        return null;
    }
}