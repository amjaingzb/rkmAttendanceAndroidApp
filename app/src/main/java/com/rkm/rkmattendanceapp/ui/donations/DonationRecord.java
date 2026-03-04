package com.rkm.rkmattendanceapp.ui.donations;
import com.rkm.attendance.model.Donation;

public class DonationRecord {
    public final Donation donation;
    public final String devoteeName;
    public final String mobile;
    public final String address;
    public final String email;
    public final String pan;
    public final String aadhaar;

    public DonationRecord(Donation donation, String devoteeName, String mobile, String address, String email, String pan, String aadhaar) {
        this.donation = donation;
        this.devoteeName = devoteeName;
        this.mobile = mobile;
        this.address = address;
        this.email = email;
        this.pan = pan;
        this.aadhaar = aadhaar;
    }
}
