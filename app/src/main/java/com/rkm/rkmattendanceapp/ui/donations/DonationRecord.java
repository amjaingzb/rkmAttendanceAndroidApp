// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/donations/DonationRecord.java
package com.rkm.rkmattendanceapp.ui.donations;

import com.rkm.attendance.model.Donation;

// A wrapper class that combines Donation data with devotee's name for easy display in the UI.
public class DonationRecord {
    public final Donation donation;
    public final String devoteeName;

    public DonationRecord(Donation donation, String devoteeName) {
        this.donation = donation;
        this.devoteeName = devoteeName;
    }
}
