// In: com/rkm/attendance/model/Devotee.java
package com.rkm.attendance.model;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class Devotee {
    private Long devoteeId;
    private String fullName;
    private String nameNorm;
    private String mobileE164;
    private String address;
    private Integer age;
    private String extraJson;
    private String email;
    private String gender;

    public Devotee() {}

    public Devotee(Long devoteeId, String fullName, String nameNorm, String mobileE164,
                   String address, Integer age, String email, String gender, String extraJson) {
        this.devoteeId = devoteeId;
        this.fullName = fullName;
        this.nameNorm = nameNorm;
        this.mobileE164 = mobileE164;
        this.address = address;
        this.age = age;
        this.email = email;
        this.gender = gender;
        this.extraJson = extraJson;
    }

    /**
     * Merges non-null and non-blank fields from the 'other' devotee into this one.
     * This is used to enrich an existing record with new data from an import or form.
     * @param other The devotee object containing the new data.
     */
    public void mergeWith(Devotee other) {
        if (other == null) return;

        if (isNotBlank(other.fullName) && (this.fullName == null || other.fullName.length() > this.fullName.length())) {
            this.fullName = other.fullName;
        }
        if (isNotBlank(other.mobileE164)) {
            this.mobileE164 = other.mobileE164;
        }
        if (isNotBlank(other.address)) {
            this.address = other.address;
        }
        if (isNotBlank(other.email)) {
            this.email = other.email;
        }
        if (isNotBlank(other.gender)) {
            this.gender = other.gender;
        }
        if (other.age != null && other.age > 0) {
            this.age = other.age;
        }
        
        if (isNotBlank(other.extraJson)) {
            if (!isNotBlank(this.extraJson)) {
                this.extraJson = other.extraJson;
            } else {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    // --- START OF FIX ---
                    // Replaced <> with the explicit type for Java 8 compatibility.
                    TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                    // --- END OF FIX ---
                    Map<String, Object> existingMap = mapper.readValue(this.extraJson, typeRef);
                    Map<String, Object> newMap = mapper.readValue(other.extraJson, typeRef);

                    existingMap.putAll(newMap);
                    this.extraJson = mapper.writeValueAsString(existingMap);

                } catch (IOException e) {
                    Log.e("DevoteeMerge", "Failed to merge extraJson fields", e);
                }
            }
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }


    public Long getDevoteeId() { return devoteeId; }
    public void setDevoteeId(Long devoteeId) { this.devoteeId = devoteeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNameNorm() { return nameNorm; }
    public void setNameNorm(String nameNorm) { this.nameNorm = nameNorm; }

    public String getMobileE164() { return mobileE164; }
    public void setMobileE164(String mobileE164) { this.mobileE164 = mobileE164; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @Override public String toString() {
        return "Devotee{" +
                "id=" + devoteeId +
                ", name='" + fullName + '\'' +
                ", mobile='" + mobileE164 + '\'' +
                ", address='" + address + '\'' +
                ", age=" + age +
                '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Devotee devotee = (Devotee) o;
        return Objects.equals(mobileE164, devotee.mobileE164) && Objects.equals(nameNorm, devotee.nameNorm);
    }

    @Override public int hashCode() { return Objects.hash(mobileE164, nameNorm); }
}
