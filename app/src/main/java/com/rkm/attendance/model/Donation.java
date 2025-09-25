// In: app/src/main/java/com/rkm/attendance/model/Donation.java
package com.rkm.attendance.model;
public class Donation {
public final Long donationId;
public final long devoteeId;
public final Long eventId;
public final double amount;
public final String paymentMethod;
public final String referenceId;
public final String purpose;
public final String donationTimestamp;
public final String createdByUser;
public final long batchId;
public final String receiptNumber;
public Donation(Long donationId, long devoteeId, Long eventId, double amount, String paymentMethod, String referenceId, String purpose, String donationTimestamp, String createdByUser, long batchId, String receiptNumber) {
    this.donationId = donationId;
    this.devoteeId = devoteeId;
    this.eventId = eventId;
    this.amount = amount;
    this.paymentMethod = paymentMethod;
    this.referenceId = referenceId;
    this.purpose = purpose;
    this.donationTimestamp = donationTimestamp;
    this.createdByUser = createdByUser;
    this.batchId = batchId;
    this.receiptNumber = receiptNumber;
}

}
