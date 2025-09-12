// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/Privilege.java
package com.rkm.rkmattendanceapp.ui;

import java.io.Serializable;

/**
 * Represents the different user privilege levels in the application.
 * Implements Serializable so it can be passed in an Intent extra.
 */
public enum Privilege implements Serializable {
    OPERATOR,
    EVENT_COORDINATOR,
    SUPER_ADMIN
}