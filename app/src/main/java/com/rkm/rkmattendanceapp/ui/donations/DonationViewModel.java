// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationViewModel.java
package com.rkm.rkmattendanceapp.ui.donations;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Devotee;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.List;

public class DonationViewModel extends AndroidViewModel {

    private static final String TAG = "DonationViewModel";
    private final AttendanceRepository repository;

    private final MutableLiveData<List<Devotee>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<AttendanceRepository.ActiveBatchData> activeBatchData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> batchClosed = new MutableLiveData<>(false);

    public DonationViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<Devotee>> getSearchResults() { return searchResults; }
    public LiveData<AttendanceRepository.ActiveBatchData> getActiveBatchData() { return activeBatchData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getBatchClosed() { return batchClosed; }

    public void loadOrRefreshActiveBatch() {
        new Thread(() -> {
            try {
                AttendanceRepository.ActiveBatchData data = repository.getActiveDonationBatchOrCreateNew();
                activeBatchData.postValue(data);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load active batch data", e);
                errorMessage.postValue("Failed to load active batch.");
            }
        }).start();
    }
    
    public void closeActiveBatch() {
        AttendanceRepository.ActiveBatchData currentData = activeBatchData.getValue();
        if (currentData == null || currentData.batch == null) {
            errorMessage.postValue("No active batch to close.");
            return;
        }
        new Thread(() -> {
            try {
                repository.closeActiveBatch(currentData.batch.batchId, "DonationCollector");
                batchClosed.postValue(true);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to close batch", e);
                errorMessage.postValue("Failed to close batch.");
            }
        }).start();
    }

    public void searchDevotees(String query) {
        if (query == null || query.length() < 3) {
            searchResults.postValue(null);
            return;
        }
        new Thread(() -> {
            try {
                List<Devotee> results = repository.searchSimpleDevotees(query);
                searchResults.postValue(results);
            } catch (Exception e) {
                AppLogger.e(TAG, "Search failed for query: " + query, e);
                errorMessage.postValue("Search failed.");
            }
        }).start();
    }
    
    public void deleteDonation(long donationId) {
        new Thread(() -> {
            try {
                repository.deleteDonation(donationId);
                loadOrRefreshActiveBatch();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to delete donation with ID: " + donationId, e);
                errorMessage.postValue("Failed to delete donation.");
            }
        }).start();
    }
}
