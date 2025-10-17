// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/settings/SettingsViewModel.java
package com.rkm.rkmattendanceapp.ui.settings;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.ConfigItem;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.util.List;

public class SettingsViewModel extends AndroidViewModel {
    private static final String TAG = "SettingsViewModel";
    private final AttendanceRepository repository;

    private final MutableLiveData<List<ConfigItem>> configItems = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) getApplication()).repository;
    }

    public LiveData<List<ConfigItem>> getConfigItems() { return configItems; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }

    public void loadSettings() {
        new Thread(() -> {
            try {
                // ANNOTATION: This will be implemented in the repository next
                List<ConfigItem> items = repository.getAllEditableConfigs();
                configItems.postValue(items);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load settings", e);
                errorMessage.postValue("Could not load settings.");
            }
        }).start();
    }

    public void saveSetting(String key, String value) {
        new Thread(() -> {
            try {
                // ANNOTATION: This will be implemented in the repository next
                repository.updateConfigValue(key, value);
                saveSuccess.postValue(true);
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to save setting for key: " + key, e);
                errorMessage.postValue("Save failed: " + e.getMessage());
            }
        }).start();
    }
    
    public void onSaveHandled() {
        saveSuccess.setValue(false);
    }
}
