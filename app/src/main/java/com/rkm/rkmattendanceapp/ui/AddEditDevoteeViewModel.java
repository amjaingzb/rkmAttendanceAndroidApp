// In: src/main/java/com/rkm/rkmattendanceapp/ui/AddEditDevoteeViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.AttendanceApplication;

public class AddEditDevoteeViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;

    // To hold the devotee being edited
    private final MutableLiveData<Devotee> devotee = new MutableLiveData<>();
    
    // To signal when the save operation is complete
    private final MutableLiveData<Boolean> saveFinished = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AddEditDevoteeViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Devotee> getDevotee() {
        return devotee;
    }

    public LiveData<Boolean> getSaveFinished() {
        return saveFinished;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Load a devotee for editing
    public void loadDevotee(long devoteeId) {
        new Thread(() -> {
            try {
                Devotee d = repository.getDevoteeById(devoteeId);
                devotee.postValue(d);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to load devotee details.");
            }
        }).start();
    }

    // Save a new or updated devotee
    public void saveDevotee(Devotee devoteeToSave, boolean isOnSpotRegistration, long eventId) {
        new Thread(() -> {
            try {
                if (isOnSpotRegistration) {
                    // Use the special method that does both actions
                    repository.onSpotRegisterAndMarkPresent(eventId, devoteeToSave);
                } else {
                    // Use the original Admin logic
                    if (devoteeToSave.getDevoteeId() == null) {
                        repository.addNewDevotee(devoteeToSave);
                    } else {
                        repository.updateDevotee(devoteeToSave);
                    }
                }
                saveFinished.postValue(true);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
}