// =============================
// com/rkm/attendance/model/Devotee.java
// =============================
package com.rkm.attendance.model;

import java.util.Objects;

public class Devotee {
    private Long devoteeId; // nullable for new rows
    private String fullName; // required
    private String nameNorm; // lowercase + single-spaced
    private String mobileE164; // required, but we trust it as exact key
    private String address; // nullable
    private Integer age; // nullable
    private String extraJson; // nullable JSON
    private String email;   // NEW
    private String gender;  // NEW

    public Devotee() {}

    public Devotee(Long devoteeId, String fullName, String nameNorm, String mobileE164,
                   String address, Integer age, String email, String gender, String extraJson) { // NEW params
        this.devoteeId = devoteeId;
        this.fullName = fullName;
        this.nameNorm = nameNorm;
        this.mobileE164 = mobileE164;
        this.address = address;
        this.age = age;
        this.email = email;       // NEW
        this.gender = gender;     // NEW
        this.extraJson = extraJson;
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

