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
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DevoteeListViewModel extends AndroidViewModel {

    private static final String TAG = "DevoteeListViewModel";
    private final AttendanceRepository repository;
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> devoteeList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private List<DevoteeDao.EnrichedDevotee> fullDevoteeList = new ArrayList<>();

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
                fullDevoteeList = repository.getAllEnrichedDevotees();
                devoteeList.postValue(fullDevoteeList);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load all devotees", e);
                errorMessage.postValue("Failed to load devotees: " + e.getMessage());
            }
        }).start();
    }

    public void filterDevotees(String query) {
        if (query == null || query.trim().isEmpty()) {
            devoteeList.setValue(fullDevoteeList);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();

        List<DevoteeDao.EnrichedDevotee> filteredList = fullDevoteeList.stream()
                .filter(enriched -> {
                    String name = enriched.devotee().getFullName().toLowerCase();
                    String mobile = enriched.devotee().getMobileE164();
                    return name.contains(lowerCaseQuery) || mobile.contains(lowerCaseQuery);
                })
                .collect(Collectors.toList());

        devoteeList.setValue(filteredList);
    }
}
