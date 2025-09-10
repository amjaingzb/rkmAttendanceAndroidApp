// In: src/main/java/com/rkm/attendance/db/EventDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EventDao {
    private final SQLiteDatabase db;

    public EventDao(SQLiteDatabase db) {
        this.db = db;
    }

    public List<Event> listAll() {
        List<Event> out = new ArrayList<>();
        String sql = "SELECT * FROM event ORDER BY COALESCE(event_date,'9999-12-31') DESC, event_id DESC";

        // Use rawQuery for selects, which returns a Cursor
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                out.add(fromCursor(cursor));
            }
        }
        return out;
    }

    public long insert(Event e) {
        ContentValues values = new ContentValues();
        values.put("event_code", emptyToNull(e.getEventCode()));
        values.put("event_name", e.getEventName());
        values.put("event_date", emptyToNull(e.getEventDate()));
        values.put("active_from_ts", e.getActiveFromTs());
        values.put("active_until_ts", e.getActiveUntilTs());
        values.put("remark", emptyToNull(e.getRemark()));

        // The insert method returns the new row ID
        long id = db.insertOrThrow("event", null, values);

        if (e.getEventCode() == null || e.getEventCode().trim().isEmpty()) {
            String code = "EVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + id;
            ContentValues updateValues = new ContentValues();
            updateValues.put("event_code", code);
            db.update("event", updateValues, "event_id = ?", new String[]{String.valueOf(id)});
            e.setEventCode(code); // Update the model object as well
        }
        return id;
    }

    public void update(Event e) {
        ContentValues values = new ContentValues();
        values.put("event_code", emptyToNull(e.getEventCode()));
        values.put("event_name", e.getEventName());
        values.put("event_date", emptyToNull(e.getEventDate()));
        values.put("active_from_ts", e.getActiveFromTs());
        values.put("active_until_ts", e.getActiveUntilTs());
        values.put("remark", emptyToNull(e.getRemark()));

        db.update("event", values, "event_id = ?", new String[]{String.valueOf(e.getEventId())});
    }

    public Event findCurrentlyActiveEvent() {
        // Using strftime for cross-version date/time comparison in SQLite
        String now = "strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')";
        String sql = "SELECT * FROM event WHERE " + now + " BETWEEN active_from_ts AND active_until_ts LIMIT 1";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    public Event get(long id) {
        try (Cursor cursor = db.query("event", null, "event_id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    public int delete(long id) {
        // The delete method returns the number of rows affected
        return db.delete("event", "event_id = ?", new String[]{String.valueOf(id)});
    }

    public List<String[]> listAttendanceRows(long eventId) {
        ArrayList<String[]> out = new ArrayList<>();
        String sql = "SELECT a.attendance_id, a.cnt, d.mobile_e164 AS mobile, d.full_name AS name, a.remark " +
                     "FROM attendance a LEFT JOIN devotee d ON d.devotee_id = a.devotee_id " +
                     "WHERE a.event_id = ? ORDER BY a.attendance_id DESC";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId)})) {
            // Get column indices once for efficiency
            int idCol = cursor.getColumnIndex("attendance_id");
            int cntCol = cursor.getColumnIndex("cnt");
            int mobileCol = cursor.getColumnIndex("mobile");
            int nameCol = cursor.getColumnIndex("name");
            int remarkCol = cursor.getColumnIndex("remark");
            
            while (cursor.moveToNext()) {
                out.add(new String[]{
                        cursor.getString(idCol),
                        cursor.getString(cntCol),
                        cursor.getString(mobileCol),
                        cursor.getString(nameCol),
                        cursor.getString(remarkCol)
                });
            }
        }
        return out;
    }
    
    // Helper to create an Event from a Cursor
    private Event fromCursor(Cursor cursor) {
        return new Event(
                cursor.getLong(cursor.getColumnIndexOrThrow("event_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("event_code")),
                cursor.getString(cursor.getColumnIndexOrThrow("event_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("event_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("active_from_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("active_until_ts")),
                cursor.getString(cursor.getColumnIndexOrThrow("remark"))
        );
    }

    // A simple helper for ContentValues
    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }

    // --- Attendance-related methods ---

    public static class AttendanceInfo {
        public final long attendanceId;
        public final int cnt;
        public AttendanceInfo(long id, int c) { this.attendanceId = id; this.cnt = c; }
    }

    public AttendanceInfo findAttendance(long eventId, long devoteeId) {
        String sql = "SELECT attendance_id, cnt FROM attendance WHERE event_id = ? AND devotee_id = ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId), String.valueOf(devoteeId)})) {
            if (cursor.moveToFirst()) {
                return new AttendanceInfo(
                        cursor.getLong(cursor.getColumnIndexOrThrow("attendance_id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("cnt"))
                );
            }
            return null;
        }
    }

    // For an operator marking a pre-registered person as present.
    public void markAsAttended(long eventId, long devoteeId) {
        ContentValues values = new ContentValues();
        values.put("cnt", 1);
        db.update("attendance", values, "event_id = ? AND devotee_id = ? AND reg_type = 'PRE_REG'",
                new String[]{String.valueOf(eventId), String.valueOf(devoteeId)});
    }

    // Handles both new spot registrations and historical data import.
    public void upsertAttendance(long eventId, long devoteeId, String regType, int count, String remark) {
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("devotee_id", devoteeId);
        values.put("reg_type", regType);
        values.put("cnt", count);
        values.put("remark", emptyToNull(remark));

        // Use Android's built-in upsert method (INSERT ON CONFLICT DO UPDATE)
        db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public void updateAttendanceCount(long eventId, long devoteeId, int newCnt) {
        ContentValues values = new ContentValues();
        values.put("cnt", newCnt);
        db.update("attendance", values, "event_id = ? AND devotee_id = ?",
                new String[]{String.valueOf(eventId), String.valueOf(devoteeId)});
    }

    public long insertAttendanceWithCount(long eventId, long devoteeId, int cnt, String remark) {
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("devotee_id", devoteeId);
        values.put("cnt", cnt);
        values.put("remark", emptyToNull(remark));
        return db.insertOrThrow("attendance", null, values);
    }
    
    public List<DevoteeDao.EnrichedDevotee> getEnrichedAttendeesForEvent(long eventId) {
        List<DevoteeDao.EnrichedDevotee> results = new ArrayList<>();
        // This query reuses the logic from the main enriched search but is filtered for one event
        String sql = "WITH att_stats AS (" +
                "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
                "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
                "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
            ") " +
            "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date " +
            "FROM devotee d " +
            "LEFT JOIN whatsapp_group_map wgm ON d.mobile_e164 = wgm.phone_number_10 " +
            "LEFT JOIN att_stats ats ON d.devotee_id = ats.devotee_id " +
            "WHERE d.devotee_id IN (SELECT devotee_id FROM attendance WHERE event_id = ?) " +
            "ORDER BY d.full_name";

        DevoteeDao devoteeDao = new DevoteeDao(db); // Need an instance to call fromRow
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId)})) {
            while (cursor.moveToNext()) {
                Devotee devotee = devoteeDao.fromCursor(cursor); // Use the helper from DevoteeDao
                
                int groupCol = cursor.getColumnIndex("group_number");
                Integer group = cursor.isNull(groupCol) ? null : cursor.getInt(groupCol);
                
                int attendance = cursor.getInt(cursor.getColumnIndexOrThrow("cumulative_attendance"));
                String lastDate = cursor.getString(cursor.getColumnIndexOrThrow("last_attendance_date"));
                
                results.add(new DevoteeDao.EnrichedDevotee(devotee, group, attendance, lastDate));
            }
        }
        return results;
    }
}