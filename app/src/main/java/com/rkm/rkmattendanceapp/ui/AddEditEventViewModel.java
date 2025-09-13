// In: ui/AddEditEventViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// NEW: Import the custom exception
import com.rkm.attendance.core.OverlapException;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.AttendanceApplication;

public class AddEditEventViewModel extends AndroidViewModel {

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
                e.printStackTrace();
                errorMessage.postValue("Failed to load event details.");
            }
        }).start();
    }

    // MODIFIED: This method now handles the OverlapException.
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
                // This is the specific error for our business rule
                errorMessage.postValue(e.getMessage());
            } catch (Exception e) {
                // This is for all other generic errors
                e.printStackTrace();
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
}