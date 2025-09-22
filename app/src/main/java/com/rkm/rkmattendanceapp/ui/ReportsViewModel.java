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
import com.rkm.attendance.importer.CsvExporter;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.R;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.util.List;

public class ReportsViewModel extends AndroidViewModel {

    private static final String TAG = "ReportsViewModel";
    private final AttendanceRepository repository;
    private final MutableLiveData<DevoteeDao.CounterStats> counterStats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
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

    public void startDevoteeExport() {
        AppLogger.d(TAG, "Starting devotee export process...");
        new Thread(() -> {
            try {
                List<DevoteeDao.EnrichedDevotee> allDevotees = repository.getAllEnrichedDevotees();
                AppLogger.d(TAG, "Fetched " + allDevotees.size() + " devotees for export.");

                if (allDevotees.isEmpty()) {
                    errorMessage.postValue("No devotee records to export.");
                    return;
                }

                CsvExporter exporter = new CsvExporter();
                
                // --- START OF FIX ---
                // Construct the authority string here, where we have access to the context
                String authority = getApplication().getPackageName() + ".fileprovider";
                Uri uri = exporter.exportDevotees(getApplication(), allDevotees, authority);
                // --- END OF FIX ---
                
                shareableFileUri.postValue(uri);

            } catch (Exception e) {
                AppLogger.e(TAG, "Export process failed.", e);
                errorMessage.postValue("Export failed: " + e.getMessage());
            }
        }).start();
    }

    public void onShareIntentHandled() {
        shareableFileUri.setValue(null);
    }
}
