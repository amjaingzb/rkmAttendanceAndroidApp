// In: src/main/java/com/rkm/attendance/core/AttendanceRepository.java
package com.rkm.attendance.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.opencsv.CSVReaderHeaderAware;
import com.rkm.attendance.db.*;
import com.rkm.attendance.db.DevoteeDao.EnrichedDevotee;
import com.rkm.attendance.model.*;
import com.rkm.rkmattendanceapp.ui.EventStatus;
import com.rkm.attendance.importer.AttendanceImporter;
import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.attendance.importer.WhatsAppGroupImporter;
import com.rkm.rkmattendanceapp.ui.donations.DonationRecord;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final DonationDao donationDao;
    private final DonationBatchDao donationBatchDao;
    private final SQLiteDatabase database;
    private static final String TAG = "AttendanceRepository";
    private static final DateTimeFormatter SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AttendanceRepository(SQLiteDatabase database) {
        this.database = database;
        this.devoteeDao = new DevoteeDao(database);
        this.eventDao = new EventDao(database);
        this.whatsAppGroupDao = new WhatsAppGroupDao(database);
        this.configDao = new ConfigDao(database);
        this.donationDao = new DonationDao(database);
        this.donationBatchDao = new DonationBatchDao(database);
    }
    
    public static class ActiveBatchData {
        public final DonationBatch batch;
        public final DonationDao.BatchSummary summary;
        public final List<DonationRecord> donations;

        public ActiveBatchData(DonationBatch batch, DonationDao.BatchSummary summary, List<DonationRecord> donations) {
            this.batch = batch;
            this.summary = summary;
            this.donations = donations;
        }
    }
    
    // --- DONATION BATCH WORKFLOW METHODS ---
    public ActiveBatchData getActiveDonationBatchOrCreateNew() {
        DonationBatch activeBatch = donationBatchDao.findActiveBatch();
        if (activeBatch == null) {
            // FIX: Generate local timestamp here and pass it to the DAO
            String now = SQL_FORMATTER.format(LocalDateTime.now());
            long newBatchId = donationBatchDao.insertNewBatch(now);
            activeBatch = donationBatchDao.findActiveBatch(); // Re-fetch to get all details
        }
        
        DonationDao.BatchSummary summary = donationDao.getBatchSummary(activeBatch.batchId);
        List<DonationRecord> donations = donationDao.getDonationsForBatch(activeBatch.batchId);
        
        return new ActiveBatchData(activeBatch, summary, donations);
    }

    public void recordDonationInBatch(long devoteeId, double amount, String paymentMethod, String refId, String purpose, String user, long batchId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        long donationId = donationDao.insert(devoteeId, null, amount, paymentMethod, refId, purpose, user, batchId, null);
        String receiptNumber = String.format(Locale.US, "RKM-%s-%d", timestamp, donationId);
        ContentValues values = new ContentValues();
        values.put("receipt_number", receiptNumber);
        database.update("donations", values, "donation_id = ?", new String[]{String.valueOf(donationId)});
    }
    
    public void deleteDonation(long donationId) {
        donationDao.delete(donationId);
    }
    
    public void closeActiveBatch(long batchId, String user) {
        donationBatchDao.closeBatch(batchId, user);
    }
    
    public List<Devotee> searchSimpleDevotees(String query) {
        return devoteeDao.searchSimpleDevotees(query);
    }
    // --- END DONATION BATCH WORKFLOW METHODS ---
    
    // ... other existing methods ...
    public static class WhatsAppInvite { public final String link; public final String messageTemplate; public WhatsAppInvite(String link, String messageTemplate) { this.link = link; this.messageTemplate = messageTemplate; } }
    public WhatsAppInvite getWhatsAppInviteDetails() { String link = configDao.getValue(ConfigDao.KEY_WHATSAPP_INVITE_LINK); String message = configDao.getValue(ConfigDao.KEY_WHATSAPP_INVITE_MESSAGE); return new WhatsAppInvite(link, message); }
    public EnrichedDevotee getEnrichedDevoteeById(long devoteeId, long eventId) { Devotee devotee = devoteeDao.getById(devoteeId); if (devotee == null) return null; EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devotee.getDevoteeId(), eventId); EventStatus eventStatus = (status == null) ? EventStatus.WALK_IN : (status.count > 0 ? EventStatus.PRESENT : EventStatus.PRE_REGISTERED); Integer whatsAppGroup = devoteeDao.getWhatsAppGroup(devotee.getMobileE164()); return new EnrichedDevotee(devotee, whatsAppGroup, 0, null, eventStatus); }
    public boolean checkSuperAdminPin(String pin) { return configDao.checkSuperAdminPin(pin); }
    public boolean checkEventCoordinatorPin(String pin) { return configDao.checkEventCoordinatorPin(pin); }
    public boolean checkDonationCollectorPin(String pin) { return configDao.checkDonationCollectorPin(pin); }
    public List<Event> getAllEvents() { return eventDao.listAll(); }
    public Event getEventById(long eventId) { return eventDao.get(eventId); }
    public long createEvent(String name, String date, String remark, String activeFrom, String activeUntil) throws OverlapException { if (name == null || name.trim().isEmpty()) { throw new IllegalArgumentException("Event Name is mandatory."); } if (date == null || date.trim().isEmpty()) { throw new IllegalArgumentException("Event Date is mandatory."); } String finalActiveFrom = activeFrom, finalActiveUntil = activeUntil; if (finalActiveFrom == null || finalActiveFrom.trim().isEmpty()) { finalActiveFrom = date + " 06:00:00"; } if (finalActiveUntil == null || finalActiveUntil.trim().isEmpty()) { finalActiveUntil = date + " 22:00:00"; } if (eventDao.hasOverlap(finalActiveFrom, finalActiveUntil, null)) { throw new OverlapException("Time window overlaps with an existing event."); } Event newEvent = new Event(null, null, name, date, finalActiveFrom, finalActiveUntil, remark); long newId = eventDao.insert(newEvent); String code = "EVT-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + "-" + newId; newEvent.setEventId(newId); newEvent.setEventCode(code); eventDao.update(newEvent); return newId; }
    public void updateEvent(Event event) throws OverlapException { if (event == null || event.getEventName() == null || event.getEventName().trim().isEmpty() || event.getEventDate() == null || event.getEventDate().trim().isEmpty()) { throw new IllegalArgumentException("Event details are mandatory."); } if (eventDao.hasOverlap(event.getActiveFromTs(), event.getActiveUntilTs(), event.getEventId())) { throw new OverlapException("Time window overlaps with an existing event."); } eventDao.update(event); }
    public void deleteEvent(long eventId) { eventDao.delete(eventId); }
    public Event getActiveEvent() { return eventDao.findCurrentlyActiveEvent(); }
    public Devotee getDevoteeById(long devoteeId) { return devoteeDao.getById(devoteeId); }
    public void updateDevotee(Devotee devotee) { devoteeDao.update(devotee); }
    public int deleteDevotees(List<Long> devoteeIds) { return devoteeDao.deleteByIds(devoteeIds); }
    public Devotee saveOrMergeDevoteeFromAdmin(Devotee devoteeFromForm) { long finalId = devoteeDao.resolveOrCreateDevotee( devoteeFromForm.getFullName(), devoteeFromForm.getMobileE164(), devoteeFromForm.getAddress(), devoteeFromForm.getAge(), devoteeFromForm.getEmail(), devoteeFromForm.getGender(), devoteeFromForm.getAadhaar(), devoteeFromForm.getPan() ); Devotee definitiveRecord = devoteeDao.getById(finalId); definitiveRecord.mergeWith(devoteeFromForm); devoteeDao.update(definitiveRecord); return definitiveRecord; }
    public List<EnrichedDevotee> getAllEnrichedDevotees() { return devoteeDao.getAllEnrichedDevotees(); }
    public Devotee addNewDevoteeOnSpot(Devotee newDevoteeData) { return saveOrMergeDevoteeFromAdmin(newDevoteeData); }
    public boolean markDevoteeAsPresent(long eventId, long devoteeId) { EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devoteeId, eventId); if (status != null && status.count > 0) { return false; } if (status != null) { eventDao.markAsAttended(eventId, devoteeId); } else { eventDao.insertSpotRegistration(eventId, devoteeId); } return true; }
    public List<Devotee> getCheckedInAttendeesForEvent(long eventId) { return eventDao.findCheckedInAttendeesForEvent(eventId); }
    public List<EnrichedDevotee> searchDevoteesForEvent(String query, long eventId) { List<Devotee> allMatches = devoteeDao.searchSimpleDevotees(query); return allMatches.stream() .map(devotee -> { EventDao.AttendanceStatus status = eventDao.getAttendanceStatus(devotee.getDevoteeId(), eventId); EventStatus eventStatus = (status == null) ? EventStatus.WALK_IN : (status.count > 0 ? EventStatus.PRESENT : EventStatus.PRE_REGISTERED); Integer whatsAppGroup = devoteeDao.getWhatsAppGroup(devotee.getMobileE164()); return new EnrichedDevotee(devotee, whatsAppGroup, 0, null, eventStatus); }) .sorted(Comparator.comparingInt((EnrichedDevotee e) -> e.getEventStatus() == EventStatus.PRESENT ? 1 : 0) .thenComparing(e -> e.devotee().getFullName(), String.CASE_INSENSITIVE_ORDER)) .collect(Collectors.toList()); }
    public DevoteeDao.CounterStats getCounterStats() { return devoteeDao.getCounterStats(); }
    public EventDao.EventStats getEventStats(long eventId) { return eventDao.getEventStats(eventId); }
    public List<EventDao.EventWithAttendance> getEventsWithAttendance() { return eventDao.getEventsWithAttendanceCounts(); }
    public CsvImporter.ImportStats importMasterDevoteeList(Context context, Uri uri, ImportMapping mapping) throws Exception { CsvImporter importer = new CsvImporter(database); CsvImporter.ImportStats stats = new CsvImporter.ImportStats(); try (InputStream inputStream = context.getContentResolver().openInputStream(uri); InputStreamReader reader = new InputStreamReader(inputStream); CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) { if (inputStream == null) { throw new Exception("Could not open file URI"); } database.beginTransaction(); try { Map<String, String> row; while ((row = csvReader.readMap()) != null) { stats.processed++; try { Devotee parsedDevotee = importer.toDevotee(row, mapping); if (parsedDevotee == null) { stats.skipped++; continue; } Devotee existing = devoteeDao.findByKey(parsedDevotee.getMobileE164(), parsedDevotee.getNameNorm()); if (existing != null) { stats.updatedChanged++; } else { stats.inserted++; } saveOrMergeDevoteeFromAdmin(parsedDevotee); } catch (IllegalArgumentException e) { AppLogger.w(TAG, "Skipping bad row in master import: " + row.toString(), e); stats.skipped++; } } database.setTransactionSuccessful(); } finally { database.endTransaction(); } } return stats; }
    public CsvImporter.ImportStats importAttendanceList(Context context, Uri uri, ImportMapping mapping, long eventId) throws Exception { AttendanceImporter importer = new AttendanceImporter(); CsvImporter.ImportStats stats = new CsvImporter.ImportStats(); List<Map<String, String>> allRows = new ArrayList<>(); try (InputStream inputStream = context.getContentResolver().openInputStream(uri); InputStreamReader reader = new InputStreamReader(inputStream); CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) { Map<String, String> row; while((row = csvReader.readMap()) != null) { allRows.add(row); } } if (allRows.isEmpty()) { return stats; } database.beginTransaction(); try { for (Map<String, String> row : allRows) { stats.processed++; try { AttendanceImporter.ParsedAttendanceRow parsedRow = importer.parseRow(row, mapping); if (parsedRow == null) { stats.skipped++; continue; } saveOrMergeDevoteeFromAdmin(parsedRow.devotee); } catch (IllegalArgumentException e) { AppLogger.w(TAG, "Skipping bad devotee row during pass 1 of attendance import: " + row, e); stats.skipped++; } } database.setTransactionSuccessful(); } finally { database.endTransaction(); } database.beginTransaction(); try { for (Map<String, String> row : allRows) { try { AttendanceImporter.ParsedAttendanceRow parsedRow = importer.parseRow(row, mapping); if (parsedRow == null) continue; Devotee devotee = devoteeDao.findByKey(parsedRow.devotee.getMobileE164(), parsedRow.devotee.getNameNorm()); if (devotee == null) { AppLogger.w(TAG, "Could not find devotee in pass 2, skipping attendance link: " + parsedRow.devotee.getFullName()); continue; } eventDao.upsertAttendance(eventId, devotee.getDevoteeId(), "PRE_REG", parsedRow.count, "Imported"); stats.inserted++; } catch (Exception e) { AppLogger.w(TAG, "Skipping attendance link row during pass 2: " + row, e); } } database.setTransactionSuccessful(); } finally { database.endTransaction(); } stats.processed = allRows.size(); return stats; }
    public WhatsAppGroupImporter.Stats importWhatsAppGroups(Context context, Uri uri, ImportMapping mapping) throws Exception { WhatsAppGroupImporter importer = new WhatsAppGroupImporter(database); try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) { if (inputStream == null) { throw new Exception("Could not open file URI"); } return importer.importCsv(inputStream, mapping); } }
}
