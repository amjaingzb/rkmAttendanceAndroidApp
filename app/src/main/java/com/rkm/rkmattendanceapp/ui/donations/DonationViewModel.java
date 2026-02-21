package com.rkm.rkmattendanceapp.ui.donations;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rkm.attendance.core.AttendanceRepository;
import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Donation;
import com.rkm.rkmattendanceapp.AttendanceApplication;
import com.rkm.rkmattendanceapp.util.AppLogger;

import java.io.InputStream;
import java.util.List;

// New imports for Receipt generation
import com.rkm.rkmattendanceapp.util.NumberToWords;
import com.rkm.rkmattendanceapp.util.PdfReceiptGenerator;
import com.rkm.rkmattendanceapp.ui.reports.models.DonationReportModels.FullDonationRecord;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import android.util.Pair; // Import this


public class DonationViewModel extends AndroidViewModel {

    private static final String TAG = "DonationViewModel";
    private final AttendanceRepository repository;

    private final MutableLiveData<List<Devotee>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<AttendanceRepository.ActiveBatchData> activeBatchData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> batchClosedEvent = new MutableLiveData<>(false);

    // Receipt LiveData
    // The Pair will hold <FileUri, PhoneNumber>

    private Bitmap logo , sign ;

    public enum ShareMode { WHATSAPP, EMAIL }

    public static class ReceiptResult {
        public final Uri uri;
        public final String targetContact; // Phone for WA, Email for Email
        public final ShareMode mode;

        public ReceiptResult(Uri uri, String targetContact, ShareMode mode) {
            this.uri = uri;
            this.targetContact = targetContact;
            this.mode = mode;
        }
    }

    public DonationViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((AttendanceApplication) application).repository;
    }

    public LiveData<List<Devotee>> getSearchResults() { return searchResults; }
    public LiveData<AttendanceRepository.ActiveBatchData> getActiveBatchData() { return activeBatchData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getBatchClosedEvent() { return batchClosedEvent; }
    // Change Pair<Uri, String> to ReceiptResult
    private final MutableLiveData<ReceiptResult> receiptResult = new MutableLiveData<>();
    public LiveData<ReceiptResult> getReceiptResult() { return receiptResult; }

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

    public void loadAssets() {
        new Thread(() -> {
            try {
                AssetManager assetManager = getApplication().getAssets();
                


                // 2. Open the file and read bytes
                InputStream is = assetManager.open("logo.jpeg");
                logo = new byte[is.available()];
                is.read(logo);
                is.close();

                InputStream isSign = assetManager.open("sign.png");
                sign = new byte[isSign.available()];
                isSign.read(sign);
                isSign.close();
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to load active batch data", e);
                errorMessage.postValue("Failed to load active batch.");
            }
        }).start();
    }

    public void startNewBatch() {
        batchClosedEvent.setValue(false); // Reset the "closed" state
        loadOrRefreshActiveBatch();
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
                activeBatchData.postValue(null); // Clear the active data
                batchClosedEvent.postValue(true); // Fire the "closed" event
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

    // --- NEW: Receipt Generation Logic ---
    public void generateAndShareReceipt(android.content.Context context, long donationId, ShareMode mode) {
        new Thread(() -> {
            try {
                FullDonationRecord record = repository.getRecordForReceipt(donationId);
                if (record == null) {
                    errorMessage.postValue("Donation record not found.");
                    return;
                }

                // --- VALIDATION FOR EMAIL ---
                String email = record.devotee.getEmail() != null ? record.devotee.getEmail().trim() : "";
                if (mode == ShareMode.EMAIL) {
                    if (email.isEmpty()) {
                        errorMessage.postValue("Email ID not available/valid for this donor.");
                        return;
                    }
                }
                // ----------------------------

                PdfReceiptGenerator generator = new PdfReceiptGenerator(logo,sign);

                // Prepare Data
                String receiptNo = record.donation.receiptNumber != null ? record.donation.receiptNumber : "TMP-" + donationId;
                String date = record.donation.donationTimestamp.split(" ")[0];
                String donorName = record.devotee.getFullName();
                String mobile = record.devotee.getMobileE164();

                String uin = "";
                String idType = "";
                if (record.devotee.getPan() != null && !record.devotee.getPan().isEmpty()) {
                    uin = record.devotee.getPan();
                    idType = "PAN";
                } else if (record.devotee.getAadhaar() != null && !record.devotee.getAadhaar().isEmpty()) {
                    uin = record.devotee.getAadhaar();
                    idType = "Aadhaar";
                }

                String amountFigs = String.format(Locale.US, "%.2f", record.donation.amount);
                String amountWords = NumberToWords.convert(record.donation.amount);
                String purpose = record.donation.purpose;
                String payMode = record.donation.paymentMethod;
                String details = record.donation.referenceId != null ? "Ref: " + record.donation.referenceId : "";

                byte[] pdfBytes = generator.generatePdfReceipt(
                        receiptNo, date, donorName, mobile, email, uin, idType,
                        amountFigs, amountWords, purpose, payMode, details
                );

                File cachePath = new File(context.getCacheDir(), "receipts");
                cachePath.mkdirs();
                File file = new File(cachePath, receiptNo + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }

                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        context, context.getPackageName() + ".fileprovider", file);

                // Decide which contact info to pass back based on mode
                String target = (mode == ShareMode.WHATSAPP) ? mobile : email;

                receiptResult.postValue(new ReceiptResult(uri, target, mode));

            } catch (Exception e) {
                AppLogger.e(TAG, "Receipt generation failed", e);
                errorMessage.postValue("Failed to generate receipt: " + e.getMessage());
            }
        }).start();
    }

    public void onReceiptHandled() { receiptResult.setValue(null); }
}