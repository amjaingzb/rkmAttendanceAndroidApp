// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDevoteeViewModel.java
package com.rkm.rkmattendanceapp.ui.reports;

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
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.util.List;
import java.util.stream.Collectors;

public class ReportDevoteeViewModel extends AndroidViewModel {

    private static final String TAG = "ReportDevoteeVM";
    private final AttendanceRepository repository;
    private final MutableLiveData<List<DevoteeDao.EnrichedDevotee>> devoteeList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Uri> shareableFileUri = new MutableLiveData<>();
    private List<DevoteeDao.EnrichedDevotee> cachedList = null; // Cache for export

    public ReportDevoteeViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<DevoteeDao.EnrichedDevotee>> getDevoteeList() { return devoteeList; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Uri> getShareableFileUri() { return shareableFileUri; }

    public void loadDevoteeActivity() {
        AppLogger.d(TAG, "Loading devotee activity report...");
        new Thread(() -> {
            try {
                List<DevoteeDao.EnrichedDevotee> devotees = repository.getAllEnrichedDevotees();
                // Sort the list by cumulative attendance, descending
                List<DevoteeDao.EnrichedDevotee> sortedList = devotees.stream()
                        .sorted((d1, d2) -> Integer.compare(d2.cumulativeAttendance(), d1.cumulativeAttendance()))
                        .collect(Collectors.toList());
                
                cachedList = sortedList; // Cache the sorted list
                devoteeList.postValue(sortedList);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load devotee activity report", e);
                errorMessage.postValue("Failed to load report data.");
            }
        }).start();
    }
    
    public void startDevoteeActivityExport() {
        AppLogger.d(TAG, "Starting devotee activity export...");
        new Thread(() -> {
            if (cachedList == null || cachedList.isEmpty()) {
                errorMessage.postValue("No data to export.");
                return;
            }
            try {
                CsvExporter exporter = new CsvExporter();
                String authority = getApplication().getPackageName() + ".fileprovider";
                Uri uri = exporter.exportDevotees(getApplication(), cachedList, authority);
                shareableFileUri.postValue(uri);
            } catch (Exception e) {
                AppLogger.e(TAG, "Export failed", e);
                errorMessage.postValue("Export failed: " + e.getMessage());
            }
        }).start();
    }

    public void onShareIntentHandled() {
        shareableFileUri.setValue(null);
    }
}
