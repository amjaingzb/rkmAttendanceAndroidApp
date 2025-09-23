// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportEventListViewModel.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.db.EventDao;
import com.rkm.attendance.importer.CsvExporter;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;
import java.util.List;

public class ReportEventListViewModel extends AndroidViewModel {
    private static final String TAG = "ReportEventListVM";
    private final AttendanceRepository repository;
    private final MutableLiveData<List<EventDao.EventWithAttendance>> eventList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Uri> shareableFileUri = new MutableLiveData<>();

    public ReportEventListViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<EventDao.EventWithAttendance>> getEventList() { return eventList; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Uri> getShareableFileUri() { return shareableFileUri; }

    public void loadEventsWithAttendance() {
        AppLogger.d(TAG, "Loading events with attendance counts...");
        new Thread(() -> {
            try {
                List<EventDao.EventWithAttendance> events = repository.getEventsWithAttendance();
                eventList.postValue(events);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load events with attendance", e);
                errorMessage.postValue("Failed to load event list.");
            }
        }).start();
    }

    public void startAttendanceExport(long eventId) {
        AppLogger.d(TAG, "Starting attendance export for eventId: " + eventId);
        new Thread(() -> {
            try {
                List<Devotee> attendees = repository.getCheckedInAttendeesForEvent(eventId);
                if (attendees == null || attendees.isEmpty()) {
                    errorMessage.postValue("No attendees to export for this event.");
                    return;
                }
                CsvExporter exporter = new CsvExporter();
                String authority = getApplication().getPackageName() + ".fileprovider";
                Uri uri = exporter.exportSimpleDevoteeList(getApplication(), attendees, authority);
                shareableFileUri.postValue(uri);
            } catch (Exception e) {
                AppLogger.e(TAG, "Export process failed for eventId: " + eventId, e);
                errorMessage.postValue("Export failed: " + e.getMessage());
            }
        }).start();
    }
    
    public void onShareIntentHandled() {
        shareableFileUri.setValue(null);
    }
}
