// In: src/main/java/com/rkm/attendance/core/AttendanceRepository.java
package com.rkm.attendance.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.opencsv.CSVReaderHeaderAware;
import com.rkm.attendance.db.*;
import com.rkm.attendance.db.DevoteeDao.EnrichedDevotee;
import com.rkm.attendance.model.*;
import com.rkm.rkmattendanceapp.ui.EventStatus;
import com.rkm.attendance.importer.AttendanceImporter;
import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.attendance.importer.WhatsAppGroupImporter;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AttendanceRepository {
    private final DevoteeDao devoteeDao;
    private final EventDao eventDao;
    private final WhatsAppGroupDao whatsAppGroupDao;
    private final ConfigDao configDao;
    private final SQLiteDatabase database;
    private static final String TAG = "AttendanceRepository";

    public AttendanceRepository(SQLiteDatabase database) {
        this.database = database;
        this.devoteeDao = new DevoteeDao(database);
        this.eventDao = new EventDao(database);
        this.whatsAppGroupDao = new WhatsAppGroupDao(database);
        this.configDao = new ConfigDao(database);
    }

    // --- START OF FIX ---
    // This is the definitive, correct version of the search method for the Operator screen.
    public List<EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) {
        // First, get the basic list of devotees who match the name/mobile search query.
        List<Devotee> allMatches = devoteeDao.searchSimpleDevotees(query);
        
        // Now, for each of those devotees, enrich them with their specific status for THIS event.
        return allMatches.stream()
                .map(devotee -> {
                    // Get their attendance status for the current event
                    EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devotee.getDevoteeId(), eventId);
                    EventStatus eventStatus = (status == null) ? EventStatus.WALK_IN : (status.count > 0 ? EventStatus.PRESENT : EventStatus.PRE_REGISTERED);
                    
                    // ALSO, get their WhatsApp group status from the master map
                    Integer whatsAppGroup = devoteeDao.getWhatsAppGroup(devotee.getMobileE164());

                    // Create the fully enriched object with ALL the necessary data for the UI
                    return new EnrichedDevotee(devotee, whatsAppGroup, 0, null, eventStatus);
                })
                .sorted(Comparator.comparingInt((EnrichedDevotee e) -> e.getEventStatus() == EventStatus.PRESENT ? 1 : 0)
                        .thenComparing(e -> e.devotee().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
    // --- END OF FIX ---


    // ... All other methods are unchanged and complete ...
    public CsvImporter.ImportStats importAttendanceList(Context context, Uri uri, ImportMapping mapping, long eventId) throws Exception { AppLogger.d(TAG, "Starting importAttendanceList for eventId: " + eventId); AttendanceImporter importer = new AttendanceImporter(); CsvImporter.ImportStats stats = new CsvImporter.ImportStats(); List<Map<String, String>> allRows = new ArrayList<>(); try (InputStream inputStream = context.getContentResolver().openInputStream(uri); InputStreamReader reader = new InputStreamReader(inputStream); CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) { Map<String, String> row; while((row = csvReader.readMap()) != null) { allRows.add(row); } } AppLogger.d(TAG, "Loaded " + allRows.size() + " rows from CSV into memory."); if (allRows.isEmpty()) { AppLogger.w(TAG, "CSV file is empty or could not be read. Aborting import."); return stats; } AppLogger.d(TAG, "Starting PASS 1: Save/Merge Devotees."); database.beginTransaction(); try { for (Map<String, String> row : allRows) { stats.processed++; try { AttendanceImporter.ParsedAttendanceRow parsedRow = importer.parseRow(row, mapping); if (parsedRow == null) { AppLogger.w(TAG, "PASS 1: Row skipped due to null parse result. Row: " + row); stats.skipped++; continue; } saveOrMergeDevoteeFromAdmin(parsedRow.devotee); } catch (IllegalArgumentException e) { AppLogger.e(TAG, "PASS 1: Skipping bad devotee row. Row: " + row, e); stats.skipped++; } } AppLogger.d(TAG, "PASS 1: Loop finished. Setting transaction successful."); database.setTransactionSuccessful(); } catch (Exception e) { AppLogger.e(TAG, "PASS 1: CRITICAL ERROR during transaction.", e); } finally { AppLogger.d(TAG, "PASS 1: Ending transaction."); database.endTransaction(); } AppLogger.d(TAG, "Starting PASS 2: Link Attendance."); database.beginTransaction(); try { for (Map<String, String> row : allRows) { try { AttendanceImporter.ParsedAttendanceRow parsedRow = importer.parseRow(row, mapping); if (parsedRow == null) continue; Devotee devotee = devoteeDao.findByKey(parsedRow.devotee.getMobileE164(), parsedRow.devotee.getNameNorm()); if (devotee == null) { AppLogger.e(TAG, "PASS 2: FATAL! Could not find devotee in DB that should have been saved in Pass 1: " + parsedRow.devotee.getFullName()); continue; } eventDao.upsertAttendance(eventId, devotee.getDevoteeId(), "PRE_REG", parsedRow.count, "Imported"); stats.inserted++; } catch (Exception e) { AppLogger.e(TAG, "PASS 2: ERROR linking attendance for row: " + row, e); } } AppLogger.d(TAG, "PASS 2: Loop finished. Setting transaction successful."); database.setTransactionSuccessful(); } catch (Exception e) { AppLogger.e(TAG, "PASS 2: CRITICAL ERROR during transaction.", e); } finally { AppLogger.d(TAG, "PASS 2: Ending transaction."); database.endTransaction(); } stats.processed = allRows.size(); AppLogger.d(TAG, "Import finished. Final Stats: " + stats.processed + " processed, " + stats.inserted + " inserted/linked, " + stats.skipped + " skipped."); return stats; }
    public WhatsAppGroupImporter.Stats importWhatsAppGroups(Context context, Uri uri, ImportMapping mapping) throws Exception { WhatsAppGroupImporter importer = new WhatsAppGroupImporter(database); try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) { if (inputStream == null) { throw new Exception("Could not open file URI"); } return importer.importCsv(inputStream, mapping); } }
    public boolean checkSuperAdminPin(String pin) { return configDao.checkSuperAdminPin(pin); }
    public boolean checkEventCoordinatorPin(String pin) { return configDao.checkEventCoordinatorPin(pin); }
    public List<Event> getAllEvents() { return eventDao.listAll(); }
    public Event getEventById(long eventId) { return eventDao.get(eventId); }
    public long createEvent(String name, String date, String remark, String activeFrom, String activeUntil) throws OverlapException { if (name == null || name.trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); } if (date == null || date.trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); } String finalActiveFrom = activeFrom, finalActiveUntil = activeUntil; if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) { finalActiveFrom = date + " 06:00:00"; } if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) { finalActiveUntil = date + " 22:00:00"; } if (eventDao.hasOverlap(finalActiveFrom, finalActiveUntil, null)) { throw new OverlapException("Time window overlaps with an existing event."); } Event newEvent = new Event(null, null, name, date, finalActiveFrom, finalActiveUntil, remark); long newId = eventDao.insert(newEvent); String code = "EVT-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + "-" + newId; newEvent.setEventId(newId); newEvent.setEventCode(code); eventDao.update(newEvent); return newId; }
    public void updateEvent(Event event) throws OverlapException { if (event == null || event.getEventName() == null || event.getEventName().trim().isEmpty() || event.getEventDate() == null || event.getEventDate().trim().isEmpty()) { throw new IllegalArgumentException("Event details are mandatory."); } if (eventDao.hasOverlap(event.getActiveFromTs(), event.getActiveUntilTs(), event.getEventId())) { throw new OverlapException("Time window overlaps with an existing event."); } eventDao.update(event); }
    public void deleteEvent(long eventId) { eventDao.delete(eventId); }
    public Event getActiveEvent() { return eventDao.findCurrentlyActiveEvent(); }
    public Devotee getDevoteeById(long devoteeId) { return devoteeDao.getById(devoteeId); }
    public void updateDevotee(Devotee devotee) { devoteeDao.update(devotee); }
    public int deleteDevotees(List<Long> devoteeIds) { return devoteeDao.deleteByIds(devoteeIds); }
    public Devotee saveOrMergeDevoteeFromAdmin(Devotee devoteeFromForm) { long finalId = devoteeDao.resolveOrCreateDevotee( devoteeFromForm.getFullName(), devoteeFromForm.getMobileE164(), devoteeFromForm.getAddress(), devoteeFromForm.getAge(), devoteeFromForm.getEmail(), devoteeFromForm.getGender() ); Devotee definitiveRecord = devoteeDao.getById(finalId); definitiveRecord.mergeWith(devoteeFromForm); devoteeDao.update(definitiveRecord); return definitiveRecord; }
    public List<EnrichedDevotee> getAllEnrichedDevotees() { return devoteeDao.getAllEnrichedDevotees(); }
    public boolean markDevoteeAsPresent(long eventId, long devoteeId) { EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devoteeId, eventId); if (status != null && status.count > 0) { return false; } if (status != null) { eventDao.markAsAttended(eventId, devoteeId); } else { eventDao.insertSpotRegistration(eventId, devoteeId); } return true; }
    public long onSpotRegisterAndMarkPresent(long eventId, Devotee newDevoteeData) { Devotee mergedDevotee = saveOrMergeDevoteeFromAdmin(newDevoteeData); markDevoteeAsPresent(eventId, mergedDevotee.getDevoteeId()); return mergedDevotee.getDevoteeId(); }
    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) { return eventDao.findCheckedInAttendeesForEvent(eventId); }
    public DevoteeDao.CounterStats getCounterStats() { return devoteeDao.getCounterStats(); }
    public EventDao.EventStats getEventStats(long eventId) { return eventDao.getEventStats(eventId); }
    public CsvImporter.ImportStats importMasterDevoteeList(Context context, Uri uri, ImportMapping mapping) throws Exception { CsvImporter importer = new CsvImporter(database); CsvImporter.ImportStats stats = new CsvImporter.ImportStats(); try (InputStream inputStream = context.getContentResolver().openInputStream(uri); InputStreamReader reader = new InputStreamReader(inputStream); CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) { if (inputStream == null) { throw new Exception("Could not open file URI"); } database.beginTransaction(); try { Map<String, String> row; while ((row = csvReader.readMap()) != null) { stats.processed++; try { Devotee parsedDevotee = importer.toDevotee(row, mapping); if (parsedDevotee == null) { stats.skipped++; continue; } Devotee existing = devoteeDao.findByKey(parsedDevotee.getMobileE164(), parsedDevotee.getNameNorm()); if (existing != null) { stats.updatedChanged++; } else { stats.inserted++; } saveOrMergeDevoteeFromAdmin(parsedDevotee); } catch (IllegalArgumentException e) { AppLogger.w(TAG, "Skipping bad row in master import: " + row.toString(), e); stats.skipped++; } } database.setTransactionSuccessful(); } finally { database.endTransaction(); } } return stats; }
}
