// In: src/main/java/com/rkm/attendance/core/AttendanceRepository.java
package com.rkm.attendance.core;

import android.database.sqlite.SQLiteDatabase;
import com.rkm.attendance.db.*;
import com.rkm.attendance.db.DevoteeDao.CounterStats;
import com.rkm.attendance.db.DevoteeDao.EnrichedDevotee;
import com.rkm.attendance.db.EventDao.EventStats;
import com.rkm.attendance.importer.*;
import com.rkm.attendance.model.*;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The single source of truth and entry point for all application data and business logic.
 */
public class AttendanceRepository {

    private final DevoteeDao devoteeDao;
    private final EventDao eventDao;
    private final WhatsAppGroupDao whatsAppGroupDao;
    private final ConfigDao configDao;
    private final SQLiteDatabase database;

    public AttendanceRepository(SQLiteDatabase database) {
        this.database = database;
        this.devoteeDao = new DevoteeDao(database);
        this.eventDao = new EventDao(database);
        this.whatsAppGroupDao = new WhatsAppGroupDao(database);
        this.configDao = new ConfigDao(database);
    }

    // ... (All other methods remain the same until searchDevoteesForEvent) ...
    public boolean checkSuperAdminPin(String pin) { return configDao.checkSuperAdminPin(pin); }
    public boolean checkEventCoordinatorPin(String pin) { return configDao.checkEventCoordinatorPin(pin); }
    public List<Event> getAllEvents() { return eventDao.listAll(); }
    public Event getEventById(long eventId) { return eventDao.get(eventId); }
    public long createEvent(String name, String date, String remark, String activeFrom, String activeUntil) {
        if (name == null || name.trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); }
        if (date == null || date.trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); }
        String finalActiveFrom = activeFrom;
        String finalActiveUntil = activeUntil;
        if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) { finalActiveFrom = date + " 06:00:00"; }
        if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) { finalActiveUntil = date + " 22:00:00"; }
        Event newEvent = new Event(null, null, name, date, finalActiveFrom, finalActiveUntil, remark);
        return eventDao.insert(newEvent);
    }
    public void updateEvent(Event event) {
        if (event == null) { throw new IllegalArgumentException("Event cannot be null."); }
        if (event.getEventName() == null || event.getEventName().trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); }
        if (event.getEventDate() == null || event.getEventDate().trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); }
        eventDao.update(event);
    }
    public void deleteEvent(long eventId) { eventDao.delete(eventId); }
    public List<String[]> getAttendanceRowsForEvent(long eventId) { return eventDao.listAttendanceRows(eventId); }
    public boolean markDevoteeAsPresent(long eventId, long devoteeId) {
        EventDao.AttendanceInfo info = eventDao.findAttendance(eventId, devoteeId);
        if (info != null && info.cnt > 0) { return false; }
        if (info != null) { eventDao.markAsAttended(eventId, devoteeId); }
        else { eventDao.insertSpotRegistration(eventId, devoteeId); }
        return true;
    }
    public long onSpotRegisterAndMarkPresent(long eventId, Devotee newDevoteeData) {
        Devotee mergedDevotee = saveOrMergeDevoteeFromAdmin(newDevoteeData);
        markDevoteeAsPresent(eventId, mergedDevotee.getDevoteeId());
        return mergedDevotee.getDevoteeId();
    }
    public List<EnrichedDevotee> searchEnrichedDevotees(String query) {
        String mobileInput = (query != null && query.matches(".*\\d.*")) ? query : null;
        String nameInput = (mobileInput == null) ? query : null;
        return devoteeDao.searchEnrichedDevotees(mobileInput, nameInput);
    }
    public long addNewDevotee(Devotee newDevoteeData) {
        Devotee finalDevotee = saveOrMergeDevoteeFromAdmin(newDevoteeData);
        return finalDevotee.getDevoteeId();
    }
    public Devotee saveOrMergeDevoteeFromAdmin(Devotee devoteeFromForm) {
        long finalId = devoteeDao.resolveOrCreateDevotee(
                devoteeFromForm.getFullName(), devoteeFromForm.getMobileE164(),
                devoteeFromForm.getAddress(), devoteeFromForm.getAge(),
                devoteeFromForm.getEmail(), devoteeFromForm.getGender()
        );
        Devotee definitiveRecord = devoteeDao.getById(finalId);
        definitiveRecord.mergeWith(devoteeFromForm);
        devoteeDao.update(definitiveRecord);
        return definitiveRecord;
    }
    public Devotee getDevoteeById(long devoteeId) { return devoteeDao.getById(devoteeId); }
    public void updateDevotee(Devotee devotee) { devoteeDao.update(devotee); }
    public int deleteDevotees(List<Long> devoteeIds) { return devoteeDao.deleteByIds(devoteeIds); }
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
    public CounterStats getCounterStats() { return devoteeDao.getCounterStats(); }
    public List<EnrichedDevotee> getAllEnrichedDevotees() { return devoteeDao.getAllEnrichedDevotees(); }
    public List<EnrichedDevotee> getEnrichedAttendeesForEvent(long eventId) { return eventDao.getEnrichedAttendeesForEvent(eventId); }
    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) { return eventDao.findCheckedInAttendeesForEvent(eventId); }
    public EventStats getEventStats(long eventId) { return eventDao.getEventStats(eventId); }
    public Event getActiveEvent() { return eventDao.findCurrentlyActiveEvent(); }


    // MODIFIED: This is the new, correct implementation that fixes the bug.
    public List<DevoteeDao.EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) {
        // 1. Get a simple list of matching devotees from the master table using the new, reliable DAO method.
        List<Devotee> allMatches = devoteeDao.searchSimpleDevotees(query);

        // 2. Now, enrich these simple results with the status for the CURRENT event.
        return allMatches.stream()
                .map(devotee -> {
                    // Check if this devotee has a PRE_REG record for this specific event.
                    boolean isPreReg = eventDao.isDevoteePreRegisteredForEvent(
                            devotee.getDevoteeId(),
                            eventId
                    );

                    // Create the rich EnrichedDevotee object for the search result list.
                    // We pass 0s and nulls for stats not needed in the search UI.
                    return new EnrichedDevotee(
                            devotee,
                            null, // WhatsApp group not needed here
                            0,    // Cumulative attendance not needed here
                            null, // Last attendance date not needed here
                            isPreReg // This is the critical, event-specific flag
                    );
                })
                .collect(Collectors.toList());
    }
}