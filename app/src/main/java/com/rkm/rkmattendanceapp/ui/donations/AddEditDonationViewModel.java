// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/AddEditDonationViewModel.java
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

public class AddEditDonationViewModel extends AndroidViewModel {
    private static final String TAG = "AddEditDonationVM";
    private final AttendanceRepository repository;

    private final MutableLiveData<Devotee> devotee = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveFinished = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AddEditDonationViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Devotee> getDevotee() { return devotee; }
    public LiveData<Boolean> getSaveFinished() { return saveFinished; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadDevotee(long devoteeId) {
        new Thread(() -> {
            try {
                Devotee d = repository.getDevoteeById(devoteeId);
                devotee.postValue(d);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load devotee with ID: " + devoteeId, e);
                errorMessage.postValue("Failed to load devotee details.");
            }
        }).start();
    }

    public void saveDonation(long devoteeId, double amount, String paymentMethod, String refId, String purpose) {
        new Thread(() -> {
            try {
                repository.recordDonation(devoteeId, null, amount, paymentMethod, refId, purpose, "DonationCollector");
                saveFinished.postValue(true);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to save donation", e);
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
}
