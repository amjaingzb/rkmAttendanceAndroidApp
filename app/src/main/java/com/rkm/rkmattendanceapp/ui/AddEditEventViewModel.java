// In: ui/AddEditEventViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.OverlapException;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class AddEditEventViewModel extends AndroidViewModel {

    private static final String TAG = "AddEditEventViewModel";
    private final AttendanceRepository repository;
    private final MutableLiveData<Event> event = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveFinished = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AddEditEventViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Event> getEvent() { return event; }
    public LiveData<Boolean> getSaveFinished() { return saveFinished; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadEvent(long eventId) {
        new Thread(() -> {
            try {
                Event e = repository.getEventById(eventId);
                event.postValue(e);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load event with ID: " + eventId, e);
                errorMessage.postValue("Failed to load event details.");
            }
        }).start();
    }
    
    public void saveEvent(Event eventToSave) {
        new Thread(() -> {
            try {
                if (eventToSave.getEventId() == null) {
                    repository.createEvent(
                            eventToSave.getEventName(),
                            eventToSave.getEventDate(),
                            eventToSave.getRemark(),
                            eventToSave.getActiveFromTs(),
                            eventToSave.getActiveUntilTs()
                    );
                } else {
                    repository.updateEvent(eventToSave);
                }
                saveFinished.postValue(true);
            } catch (OverlapException e) {
                errorMessage.postValue(e.getMessage());
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to save event", e);
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
}
