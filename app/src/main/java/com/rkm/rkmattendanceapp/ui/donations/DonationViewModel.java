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
    private final MutableLiveData<List<DonationRecord>> todaysDonations = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DonationViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<Devotee>> getSearchResults() { return searchResults; }
    public LiveData<List<DonationRecord>> getTodaysDonations() { return todaysDonations; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadTodaysDonations() {
        new Thread(() -> {
            try {
                List<DonationRecord> records = repository.getTodaysDonations();
                todaysDonations.postValue(records);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load today's donations", e);
                errorMessage.postValue("Failed to load donations.");
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
                // After deleting, reload the list to reflect the change
                loadTodaysDonations();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to delete donation with ID: " + donationId, e);
                errorMessage.postValue("Failed to delete donation.");
            }
        }).start();
    }
}
