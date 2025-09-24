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
import com.rkm.rkmattendanceapp.util.AppLogger;

public class AddEditDevoteeViewModel extends AndroidViewModel {

    private static final String TAG = "AddEditDevoteeVM";
    private final AttendanceRepository repository;
    private final MutableLiveData<Devotee> devotee = new MutableLiveData<>();
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

    public void loadDevotee(long devoteeId) {
        new Thread(() -> {
            try {
                Devotee d = repository.getDevoteeById(devoteeId);
                devotee.postValue(d);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load devotee with ID: " + devoteeId, e);
                errorMessage.postValue("Failed to load devotee details.");
            }
        }).start();
    }

    public void saveDevotee(Devotee devoteeToSave, boolean isOnSpotRegistration, long eventId) {
        new Thread(() -> {
            try {
                if (isOnSpotRegistration) {
                    repository.onSpotRegisterAndMarkPresent(eventId, devoteeToSave);
                } else {
                    if (devoteeToSave.getDevoteeId() == null) {
                        repository.saveOrMergeDevoteeFromAdmin(devoteeToSave);
                    } else {
                        repository.updateDevotee(devoteeToSave);
                    }
                }
                saveFinished.postValue(true);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to save devotee", e);
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
}
