// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.List;

public class EventListViewModel extends AndroidViewModel {

    private static final String TAG = "EventListViewModel";
    private final AttendanceRepository repository;
    private final MutableLiveData<List<Event>> eventList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public EventListViewModel(Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<Event>> getEventList() {
        return eventList;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadEvents() {
        new Thread(() -> {
            try {
                List<Event> events = repository.getAllEvents();
                eventList.postValue(events);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load events from repository", e);
                errorMessage.postValue("Failed to load events: " + e.getMessage());
            }
        }).start();
    }

    public void createEvent(String eventName, String eventDate, String remark) {
        new Thread(() -> {
            try {
                repository.createEvent(eventName, eventDate, remark, null, null);
                loadEvents();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to create event", e);
                errorMessage.postValue("Failed to create event: " + e.getMessage());
            }
        }).start();
    }

    public void updateEvent(Event event) {
        new Thread(() -> {
            try {
                repository.updateEvent(event);
                loadEvents();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to update event", e);
                errorMessage.postValue("Failed to update event: " + e.getMessage());
            }
        }).start();
    }

    public void deleteEvent(long eventId) {
        new Thread(() -> {
            try {
                repository.deleteEvent(eventId);
                loadEvents();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to delete event with ID: " + eventId, e);
                errorMessage.postValue("Failed to delete event: " + e.getMessage());
            }
        }).start();
    }

    public void setActiveEvent(long eventId) {
        new Thread(() -> {
            errorMessage.postValue("Set Active Event feature not yet implemented.");
        }).start();
    }
}
