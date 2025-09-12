// In: src/main/java/com/rkm/attendance/db/EventDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

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

        long id = db.insertOrThrow("event", null, values);

        if (e.getEventCode() == null || e.getEventCode().trim().isEmpty()) {
            String code = "EVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + id;
            ContentValues updateValues = new ContentValues();
            updateValues.put("event_code", code);
            db.update("event", updateValues, "event_id = ?", new String[]{String.valueOf(id)});
            e.setEventCode(code);
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
        return db.delete("event", "event_id = ?", new String[]{String.valueOf(id)});
    }

    public List<String[]> listAttendanceRows(long eventId) {
        ArrayList<String[]> out = new ArrayList<>();
        String sql = "SELECT a.attendance_id, a.cnt, d.mobile_e164 AS mobile, d.full_name AS name, a.remark " +
                "FROM attendance a LEFT JOIN devotee d ON d.devotee_id = a.devotee_id " +
                "WHERE a.event_id = ? ORDER BY a.attendance_id DESC";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId)})) {
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

    // NEW: A simple, fast query to check for a pre-registration record.
    public boolean isDevoteePreRegisteredForEvent(long devoteeId, long eventId) {
        String sql = "SELECT 1 FROM attendance WHERE devotee_id = ? AND event_id = ? AND reg_type = 'PRE_REG' LIMIT 1";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(devoteeId), String.valueOf(eventId)})) {
            return cursor.moveToFirst();
        }
    }


    public void markAsAttended(long eventId, long devoteeId) {
        ContentValues values = new ContentValues();
        values.put("cnt", 1);
        db.update("attendance", values, "event_id = ? AND devotee_id = ? AND reg_type = 'PRE_REG'",
                new String[]{String.valueOf(eventId), String.valueOf(devoteeId)});
    }

    public void upsertAttendance(long eventId, long devoteeId, String regType, int count, String remark) {
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("devotee_id", devoteeId);
        values.put("reg_type", regType);
        values.put("cnt", count);
        values.put("remark", emptyToNull(remark));
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
        String sql = "WITH att_stats AS (" +
                "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
                "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
                "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
                ") " +
                "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date, " +
                "(att.reg_type = 'PRE_REG') AS is_prereg " +
                "FROM devotee d " +
                "JOIN attendance att ON d.devotee_id = att.devotee_id " +
                "LEFT JOIN whatsapp_group_map wgm ON d.mobile_e164 = wgm.phone_number_10 " +
                "LEFT JOIN att_stats ats ON d.devotee_id = ats.devotee_id " +
                "WHERE att.event_id = ? " +
                "ORDER BY d.full_name COLLATE NOCASE";

        DevoteeDao devoteeDao = new DevoteeDao(db);
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId)})) {
            while (cursor.moveToNext()) {
                Devotee devotee = devoteeDao.fromCursor(cursor);
                int groupCol = cursor.getColumnIndex("group_number");
                Integer group = cursor.isNull(groupCol) ? null : cursor.getInt(groupCol);
                int attendance = cursor.getInt(cursor.getColumnIndexOrThrow("cumulative_attendance"));
                String lastDate = cursor.getString(cursor.getColumnIndexOrThrow("last_attendance_date"));
                boolean isPreReg = cursor.getInt(cursor.getColumnIndexOrThrow("is_prereg")) == 1;
                results.add(new DevoteeDao.EnrichedDevotee(devotee, group, attendance, lastDate, isPreReg));
            }
        }
        return results;
    }

    public static final class EventStats {
        public final long preRegistered;
        public final long attended;
        public final long spotRegistered;
        public final long total;

        public EventStats(long preRegistered, long attended, long spotRegistered, long total) {
            this.preRegistered = preRegistered;
            this.attended = attended;
            this.spotRegistered = spotRegistered;
            this.total = total;
        }
    }

    public EventStats getEventStats(long eventId) {
        String preRegSql = "SELECT COUNT(*) FROM attendance WHERE event_id = ? AND reg_type = 'PRE_REG'";
        String attendedSql = "SELECT COUNT(*) FROM attendance WHERE event_id = ? AND cnt > 0";
        String spotRegSql = "SELECT COUNT(*) FROM attendance WHERE event_id = ? AND reg_type = 'SPOT_REG'";

        long preRegistered = simpleCountQuery(preRegSql, new String[]{String.valueOf(eventId)});
        long attended = simpleCountQuery(attendedSql, new String[]{String.valueOf(eventId)});
        long spotRegistered = simpleCountQuery(spotRegSql, new String[]{String.valueOf(eventId)});

        return new EventStats(preRegistered, attended, spotRegistered, attended);
    }

    public List<Devotee> findCheckedInAttendeesForEvent(long eventId) {
        List<Devotee> results = new ArrayList<>();
        String sql = "SELECT d.* FROM devotee d " +
                "JOIN attendance a ON d.devotee_id = a.devotee_id " +
                "WHERE a.event_id = ? AND a.cnt > 0 " +
                "ORDER BY d.full_name COLLATE NOCASE";

        DevoteeDao devoteeDao = new DevoteeDao(db);
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(eventId)})) {
            while (cursor.moveToNext()) {
                results.add(devoteeDao.fromCursor(cursor));
            }
        }
        return results;
    }

    private long simpleCountQuery(String sql, String[] args) {
        try (SQLiteStatement statement = db.compileStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    statement.bindString(i + 1, args[i]);
                }
            }
            return statement.simpleQueryForLong();
        }
    }
    public long insertSpotRegistration(long eventId, long devoteeId) {
        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("devotee_id", devoteeId);
        values.put("reg_type", "SPOT_REG");
        values.put("cnt", 1);
        return db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}