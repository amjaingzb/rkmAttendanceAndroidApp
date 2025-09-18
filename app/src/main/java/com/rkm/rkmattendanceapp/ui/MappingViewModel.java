// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/MappingViewModel.java
package com.rkm.rkmattendanceapp.ui;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.importer.CsvImporter;
import com.rkm.attendance.importer.ImportMapping;
import com.rkm.attendance.importer.WhatsAppGroupImporter;
import com.rkm.rkmattendanceapp.AttendanceApplication;

public class MappingViewModel extends AndroidViewModel {

    private final AttendanceRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<CsvImporter.ImportStats> importStats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MappingViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<CsvImporter.ImportStats> getImportStats() { return importStats; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void startDevoteeImport(Uri fileUri, ImportMapping mapping) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                CsvImporter.ImportStats stats = repository.importMasterDevoteeList(getApplication(), fileUri, mapping);
                importStats.postValue(stats);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    public void startAttendanceImport(Uri fileUri, ImportMapping mapping, long eventId) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                CsvImporter.ImportStats stats = repository.importAttendanceList(getApplication(), fileUri, mapping, eventId);
                importStats.postValue(stats);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // --- START OF FIX #2 ---
    // This method now correctly receives the stats and translates them for the UI.
    public void startWhatsAppImport(Uri fileUri, ImportMapping mapping) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                WhatsAppGroupImporter.Stats whatsAppStats = repository.importWhatsAppGroups(getApplication(), fileUri, mapping);
                
                // Translate from WhatsApp stats to the generic CsvImporter stats for the dialog
                CsvImporter.ImportStats finalStats = new CsvImporter.ImportStats();
                finalStats.processed = whatsAppStats.processed;
                finalStats.updatedChanged = whatsAppStats.insertedOrUpdated; // We'll show "updated" for clarity
                finalStats.skipped = whatsAppStats.skipped;
                
                importStats.postValue(finalStats);

            } catch (Exception e) {
                e.printStackTrace();
                errorMessage.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    // --- END OF FIX #2 ---
}
