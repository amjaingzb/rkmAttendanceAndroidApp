// In: src/main/java/com/rkm/rkmattendanceapp/ui/ReportsViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.AttendanceApplication;

public class ReportsViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private final MutableLiveData<DevoteeDao.CounterStats> counterStats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<DevoteeDao.CounterStats> getCounterStats() {
        return counterStats;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadStats() {
        new Thread(() -> {
            try {
                DevoteeDao.CounterStats stats = repository.getCounterStats();
                counterStats.postValue(stats);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue("Failed to load statistics.");
            }
        }).start();
    }
}