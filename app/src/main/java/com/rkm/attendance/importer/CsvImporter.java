// In: src/main/java/com/rkm/attendance/importer/CsvImporter.java
package com.rkm.attendance.importer;

import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.ui.ImportType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CsvImporter {
    private final ObjectMapper om = new ObjectMapper();

    public CsvImporter(SQLiteDatabase db) {
        // Constructor is kept for compatibility but is no longer strictly needed by this class.
    }

    public static class ImportStats {
        public int processed, inserted, updatedChanged, updatedNoChange, skipped;
    }

    public Devotee toDevotee(Map<String, String> row, ImportMapping mapping) throws Exception {
        String fullName = value(row, mapping, "full_name");
        String mobile = value(row, mapping, "mobile");
        if (isBlank(fullName) || isBlank(mobile)) { return null; }
        
        String address = value(row, mapping, "address");
        Integer age = parseAge(value(row, mapping, "age"));
        String email = value(row, mapping, "email");
        String gender = value(row, mapping, "gender");
        String nameNorm = DevoteeDao.normalizeName(fullName);
        String mobileNorm = DevoteeDao.normalizePhone(mobile);

        if (isBlank(mobileNorm)) { return null; }

        Map<String, Object> extras = new LinkedHashMap<>();
        Set<String> mappedHeaders = new HashSet<>();
        
        for(String target : new String[]{"full_name", "mobile", "address", "age", "email", "gender"}){
            String header = headerFor(mapping, target);
            if(header != null) mappedHeaders.add(header);
        }

        for (Map.Entry<String, String> entry : row.entrySet()) {
            String header = entry.getKey();
            if (mappedHeaders.contains(header)) { continue; }
            String target = mapping.targetFor(header);
            if ("RETAIN".equalsIgnoreCase(target)) { extras.put(header, entry.getValue()); }
        }
        
        String extraJson = extras.isEmpty() ? null : om.writeValueAsString(extras);
        return new Devotee(null, fullName, nameNorm, mobileNorm, address, age, email, gender, extraJson);
    }
    
    public static Map<String, String> guessTargets(List<String> headers, ImportType importType) {
        Map<String, String> finalMapping = new LinkedHashMap<>();
        Set<String> usedTargets = new HashSet<>();

        Map<String,String> syn = new LinkedHashMap<>();
        java.util.function.Function<String,String> norm = s -> s==null?"":s.toLowerCase().replaceAll("[^a-z0-9]","");
        
        if (importType == ImportType.WHATSAPP) {
            for (String s : Arrays.asList("mobile", "mobileno", "mobilenumber", "phone", "phoneno", "phonenumber", "contact", "contactno", "contactnumber"))
                syn.put(norm.apply(s), "phone");
            for (String s : Arrays.asList("group", "groupid", "groupno",  "groupnumber", "whatsappgroup", "whatsappgroupid"))
                syn.put(norm.apply(s), "whatsAppGroupId");
        } else {
            for (String s : Arrays.asList("name","fullname","membername","devotee","person","contactname")) syn.put(norm.apply(s), "full_name");
            for (String s : Arrays.asList("mobile","mobileno","mobilenumber", "phone","phoneno","phonenumber", "contact","contactno","contactnumber")) syn.put(norm.apply(s), "mobile");
            for (String s : Arrays.asList("address","addr","residence","location")) syn.put(norm.apply(s), "address");
            for (String s : Arrays.asList("age","years")) syn.put(norm.apply(s), "age");
            for (String s : Arrays.asList("email","e-mail","emailid","emailaddress","mail")) syn.put(norm.apply(s), "email");
            for (String s : Arrays.asList("gender","sex")) syn.put(norm.apply(s), "gender");
        }

        for (String header : headers) {
            String k = norm.apply(header);
            String guessedTarget = syn.get(k);

            // --- START OF FIX ---
            // The fallback logic for partial matches is restored here.
            if (guessedTarget == null && importType != ImportType.WHATSAPP) {
                if (k.contains("mobile") || k.contains("phone") || k.contains("contact")) guessedTarget="mobile";
                else if (k.contains("name")) guessedTarget="full_name";
            }
            // --- END OF FIX ---

            if (guessedTarget != null) {
                if (!usedTargets.contains(guessedTarget)) {
                    finalMapping.put(header, guessedTarget);
                    usedTargets.add(guessedTarget);
                } else {
                    if (importType != ImportType.WHATSAPP) {
                        finalMapping.put(header, "RETAIN");
                    }
                }
            }
        }
        return finalMapping;
    }
    
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static Integer parseAge(String s) { if (s == null) return null; String d = s.replaceAll("[^0-9]", ""); if (d.isEmpty()) return null; try { int age = Integer.parseInt(d); return (age > 0 && age < 100) ? age : null; } catch (NumberFormatException e) { return null; } }
    private static String headerFor(ImportMapping mapping, String target) { for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) { if (target.equalsIgnoreCase(entry.getValue())) { return entry.getKey(); } } return null; }
    private static String normHeader(String s) { if (s == null) return ""; String x = s.replace("\uFEFF", "").replace('\u00A0', ' '); return x.toLowerCase().replaceAll("\\s+", " ").trim(); }
    private static String value(Map<String, String> row, ImportMapping mapping, String target) { String chosen = headerFor(mapping, target); if (chosen == null) return null; String v = row.get(chosen); if (v != null) return v; String want = normHeader(chosen); for (Map.Entry<String, String> e : row.entrySet()) { if (normHeader(e.getKey()).equals(want)) { return e.getValue(); } } return null; }
}
