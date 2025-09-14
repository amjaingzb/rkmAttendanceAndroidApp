// In: src/main/java/com/rkm/attendance/core/AttendanceRepository.java
package com.rkm.attendance.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.rkm.attendance.db.*;
import com.rkm.attendance.db.DevoteeDao.EnrichedDevotee;
import com.rkm.attendance.model.*;
import com.rkm.rkmattendanceapp.ui.EventStatus;
import com.rkm.attendance.importer.AttendanceImporter;
import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.attendance.importer.WhatsAppGroupImporter;

import java.io.File;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    // ... (unchanged until importMasterDevoteeList)
    public boolean checkSuperAdminPin(String pin) { return configDao.checkSuperAdminPin(pin); }
    public boolean checkEventCoordinatorPin(String pin) { return configDao.checkEventCoordinatorPin(pin); }
    public List<Event> getAllEvents() { return eventDao.listAll(); }
    public Event getEventById(long eventId) { return eventDao.get(eventId); }
    public long createEvent(String name, String date, String remark, String activeFrom, String activeUntil) throws OverlapException {
        if (name == null || name.trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); }
        if (date == null || date.trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); }
        String finalActiveFrom = activeFrom;
        String finalActiveUntil = activeUntil;
        if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) { finalActiveFrom = date + " 06:00:00"; }
        if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) { finalActiveUntil = date + " 22:00:00"; }
        if (eventDao.hasOverlap(finalActiveFrom, finalActiveUntil, null)) {
            throw new OverlapException("Time window overlaps with an existing event.");
        }
        Event newEvent = new Event(null, null, name, date, finalActiveFrom, finalActiveUntil, remark);
        return eventDao.insert(newEvent);
    }
    public void updateEvent(Event event) throws OverlapException {
        if (event == null) { throw new IllegalArgumentException("Event cannot be null."); }
        if (event.getEventName() == null || event.getEventName().trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); }
        if (event.getEventDate() == null || event.getEventDate().trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); }
        if (eventDao.hasOverlap(event.getActiveFromTs(), event.getActiveUntilTs(), event.getEventId())) {
            throw new OverlapException("Time window overlaps with an existing event.");
        }
        eventDao.update(event);
    }
    public void deleteEvent(long eventId) { eventDao.delete(eventId); }
    public List<String[]> getAttendanceRowsForEvent(long eventId) { return eventDao.listAttendanceRows(eventId); }
    public boolean markDevoteeAsPresent(long eventId, long devoteeId) {
        EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devoteeId, eventId);
        if (status != null && status.count > 0) {
            return false;
        }
        if (status != null) {
            eventDao.markAsAttended(eventId, devoteeId);
            return true;
        }
        else {
            eventDao.insertSpotRegistration(eventId, devoteeId);
            return true;
        }
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

    // NEW: Method to handle import from a content URI
    public CsvImporter.ImportStats importMasterDevoteeList(Context context, Uri uri, ImportMapping mapping) throws Exception {
        CsvImporter importer = new CsvImporter(database);
        // We can set other importer properties here if needed
        // importer.setIncludeUnmappedAsExtras(unmappedToExtras);
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new Exception("Could not open file URI");
            }
            return importer.importCsv(inputStream, mapping);
        }
    }
    
    // This is the old method that works with a File object
    public CsvImporter.ImportStats importMasterDevoteeList(File csvFile, ImportMapping mapping, boolean unmappedToExtras) throws Exception {
        CsvImporter importer = new CsvImporter(database);
        importer.setIncludeUnmappedAsExtras(unmappedToExtras);
        return importer.importCsv(csvFile, mapping);
    }

    // ... rest of the file is unchanged ...
    public AttendanceImporter.Stats importAttendanceList(long eventId, File csvFile, ImportMapping mapping) throws Exception {
        AttendanceImporter importer = new AttendanceImporter(database);
        return importer.importForEvent(eventId, csvFile, mapping);
    }
    public WhatsAppGroupImporter.Stats importWhatsAppGroups(File csvFile, ImportMapping mapping) throws Exception {
        WhatsAppGroupImporter importer = new WhatsAppGroupImporter(database);
        return importer.importCsv(csvFile, mapping);
    }
    public DevoteeDao.CounterStats getCounterStats() { return devoteeDao.getCounterStats(); }
    public List<EnrichedDevotee> getAllEnrichedDevotees() { return devoteeDao.getAllEnrichedDevotees(); }
    public List<EnrichedDevotee> getEnrichedAttendeesForEvent(long eventId) { return eventDao.getEnrichedAttendeesForEvent(eventId); }
    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) { return eventDao.findCheckedInAttendeesForEvent(eventId); }
    public EventDao.EventStats getEventStats(long eventId) { return eventDao.getEventStats(eventId); }
    public Event getActiveEvent() { return eventDao.findCurrentlyActiveEvent(); }
    public List<EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) {
        List<Devotee> allMatches = devoteeDao.searchSimpleDevotees(query);
        List<EnrichedDevotee> enrichedList = allMatches.stream()
                .map(devotee -> {
                    EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devotee.getDevoteeId(), eventId);
                    EventStatus eventStatus;
                    if (status == null) {
                        eventStatus = EventStatus.WALK_IN;
                    } else if (status.count > 0) {
                        eventStatus = EventStatus.PRESENT;
                    } else {
                        eventStatus = EventStatus.PRE_REGISTERED;
                    }
                    return new EnrichedDevotee(devotee, null, 0, null, eventStatus);
                })
                .collect(Collectors.toList());
        return enrichedList.stream()
                .sorted(Comparator.comparingInt((EnrichedDevotee e) -> e.getEventStatus() == EventStatus.PRESENT ? 1 : 0)
                        .thenComparing(e -> e.devotee().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
