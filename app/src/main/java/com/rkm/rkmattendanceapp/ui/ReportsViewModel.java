// In: src/main/java/com/rkm/rkmattendanceapp/ui/ReportsViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.DevoteeDao;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

public class ReportsViewModel extends AndroidViewModel {

    private static final String TAG = "ReportsViewModel";
    private final AttendanceRepository repository;
    private final MutableLiveData<DevoteeDao.CounterStats> counterStats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // This LiveData is no longer used by this fragment, but we keep it for other reports.
    private final MutableLiveData<Uri> shareableFileUri = new MutableLiveData<>();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<DevoteeDao.CounterStats> getCounterStats() { return counterStats; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Uri> getShareableFileUri() { return shareableFileUri; }

    public void loadStats() {
        AppLogger.d(TAG, "Loading stats...");
        new Thread(() -> {
            try {
                DevoteeDao.CounterStats stats = repository.getCounterStats();
                counterStats.postValue(stats);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load statistics.", e);
                errorMessage.postValue("Failed to load statistics.");
            }
        }).start();
    }
    
    // The startDevoteeExport() method has been removed as it is now redundant.
}
