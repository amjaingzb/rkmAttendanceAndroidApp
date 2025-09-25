// In: ui/MarkAttendanceViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.List;
import com.rkm.attendance.db.EventDao;

public class MarkAttendanceViewModel extends AndroidViewModel {

    private static final String TAG = "MarkAttendanceVM";
    private final AttendanceRepository repository;
    private long currentEventId = -1;
    private String lastSearchQuery = null;

    private final MutableLiveData<Event> eventDetails = new MutableLiveData<>();
    private final MutableLiveData<EventDao.EventStats> eventStats = new MutableLiveData<>();
    private final MutableLiveData<List<Devotee>> checkedInList = new MutableLiveData<>();
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<AttendanceRepository.WhatsAppInvite> whatsAppInvite = new MutableLiveData<>();

    private final MutableLiveData<DevoteeDao.EnrichedDevotee> newlyAddedDevotee = new MutableLiveData<>();

    public MarkAttendanceViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Event> getEventDetails() { return eventDetails; }
    public LiveData<EventDao.EventStats> getEventStats() { return eventStats; }
    public LiveData<List<Devotee>> getCheckedInList() { return checkedInList; }
    public LiveData<List<DevoteeDao.EnrichedDevotee>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<AttendanceRepository.WhatsAppInvite> getWhatsAppInvite() { return whatsAppInvite; }
    public LiveData<DevoteeDao.EnrichedDevotee> getNewlyAddedDevotee() { return newlyAddedDevotee; }

    public void loadEventData(long eventId) {
        this.currentEventId = eventId;
        new Thread(() -> {
            try {
                Event event = repository.getEventById(eventId);
                eventDetails.postValue(event);
                refreshStatsAndCheckedInList();
                AttendanceRepository.WhatsAppInvite invite = repository.getWhatsAppInviteDetails();
                whatsAppInvite.postValue(invite);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load data for event ID: " + eventId, e);
                errorMessage.postValue("Failed to load event data.");
            }
        }).start();
    }

    public void loadNewlyAddedDevotee(long devoteeId) {
        new Thread(() -> {
            try {
                DevoteeDao.EnrichedDevotee devotee = repository.getEnrichedDevoteeById(devoteeId, currentEventId);
                newlyAddedDevotee.postValue(devotee);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load newly added devotee with ID: " + devoteeId, e);
                errorMessage.postValue("Failed to load devotee details.");
            }
        }).start();
    }
    
    public void onDialogShown() {
        newlyAddedDevotee.setValue(null);
    }

    public void searchDevotees(String query) {
        this.lastSearchQuery = query;
        if (query == null || query.length() < 3) {
            searchResults.postValue(null);
            return;
        }
        new Thread(() -> {
            try {
                List<DevoteeDao.EnrichedDevotee> results = repository.searchDevoteesForEvent(query, currentEventId);
                searchResults.postValue(results);
            } catch (Exception e) {
                AppLogger.e(TAG, "Search failed for query: " + query, e);
                errorMessage.postValue("Search failed.");
            }
        }).start();
    }
    
    public void markAttendance(long devoteeId) {
        new Thread(() -> {
            try {
                repository.markDevoteeAsPresent(currentEventId, devoteeId);
                refreshStatsAndCheckedInList();

                if (lastSearchQuery != null && lastSearchQuery.length() >= 3) {
                    List<DevoteeDao.EnrichedDevotee> updatedResults = repository.searchDevoteesForEvent(lastSearchQuery, currentEventId);
                    searchResults.postValue(updatedResults);
                }

            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to mark attendance for devotee ID: " + devoteeId, e);
                errorMessage.postValue("Failed to mark attendance.");
            }
        }).start();
    }
    
    private void refreshStatsAndCheckedInList() {
        try {
            EventDao.EventStats stats = repository.getEventStats(currentEventId);
            List<Devotee> checkedIn = repository.getCheckedInAttendeesForEvent(currentEventId);
            eventStats.postValue(stats);
            checkedInList.postValue(checkedIn);
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to refresh stats/checked-in list for event ID: " + currentEventId, e);
            errorMessage.postValue("Failed to refresh data.");
        }
    }
}
