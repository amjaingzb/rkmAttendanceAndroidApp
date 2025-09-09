// In: src/main/java/com/rkm/rkmattendanceapp/ui/EventListViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;

import java.util.List;

public class EventListViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;

    // This is the data that the UI will observe.
    // We use MutableLiveData internally to post new values.
    private final MutableLiveData<List<Event>> eventList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public EventListViewModel(Application application) {
        super(application);
        // Get the single, shared repository instance from our Application class.
        this.repository = ((AttendanceApplication) application).repository;
    }

    // The UI will call this method to get the data.
    // We expose it as LiveData so the UI can't accidentally change it.
    public LiveData<List<Event>> getEventList() {
        return eventList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // This method triggers the data fetch from the database.
    public void loadEvents() {
        // IMPORTANT: Database operations must happen on a background thread.
        // We will create a simple new thread for now. In a more complex app,
        // we would use Coroutines (Kotlin) or Executors.
        new Thread(() -> {
            try {
                List<Event> events = repository.getAllEvents();
                // Post the result back to the main thread for the UI to observe.
                eventList.postValue(events);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to load events: " + e.getMessage());
            }
        }).start();
    }
}