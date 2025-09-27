// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/reports/models/DonationReportModels.java
package com.rkm.rkmattendanceapp.ui.reports.models;

import com.rkm.attendance.model.Devotee;
import com.rkm.attendance.model.Donation;

public class DonationReportModels {

    /**
     * Data class to hold the aggregated summary for a single day's donations.
     */
    public static class DailySummary {
        public final String date; // YYYY-MM-DD format
        public final double totalAmount;
        public final int donationCount;
        public final int batchCount;

        public DailySummary(String date, double totalAmount, int donationCount, int batchCount) {
            this.date = date;
            this.totalAmount = totalAmount;
            this.donationCount = donationCount;
            this.batchCount = batchCount;
        }
    }

    /**
     * Data class that joins a donation with full devotee details for exporting.
     */
    public static class FullDonationRecord {
        public final Donation donation;
        public final Devotee devotee;

        public FullDonationRecord(Donation donation, Devotee devotee) {
            this.donation = donation;
            this.devotee = devotee;
        }
    }
}
