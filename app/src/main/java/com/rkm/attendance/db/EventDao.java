// In: src/main/java/com/rkm/attendance/db/EventDao.java
package com.rkm.attendance.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.ui.EventStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDao {
    private final SQLiteDatabase db;

    public EventDao(SQLiteDatabase db) {
        this.db = db;
    }

    public List<Event> listAll() {
        List<Event> out = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String sql = "SELECT *, " +
                "  CASE " +
                "    WHEN event_date > ? THEN 1 " +
                "    WHEN event_date = ? THEN 2 " +
                "    ELSE 3 " +
                "  END as date_group " +
                "FROM event " +
                "ORDER BY " +
                "  date_group ASC, " +
                "  CASE WHEN date_group = 1 THEN event_date END ASC, " +
                "  CASE WHEN date_group = 3 THEN event_date END DESC, " +
                "  event_name COLLATE NOCASE ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{today, today})) {
            while (cursor.moveToNext()) {
                out.add(fromCursor(cursor));
            }
        }
        return out;
    }

    public boolean hasOverlap(String fromTs, String untilTs, Long excludeEventId) {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM event WHERE (? < active_until_ts) AND (? > active_from_ts)");
        List<String> args = new ArrayList<>();
        args.add(fromTs);
        args.add(untilTs);

        if (excludeEventId != null) {
            sql.append(" AND (event_id != ?)");
            args.add(String.valueOf(excludeEventId));
        }

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            return cursor.moveToFirst();
        }
    }

    public long insert(Event e) {
        ContentValues values = new ContentValues();
        values.put("event_code", emptyToNull(e.getEventCode()));
        values.put("event_name", e.getEventName());
        values.put("event_date", emptyToNull(e.getEventDate()));
        values.put("active_from_ts", e.getActiveFromTs());
        values.put("active_until_ts", e.getActiveUntilTs());
        values.put("remark", emptyToNull(e.getRemark()));
        return db.insertOrThrow("event", null, values);
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

    // --- START OF FIX: This method has been restored. ---
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
    // --- END OF FIX ---

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

    public static class AttendanceStatus {
        public final String regType;
        public final int count;
        public AttendanceStatus(String regType, int count) {
            this.regType = regType;
            this.count = count;
        }
    }

    public AttendanceStatus getAttendanceStatus(long devoteeId, long eventId) {
        String sql = "SELECT reg_type, cnt FROM attendance WHERE devotee_id = ? AND event_id = ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(devoteeId), String.valueOf(eventId)})) {
            if (cursor.moveToFirst()) {
                return new AttendanceStatus(
                        cursor.getString(cursor.getColumnIndexOrThrow("reg_type")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("cnt"))
                );
            }
            return null;
        }
    }

    public void markAsAttended(long eventId, long devoteeId) {
        ContentValues values = new ContentValues();
        values.put("cnt", 1);
        db.update("attendance", values, "event_id = ? AND devotee_id = ?",
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

    public List<DevoteeDao.EnrichedDevotee> getEnrichedAttendeesForEvent(long eventId) {
        List<DevoteeDao.EnrichedDevotee> results = new ArrayList<>();
        String sql = "WITH att_stats AS (" +
                "SELECT a.devotee_id, SUM(a.cnt) AS total_attendance, MAX(date(e.event_date)) AS last_date " +
                "FROM attendance a JOIN event e ON a.event_id = e.event_id " +
                "WHERE e.event_date IS NOT NULL AND e.event_date != '' GROUP BY a.devotee_id" +
                ") " +
                "SELECT d.*, wgm.group_number, COALESCE(ats.total_attendance, 0) AS cumulative_attendance, ats.last_date AS last_attendance_date, " +
                "att.reg_type AS reg_type, att.cnt AS count " +
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
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                EventStatus status = (count > 0) ? EventStatus.PRESENT : EventStatus.PRE_REGISTERED;
                results.add(new DevoteeDao.EnrichedDevotee(devotee, group, attendance, lastDate, status));
            }
        }
        return results;
    }

    public static final class EventStats {
        public final long preRegistered, attended, spotRegistered, total;
        public EventStats(long p, long a, long s, long t) { this.preRegistered = p; this.attended = a; this.spotRegistered = s; this.total = t; }
    }

    public EventStats getEventStats(long eventId) {
        long preRegistered = simpleCountQuery("SELECT COUNT(*) FROM attendance WHERE event_id = ? AND reg_type = 'PRE_REG'", new String[]{String.valueOf(eventId)});
        long attended = simpleCountQuery("SELECT COUNT(*) FROM attendance WHERE event_id = ? AND cnt > 0", new String[]{String.valueOf(eventId)});
        long spotRegistered = simpleCountQuery("SELECT COUNT(*) FROM attendance WHERE event_id = ? AND reg_type = 'SPOT_REG'", new String[]{String.valueOf(eventId)});
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
