// In: src/main/java/com/rkm/rkmattendanceapp/ui/DevoteeListViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import java.util.List;

public class DevoteeListViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> devoteeList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DevoteeListViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<DevoteeDao.EnrichedDevotee>> getDevoteeList() {
        return devoteeList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadAllDevotees() {
        new Thread(() -> {
            try {
                // Use the repository method to get all devotees
                List<DevoteeDao.EnrichedDevotee> devotees = repository.getAllEnrichedDevotees();
                devoteeList.postValue(devotees);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to load devotees: " + e.getMessage());
            }
        }).start();
    }
}