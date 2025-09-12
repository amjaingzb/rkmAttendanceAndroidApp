// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/EventStatus.java
package com.rkm.rkmattendanceapp.ui;

/**
 * Represents the status of a devotee in relation to a specific event.
 */
public enum EventStatus {
    /**
     * Devotee has already been marked as present (cnt > 0).
     */
    PRESENT,

    /**
     * Devotee is in the attendance list with reg_type='PRE_REG' and cnt=0.
     */
    PRE_REGISTERED,

    /**
     * Devotee exists in the master list but has no attendance record for this event.
     */
    WALK_IN
}