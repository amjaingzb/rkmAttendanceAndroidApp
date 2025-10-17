// In: app/src/main/java/com/rkm/attendance/model/ConfigItem.java
package com.rkm.attendance.model;

public class ConfigItem {
    public final String key;
    public final String displayName;
    public String value;
    public final boolean isProtected; // To indicate if it should be masked (like a PIN)

    public ConfigItem(String key, String displayName, String value, boolean isProtected) {
        this.key = key;
        this.displayName = displayName;
        this.value = value;
        this.isProtected = isProtected;
    }
}
