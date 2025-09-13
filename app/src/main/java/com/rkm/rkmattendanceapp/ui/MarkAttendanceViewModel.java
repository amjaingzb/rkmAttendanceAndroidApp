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
    // NEW: A variable to remember the last valid search query.
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
        // NEW: Store the latest query.
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
                e.printStackTrace();
                errorMessage.postValue("Search failed.");
            }
        }).start();
    }

    // MODIFIED: This method now re-runs the search after marking attendance.
    public void markAttendance(long devoteeId) {
        new Thread(() -> {
            try {
                repository.markDevoteeAsPresent(currentEventId, devoteeId);
                // First, refresh the stats and the main checked-in list as before.
                refreshStatsAndCheckedInList();

                // NEW: Now, re-run the last search to update the search results UI in place.
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

    public void onSpotRegisterAndMarkPresent(Devotee newDevotee) {
        if (currentEventId == -1) {
            errorMessage.postValue("Error: No active event to register for.");
            return;
        }
        new Thread(() -> {
            try {
                repository.onSpotRegisterAndMarkPresent(currentEventId, newDevotee);
                refreshStatsAndCheckedInList();
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("On-spot registration failed: " + e.getMessage());
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