package com.rkm.attendance.model;

public class Event {
    private Long eventId;
    private String eventCode;
    private String eventName;   // mandatory
    private String eventDate;   // yyyy-MM-dd
    private String remark;

    public Event() {}
    public Event(Long id, String code, String name, String date, String remark) {
        this.eventId = id; this.eventCode = code; this.eventName = name; this.eventDate = date; this.remark = remark;
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    public String toString() {
        return String.format("%s (%s)", eventName, eventDate == null ? "No Date" : eventDate);
    }
}

