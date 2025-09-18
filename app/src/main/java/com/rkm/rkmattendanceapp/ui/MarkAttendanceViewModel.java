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
import java.util.List;
import com.rkm.attendance.db.EventDao;

public class MarkAttendanceViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private long currentEventId = -1;
    private String lastSearchQuery = null;

    private final MutableLiveData<Event> eventDetails = new MutableLiveData<>();
    private final MutableLiveData<EventDao.EventStats> eventStats = new MutableLiveData<>();
    private final MutableLiveData<List<Devotee>> checkedInList = new MutableLiveData<>();
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MarkAttendanceViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Event> getEventDetails() { return eventDetails; }
    public LiveData<EventDao.EventStats> getEventStats() { return eventStats; }
    public LiveData<List<Devotee>> getCheckedInList() { return checkedInList; }
    public LiveData<List<DevoteeDao.EnrichedDevotee>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadEventData(long eventId) {
        this.currentEventId = eventId;
        new Thread(() -> {
            try {
                Event event = repository.getEventById(eventId);
                eventDetails.postValue(event);
                refreshStatsAndCheckedInList();
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to load event data.");
            }
        }).start();
    }

    public void searchDevotees(String query) {
        this.lastSearchQuery = query;

        if (query == null || query.length() < 3) {
            searchResults.postValue(null);
            return;
        }
        new Thread(() -> {
            try {
                // --- START OF FIX (WhatsApp Icon) ---
                // This now calls the fully-enriched search method from the repository
                List<DevoteeDao.EnrichedDevotee> results = repository.searchDevoteesForEvent(query, currentEventId);
                // --- END OF FIX (WhatsApp Icon) ---
                searchResults.postValue(results);
            } catch (Exception e) {
                e.printStackTrace();
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
                e.printStackTrace();
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
            e.printStackTrace();
            errorMessage.postValue("Failed to refresh data.");
        }
    }
}
