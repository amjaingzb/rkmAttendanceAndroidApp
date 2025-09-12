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
    private final ConfigDao configDao;
    private final SQLiteDatabase database; // Use the Android SQLiteDatabase object

    public AttendanceRepository(SQLiteDatabase database) {
        this.database = database;
        // Pass the Android database object to the DAOs
        this.devoteeDao = new DevoteeDao(database);
        this.eventDao = new EventDao(database);
        this.whatsAppGroupDao = new WhatsAppGroupDao(database);
        this.configDao = new ConfigDao(database);
    }

    // --- Privilege & PIN Management Methods ---

    public boolean checkSuperAdminPin(String pin) {
        return configDao.checkSuperAdminPin(pin);
    }

    public boolean checkEventCoordinatorPin(String pin) {
        return configDao.checkEventCoordinatorPin(pin);
    }

    // --- Event Management Methods ---

    public List<Event> getAllEvents() {
        return eventDao.listAll();
    }

    public Event getEventById(long eventId) {
        return eventDao.get(eventId);
    }

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

        if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) {
            finalActiveFrom = date + " 06:00:00"; // Start at 6 AM
        }
        if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) {
            finalActiveUntil = date + " 22:00:00"; // End at 10 PM
        }

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
            eventDao.insertSpotRegistration(eventId, devoteeId);
        }
        return true;
    }

    public long onSpotRegisterAndMarkPresent(long eventId, Devotee newDevoteeData) {
        // MODIFIED: This now calls our new, more powerful saveOrMerge method.
        Devotee mergedDevotee = saveOrMergeDevoteeFromAdmin(newDevoteeData);
        markDevoteeAsPresent(eventId, mergedDevotee.getDevoteeId());
        return mergedDevotee.getDevoteeId();
    }

    // --- Devotee Search and Management ---

    public List<EnrichedDevotee> searchEnrichedDevotees(String query) {
        String mobileInput = (query != null && query.matches(".*\\d.*")) ? query : null;
        String nameInput = (mobileInput == null) ? query : null;
        return devoteeDao.searchEnrichedDevotees(mobileInput, nameInput);
    }

    // MODIFIED: Deprecating the simple addNewDevotee in favor of the merge logic.
    // We now use saveOrMergeDevoteeFromAdmin instead.
    public long addNewDevotee(Devotee newDevoteeData) {
        Devotee finalDevotee = saveOrMergeDevoteeFromAdmin(newDevoteeData);
        return finalDevotee.getDevoteeId();
    }


    // NEW: The core logic to fix the bug.
    /**
     * Handles saving a devotee from the Admin panel.
     * It performs a fuzzy match. If a match is found, it enriches the existing
     * record with the new data. If no match is found, it creates a new record.
     * @param devoteeFromForm The data entered by the user in the form.
     * @return The final, saved Devotee object (either new or merged).
     */
    public Devotee saveOrMergeDevoteeFromAdmin(Devotee devoteeFromForm) {
        // Use the DAO to find out if this devotee is new or a fuzzy match of an existing one.
        // This will also log the fuzzy match if one occurs.
        long finalId = devoteeDao.resolveOrCreateDevotee(
                devoteeFromForm.getFullName(),
                devoteeFromForm.getMobileE164(),
                devoteeFromForm.getAddress(),
                devoteeFromForm.getAge(),
                devoteeFromForm.getEmail(),
                devoteeFromForm.getGender()
        );

        // Now, get the definitive record from the database.
        Devotee definitiveRecord = devoteeDao.getById(finalId);

        // Use our new helper method in the model to merge the form data.
        // This will update the definitiveRecord in memory with any new, non-blank info.
        definitiveRecord.mergeWith(devoteeFromForm);

        // Save the updated/merged record back to the database.
        devoteeDao.update(definitiveRecord);

        // Return the final, complete record.
        return definitiveRecord;
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

    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) {
        return eventDao.findCheckedInAttendeesForEvent(eventId);
    }

    public EventStats getEventStats(long eventId) {
        return eventDao.getEventStats(eventId);
    }

    public Event getActiveEvent() {
        return eventDao.findCurrentlyActiveEvent();
    }

    public List<DevoteeDao.EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) {
        String mobileInput = (query != null && query.matches(".*\\d.*")) ? query : null;
        String nameInput = (mobileInput == null) ? query : null;
        return devoteeDao.searchDevoteesForEvent(mobileInput, nameInput, eventId);
    }
}