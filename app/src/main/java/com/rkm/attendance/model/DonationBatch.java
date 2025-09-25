// In: app/src/main/java/com/rkm/attendance/model/DonationBatch.java
package com.rkm.attendance.model;

public class DonationBatch {
    public final long batchId;
    public final String startTs;
    public final String endTs;
    public final String status;
    public final String depositedBy;

    public DonationBatch(long batchId, String startTs, String endTs, String status, String depositedBy) {
        this.batchId = batchId;
        this.startTs = startTs;
        this.endTs = endTs;
        this.status = status;
        this.depositedBy = depositedBy;
    }
}
