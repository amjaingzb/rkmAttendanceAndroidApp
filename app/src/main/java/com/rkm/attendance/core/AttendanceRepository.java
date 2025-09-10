// In: src/main/java/com/rkm/attendance/core/AttendanceRepository.java
package com.rkm.attendance.core;

import android.database.sqlite.SQLiteDatabase;
import com.rkm.attendance.db.*;
// Add these two explicit imports for the nested classes
import com.rkm.attendance.db.DevoteeDao.CounterStats;
import com.rkm.attendance.db.DevoteeDao.EnrichedDevotee;
import com.rkm.attendance.db.EventDao.EventStats;
import com.rkm.attendance.importer.*;
import com.rkm.attendance.model.*;

import java.io.File;
import java.util.List;

/**
 * The single source of truth and entry point for all application data and business logic.
 * This class is UI-agnostic and forms the core "headless" library for Android.
 */
public class AttendanceRepository {

    private final DevoteeDao devoteeDao;
    private final EventDao eventDao;
    private final WhatsAppGroupDao whatsAppGroupDao;
    private final SQLiteDatabase database; // Use the Android SQLiteDatabase object

    public AttendanceRepository(SQLiteDatabase database) {
        this.database = database;
        // Pass the Android database object to the DAOs
        this.devoteeDao = new DevoteeDao(database);
        this.eventDao = new EventDao(database);
        this.whatsAppGroupDao = new WhatsAppGroupDao(database);
    }

    // --- Event Management Methods ---

    public List<Event> getAllEvents() {
        return eventDao.listAll();
    }

    public Event getEventById(long eventId) {
        return eventDao.get(eventId);
    }

    // In: core/AttendanceRepository.java

    public long createEvent(String name, String date, String remark, String activeFrom, String activeUntil) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event Name is mandatory.");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Event Date is mandatory.");
        }

        // --- SMART DEFAULTS LOGIC ---
        String finalActiveFrom = activeFrom;
        String finalActiveUntil = activeUntil;

        // If the times are not provided, create defaults for the whole day.
        if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) {
            finalActiveFrom = date + " 06:00:00"; // Start at 6 AM
        }
        if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) {
            finalActiveUntil = date + " 22:00:00"; // End at 10 PM
        }

        // Call the new, correct constructor with all 7 arguments
        Event newEvent = new Event(null, null, name, date, finalActiveFrom, finalActiveUntil, remark);

        return eventDao.insert(newEvent);
    }

    public void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }
        if (event.getEventName() == null || event.getEventName().trim().isEmpty()) {
            throw new IllegalArgumentException("Event Name is mandatory.");
        }
        if (event.getEventDate() == null || event.getEventDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Event Date is mandatory.");
        }
        // The DAO will handle the actual database update
        eventDao.update(event);
    }

    public void deleteEvent(long eventId) {
        eventDao.delete(eventId);
    }

    // --- Attendance Management Methods ---

    public List<String[]> getAttendanceRowsForEvent(long eventId) {
        return eventDao.listAttendanceRows(eventId);
    }

    public boolean markDevoteeAsPresent(long eventId, long devoteeId) {
        EventDao.AttendanceInfo info = eventDao.findAttendance(eventId, devoteeId);
        if (info != null && info.cnt > 0) {
            return false; // Already present
        }

        if (info != null) { // Exists but count is 0 (must be a pre-registration)
            eventDao.markAsAttended(eventId, devoteeId);
        } else {
            // Does not exist in attendance table at all. This is a WALK-IN.
            // We must create a new SPOT_REG record.
            eventDao.insertSpotRegistration(eventId, devoteeId);
        }
        return true;
    }

    public long onSpotRegisterAndMarkPresent(long eventId, Devotee newDevoteeData) {
        long devoteeId = devoteeDao.resolveOrCreateDevotee(
                newDevoteeData.getFullName(),
                newDevoteeData.getMobileE164(),
                newDevoteeData.getAddress(),
                newDevoteeData.getAge(),
                newDevoteeData.getEmail(),
                newDevoteeData.getGender()
        );

        markDevoteeAsPresent(eventId, devoteeId);
        return devoteeId;
    }

    // --- Devotee Search and Management ---

    public List<EnrichedDevotee> searchEnrichedDevotees(String query) {
        String mobileInput = (query != null && query.matches(".*\\d.*")) ? query : null;
        String nameInput = (mobileInput == null) ? query : null;
        return devoteeDao.searchEnrichedDevotees(mobileInput, nameInput);
    }

    public long addNewDevotee(Devotee newDevoteeData) {
        return devoteeDao.resolveOrCreateDevotee(
                newDevoteeData.getFullName(),
                newDevoteeData.getMobileE164(),
                newDevoteeData.getAddress(),
                newDevoteeData.getAge(),
                newDevoteeData.getEmail(),
                newDevoteeData.getGender()
        );
    }

    public Devotee getDevoteeById(long devoteeId) {
        return devoteeDao.getById(devoteeId);
    }

    public void updateDevotee(Devotee devotee) {
        devoteeDao.update(devotee);
    }

    public int deleteDevotees(List<Long> devoteeIds) {
        return devoteeDao.deleteByIds(devoteeIds);
    }

    // --- Importer Methods ---

    public CsvImporter.ImportStats importMasterDevoteeList(File csvFile, ImportMapping mapping, boolean unmappedToExtras) throws Exception {
        CsvImporter importer = new CsvImporter(database);
        importer.setIncludeUnmappedAsExtras(unmappedToExtras);
        return importer.importCsv(csvFile, mapping);
    }

    public AttendanceImporter.Stats importAttendanceList(long eventId, File csvFile, ImportMapping mapping) throws Exception {
        AttendanceImporter importer = new AttendanceImporter(database);
        return importer.importForEvent(eventId, csvFile, mapping);
    }

    public WhatsAppGroupImporter.Stats importWhatsAppGroups(File csvFile, ImportMapping mapping) throws Exception {
        WhatsAppGroupImporter importer = new WhatsAppGroupImporter(database);
        return importer.importCsv(csvFile, mapping);
    }

    // --- Reporting Methods ---

    public CounterStats getCounterStats() {
        return devoteeDao.getCounterStats();
    }

    public List<EnrichedDevotee> getAllEnrichedDevotees() {
        return devoteeDao.getAllEnrichedDevotees();
    }

    public List<EnrichedDevotee> getEnrichedAttendeesForEvent(long eventId) {
        return eventDao.getEnrichedAttendeesForEvent(eventId);
    }

    // NEW METHOD 1: Gets the list of people already checked in for an event.
    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) {
        return eventDao.findCheckedInAttendeesForEvent(eventId);
    }

    // NEW METHOD 2: Gets the statistics for a specific event.
    public EventStats getEventStats(long eventId) {
        return eventDao.getEventStats(eventId);
    }

    public Event getActiveEvent() {
        // For now, this logic is simple. It could be expanded later.
        return eventDao.findCurrentlyActiveEvent();
    }

    public List<DevoteeDao.EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) {
        String mobileInput = (query != null && query.matches(".*\\d.*")) ? query : null;
        String nameInput = (mobileInput == null) ? query : null;
        return devoteeDao.searchDevoteesForEvent(mobileInput, nameInput, eventId);
    }
}