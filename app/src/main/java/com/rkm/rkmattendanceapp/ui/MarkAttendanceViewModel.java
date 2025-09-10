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
import com.rkm.attendance.db.EventDao; // Make sure this is imported

public class MarkAttendanceViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private long currentEventId = -1;

    // Data for the UI to observe
    private final MutableLiveData<Event> eventDetails = new MutableLiveData<>();
    private final MutableLiveData<EventDao.EventStats> eventStats = new MutableLiveData<>();
    private final MutableLiveData<List<Devotee>> checkedInList = new MutableLiveData<>();
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MarkAttendanceViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    // --- Getters for the UI ---
    public LiveData<Event> getEventDetails() { return eventDetails; }
    public LiveData<EventDao.EventStats> getEventStats() { return eventStats; }
    public LiveData<List<Devotee>> getCheckedInList() { return checkedInList; }
    public LiveData<List<DevoteeDao.EnrichedDevotee>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // --- Public Methods for the UI to call ---

    public void loadEventData(long eventId) {
        this.currentEventId = eventId;
        new Thread(() -> {
            try {
                // Load everything at once
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
        if (query == null || query.length() < 3) {
            searchResults.postValue(null); // Clear results if query is too short
            return;
        }
        new Thread(() -> {
            try {
                List<DevoteeDao.EnrichedDevotee> results = repository.searchEnrichedDevotees(query);
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
                // After marking, refresh the stats and the checked-in list
                refreshStatsAndCheckedInList();
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to mark attendance.");
            }
        }).start();
    }

    // --- Private Helper ---
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