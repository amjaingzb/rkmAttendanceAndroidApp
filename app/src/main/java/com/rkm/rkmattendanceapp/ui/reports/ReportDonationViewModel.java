// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/ReportDonationViewModel.java
package com.rkm.rkmattendanceapp.ui.reports;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.DailySummary;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.List;

public class ReportDonationViewModel extends AndroidViewModel {
    private static final String TAG = "ReportDonationVM";
    private final AttendanceRepository repository;

    private final MutableLiveData<List<DailySummary>> dailySummaries = new MutableLiveData<>();
    private final MutableLiveData<Uri> shareableFileUri = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ReportDonationViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<DailySummary>> getDailySummaries() { return dailySummaries; }
    public LiveData<Uri> getShareableFileUri() { return shareableFileUri; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    
    public void loadDailySummaries() {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<DailySummary> summaries = repository.getDailyDonationSummaries();
                dailySummaries.postValue(summaries);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load daily donation summaries", e);
                errorMessage.postValue("Failed to load report data.");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    public void exportDonationsForDay(String date) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                String authority = getApplication().getPackageName() + ".fileprovider";
                Uri uri = repository.getDonationsForDateCsvExport(getApplication(), date, authority);
                shareableFileUri.postValue(uri);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to export donations for date: " + date, e);
                errorMessage.postValue("Export failed: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void onShareIntentHandled() {
        shareableFileUri.setValue(null);
    }
}
